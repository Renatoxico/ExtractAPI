package com.example.api.service;

import com.example.api.model.Expense;
import com.example.api.repo.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ObjectifierServiceTest {

    ExpenseRepository expenseRepo;

    ValidationService validationService;

    ObjectifierService service;

    @BeforeEach
    void setup() {
        // mock only the repository (interface) to verify saveAll calls; use a real ValidationService
        expenseRepo = mock(ExpenseRepository.class);
        validationService = new ValidationService();
        service = new ObjectifierService(expenseRepo, validationService);
    }

    @Test
    void splitByLine_splitsByNewlineCorrectly() {
        String input = "line1\nline2\nline3";
        String[] result = service.splitByLine(input);
        assertArrayEquals(new String[]{"line1", "line2", "line3"}, result);
    }

    @Test
    void objectifyExtract_parsesDateAndValue_andProducesExpense() {
        // line: date at start, description between date and value, value in BR format
        String line = "12/03/2025 IFD*1234 Some Merchant 1.234,56";
        List<String> input = List.of(line);

        // call method
        List<Expense> expenses = service.objectifyExtract("sess-1", input);

        assertNotNull(expenses);
        assertEquals(1, expenses.size());
        Expense e = expenses.get(0);
        assertEquals("sess-1", e.getSessionId());
        assertEquals(new BigDecimal("1234.56"), e.getValue());
        // transactionName is cleaned by the service (digits removed), so assert contains IFD
        assertTrue(e.getTransactionName().toUpperCase().contains("IFD"));
        assertEquals("12/03/2025", e.getDate());
    }

    @Test
    void objectifyExtract_ignoresLinesWithoutLettersOrWithBlacklistedKeywordsOrZeroValue() {
        // line with only digits -> skipped, line with keyword SALDO -> skipped, zero value -> skipped
        List<String> input = List.of(
                "01/01/2025 123456 1.000,00",     // no letters
                "02/02/2025 SALDO 500,00",       // contains SALDO -> ignored
                "03/03/2025 SomeText 0,00"       // zero value -> ignored
        );

        List<Expense> res = service.objectifyExtract("sess-2", input);

        // should not produce any Expense objects
        assertTrue(res.isEmpty());
    }

    @Test
    void process_delegatesToValidationAndSavesValidatedExpenses() {
        // prepare a parsed list to be returned by the stubbed objectifyExtract
        Expense parsedExpense = new Expense("sess-xyz", new BigDecimal("10.00"), "Some Merchant", "12/03/2025", "");
        List<Expense> parsed = List.of(parsedExpense);

        // use a validation service stub that returns the same parsed list
        ValidationService valStub = new ValidationService() {
            @Override
            public List<Expense> validateExpenses(List<Expense> expenses) {
                return parsed;
            }
        };

        // use a new ObjectifierService wired with the mocked repo and our valStub, overriding objectifyExtract
        ObjectifierService sut = new ObjectifierService(expenseRepo, valStub) {
            @Override
            public List<Expense> objectifyExtract(String sessionId, List<String> expenses) {
                return parsed;
            }
        };

        // run process
        sut.process("session-xyz", "ignored input");

        // verify expenseRepo.saveAll was called with the validated list
        verify(expenseRepo, times(1)).saveAll(parsed);
    }
}
