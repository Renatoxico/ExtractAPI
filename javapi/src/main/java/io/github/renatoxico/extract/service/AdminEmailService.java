package io.github.renatoxico.extract.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.renatoxico.extract.config.AdminEmailProperties;
import io.github.renatoxico.extract.repo.AdminEmailOutboxRepository;
import io.github.renatoxico.extract.repo.AdminEmailOutboxRepository.DeliveryClaim;
import io.github.renatoxico.extract.repo.AdminEmailOutboxRepository.FailureItem;
import io.github.renatoxico.extract.repo.AdminEmailOutboxRepository.NotificationState;
import io.github.renatoxico.extract.repo.AdminEmailOutboxRepository.UserReportCount;
import io.github.renatoxico.extract.repo.AdminEmailOutboxRepository.WeeklyMetrics;
import io.github.renatoxico.extract.repo.ClassificationWorkRepository.ApplyClaim;
import io.github.renatoxico.extract.repo.ClassificationWorkRepository.ClaimedBatch;
import io.github.renatoxico.extract.repo.ClassificationWorkRepository.TaskItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class AdminEmailService {
    private static final Logger LOG = LoggerFactory.getLogger(AdminEmailService.class);
    private static final ZoneId REPORT_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Duration DELIVERY_LEASE = Duration.ofMinutes(5);
    private static final int MAX_DELIVERY_ATTEMPTS = 6;
    private static final int MAX_DELIVERIES_PER_RUN = 50;
    private static final int MAX_ERROR_LENGTH = 1_000;
    private static final List<Duration> RETRY_DELAYS = List.of(
        Duration.ofMinutes(1),
        Duration.ofMinutes(5),
        Duration.ofMinutes(15),
        Duration.ofHours(1),
        Duration.ofHours(6)
    );

    private final AdminEmailOutboxRepository repository;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactions;
    private final String sender;
    private final List<String> recipients;
    private final Clock clock;

    @Autowired
    public AdminEmailService(
        AdminEmailOutboxRepository repository,
        JavaMailSender mailSender,
        ObjectMapper objectMapper,
        PlatformTransactionManager transactionManager,
        AdminEmailProperties properties
    ) {
        this(
            repository,
            mailSender,
            objectMapper,
            new TransactionTemplate(transactionManager),
            properties.getSender(),
            properties.getRecipients(),
            Clock.systemUTC()
        );
    }

    AdminEmailService(
        AdminEmailOutboxRepository repository,
        JavaMailSender mailSender,
        ObjectMapper objectMapper,
        TransactionTemplate transactions,
        String sender,
        List<String> recipients,
        Clock clock
    ) {
        this.repository = repository;
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
        this.transactions = transactions;
        this.sender = sender;
        this.recipients = List.copyOf(recipients);
        this.clock = clock;
    }

    public void enqueueAiFailure(ClaimedBatch claim, String error) {
        Instant finishedAt = Instant.now(clock);
        ObjectNode payload = basePayload(
            "[ExtractAPI] AI classification batch failed",
            "AI classification batch " + claim.batchId() + " exhausted its retries."
        );
        ArrayNode items = payload.putArray("items");
        for (TaskItem item : claim.items()) {
            ObjectNode failure = items.addObject();
            failure.put("classificationId", item.classificationId());
            failure.put("taskId", item.taskId());
            failure.put("batchId", claim.batchId());
            failure.put("expenseName", item.expenseName());
            failure.put("stage", "AI");
            failure.put("attempts", claim.attempt());
            failure.put("finishedAt", finishedAt.toString());
            failure.put("error", error);
        }
        repository.enqueue(
            "AI_BATCH_FAILED",
            "AI_BATCH_FAILED:" + claim.batchId(),
            payload,
            null
        );
    }

    public void enqueueApplyFailure(ApplyClaim claim, String error) {
        Instant finishedAt = Instant.now(clock);
        ObjectNode payload = basePayload(
            "[ExtractAPI] Classification application failed",
            "A category suggestion could not be applied after all retries."
        );
        ArrayNode items = payload.putArray("items");
        ObjectNode failure = items.addObject();
        failure.put("classificationId", claim.classificationId());
        failure.put("taskId", claim.taskId());
        failure.put("batchId", claim.batchId());
        failure.put("expenseName", claim.expenseName());
        failure.put("stage", "APPLY");
        failure.put("attempts", claim.attempt());
        failure.put("finishedAt", finishedAt.toString());
        failure.put("suggestedCategory", claim.suggestedCategory());
        failure.put("error", error);
        repository.enqueue(
            "APPLY_TASK_FAILED",
            "APPLY_TASK_FAILED:" + claim.taskId(),
            payload,
            null
        );
    }

    public void enqueueDailyFailureReport() {
        LocalDate reportDate = LocalDate.now(clock.withZone(REPORT_ZONE));
        List<FailureItem> failures = repository.findUnresolvedFailures();
        if (failures.isEmpty()) {
            return;
        }

        ObjectNode payload = basePayload(
            "[ExtractAPI] Daily unresolved classification failures - " + reportDate,
            failures.size() + " classification failure(s) still require attention."
        );
        payload.put("reportDate", reportDate.toString());
        ArrayNode items = payload.putArray("items");
        failures.forEach(failure -> appendFailure(items, failure));
        repository.enqueue(
            "DAILY_FAILURE_REPORT",
            "DAILY_FAILURE_REPORT:" + reportDate,
            payload,
            null
        );
    }

    public void enqueueWeeklyStatusReport() {
        LocalDate endDate = LocalDate.now(clock.withZone(REPORT_ZONE));
        LocalDate startDate = endDate.minusDays(7);
        Instant start = startDate.atStartOfDay(REPORT_ZONE).toInstant();
        Instant end = endDate.atStartOfDay(REPORT_ZONE).toInstant();
        WeeklyMetrics metrics = repository.findWeeklyMetrics(start, end);

        ObjectNode payload = basePayload(
            "[ExtractAPI] Weekly status - " + startDate + " to " + endDate.minusDays(1),
            "Operational metrics for the completed Saturday-through-Friday period."
        );
        payload.put("periodStart", startDate.toString());
        payload.put("periodEndExclusive", endDate.toString());
        ObjectNode totals = payload.putObject("totals");
        totals.put("reports", metrics.reports());
        totals.put("uniqueUsers", metrics.uniqueUsers());
        totals.put("newCatalogEntries", metrics.newCatalogEntries());
        totals.put("aiFailures", metrics.aiFailures());
        totals.put("applyFailures", metrics.applyFailures());
        totals.put("aiCategories", metrics.aiCategories());
        totals.put("unresolvedFailures", metrics.unresolvedFailures());
        ArrayNode perUser = payload.putArray("perUser");
        for (UserReportCount user : metrics.perUser()) {
            ObjectNode row = perUser.addObject();
            row.put("userId", user.userId());
            putNullable(row, "email", user.email());
            putNullable(row, "displayName", user.displayName());
            row.put("reports", user.reports());
        }
        repository.enqueue(
            "WEEKLY_STATUS_REPORT",
            "WEEKLY_STATUS_REPORT:" + startDate,
            payload,
            null
        );
    }

    public void deliverPendingEmails() {
        int recovered = transactions.execute(
            status -> repository.recoverExpiredClaims()
        );
        if (recovered > 0) {
            LOG.warn("Recovered {} expired admin email delivery claim(s)", recovered);
        }
        for (int count = 0; count < MAX_DELIVERIES_PER_RUN; count++) {
            Optional<DeliveryClaim> claimed = transactions.execute(
                status -> repository.claimNext(Instant.now(clock).plus(DELIVERY_LEASE))
            );
            DeliveryClaim claim = claimed == null ? null : claimed.orElse(null);
            if (claim == null) {
                return;
            }
            deliver(claim);
        }
    }

    public ResendResult resend(long notificationId) {
        return transactions.execute(status -> {
            NotificationState notification = repository.lockNotification(notificationId)
                .orElseThrow(() -> new NoSuchElementException(
                    "Admin email notification " + notificationId + " was not found"
                ));
            return switch (notification.status()) {
                case "FAILED" -> {
                    repository.makeImmediatelyEligible(notificationId, true);
                    yield new ResendResult(notificationId, "REQUEUED");
                }
                case "SENT" -> new ResendResult(
                    repository.createResend(notification),
                    "RESENT"
                );
                case "PENDING", "RETRY" -> {
                    repository.makeImmediatelyEligible(notificationId, false);
                    yield new ResendResult(notificationId, "EXPEDITED");
                }
                case "SENDING" -> throw new IllegalStateException(
                    "Admin email notification is currently being delivered"
                );
                default -> throw new IllegalStateException(
                    "Unsupported admin email notification status " + notification.status()
                );
            };
        });
    }

    private void deliver(DeliveryClaim claim) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(sender);
            message.setTo(recipients.toArray(String[]::new));
            message.setSubject(claim.payload().path("subject").asText());
            message.setText(renderBody(claim));
            mailSender.send(message);
            transactions.executeWithoutResult(status -> {
                if (repository.lockSending(claim.id(), claim.attempt())) {
                    repository.markSent(claim.id());
                }
            });
        } catch (Exception exception) {
            String error = safeError(exception);
            boolean terminal = claim.attempt() >= MAX_DELIVERY_ATTEMPTS;
            Instant nextAttempt = Instant.now(clock).plus(retryDelay(claim.attempt()));
            transactions.executeWithoutResult(status -> {
                if (!repository.lockSending(claim.id(), claim.attempt())) {
                    return;
                }
                if (terminal) {
                    repository.markFailed(claim.id(), error);
                } else {
                    repository.markRetry(claim.id(), error, nextAttempt);
                }
            });
            if (terminal) {
                LOG.error(
                    "Admin email notification {} permanently failed after {} attempts: {}",
                    claim.id(),
                    claim.attempt(),
                    error
                );
            } else {
                LOG.warn(
                    "Admin email notification {} attempt {} failed: {}",
                    claim.id(),
                    claim.attempt(),
                    error
                );
            }
        }
    }

    private String renderBody(DeliveryClaim claim) {
        StringBuilder body = new StringBuilder();
        body.append(claim.payload().path("summary").asText()).append("\n\n");
        if (claim.payload().has("reportDate")) {
            body.append("Report date: ").append(claim.payload().path("reportDate").asText())
                .append('\n');
        }
        if (claim.payload().has("periodStart")) {
            body.append("Period: ")
                .append(claim.payload().path("periodStart").asText())
                .append(" through ")
                .append(LocalDate.parse(claim.payload().path("periodEndExclusive").asText())
                    .minusDays(1))
                .append("\n\n");
        }
        if (claim.payload().has("totals")) {
            JsonText.appendObject(body, claim.payload().path("totals"));
        }
        if (claim.payload().has("perUser")) {
            body.append("\nReports per user:\n");
            claim.payload().path("perUser").forEach(user -> body
                .append("- userId=").append(user.path("userId").asLong())
                .append(", email=").append(user.path("email").asText("-"))
                .append(", displayName=").append(user.path("displayName").asText("-"))
                .append(", reports=").append(user.path("reports").asInt())
                .append('\n'));
        }
        if (claim.payload().has("items")) {
            body.append("\nFailures:\n");
            claim.payload().path("items").forEach(item -> body
                .append("- classificationId=").append(item.path("classificationId").asLong())
                .append(", taskId=").append(item.path("taskId").asLong())
                .append(", batchId=").append(item.path("batchId").asLong())
                .append(", expenseName=").append(item.path("expenseName").asText())
                .append(", stage=").append(item.path("stage").asText())
                .append(", attempts=").append(item.path("attempts").asInt())
                .append(item.hasNonNull("suggestedCategory")
                    ? ", suggestedCategory=" + item.path("suggestedCategory").asText()
                    : "")
                .append(", finishedAt=").append(item.path("finishedAt").asText("-"))
                .append(", error=").append(item.path("error").asText("-"))
                .append('\n'));
        }
        body.append("\nNotification ID: ").append(claim.id()).append('\n');
        return body.toString();
    }

    private ObjectNode basePayload(String subject, String summary) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("schemaVersion", 1);
        payload.put("subject", subject);
        payload.put("summary", summary);
        payload.put("generatedAt", Instant.now(clock).toString());
        return payload;
    }

    private void appendFailure(ArrayNode items, FailureItem failure) {
        ObjectNode item = items.addObject();
        item.put("classificationId", failure.classificationId());
        item.put("taskId", failure.taskId());
        item.put("batchId", failure.batchId());
        item.put("expenseName", failure.expenseName());
        item.put("stage", failure.stage());
        item.put("attempts", failure.attempts());
        putNullable(item, "suggestedCategory", failure.suggestedCategory());
        putNullable(item, "error", failure.error());
        item.put("finishedAt", DateTimeFormatter.ISO_INSTANT.format(failure.finishedAt()));
    }

    private void putNullable(ObjectNode node, String field, String value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value);
        }
    }

    private Duration retryDelay(int completedAttempts) {
        int index = Math.max(0, Math.min(completedAttempts - 1, RETRY_DELAYS.size() - 1));
        return RETRY_DELAYS.get(index);
    }

    private String safeError(Exception exception) {
        String message = exception.getMessage();
        String sanitized = exception.getClass().getSimpleName()
            + (message == null || message.isBlank() ? "" : ": " + message)
            .replace('\r', ' ')
            .replace('\n', ' ');
        return sanitized.substring(0, Math.min(sanitized.length(), MAX_ERROR_LENGTH));
    }

    private static final class JsonText {
        private JsonText() {
        }

        static void appendObject(StringBuilder target, com.fasterxml.jackson.databind.JsonNode object) {
            object.fields().forEachRemaining(entry -> target
                .append(entry.getKey())
                .append(": ")
                .append(entry.getValue().asText())
                .append('\n'));
        }
    }

    public record ResendResult(long notificationId, String action) {
    }
}
