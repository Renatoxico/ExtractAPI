package io.github.renatoxico.extract.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduledTasksServiceTest {

    @Mock
    private ClassificationPipelineService classificationPipeline;
    @Mock
    private AdminEmailService adminEmailService;

    @InjectMocks
    private ScheduledTasksService service;

    @Test
    void delegatesCatalogRegistration() {
        service.registerMissingCatalogEntries();

        verify(classificationPipeline).registerMissingCatalogEntries();
    }

    @Test
    void delegatesBatchCreation() {
        service.createClassificationBatch();

        verify(classificationPipeline).createNextClassificationBatch();
    }

    @Test
    void delegatesAiProcessing() {
        service.processAiClassificationBatch();

        verify(classificationPipeline).processNextAiBatch();
    }

    @Test
    void delegatesAiRecovery() {
        service.recoverExpiredAiBatches();

        verify(classificationPipeline).recoverExpiredAiBatches();
    }

    @Test
    void delegatesCatalogApplication() {
        service.applyClassificationTasks();

        verify(classificationPipeline).applyReadyTasks();
    }

    @Test
    void delegatesApplyRecovery() {
        service.recoverExpiredApplyTasks();

        verify(classificationPipeline).recoverExpiredApplyTasks();
    }

    @Test
    void delegatesExpensePropagation() {
        service.propagateCatalogCategories();

        verify(classificationPipeline).propagateCatalogCategories();
    }

    @Test
    void delegatesAdminEmailDelivery() {
        service.deliverAdminEmails();

        verify(adminEmailService).deliverPendingEmails();
    }

    @Test
    void delegatesDailyAdminReport() {
        service.sendDailyFailureReport();

        verify(adminEmailService).enqueueDailyFailureReport();
    }

    @Test
    void delegatesWeeklyAdminReport() {
        service.sendWeeklyStatusReport();

        verify(adminEmailService).enqueueWeeklyStatusReport();
    }
}
