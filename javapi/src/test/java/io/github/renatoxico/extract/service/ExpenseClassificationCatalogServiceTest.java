package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.repo.ExpenseClassificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseClassificationCatalogServiceTest {

    @Mock
    private ExpenseClassificationRepository classificationRepository;

    @InjectMocks
    private ExpenseClassificationCatalogService service;

    @Test
    void shouldPopulateCatalogFromReport() {
        when(classificationRepository.populateFromReport("report-123")).thenReturn(3);

        int insertedEntries = service.populateFromReport("report-123");

        assertThat(insertedEntries).isEqualTo(3);
        verify(classificationRepository).populateFromReport("report-123");
    }

    @Test
    void shouldPopulateMissingCatalogEntries() {
        when(classificationRepository.populateMissing()).thenReturn(5);

        int insertedEntries = service.populateMissing();

        assertThat(insertedEntries).isEqualTo(5);
        verify(classificationRepository).populateMissing();
    }

    @Test
    void shouldApplyCategoriesToReport() {
        when(classificationRepository.applyCategoriesToReport("report-123")).thenReturn(4);

        int updatedExpenses = service.applyCategoriesToReport("report-123");

        assertThat(updatedExpenses).isEqualTo(4);
        verify(classificationRepository).applyCategoriesToReport("report-123");
    }

    @Test
    void shouldApplyCategoriesToMissingExpenses() {
        when(classificationRepository.applyCategoriesToMissingExpenses()).thenReturn(7);

        int updatedExpenses = service.applyCategoriesToMissingExpenses();

        assertThat(updatedExpenses).isEqualTo(7);
        verify(classificationRepository).applyCategoriesToMissingExpenses();
    }
}
