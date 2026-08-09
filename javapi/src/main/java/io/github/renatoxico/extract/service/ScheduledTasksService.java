package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.model.ExpenseCategoryAssignment;
import io.github.renatoxico.extract.repo.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduledTasksService {
    private static final Logger LOG = LoggerFactory.getLogger(ScheduledTasksService.class);
    private final AiProcessorService aiProcessorService;
    private final ExpenseReportingService expenseReportingService;
    private final ExpenseRepository expenseRepo;
    private final ExpenseClassificationCatalogService catalogService;

    public ScheduledTasksService(
        AiProcessorService aiProcessorService,
        ExpenseReportingService expenseReportingService,
        ExpenseRepository expenseRepo,
        ExpenseClassificationCatalogService catalogService
    ) {
        this.aiProcessorService = aiProcessorService;
        this.expenseReportingService = expenseReportingService;
        this.expenseRepo = expenseRepo;
        this.catalogService = catalogService;
    }

    @Scheduled(cron = "0 */5 * * * *")
    private void enrichCategories() {
        LOG.info("Starting scheduled AI enrichment task...");
        List<ExpenseCategoryAssignment> enrichedExpenses = expenseReportingService.getUnclassifiedExpenseNames();
        if (enrichedExpenses.stream().count() == 0) {
            LOG.info("No expenses found for enrichment.");
            return;
        }
        try {
            enrichedExpenses = aiProcessorService.processWithGemini(enrichedExpenses);
            for (ExpenseCategoryAssignment assignment : enrichedExpenses) {
                if (assignment.category() != null && !assignment.category().isEmpty()) {
                    LOG.info("Updating {} to category {}", assignment.expenseName(), assignment.category());
                    expenseRepo.updateCategory(assignment.expenseName(), assignment.category());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Scheduled(cron = "0 */1 * * * *")
    private void updateMatchedExpenses() {
        LOG.info("Starting scheduled Matched Expenses update task...");
        expenseRepo.updateMatchedExpenses();
        LOG.info("Finished scheduled Matched Expenses update task...");
    }

    @Scheduled(cron = "${catalog.backfill.cron:-}")
    public void populateMissingClassifications() {
        LOG.info("Starting expense classification catalog backfill");
        catalogService.populateMissing();
        LOG.info("Finished expense classification catalog backfill");
    }

    @Scheduled(cron = "${catalog.expense-update.cron:-}")
    public void applyCatalogCategoriesToMissingExpenses() {
        LOG.info("Starting catalog category application to expenses");
        catalogService.applyCategoriesToMissingExpenses();
        LOG.info("Finished catalog category application to expenses");
    }
}
