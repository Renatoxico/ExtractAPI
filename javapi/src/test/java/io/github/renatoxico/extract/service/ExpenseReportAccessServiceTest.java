package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.repo.ExpenseReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseReportAccessServiceTest {
    @Mock
    private ExpenseReportRepository expenseReportRepository;

    @InjectMocks
    private ExpenseReportAccessService expenseReportAccessService;

    @Test
    void requireOwnershipAllowsOwner() {
        when(expenseReportRepository.existsBySessionIdAndOwnerId("session-123", 42L)).thenReturn(true);

        assertDoesNotThrow(() -> expenseReportAccessService.requireOwnership("session-123", 42L));
    }

    @Test
    void requireOwnershipRejectsDifferentUserForSameReport() {
        when(expenseReportRepository.existsBySessionIdAndOwnerId("session-123", 99L)).thenReturn(false);

        ProcessingException exception = assertThrows(
            ProcessingException.class,
            () -> expenseReportAccessService.requireOwnership("session-123", 99L)
        );

        assertEquals("SESSION_NOT_FOUND", exception.getErrorCode());
    }
}
