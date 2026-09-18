package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.repo.ExpenseClassificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
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
        LOG.atLevel(insertedEntries > 0 ? Level.INFO : Level.DEBUG)
            .log("Added {} catalog entries from report {}", insertedEntries, reportId);
        return insertedEntries;
    }

    @Transactional
    public int populateMissing() {
        int insertedEntries = classificationRepository.populateMissing();
        LOG.atLevel(insertedEntries > 0 ? Level.INFO : Level.DEBUG)
            .log("Added {} missing entries to the expense classification catalog", insertedEntries);
        return insertedEntries;
    }

    @Transactional
    public int applyCategoriesToReport(String reportId) {
        int updatedExpenses = classificationRepository.applyCategoriesToReport(reportId);
        LOG.atLevel(updatedExpenses > 0 ? Level.INFO : Level.DEBUG)
            .log("Applied catalog categories to {} expenses from report {}", updatedExpenses, reportId);
        return updatedExpenses;
    }

}
