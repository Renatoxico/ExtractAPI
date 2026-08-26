package io.github.renatoxico.extract.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.renatoxico.extract.config.ClassificationProperties;
import io.github.renatoxico.extract.repo.ClassificationWorkRepository;
import io.github.renatoxico.extract.repo.ClassificationWorkRepository.ApplyClaim;
import io.github.renatoxico.extract.repo.ClassificationWorkRepository.ClaimedBatch;
import io.github.renatoxico.extract.repo.ClassificationWorkRepository.TaskItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassificationTransactionServiceTest {
    @Mock
    private ClassificationWorkRepository repository;

    private ClassificationProperties properties;
    private ClassificationTransactionService service;

    @BeforeEach
    void setUp() {
        properties = new ClassificationProperties();
        service = new ClassificationTransactionService(repository, properties, new ObjectMapper());
    }

    @Test
    void partialResponseAdvancesValidTaskAndRetriesOnlyMissingTask() {
        ClaimedBatch claim = new ClaimedBatch(7L, 1, List.of(
            new TaskItem(11L, "FIRST"),
            new TaskItem(12L, "SECOND")
        ));
        AiProcessorService.AiResponse response = new AiProcessorService.AiResponse(
            "11|Supermercado",
            new AiCategoryResponseParser.ParseResult(
                List.of(new AiCategoryResponseParser.ParsedItem(11L, "Supermercado")),
                List.of()
            )
        );
        when(repository.lockProcessingBatch(7L, 1)).thenReturn(true);

        boolean completed = service.completeBatch(claim, response);

        assertThat(completed).isTrue();
        verify(repository).markTaskReady(11L, 7L, "Supermercado");
        verify(repository, never()).markTaskFailed(any(Long.class), any());
        verify(repository).markBatchRetry(
            org.mockito.ArgumentMatchers.eq(7L),
            any(),
            org.mockito.ArgumentMatchers.eq(
                "AI response did not contain a valid result for every task"
            ),
            any()
        );
        verify(repository, never()).markBatchSucceeded(any(Long.class), any());
    }

    @Test
    void missingItemFailsAfterThirdAiAttempt() {
        ClaimedBatch claim = new ClaimedBatch(
            7L,
            3,
            List.of(new TaskItem(12L, "SECOND"))
        );
        AiProcessorService.AiResponse response = new AiProcessorService.AiResponse(
            "invalid",
            new AiCategoryResponseParser.ParseResult(List.of(), List.of("invalid"))
        );
        when(repository.lockProcessingBatch(7L, 3)).thenReturn(true);

        service.completeBatch(claim, response);

        verify(repository).markTaskFailed(
            12L,
            "AI response did not contain a valid result for every task"
        );
        verify(repository).markBatchFailed(
            org.mockito.ArgumentMatchers.eq(7L),
            any(),
            org.mockito.ArgumentMatchers.eq(
                "AI response did not contain a valid result for every task"
            )
        );
    }

    @Test
    void staleBatchCompletionIsRejected() {
        ClaimedBatch claim = new ClaimedBatch(
            7L,
            1,
            List.of(new TaskItem(11L, "FIRST"))
        );
        AiProcessorService.AiResponse response = new AiProcessorService.AiResponse(
            "11|Supermercado",
            new AiCategoryResponseParser.ParseResult(
                List.of(new AiCategoryResponseParser.ParsedItem(11L, "Supermercado")),
                List.of()
            )
        );
        when(repository.lockProcessingBatch(7L, 1)).thenReturn(false);

        assertThat(service.completeBatch(claim, response)).isFalse();

        verify(repository, never()).markBatchSucceeded(any(Long.class), any());
    }

    @Test
    void thirdCatalogApplyFailureBecomesTerminal() {
        ApplyClaim claim = new ApplyClaim(9L, 3L, "Supermercado", 3);
        when(repository.lockApplyingTask(9L, 3)).thenReturn(true);

        service.failApplyTask(claim, "database unavailable");

        verify(repository).markTaskFailed(9L, "database unavailable");
        verify(repository, never()).markTaskApplyRetry(eq(9L), any(), any());
    }

    @Test
    void catalogAndTaskApplicationUseOneRepositoryOperationAfterFence() {
        ApplyClaim claim = new ApplyClaim(9L, 3L, "Supermercado", 1);
        when(repository.lockApplyingTask(9L, 1)).thenReturn(true);

        assertThat(service.applyCatalogSuggestion(claim)).isTrue();

        verify(repository).applyCatalogSuggestion(claim);
    }
}
