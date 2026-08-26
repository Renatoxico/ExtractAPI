package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.config.ClassificationProperties;
import io.github.renatoxico.extract.repo.ClassificationWorkRepository.ApplyClaim;
import io.github.renatoxico.extract.repo.ClassificationWorkRepository.ClaimedBatch;
import io.github.renatoxico.extract.repo.ClassificationWorkRepository.ExpiredClaim;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClassificationPipelineService {
    private static final Logger LOG = LoggerFactory.getLogger(ClassificationPipelineService.class);
    private static final int MAX_ERROR_LENGTH = 1_000;

    private final ClassificationTransactionService transactions;
    private final ClassificationProperties properties;
    private final AiProcessorService aiProcessorService;
    private final ExpenseClassificationCatalogService catalogService;

    public ClassificationPipelineService(
        ClassificationTransactionService transactions,
        ClassificationProperties properties,
        AiProcessorService aiProcessorService,
        ExpenseClassificationCatalogService catalogService
    ) {
        this.transactions = transactions;
        this.properties = properties;
        this.aiProcessorService = aiProcessorService;
        this.catalogService = catalogService;
    }

    public void registerMissingCatalogEntries() {
        int catalogEntries = catalogService.populateMissing();
        LOG.info("Catalog registration created {} entries", catalogEntries);
    }

    public void createNextClassificationBatch() {
        Optional<Long> batchId = transactions.createPendingBatch();
        LOG.info("Pending classification batch created={}", batchId.isPresent());
    }

    public void processNextAiBatch() {
        Optional<ClaimedBatch> claimedBatch = transactions.claimNextBatch();
        if (claimedBatch.isEmpty()) {
            return;
        }

        ClaimedBatch claim = claimedBatch.get();
        List<AiProcessorService.RequestItem> requestItems = claim.items().stream()
            .map(item -> new AiProcessorService.RequestItem(item.taskId(), item.expenseName()))
            .toList();

        try {
            AiProcessorService.AiResponse response = aiProcessorService.processWithGemini(requestItems);
            if (response.parseResult().acceptedItems().isEmpty()) {
                transactions.failBatch(
                    claim,
                    response,
                    "AI response contained no valid task classifications"
                );
                return;
            }
            transactions.completeBatch(claim, response);
        } catch (Exception exception) {
            LOG.warn(
                "AI classification batch {} attempt {} failed",
                claim.batchId(),
                claim.attempt()
            );
            transactions.failBatch(claim, null, safeError(exception));
        }
    }

    public void applyReadyTasks() {
        for (int count = 0; count < properties.getBatchSize(); count++) {
            Optional<ApplyClaim> claimedTask = transactions.claimNextApplyTask();
            if (claimedTask.isEmpty()) {
                return;
            }

            ApplyClaim claim = claimedTask.get();
            try {
                transactions.applyCatalogSuggestion(claim);
            } catch (Exception exception) {
                LOG.warn(
                    "Catalog application task {} attempt {} failed",
                    claim.taskId(),
                    claim.attempt()
                );
                transactions.failApplyTask(claim, safeError(exception));
            }
        }
    }

    public void propagateCatalogCategories() {
        List<Long> expenseIds = transactions.findExpenseIdsReadyForPropagation();
        for (Long expenseId : expenseIds) {
            try {
                transactions.propagateCategoryToExpense(expenseId);
            } catch (Exception exception) {
                LOG.warn("Category propagation failed for expense id {}", expenseId);
            }
        }
    }

    public void recoverExpiredAiBatches() {
        for (ExpiredClaim claim : transactions.findExpiredBatchClaims()) {
            transactions.recoverExpiredBatch(claim);
        }
    }

    public void recoverExpiredApplyTasks() {
        for (ExpiredClaim claim : transactions.findExpiredApplyClaims()) {
            transactions.recoverExpiredApplyTask(claim);
        }
    }

    private String safeError(Exception exception) {
        String message = exception.getMessage();
        String sanitized = exception.getClass().getSimpleName()
            + (message == null || message.isBlank() ? "" : ": " + message)
            .replace('\r', ' ')
            .replace('\n', ' ');
        return sanitized.substring(0, Math.min(sanitized.length(), MAX_ERROR_LENGTH));
    }
}
