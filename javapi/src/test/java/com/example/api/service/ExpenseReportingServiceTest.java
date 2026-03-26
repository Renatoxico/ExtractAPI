package com.example.api.service;

import com.example.api.exception.ProcessingException;
import com.example.api.model.Expense;
import com.example.api.repo.ExpenseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseReportingServiceTest {

    @Mock
    private ExpenseRepository expenseRepo;

    @Mock
    private ValidationService validationService;

    @InjectMocks
    private ExpenseReportingService service;

    private static List<Object[]> listOf(Object[]... items) {
        List<Object[]> list = new ArrayList<>();
        Collections.addAll(list, items);
        return list;
    }

    // --- generateId ---

    @Test
    void shouldGenerateNonNullId() {
        String id = service.generateId();
        assertThat(id).isNotNull().isNotBlank();
    }

    @Test
    void shouldGenerateUniqueIds() {
        String id1 = service.generateId();
        String id2 = service.generateId();
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void shouldGenerateUrlSafeId() {
        String id = service.generateId();
        assertThat(id).matches("[a-z0-9_-]+");
    }

    @Test
    void shouldGenerateSufficientEntropy() {
        // 24 bytes -> 32 base64 chars
        String id = service.generateId();
        assertThat(id.length()).isGreaterThanOrEqualTo(20);
    }

    // --- getFullReport ---

    @Test
    void shouldReturnCompleteReportWithAllSections() {
        String sessionId = "test-session";

        when(expenseRepo.getGroupedExpenses(sessionId)).thenReturn(
                listOf(new Object[]{"SUPERMERCADO", new BigDecimal("150.00"), 3L, "Supermercado"})
        );
        when(expenseRepo.getNoteableDays(sessionId)).thenReturn(
                listOf(new Object[]{"15/03/2025", 5L, new BigDecimal("500.00")})
        );
        when(expenseRepo.getAllExpenses(sessionId)).thenReturn(
                listOf(new Object[]{"SUPERMERCADO", "15/03/2025", new BigDecimal("150.00"), "Supermercado"})
        );
        when(expenseRepo.getExpensesByType(sessionId)).thenReturn(
                listOf(new Object[]{new BigDecimal("150.00"), "Supermercado"})
        );
        when(expenseRepo.getBiggestExpense(sessionId)).thenReturn(
                listOf(new Object[]{"TV SAMSUNG", "20/03/2025", new BigDecimal("3500.00"), "E-commerce"})
        );

        Map<String, Object> report = service.getFullReport(sessionId);

        assertThat(report).containsKeys(
                "SmartGroupExpenselist", "NotableDays", "AllExpenses",
                "ExpensesByCategory", "BiggestSingularExpense"
        );
        assertThat(report.get("BiggestSingularExpense")).isNotNull();
    }

    @Test
    void shouldHandleEmptyBiggestExpense() {
        String sessionId = "test-session";

        when(expenseRepo.getGroupedExpenses(sessionId)).thenReturn(Collections.emptyList());
        when(expenseRepo.getNoteableDays(sessionId)).thenReturn(Collections.emptyList());
        when(expenseRepo.getAllExpenses(sessionId)).thenReturn(Collections.emptyList());
        when(expenseRepo.getExpensesByType(sessionId)).thenReturn(Collections.emptyList());
        when(expenseRepo.getBiggestExpense(sessionId)).thenReturn(Collections.emptyList());

        Map<String, Object> report = service.getFullReport(sessionId);

        assertThat(report.get("BiggestSingularExpense")).isNull();
    }

    // --- exportReportCSV ---

    @Test
    void shouldExportValidCSVWithHeaders() {
        String sessionId = "test-session";
        Expense expense = new Expense(sessionId, new BigDecimal("99.90"), "UBER", "15/03/2025", "Transporte");
        when(expenseRepo.getAllExpenses2(sessionId)).thenReturn(List.of(expense));

        byte[] csv = service.exportReportCSV(sessionId);
        String csvContent = new String(csv);

        assertThat(csvContent).contains("\"id\"");
        assertThat(csvContent).contains("\"transactionName\"");
        assertThat(csvContent).contains("\"UBER\"");
        assertThat(csvContent).contains("\"99.90\"");
    }

    @Test
    void shouldThrowWhenNoExpensesForExport() {
        when(expenseRepo.getAllExpenses2("empty")).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.exportReportCSV("empty"))
                .isInstanceOf(ProcessingException.class)
                .satisfies(ex -> {
                    ProcessingException pe = (ProcessingException) ex;
                    assertThat(pe.getErrorCode()).isEqualTo("SESSION_NOT_FOUND");
                });
    }

    @Test
    void shouldThrowWhenExpensesNull() {
        when(expenseRepo.getAllExpenses2("null-session")).thenReturn(null);

        assertThatThrownBy(() -> service.exportReportCSV("null-session"))
                .isInstanceOf(ProcessingException.class);
    }

    @Test
    void shouldHandleExpensesWithNullFields() {
        String sessionId = "test-session";
        Expense expense = new Expense(sessionId, null, null, null, null);
        when(expenseRepo.getAllExpenses2(sessionId)).thenReturn(List.of(expense));

        byte[] csv = service.exportReportCSV(sessionId);
        String csvContent = new String(csv);

        // Should use empty strings for null fields
        assertThat(csvContent).contains("\"\"");
    }
}
