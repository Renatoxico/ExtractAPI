package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.model.AppUser;
import io.github.renatoxico.extract.model.ExpenseReport;
import io.github.renatoxico.extract.repo.AppUserRepository;
import io.github.renatoxico.extract.repo.ExpenseReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseReportAccessServiceTest {
    @Mock
    private ExpenseReportRepository expenseReportRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private ExpenseReportAccessService expenseReportAccessService;

    @Test
    void registerOwnershipUsesAuthenticatedLocalUser() {
        AppUser owner = mock(AppUser.class);
        when(appUserRepository.getReferenceById(42L)).thenReturn(owner);

        expenseReportAccessService.registerOwnership("session-123", 42L);

        ArgumentCaptor<ExpenseReport> reportCaptor = ArgumentCaptor.forClass(ExpenseReport.class);
        verify(expenseReportRepository).save(reportCaptor.capture());
        assertEquals("session-123", reportCaptor.getValue().getSessionId());
        assertEquals(owner, reportCaptor.getValue().getOwner());
    }

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
