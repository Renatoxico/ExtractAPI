package com.example.api.service;

import com.example.api.model.CategoryMapper;
import com.example.api.repo.ExpenseRepository;
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

    public ScheduledTasksService(AiProcessorService aiProcessorService, ExpenseReportingService expenseReportingService, ExpenseRepository expenseRepo) {
        this.aiProcessorService = aiProcessorService;
        this.expenseReportingService = expenseReportingService;
        this.expenseRepo = expenseRepo;
    }

    @Scheduled(cron = "0 */2 * * * *")
    private void enrichCategories() {
        LOG.info("Starting scheduled AI enrichment task...");
        List<CategoryMapper> enrichedExpenses = expenseReportingService.getExpenseNames();
        if (enrichedExpenses.stream().count() == 0) {
            LOG.info("No expenses found for enrichment.");
            return;
        }
        try {
            enrichedExpenses = aiProcessorService.processWithGemini(enrichedExpenses);
            for (CategoryMapper cm : enrichedExpenses) {
                if (cm.getTransactionType() != null && !cm.getTransactionType().isEmpty()) {
                    LOG.info("Updating {} to category {}", cm.getExpenseName(), cm.getTransactionType());
                    expenseRepo.updateTransactionType(cm.getExpenseName(), cm.getTransactionType());
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
}
