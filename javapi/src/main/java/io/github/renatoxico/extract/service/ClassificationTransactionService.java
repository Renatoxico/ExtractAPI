package io.github.renatoxico.extract.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.renatoxico.extract.config.ClassificationProperties;
import io.github.renatoxico.extract.repo.ClassificationWorkRepository;
import io.github.renatoxico.extract.repo.ClassificationWorkRepository.ApplyClaim;
import io.github.renatoxico.extract.repo.ClassificationWorkRepository.ClassificationCandidate;
import io.github.renatoxico.extract.repo.ClassificationWorkRepository.ClaimedBatch;
import io.github.renatoxico.extract.repo.ClassificationWorkRepository.ExpiredClaim;
import io.github.renatoxico.extract.repo.ClassificationWorkRepository.TaskItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ClassificationTransactionService {
    private final ClassificationWorkRepository workRepository;
    private final ClassificationProperties properties;
    private final ObjectMapper objectMapper;
    private final AdminEmailService adminEmailService;

    public ClassificationTransactionService(
        ClassificationWorkRepository workRepository,
        ClassificationProperties properties,
        ObjectMapper objectMapper,
        AdminEmailService adminEmailService
    ) {
        this.workRepository = workRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.adminEmailService = adminEmailService;
    }

    @Transactional
    public Optional<Long> createPendingBatch() {
        List<ClassificationCandidate> candidates = workRepository.lockClassificationsNeedingBatch(
            properties.getBatchSize()
        );
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(workRepository.createPendingBatch(candidates));
    }

    @Transactional
    public Optional<ClaimedBatch> claimNextBatch() {
        return workRepository.lockNextEligibleBatch().flatMap(header -> {
            int attempt = header.attempts() + 1;
            Instant leaseExpiresAt = Instant.now().plus(properties.getLeaseDuration());
            workRepository.markBatchProcessing(header.batchId(), attempt, leaseExpiresAt);
            workRepository.closeWaitingTasksWithKnownCategory(header.batchId());
            List<TaskItem> items = workRepository.findWaitingBatchItems(header.batchId());
            if (items.isEmpty()) {
                workRepository.markBatchSucceeded(header.batchId(), emptyOutputPayload());
                return Optional.empty();
            }
            return Optional.of(new ClaimedBatch(header.batchId(), attempt, items));
        });
    }

    @Transactional
    public boolean completeBatch(
        ClaimedBatch claim,
        AiProcessorService.AiResponse response
    ) {
        if (!workRepository.lockProcessingBatch(claim.batchId(), claim.attempt())) {
            return false;
        }

        Map<Long, AiCategoryResponseParser.ParsedItem> acceptedByTask =
            response.parseResult().acceptedItems().stream()
                .collect(Collectors.toMap(
                    AiCategoryResponseParser.ParsedItem::taskId,
                    Function.identity()
                ));

        List<TaskItem> unresolvedItems = new ArrayList<>();
        for (TaskItem item : claim.items()) {
            AiCategoryResponseParser.ParsedItem accepted = acceptedByTask.get(item.taskId());
            if (accepted != null) {
                workRepository.markTaskReady(item.taskId(), claim.batchId(), accepted.category());
            } else {
                unresolvedItems.add(item);
            }
        }

        JsonNode outputPayload = toOutputPayload(response);
        if (unresolvedItems.isEmpty()) {
            workRepository.markBatchSucceeded(claim.batchId(), outputPayload);
        } else if (claim.attempt() >= properties.getMaxAiAttempts()) {
            ClaimedBatch failedClaim = new ClaimedBatch(
                claim.batchId(),
                claim.attempt(),
                unresolvedItems
            );
            failWaitingTasks(
                failedClaim,
                "AI response did not contain a valid result for every task"
            );
            workRepository.markBatchFailed(
                claim.batchId(),
                outputPayload,
                "AI response did not contain a valid result for every task"
            );
            adminEmailService.enqueueAiFailure(
                failedClaim,
                "AI response did not contain a valid result for every task"
            );
        } else {
            workRepository.markBatchRetry(
                claim.batchId(),
                outputPayload,
                "AI response did not contain a valid result for every task",
                Instant.now().plus(properties.retryDelay(claim.attempt()))
            );
        }
        return true;
    }

    @Transactional
    public boolean failBatch(
        ClaimedBatch claim,
        AiProcessorService.AiResponse response,
        String error
    ) {
        if (!workRepository.lockProcessingBatch(claim.batchId(), claim.attempt())) {
            return false;
        }

        JsonNode outputPayload = response == null ? null : toOutputPayload(response);
        if (claim.attempt() < properties.getMaxAiAttempts()) {
            workRepository.markBatchRetry(
                claim.batchId(),
                outputPayload,
                error,
                Instant.now().plus(properties.retryDelay(claim.attempt()))
            );
        } else {
            failWaitingTasks(claim, error);
            workRepository.markBatchFailed(claim.batchId(), outputPayload, error);
            adminEmailService.enqueueAiFailure(claim, error);
        }
        return true;
    }

    public List<ExpiredClaim> findExpiredBatchClaims() {
        return workRepository.findExpiredBatchClaims();
    }

    @Transactional
    public void recoverExpiredBatch(ExpiredClaim expiredClaim) {
        if (!workRepository.lockProcessingBatch(expiredClaim.id(), expiredClaim.attempt())) {
            return;
        }
        workRepository.closeWaitingTasksWithKnownCategory(expiredClaim.id());
        List<TaskItem> items = workRepository.findWaitingBatchItems(expiredClaim.id());
        if (items.isEmpty()) {
            workRepository.markBatchSucceeded(expiredClaim.id(), emptyOutputPayload());
        } else if (expiredClaim.attempt() < properties.getMaxAiAttempts()) {
            workRepository.markBatchRetry(
                expiredClaim.id(),
                null,
                "AI worker lease expired",
                Instant.now().plus(properties.retryDelay(expiredClaim.attempt()))
            );
        } else {
            failWaitingTasks(
                new ClaimedBatch(expiredClaim.id(), expiredClaim.attempt(), items),
                "AI worker lease expired"
            );
            workRepository.markBatchFailed(
                expiredClaim.id(),
                null,
                "AI worker lease expired"
            );
            adminEmailService.enqueueAiFailure(
                new ClaimedBatch(expiredClaim.id(), expiredClaim.attempt(), items),
                "AI worker lease expired"
            );
        }
    }

    @Transactional
    public Optional<ApplyClaim> claimNextApplyTask() {
        return workRepository.lockNextApplyTask(
            properties.getMaxApplyAttempts(),
            Instant.now().plus(properties.getLeaseDuration())
        );
    }

    @Transactional
    public boolean applyCatalogSuggestion(ApplyClaim claim) {
        if (!workRepository.lockApplyingTask(claim.taskId(), claim.attempt())) {
            return false;
        }
        workRepository.applyCatalogSuggestion(claim);
        return true;
    }

    @Transactional
    public boolean failApplyTask(ApplyClaim claim, String error) {
        if (!workRepository.lockApplyingTask(claim.taskId(), claim.attempt())) {
            return false;
        }
        recordApplyFailure(claim, error);
        return true;
    }

    public List<ApplyClaim> findExpiredApplyClaims() {
        return workRepository.findExpiredApplyClaims();
    }

    @Transactional
    public void recoverExpiredApplyTask(ApplyClaim expiredClaim) {
        if (!workRepository.lockApplyingTask(expiredClaim.taskId(), expiredClaim.attempt())) {
            return;
        }
        recordApplyFailure(expiredClaim, "Catalog apply worker lease expired");
    }

    public List<Long> findExpenseIdsReadyForPropagation() {
        return workRepository.findExpenseIdsReadyForPropagation(
            properties.getPropagationBatchSize()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int propagateCategoryToExpense(long expenseId) {
        return workRepository.propagateCategoryToExpense(expenseId);
    }

    JsonNode toOutputPayload(AiProcessorService.AiResponse response) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("schemaVersion", 1);
        payload.put("rawText", response.rawResponse());

        ArrayNode acceptedItems = payload.putArray("acceptedItems");
        for (AiCategoryResponseParser.ParsedItem item : response.parseResult().acceptedItems()) {
            ObjectNode acceptedItem = acceptedItems.addObject();
            acceptedItem.put("taskId", item.taskId());
            acceptedItem.put("category", item.category());
        }

        ArrayNode diagnostics = payload.putArray("diagnostics");
        response.parseResult().diagnostics().forEach(diagnostics::add);
        return payload;
    }

    private void failWaitingTasks(ClaimedBatch claim, String error) {
        for (TaskItem item : claim.items()) {
            workRepository.markTaskFailed(item.taskId(), error);
        }
    }

    private void recordApplyFailure(ApplyClaim claim, String error) {
        if (claim.attempt() >= properties.getMaxApplyAttempts()) {
            workRepository.markTaskFailed(claim.taskId(), error);
            adminEmailService.enqueueApplyFailure(claim, error);
        } else {
            workRepository.markTaskApplyRetry(
                claim.taskId(),
                error,
                Instant.now().plus(properties.retryDelay(claim.attempt()))
            );
        }
    }

    private JsonNode emptyOutputPayload() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("schemaVersion", 1);
        payload.put("rawText", "");
        payload.putArray("acceptedItems");
        payload.putArray("diagnostics").add("All tasks were already satisfied by the catalog");
        return payload;
    }
}
