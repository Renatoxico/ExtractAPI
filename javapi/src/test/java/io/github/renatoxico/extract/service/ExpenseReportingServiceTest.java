package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.model.Expense;
import io.github.renatoxico.extract.model.ExpenseDTO;
import io.github.renatoxico.extract.model.ExpensesCategories;
import io.github.renatoxico.extract.model.ExpensesGroupedDTO;
import io.github.renatoxico.extract.model.NoteableDay;
import io.github.renatoxico.extract.model.ReportExport;
import io.github.renatoxico.extract.repo.ExpenseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseReportingServiceTest {

    @Mock
    private ExpenseRepository expenseRepo;

    @Mock
    private ValidationService validationService;

    @InjectMocks
    private ExpenseReportingService service;

    // --- generateId ---

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

        List<ExpensesGroupedDTO> groupedExpenses = List.of(
                new ExpensesGroupedDTO("SUPERMERCADO", new BigDecimal("150.00"), 3L, "Supermercado")
        );
        List<NoteableDay> notableDays = List.of(
                new NoteableDay("15/03/2025", 5L, new BigDecimal("500.00"))
        );
        List<ExpenseDTO> allExpenses = List.of(
                new ExpenseDTO("TV SAMSUNG", new BigDecimal("3500.00"), "20/03/2025", "E-commerce"),
                new ExpenseDTO("SUPERMERCADO", new BigDecimal("150.00"), "15/03/2025", "Supermercado")
        );
        List<ExpensesCategories> expensesByType = List.of(
                new ExpensesCategories(new BigDecimal("150.00"), "Supermercado")
        );

        when(expenseRepo.getGroupedExpenses(sessionId)).thenReturn(groupedExpenses);
        when(expenseRepo.getNoteableDays(sessionId)).thenReturn(notableDays);
        when(expenseRepo.getAllExpenses(sessionId)).thenReturn(allExpenses);
        when(expenseRepo.getExpensesByType(sessionId)).thenReturn(expensesByType);

        ReportExport report = service.getFullReport(sessionId);

        assertThat(report)
                .extracting(
                        "smartGroupExpenselist",
                        "notableDays",
                        "allExpenses",
                        "expensesByCategory",
                        "biggestSingularExpense",
                        "sessionToken"
                )
                .containsExactly(
                        groupedExpenses,
                        notableDays,
                        allExpenses,
                        expensesByType,
                        allExpenses.getFirst(),
                        sessionId
                );
        verify(expenseRepo).getAllExpenses(sessionId);
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
