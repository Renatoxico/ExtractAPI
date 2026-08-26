package io.github.renatoxico.extract.repo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class ClassificationWorkRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public ClassificationWorkRepository(
        JdbcTemplate jdbc,
        ObjectMapper objectMapper
    ) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public List<ClassificationCandidate> lockClassificationsNeedingBatch(int limit) {
        return jdbc.query("""
            SELECT classification.id, classification.expense_name
            FROM expense_classification classification
            WHERE NULLIF(classification.category, '') IS NULL
              AND NOT EXISTS (
                  SELECT 1
                  FROM expense_classification_task task
                  WHERE task.classification_id = classification.id
                    AND task.status NOT IN ('APPLIED', 'FAILED')
              )
            ORDER BY classification.id
            LIMIT ?
            FOR UPDATE OF classification SKIP LOCKED
            """, (resultSet, rowNumber) -> new ClassificationCandidate(
                resultSet.getLong("id"),
                resultSet.getString("expense_name")
            ), limit);
    }

    public long createPendingBatch(List<ClassificationCandidate> candidates) {
        Long batchId = jdbc.queryForObject("""
            INSERT INTO ai_classification_batch (status, input_payload)
            VALUES ('PENDING', '{"schemaVersion":1,"items":[]}'::jsonb)
            RETURNING id
            """, Long.class);
        if (batchId == null) {
            throw new IllegalStateException("Database did not return a classification batch id");
        }

        List<TaskItem> items = candidates.stream()
            .map(candidate -> createTask(batchId, candidate))
            .toList();
        jdbc.update("""
            UPDATE ai_classification_batch
            SET input_payload = CAST(? AS jsonb), updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, toInputPayload(items).toString(), batchId);
        return batchId;
    }

    private TaskItem createTask(long batchId, ClassificationCandidate candidate) {
        Long taskId = jdbc.queryForObject("""
            INSERT INTO expense_classification_task (batch_id, classification_id, status)
            VALUES (?, ?, 'PENDING_AI')
            RETURNING id
            """, Long.class, batchId, candidate.classificationId());
        if (taskId == null) {
            throw new IllegalStateException("Database did not return a classification task id");
        }
        return new TaskItem(taskId, candidate.expenseName());
    }

    public Optional<BatchHeader> lockNextEligibleBatch() {
        try {
            return Optional.ofNullable(jdbc.queryForObject("""
                SELECT id, attempts
                FROM ai_classification_batch
                WHERE status = 'PENDING'
                   OR (status = 'RETRY' AND next_attempt_at <= CURRENT_TIMESTAMP)
                ORDER BY id
                LIMIT 1
                FOR UPDATE SKIP LOCKED
                """, (resultSet, rowNumber) -> new BatchHeader(
                    resultSet.getLong("id"),
                    resultSet.getInt("attempts")
                )));
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    public void markBatchProcessing(long batchId, int attempt, Instant leaseExpiresAt) {
        jdbc.update("""
            UPDATE ai_classification_batch
            SET status = 'PROCESSING',
                attempts = ?,
                started_at = COALESCE(started_at, CURRENT_TIMESTAMP),
                updated_at = CURRENT_TIMESTAMP,
                next_attempt_at = NULL,
                lease_expires_at = ?,
                last_error = NULL
            WHERE id = ?
            """, attempt, Timestamp.from(leaseExpiresAt), batchId);
    }

    public int closeWaitingTasksWithKnownCategory(long batchId) {
        return jdbc.update("""
            UPDATE expense_classification_task task
            SET status = 'APPLIED',
                last_error = NULL,
                next_attempt_at = NULL,
                lease_expires_at = NULL,
                updated_at = CURRENT_TIMESTAMP,
                applied_at = CURRENT_TIMESTAMP,
                finished_at = CURRENT_TIMESTAMP
            FROM expense_classification classification
            WHERE task.batch_id = ?
              AND task.status = 'PENDING_AI'
              AND classification.id = task.classification_id
              AND NULLIF(classification.category, '') IS NOT NULL
            """, batchId);
    }

    public List<TaskItem> findWaitingBatchItems(long batchId) {
        return jdbc.query("""
            SELECT task.id, classification.expense_name
            FROM expense_classification_task task
            JOIN expense_classification classification
              ON classification.id = task.classification_id
            WHERE task.batch_id = ?
              AND task.status = 'PENDING_AI'
              AND NULLIF(classification.category, '') IS NULL
            ORDER BY task.id
            """, (resultSet, rowNumber) -> new TaskItem(
                resultSet.getLong("id"),
                resultSet.getString("expense_name")
            ), batchId);
    }

    public boolean lockProcessingBatch(long batchId, int expectedAttempt) {
        try {
            Long id = jdbc.queryForObject("""
                SELECT id
                FROM ai_classification_batch
                WHERE id = ? AND status = 'PROCESSING' AND attempts = ?
                FOR UPDATE
                """, Long.class, batchId, expectedAttempt);
            return id != null;
        } catch (EmptyResultDataAccessException ignored) {
            return false;
        }
    }

    public void markBatchSucceeded(long batchId, JsonNode outputPayload) {
        jdbc.update("""
            UPDATE ai_classification_batch
            SET status = 'SUCCEEDED',
                output_payload = CAST(? AS jsonb),
                last_error = NULL,
                next_attempt_at = NULL,
                lease_expires_at = NULL,
                updated_at = CURRENT_TIMESTAMP,
                finished_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, outputPayload.toString(), batchId);
    }

    public void markBatchRetry(
        long batchId,
        JsonNode outputPayload,
        String error,
        Instant nextAttemptAt
    ) {
        jdbc.update("""
            UPDATE ai_classification_batch
            SET status = 'RETRY',
                output_payload = COALESCE(CAST(? AS jsonb), output_payload),
                last_error = ?,
                next_attempt_at = ?,
                lease_expires_at = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, jsonValue(outputPayload), error, Timestamp.from(nextAttemptAt), batchId);
    }

    public void markBatchFailed(long batchId, JsonNode outputPayload, String error) {
        jdbc.update("""
            UPDATE ai_classification_batch
            SET status = 'FAILED',
                output_payload = COALESCE(CAST(? AS jsonb), output_payload),
                last_error = ?,
                next_attempt_at = NULL,
                lease_expires_at = NULL,
                updated_at = CURRENT_TIMESTAMP,
                finished_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, jsonValue(outputPayload), error, batchId);
    }

    public void markTaskReady(long taskId, long batchId, String category) {
        jdbc.update("""
            UPDATE expense_classification_task
            SET status = 'READY_TO_APPLY',
                suggested_category = ?,
                last_error = NULL,
                next_attempt_at = NULL,
                lease_expires_at = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND batch_id = ? AND status = 'PENDING_AI'
            """, category, taskId, batchId);
    }

    public void markTaskFailed(long taskId, String error) {
        jdbc.update("""
            UPDATE expense_classification_task
            SET status = 'FAILED',
                last_error = ?,
                next_attempt_at = NULL,
                lease_expires_at = NULL,
                updated_at = CURRENT_TIMESTAMP,
                finished_at = CURRENT_TIMESTAMP
            WHERE id = ? AND status IN ('PENDING_AI', 'APPLYING')
            """, error, taskId);
    }

    public List<ExpiredClaim> findExpiredBatchClaims() {
        return jdbc.query("""
            SELECT id, attempts
            FROM ai_classification_batch
            WHERE status = 'PROCESSING'
              AND lease_expires_at < CURRENT_TIMESTAMP
            ORDER BY id
            """, (resultSet, rowNumber) -> new ExpiredClaim(
                resultSet.getLong("id"),
                resultSet.getInt("attempts")
            ));
    }

    public Optional<ApplyClaim> lockNextApplyTask(int maxAttempts, Instant leaseExpiresAt) {
        try {
            ApplyClaim claim = jdbc.queryForObject("""
                SELECT task.id, task.classification_id, task.suggested_category, task.apply_attempts
                FROM expense_classification_task task
                WHERE task.suggested_category IS NOT NULL
                  AND task.apply_attempts < ?
                  AND (
                        task.status = 'READY_TO_APPLY'
                        OR (task.status = 'RETRY' AND task.next_attempt_at <= CURRENT_TIMESTAMP)
                  )
                ORDER BY task.id
                LIMIT 1
                FOR UPDATE SKIP LOCKED
                """, (resultSet, rowNumber) -> new ApplyClaim(
                    resultSet.getLong("id"),
                    resultSet.getLong("classification_id"),
                    resultSet.getString("suggested_category"),
                    resultSet.getInt("apply_attempts") + 1
                ), maxAttempts);
            if (claim == null) {
                return Optional.empty();
            }

            jdbc.update("""
                UPDATE expense_classification_task
                SET status = 'APPLYING',
                    apply_attempts = ?,
                    started_at = COALESCE(started_at, CURRENT_TIMESTAMP),
                    next_attempt_at = NULL,
                    lease_expires_at = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, claim.attempt(), Timestamp.from(leaseExpiresAt), claim.taskId());
            return Optional.of(claim);
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    public boolean lockApplyingTask(long taskId, int expectedAttempt) {
        try {
            Long id = jdbc.queryForObject("""
                SELECT id
                FROM expense_classification_task
                WHERE id = ? AND status = 'APPLYING' AND apply_attempts = ?
                FOR UPDATE
                """, Long.class, taskId, expectedAttempt);
            return id != null;
        } catch (EmptyResultDataAccessException ignored) {
            return false;
        }
    }

    public void applyCatalogSuggestion(ApplyClaim claim) {
        jdbc.update("""
            UPDATE expense_classification
            SET category = ?, source = 'AI', updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND NULLIF(category, '') IS NULL
            """, claim.suggestedCategory(), claim.classificationId());

        jdbc.update("""
            UPDATE expense_classification_task
            SET status = 'APPLIED',
                last_error = NULL,
                next_attempt_at = NULL,
                lease_expires_at = NULL,
                updated_at = CURRENT_TIMESTAMP,
                applied_at = CURRENT_TIMESTAMP,
                finished_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, claim.taskId());
    }

    public void markTaskApplyRetry(long taskId, String error, Instant nextAttemptAt) {
        jdbc.update("""
            UPDATE expense_classification_task
            SET status = 'RETRY',
                last_error = ?,
                next_attempt_at = ?,
                lease_expires_at = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND status = 'APPLYING'
            """, error, Timestamp.from(nextAttemptAt), taskId);
    }

    public List<ExpiredClaim> findExpiredApplyClaims() {
        return jdbc.query("""
            SELECT id, apply_attempts AS attempts
            FROM expense_classification_task
            WHERE status = 'APPLYING'
              AND lease_expires_at < CURRENT_TIMESTAMP
            ORDER BY id
            """, (resultSet, rowNumber) -> new ExpiredClaim(
                resultSet.getLong("id"),
                resultSet.getInt("attempts")
            ));
    }

    public List<Long> findExpenseIdsReadyForPropagation(int limit) {
        return jdbc.queryForList("""
            SELECT expense.id
            FROM expense expense
            JOIN expense_classification classification
              ON classification.expense_name = expense.expense_name
            WHERE expense.category IS NULL
              AND NULLIF(classification.category, '') IS NOT NULL
            ORDER BY expense.id
            LIMIT ?
            """, Long.class, limit);
    }

    public int propagateCategoryToExpense(long expenseId) {
        return jdbc.update("""
            UPDATE expense target
            SET category = classification.category
            FROM expense_classification classification
            WHERE target.id = ?
              AND target.category IS NULL
              AND classification.expense_name = target.expense_name
              AND NULLIF(classification.category, '') IS NOT NULL
            """, expenseId);
    }

    private ObjectNode toInputPayload(List<TaskItem> items) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("schemaVersion", 1);
        ArrayNode payloadItems = payload.putArray("items");
        for (TaskItem item : items) {
            ObjectNode payloadItem = payloadItems.addObject();
            payloadItem.put("taskId", item.taskId());
            payloadItem.put("expenseName", item.expenseName());
        }
        return payload;
    }

    private String jsonValue(JsonNode value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not serialize classification payload", exception);
        }
    }

    public record ClassificationCandidate(long classificationId, String expenseName) {
    }

    public record TaskItem(long taskId, String expenseName) {
    }

    public record BatchHeader(long batchId, int attempts) {
    }

    public record ClaimedBatch(long batchId, int attempt, List<TaskItem> items) {
    }

    public record ApplyClaim(
        long taskId,
        long classificationId,
        String suggestedCategory,
        int attempt
    ) {
    }

    public record ExpiredClaim(long id, int attempt) {
    }
}
