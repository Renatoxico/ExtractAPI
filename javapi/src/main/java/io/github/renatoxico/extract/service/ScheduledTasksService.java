package io.github.renatoxico.extract.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledTasksService {
    private static final Logger LOG = LoggerFactory.getLogger(ScheduledTasksService.class);
    private static final String REPORT_ZONE = "America/Sao_Paulo";
    private final ClassificationPipelineService classificationPipeline;
    private final AdminEmailService adminEmailService;

    public ScheduledTasksService(
        ClassificationPipelineService classificationPipeline,
        AdminEmailService adminEmailService
    ) {
        this.classificationPipeline = classificationPipeline;
        this.adminEmailService = adminEmailService;
    }

    @Scheduled(cron = "${classification.catalog-registration-cron:0 * * * * *}")
    public void registerMissingCatalogEntries() {
        LOG.debug("Starting expense-to-catalog registration");
        classificationPipeline.registerMissingCatalogEntries();
    }

    @Scheduled(cron = "${classification.batch-creation-cron:10 * * * * *}")
    public void createClassificationBatch() {
        LOG.debug("Starting classification batch creation");
        classificationPipeline.createNextClassificationBatch();
    }

    @Scheduled(cron = "${classification.ai-cron:0 */5 * * * *}")
    public void processAiClassificationBatch() {
        LOG.debug("Starting durable AI classification worker");
        classificationPipeline.processNextAiBatch();
    }

    @Scheduled(cron = "${classification.ai-recovery-cron:20 * * * * *}")
    public void recoverExpiredAiBatches() {
        LOG.debug("Starting expired AI batch recovery");
        classificationPipeline.recoverExpiredAiBatches();
    }

    @Scheduled(cron = "${classification.apply-cron:30 * * * * *}")
    public void applyClassificationTasks() {
        LOG.debug("Starting catalog classification apply worker");
        classificationPipeline.applyReadyTasks();
    }

    @Scheduled(cron = "${classification.apply-recovery-cron:40 * * * * *}")
    public void recoverExpiredApplyTasks() {
        LOG.debug("Starting expired classification apply recovery");
        classificationPipeline.recoverExpiredApplyTasks();
    }

    @Scheduled(cron = "${classification.propagation-cron:45 * * * * *}")
    public void propagateCatalogCategories() {
        LOG.debug("Starting catalog-to-expense propagation worker");
        classificationPipeline.propagateCatalogCategories();
    }

    @Scheduled(cron = "0 * * * * *", zone = REPORT_ZONE)
    public void deliverAdminEmails() {
        LOG.debug("Starting admin email delivery worker");
        adminEmailService.deliverPendingEmails();
    }

    @Scheduled(cron = "0 55 23 * * *", zone = REPORT_ZONE)
    public void sendDailyFailureReport() {
        LOG.debug("Creating daily classification failure report");
        adminEmailService.enqueueDailyFailureReport();
    }

    @Scheduled(cron = "0 0 8 * * SAT", zone = REPORT_ZONE)
    public void sendWeeklyStatusReport() {
        LOG.debug("Creating weekly admin status report");
        adminEmailService.enqueueWeeklyStatusReport();
    }
}
