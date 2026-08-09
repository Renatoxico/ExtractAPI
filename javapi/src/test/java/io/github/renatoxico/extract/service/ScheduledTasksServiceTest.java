package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.repo.ExpenseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduledTasksServiceTest {

    @Mock
    private AiProcessorService aiProcessorService;

    @Mock
    private ExpenseReportingService expenseReportingService;

    @Mock
    private ExpenseRepository expenseRepo;

    @Mock
    private ExpenseClassificationCatalogService catalogService;

    @InjectMocks
    private ScheduledTasksService service;

    @Test
    void shouldPopulateMissingClassificationsWhenTriggered() {
        service.populateMissingClassifications();

        verify(catalogService).populateMissing();
    }

    @Test
    void shouldApplyCatalogCategoriesToMissingExpensesWhenTriggered() {
        service.applyCatalogCategoriesToMissingExpenses();

        verify(catalogService).applyCategoriesToMissingExpenses();
    }
}
