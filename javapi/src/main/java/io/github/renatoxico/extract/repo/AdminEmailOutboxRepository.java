package io.github.renatoxico.extract.repo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AdminEmailOutboxRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public AdminEmailOutboxRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public Optional<Long> enqueue(
        String type,
        String deduplicationKey,
        JsonNode payload,
        Long originalNotificationId
    ) {
        List<Long> ids = jdbc.queryForList("""
            INSERT INTO admin_email_outbox (
                notification_type, deduplication_key, payload, original_notification_id
            )
            VALUES (?, ?, CAST(? AS jsonb), ?)
            ON CONFLICT (deduplication_key) DO NOTHING
            RETURNING id
            """, Long.class, type, deduplicationKey, payload.toString(), originalNotificationId);
        return ids.stream().findFirst();
    }

    public Optional<DeliveryClaim> claimNext(Instant leaseExpiresAt) {
        try {
            DeliveryClaim claim = jdbc.queryForObject("""
                SELECT id, notification_type, payload, attempts
                FROM admin_email_outbox
                WHERE status = 'PENDING'
                   OR (status = 'RETRY' AND next_attempt_at <= CURRENT_TIMESTAMP)
                ORDER BY id
                LIMIT 1
                FOR UPDATE SKIP LOCKED
                """, (resultSet, rowNumber) -> new DeliveryClaim(
                    resultSet.getLong("id"),
                    resultSet.getString("notification_type"),
                    readJson(resultSet.getString("payload")),
                    resultSet.getInt("attempts") + 1
                ));
            if (claim == null) {
                return Optional.empty();
            }
            jdbc.update("""
                UPDATE admin_email_outbox
                SET status = 'SENDING',
                    attempts = ?,
                    next_attempt_at = NULL,
                    lease_expires_at = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, claim.attempt(), Timestamp.from(leaseExpiresAt), claim.id());
            return Optional.of(claim);
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    public boolean lockSending(long id, int expectedAttempt) {
        try {
            Long locked = jdbc.queryForObject("""
                SELECT id
                FROM admin_email_outbox
                WHERE id = ? AND status = 'SENDING' AND attempts = ?
                FOR UPDATE
                """, Long.class, id, expectedAttempt);
            return locked != null;
        } catch (EmptyResultDataAccessException ignored) {
            return false;
        }
    }

    public void markSent(long id) {
        jdbc.update("""
            UPDATE admin_email_outbox
            SET status = 'SENT',
                next_attempt_at = NULL,
                lease_expires_at = NULL,
                last_error = NULL,
                sent_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, id);
    }

    public void markRetry(long id, String error, Instant nextAttemptAt) {
        jdbc.update("""
            UPDATE admin_email_outbox
            SET status = 'RETRY',
                last_error = ?,
                next_attempt_at = ?,
                lease_expires_at = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, error, Timestamp.from(nextAttemptAt), id);
    }

    public void markFailed(long id, String error) {
        jdbc.update("""
            UPDATE admin_email_outbox
            SET status = 'FAILED',
                last_error = ?,
                next_attempt_at = NULL,
                lease_expires_at = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, error, id);
    }

    public int recoverExpiredClaims() {
        return jdbc.update("""
            UPDATE admin_email_outbox
            SET status = 'RETRY',
                last_error = 'Email delivery worker lease expired',
                next_attempt_at = CURRENT_TIMESTAMP,
                lease_expires_at = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE status = 'SENDING'
              AND lease_expires_at < CURRENT_TIMESTAMP
            """);
    }

    public Optional<NotificationState> lockNotification(long id) {
        try {
            return Optional.ofNullable(jdbc.queryForObject("""
                SELECT id, notification_type, deduplication_key, payload, status
                FROM admin_email_outbox
                WHERE id = ?
                FOR UPDATE
                """, (resultSet, rowNumber) -> new NotificationState(
                    resultSet.getLong("id"),
                    resultSet.getString("notification_type"),
                    resultSet.getString("deduplication_key"),
                    readJson(resultSet.getString("payload")),
                    resultSet.getString("status")
                ), id));
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    public void makeImmediatelyEligible(long id, boolean resetAttempts) {
        jdbc.update("""
            UPDATE admin_email_outbox
            SET status = 'PENDING',
                attempts = CASE WHEN ? THEN 0 ELSE attempts END,
                next_attempt_at = NULL,
                lease_expires_at = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, resetAttempts, id);
    }

    public long createResend(NotificationState original) {
        return enqueue(
            original.type(),
            "MANUAL_RESEND:" + original.id() + ":" + UUID.randomUUID(),
            original.payload(),
            original.id()
        ).orElseThrow(() -> new IllegalStateException("Could not create email resend"));
    }

    public List<FailureItem> findUnresolvedFailures() {
        return jdbc.query("""
            SELECT classification.id AS classification_id,
                   classification.expense_name,
                   latest.id AS task_id,
                   latest.batch_id,
                   CASE WHEN latest.suggested_category IS NULL THEN 'AI' ELSE 'APPLY' END AS stage,
                   CASE WHEN latest.suggested_category IS NULL
                        THEN batch.attempts ELSE latest.apply_attempts END AS attempts,
                   latest.suggested_category,
                   latest.last_error,
                   latest.finished_at
            FROM expense_classification classification
            JOIN LATERAL (
                SELECT task.*
                FROM expense_classification_task task
                WHERE task.classification_id = classification.id
                ORDER BY task.created_at DESC, task.id DESC
                LIMIT 1
            ) latest ON true
            JOIN ai_classification_batch batch ON batch.id = latest.batch_id
            WHERE NULLIF(classification.category, '') IS NULL
              AND latest.status = 'FAILED'
            ORDER BY classification.id
            """, (resultSet, rowNumber) -> new FailureItem(
                resultSet.getLong("classification_id"),
                resultSet.getLong("task_id"),
                resultSet.getLong("batch_id"),
                resultSet.getString("expense_name"),
                resultSet.getString("stage"),
                resultSet.getInt("attempts"),
                resultSet.getString("suggested_category"),
                resultSet.getString("last_error"),
                resultSet.getTimestamp("finished_at").toInstant()
            ));
    }

    public WeeklyMetrics findWeeklyMetrics(Instant start, Instant end) {
        Timestamp from = Timestamp.from(start);
        Timestamp to = Timestamp.from(end);
        int reports = count("""
            SELECT COUNT(*) FROM expense_report
            WHERE created_at >= ? AND created_at < ?
            """, from, to);
        int users = count("""
            SELECT COUNT(DISTINCT app_user_id) FROM expense_report
            WHERE created_at >= ? AND created_at < ?
            """, from, to);
        int catalogEntries = count("""
            SELECT COUNT(*) FROM expense_classification
            WHERE created_at >= ? AND created_at < ?
            """, from, to);
        int aiFailures = count("""
            SELECT COUNT(*) FROM expense_classification_task
            WHERE status = 'FAILED' AND suggested_category IS NULL
              AND finished_at >= ? AND finished_at < ?
            """, from, to);
        int applyFailures = count("""
            SELECT COUNT(*) FROM expense_classification_task
            WHERE status = 'FAILED' AND suggested_category IS NOT NULL
              AND finished_at >= ? AND finished_at < ?
            """, from, to);
        int aiCategories = count("""
            SELECT COUNT(*) FROM expense_classification
            WHERE source = 'AI' AND updated_at >= ? AND updated_at < ?
            """, from, to);
        List<UserReportCount> perUser = jdbc.query("""
            SELECT report.app_user_id,
                   app.email,
                   app.display_name,
                   COUNT(*) AS report_count
            FROM expense_report report
            JOIN app_user app ON app.id = report.app_user_id
            WHERE report.created_at >= ? AND report.created_at < ?
            GROUP BY report.app_user_id, app.email, app.display_name
            ORDER BY report_count DESC, report.app_user_id
            """, (resultSet, rowNumber) -> new UserReportCount(
                resultSet.getLong("app_user_id"),
                resultSet.getString("email"),
                resultSet.getString("display_name"),
                resultSet.getInt("report_count")
            ), from, to);
        return new WeeklyMetrics(
            reports,
            users,
            catalogEntries,
            aiFailures,
            applyFailures,
            aiCategories,
            findUnresolvedFailures().size(),
            perUser
        );
    }

    private int count(String sql, Object... args) {
        Integer value = jdbc.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read admin email payload", exception);
        }
    }

    public record DeliveryClaim(long id, String type, JsonNode payload, int attempt) {
    }

    public record NotificationState(
        long id,
        String type,
        String deduplicationKey,
        JsonNode payload,
        String status
    ) {
    }

    public record FailureItem(
        long classificationId,
        long taskId,
        long batchId,
        String expenseName,
        String stage,
        int attempts,
        String suggestedCategory,
        String error,
        Instant finishedAt
    ) {
    }

    public record UserReportCount(long userId, String email, String displayName, int reports) {
    }

    public record WeeklyMetrics(
        int reports,
        int uniqueUsers,
        int newCatalogEntries,
        int aiFailures,
        int applyFailures,
        int aiCategories,
        int unresolvedFailures,
        List<UserReportCount> perUser
    ) {
    }
}
