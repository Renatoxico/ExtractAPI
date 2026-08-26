package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.repo.ExpenseClassificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseClassificationCatalogService {
    private static final Logger LOG = LoggerFactory.getLogger(ExpenseClassificationCatalogService.class);

    private final ExpenseClassificationRepository classificationRepository;

    public ExpenseClassificationCatalogService(ExpenseClassificationRepository classificationRepository) {
        this.classificationRepository = classificationRepository;
    }

    @Transactional
    public int populateFromReport(String reportId) {
        int insertedEntries = classificationRepository.populateFromReport(reportId);
        LOG.info("Added {} catalog entries from report {}", insertedEntries, reportId);
        return insertedEntries;
    }

    @Transactional
    public int populateMissing() {
        int insertedEntries = classificationRepository.populateMissing();
        LOG.info("Added {} missing entries to the expense classification catalog", insertedEntries);
        return insertedEntries;
    }

    @Transactional
    public int applyCategoriesToReport(String reportId) {
        int updatedExpenses = classificationRepository.applyCategoriesToReport(reportId);
        LOG.info("Applied catalog categories to {} expenses from report {}", updatedExpenses, reportId);
        return updatedExpenses;
    }

}
