package io.github.renatoxico.extract.service;

import com.google.firebase.auth.FirebaseAuth;
import io.github.renatoxico.extract.repo.ClassificationWorkRepository.ApplyClaim;
import io.github.renatoxico.extract.repo.ClassificationWorkRepository.ClaimedBatch;
import io.github.renatoxico.extract.repo.ClassificationWorkRepository.ExpiredClaim;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.flyway.enabled=true",
    "spring.config.import=",
    "classification.catalog-registration-cron=-",
    "classification.batch-creation-cron=-",
    "classification.ai-cron=-",
    "classification.ai-recovery-cron=-",
    "classification.apply-cron=-",
    "classification.apply-recovery-cron=-",
    "classification.propagation-cron=-",
    "ai.api-key=test-gemini-api-key-1234567890123456",
    "cors.allowed-origin=http://localhost:5173"
})
@Testcontainers(disabledWithoutDocker = true)
class ClassificationPipelinePostgresIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ClassificationTransactionService transactions;

    @MockitoBean
    private FirebaseAuth firebaseAuth;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("""
            TRUNCATE TABLE
                admin_email_outbox,
                expense_classification_task,
                ai_classification_batch,
                expense_classification,
                expense,
                expense_report,
                app_user
            RESTART IDENTITY CASCADE
            """);
    }

    @Test
    void flywayCreatesLatestSchemaWithJsonbPayloads() {
        String currentVersion = jdbc.queryForObject("""
            SELECT version
            FROM flyway_schema_history
            WHERE success = true
            ORDER BY installed_rank DESC
            LIMIT 1
            """, String.class);
        String inputType = jdbc.queryForObject("""
            SELECT data_type
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'ai_classification_batch'
              AND column_name = 'input_payload'
            """, String.class);

        assertThat(currentVersion).isEqualTo("8");
        assertThat(inputType).isEqualTo("jsonb");
    }

    @Test
    void batchCreationIsIdempotentWhileWorkIsActiveAndBatchesAtMostFiftyTasks() {
        for (int index = 0; index < 55; index++) {
            insertClassification("EXPENSE-" + index, null, null);
        }

        assertThat(transactions.createPendingBatch()).isPresent();

        Integer firstBatchSize = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM expense_classification_task
            WHERE batch_id = (SELECT MIN(id) FROM ai_classification_batch)
            """, Integer.class);
        assertThat(firstBatchSize).isEqualTo(50);

        assertThat(transactions.createPendingBatch()).isPresent();
        Integer secondBatchSize = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM expense_classification_task
            WHERE batch_id = (SELECT MAX(id) FROM ai_classification_batch)
            """, Integer.class);
        assertThat(secondBatchSize).isEqualTo(5);
        assertThat(transactions.createPendingBatch()).isEmpty();
    }

    @Test
    void nullCatalogAfterAppliedTaskCreatesNewHistoricalTaskAndBatch() {
        long classificationId = insertClassification("REPROCESS-ME", null, null);
        ClaimedBatch firstBatch = createAndClaim();
        long firstTaskId = firstBatch.items().getFirst().taskId();
        transactions.completeBatch(
            firstBatch,
            responseFor(firstTaskId, "Supermercado")
        );
        transactions.applyCatalogSuggestion(transactions.claimNextApplyTask().orElseThrow());

        jdbc.update("""
            UPDATE expense_classification
            SET category = NULL, source = NULL
            WHERE id = ?
            """, classificationId);

        long secondBatchId = transactions.createPendingBatch().orElseThrow();
        List<Long> taskIds = jdbc.queryForList("""
            SELECT id
            FROM expense_classification_task
            WHERE classification_id = ?
            ORDER BY id
            """, Long.class, classificationId);

        assertThat(taskIds).hasSize(2);
        assertThat(taskIds.getFirst()).isEqualTo(firstTaskId);
        assertThat(taskStatus(taskIds.getFirst())).isEqualTo("APPLIED");
        assertThat(taskStatus(taskIds.getLast())).isEqualTo("PENDING_AI");
        assertThat(secondBatchId).isNotEqualTo(firstBatch.batchId());
    }

    @Test
    void latestAppliedTaskAllowsReprocessingDespiteOlderFailureHistory() {
        long classificationId = insertClassification("HISTORICAL-FAILURE", null, null);
        long failedBatchId = insertBatch("FAILED");
        insertTask(failedBatchId, classificationId, "FAILED");
        long appliedBatchId = insertBatch("SUCCEEDED");
        insertTask(appliedBatchId, classificationId, "APPLIED");

        assertThat(transactions.createPendingBatch()).isPresent();
        assertThat(jdbc.queryForObject("""
            SELECT status
            FROM expense_classification_task
            WHERE classification_id = ?
            ORDER BY id DESC
            LIMIT 1
            """, String.class, classificationId)).isEqualTo("PENDING_AI");
    }

    @Test
    void concurrentWorkersClaimDifferentBatches() throws Exception {
        for (int index = 0; index < 100; index++) {
            insertClassification("CONCURRENT-" + index, null, null);
        }
        transactions.createPendingBatch();
        transactions.createPendingBatch();

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Optional<ClaimedBatch>> first = executor.submit(transactions::claimNextBatch);
            Future<Optional<ClaimedBatch>> second = executor.submit(transactions::claimNextBatch);

            ClaimedBatch firstClaim = first.get().orElseThrow();
            ClaimedBatch secondClaim = second.get().orElseThrow();

            assertThat(firstClaim.batchId()).isNotEqualTo(secondClaim.batchId());
            assertThat(firstClaim.items()).hasSize(50);
            assertThat(secondClaim.items()).hasSize(50);
            assertThat(firstClaim.items().stream().map(item -> item.taskId()).toList())
                .doesNotContainAnyElementsOf(
                    secondClaim.items().stream().map(item -> item.taskId()).toList()
                );
        }
    }

    @Test
    void concurrentBatchCreatorsCreateDisjointTasks() throws Exception {
        for (int index = 0; index < 100; index++) {
            insertClassification("CREATE-CONCURRENT-" + index, null, null);
        }

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Optional<Long>> first = executor.submit(transactions::createPendingBatch);
            Future<Optional<Long>> second = executor.submit(transactions::createPendingBatch);

            assertThat(first.get()).isPresent();
            assertThat(second.get()).isPresent();
        }

        assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM expense_classification_task",
            Integer.class
        )).isEqualTo(100);
        assertThat(jdbc.queryForObject(
            "SELECT COUNT(DISTINCT classification_id) FROM expense_classification_task",
            Integer.class
        )).isEqualTo(100);
    }

    @Test
    void partialResponsePersistsPayloadAndRetriesOnlyMissingItem() {
        insertClassification("FIRST", null, null);
        insertClassification("SECOND", null, null);
        ClaimedBatch claim = createAndClaim();
        long acceptedTaskId = claim.items().getFirst().taskId();
        long missingTaskId = claim.items().getLast().taskId();
        AiProcessorService.AiResponse response = responseFor(
            acceptedTaskId,
            "Supermercado"
        );

        assertThat(transactions.completeBatch(claim, response)).isTrue();

        assertThat(taskStatus(acceptedTaskId)).isEqualTo("READY_TO_APPLY");
        assertThat(taskStatus(missingTaskId)).isEqualTo("PENDING_AI");
        assertThat(batchStatus(claim.batchId())).isEqualTo("RETRY");
        assertThat(jdbc.queryForObject("""
            SELECT output_payload ->> 'rawText'
            FROM ai_classification_batch
            WHERE id = ?
            """, String.class, claim.batchId())).isEqualTo(response.rawResponse());

        jdbc.update("""
            UPDATE ai_classification_batch
            SET next_attempt_at = CURRENT_TIMESTAMP - INTERVAL '1 second'
            WHERE id = ?
            """, claim.batchId());
        ClaimedBatch retry = transactions.claimNextBatch().orElseThrow();
        assertThat(retry.items()).extracting(item -> item.taskId()).containsExactly(missingTaskId);
    }

    @Test
    void aiTransportRetryChangesOnlyBatchUntilAttemptsAreExhausted() {
        insertClassification("RETRY-ME", null, null);
        ClaimedBatch firstAttempt = createAndClaim();
        long taskId = firstAttempt.items().getFirst().taskId();

        transactions.failBatch(firstAttempt, null, "transport unavailable");

        assertThat(batchStatus(firstAttempt.batchId())).isEqualTo("RETRY");
        assertThat(taskStatus(taskId)).isEqualTo("PENDING_AI");
        assertThat(jdbc.queryForObject(
            "SELECT last_error FROM expense_classification_task WHERE id = ?",
            String.class,
            taskId
        )).isNull();

        ClaimedBatch finalAttempt = firstAttempt;
        for (int attempt = 2; attempt <= 3; attempt++) {
            jdbc.update("""
                UPDATE ai_classification_batch
                SET next_attempt_at = CURRENT_TIMESTAMP - INTERVAL '1 second'
                WHERE id = ?
                """, firstAttempt.batchId());
            finalAttempt = transactions.claimNextBatch().orElseThrow();
            transactions.failBatch(finalAttempt, null, "transport unavailable");
        }

        assertThat(batchStatus(finalAttempt.batchId())).isEqualTo("FAILED");
        assertThat(taskStatus(taskId)).isEqualTo("FAILED");
        assertThat(transactions.createPendingBatch()).isEmpty();
        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM admin_email_outbox
            WHERE deduplication_key = ?
            """, Integer.class, "AI_BATCH_FAILED:" + finalAttempt.batchId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM expense_classification_task
            WHERE classification_id = (
                SELECT classification_id
                FROM expense_classification_task
                WHERE id = ?
            )
            """, Integer.class, taskId)).isEqualTo(1);
    }

    @Test
    void existingCatalogValueWinsAndTaskStillCloses() {
        long classificationId = insertClassification("MERCHANT", null, null);
        ClaimedBatch batch = createAndClaim();
        long taskId = batch.items().getFirst().taskId();
        transactions.completeBatch(batch, responseFor(taskId, "Supermercado"));
        ApplyClaim applyClaim = transactions.claimNextApplyTask().orElseThrow();

        jdbc.update("""
            UPDATE expense_classification
            SET category = 'Moradia / Contas', source = 'RULE'
            WHERE id = ?
            """, classificationId);

        assertThat(transactions.applyCatalogSuggestion(applyClaim)).isTrue();
        assertThat(jdbc.queryForMap("""
            SELECT category, source
            FROM expense_classification
            WHERE id = ?
            """, classificationId))
            .containsEntry("category", "Moradia / Contas")
            .containsEntry("source", "RULE");
        assertThat(taskStatus(taskId)).isEqualTo("APPLIED");
    }

    @Test
    void expiredLeaseIsRecoveredAndRejectsLateCompletion() {
        insertClassification("STALE", null, null);
        ClaimedBatch claim = createAndClaim();
        jdbc.update("""
            UPDATE ai_classification_batch
            SET lease_expires_at = CURRENT_TIMESTAMP - INTERVAL '1 minute'
            WHERE id = ?
            """, claim.batchId());

        ExpiredClaim expired = transactions.findExpiredBatchClaims().getFirst();
        transactions.recoverExpiredBatch(expired);

        assertThat(batchStatus(claim.batchId())).isEqualTo("RETRY");
        assertThat(transactions.completeBatch(
            claim,
            responseFor(claim.items().getFirst().taskId(), "Supermercado")
        )).isFalse();
    }

    @Test
    void expensePropagationUsesIndependentTransactionsAndIsIdempotent() {
        long ownerId = insertOwner();
        insertReport("report-1", ownerId);
        long failingExpenseId = insertExpense("report-1", "BROKEN", null);
        long successfulExpenseId = insertExpense("report-1", "WORKING", null);
        insertClassification("BROKEN", "Supermercado", "RULE");
        insertClassification("WORKING", "Moradia / Contas", "RULE");
        createRejectingTrigger();

        try {
            assertThatThrownBy(() -> transactions.propagateCategoryToExpense(failingExpenseId))
                .isInstanceOf(RuntimeException.class);
            assertThat(transactions.propagateCategoryToExpense(successfulExpenseId)).isEqualTo(1);
            assertThat(expenseCategory(failingExpenseId)).isNull();
            assertThat(expenseCategory(successfulExpenseId)).isEqualTo("Moradia / Contas");
            assertThat(transactions.propagateCategoryToExpense(successfulExpenseId)).isZero();
        } finally {
            jdbc.execute("DROP TRIGGER reject_broken_expense ON expense");
            jdbc.execute("DROP FUNCTION reject_broken_expense_category()");
        }
    }

    private ClaimedBatch createAndClaim() {
        transactions.createPendingBatch().orElseThrow();
        return transactions.claimNextBatch().orElseThrow();
    }

    private AiProcessorService.AiResponse responseFor(long taskId, String category) {
        String rawResponse = taskId + "|" + category;
        return new AiProcessorService.AiResponse(
            rawResponse,
            new AiCategoryResponseParser().parse(rawResponse, Set.of(taskId))
        );
    }

    private long insertClassification(String expenseName, String category, String source) {
        Long id = jdbc.queryForObject("""
            INSERT INTO expense_classification (expense_name, category, source)
            VALUES (?, ?, ?)
            RETURNING id
            """, Long.class, expenseName, category, source);
        return id == null ? 0 : id;
    }

    private long insertBatch(String status) {
        Long id = jdbc.queryForObject("""
            INSERT INTO ai_classification_batch (
                status, input_payload, finished_at
            )
            VALUES (?, '{"schemaVersion":1,"items":[]}'::jsonb, CURRENT_TIMESTAMP)
            RETURNING id
            """, Long.class, status);
        return id == null ? 0 : id;
    }

    private void insertTask(long batchId, long classificationId, String status) {
        jdbc.update("""
            INSERT INTO expense_classification_task (
                batch_id, classification_id, status, finished_at
            )
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
            """, batchId, classificationId, status);
    }

    private long insertOwner() {
        Long id = jdbc.queryForObject("""
            INSERT INTO app_user (firebase_uid, email)
            VALUES ('integration-user', 'integration@example.com')
            RETURNING id
            """, Long.class);
        return id == null ? 0 : id;
    }

    private void insertReport(String reportId, long ownerId) {
        jdbc.update("""
            INSERT INTO expense_report (id, app_user_id, created_at)
            VALUES (?, ?, CURRENT_TIMESTAMP)
            """, reportId, ownerId);
    }

    private long insertExpense(String reportId, String expenseName, String category) {
        Long id = jdbc.queryForObject("""
            INSERT INTO expense (report_id, expense_name, category, amount, date)
            VALUES (?, ?, ?, ?, '2026-08-22')
            RETURNING id
            """, Long.class, reportId, expenseName, category, BigDecimal.TEN);
        return id == null ? 0 : id;
    }

    private void createRejectingTrigger() {
        jdbc.execute("""
            CREATE FUNCTION reject_broken_expense_category()
            RETURNS trigger AS $$
            BEGIN
                IF NEW.expense_name = 'BROKEN' AND NEW.category IS NOT NULL THEN
                    RAISE EXCEPTION 'simulated propagation failure';
                END IF;
                RETURN NEW;
            END;
            $$ LANGUAGE plpgsql
            """);
        jdbc.execute("""
            CREATE TRIGGER reject_broken_expense
            BEFORE UPDATE ON expense
            FOR EACH ROW EXECUTE FUNCTION reject_broken_expense_category()
            """);
    }

    private String taskStatus(long taskId) {
        return jdbc.queryForObject(
            "SELECT status FROM expense_classification_task WHERE id = ?",
            String.class,
            taskId
        );
    }

    private String batchStatus(long batchId) {
        return jdbc.queryForObject(
            "SELECT status FROM ai_classification_batch WHERE id = ?",
            String.class,
            batchId
        );
    }

    private String expenseCategory(long expenseId) {
        return jdbc.queryForObject(
            "SELECT category FROM expense WHERE id = ?",
            String.class,
            expenseId
        );
    }
}
