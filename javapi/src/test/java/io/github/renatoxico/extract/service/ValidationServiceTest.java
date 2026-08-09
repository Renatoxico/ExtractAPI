package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.model.Expense;
import io.github.renatoxico.extract.model.FileValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }


    @Test
    void testValidateFiles_NoFilesProvided_ReturnsNO_FILES_PROVIDED() {
        MultipartFile[] files = new MultipartFile[0];

        FileValidationResult result = validationService.validateFiles(files);

        assertFalse(result.valid());
        assertEquals("NO_FILES_PROVIDED", result.errorCode());
        assertEquals(HttpStatus.BAD_REQUEST, result.httpStatus());
        assertTrue(result.message().contains("No files found"));
    }

    @Test
    void testValidateFiles_TooManyFiles_ReturnsTOO_MANY_FILES() {
        MultipartFile[] files = new MultipartFile[7];
        for (int i = 0; i < 7; i++) {
            files[i] = new MockMultipartFile(
                "file" + i,
                "test" + i + ".pdf",
                "application/pdf",
                "PDF content".getBytes()
            );
        }

        FileValidationResult result = validationService.validateFiles(files);

        assertFalse(result.valid());
        assertEquals("TOO_MANY_FILES", result.errorCode());
        assertEquals(HttpStatus.BAD_REQUEST, result.httpStatus());
        assertTrue(result.message().contains("Too many files"));
    }

    @Test
    void testValidateFiles_InvalidFileType_ReturnsINVALID_FILE_TYPE() {
        MultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "Text content".getBytes()
        );
        MultipartFile[] files = {file};

        FileValidationResult result = validationService.validateFiles(files);

        assertFalse(result.valid());
        assertEquals("INVALID_FILE_TYPE", result.errorCode());
        assertEquals(HttpStatus.BAD_REQUEST, result.httpStatus());
        assertTrue(result.message().contains("not a valid PDF"));
    }

    @Test
    void testValidateFiles_WrongExtension_ReturnsINVALID_FILE_TYPE() {
        MultipartFile file = new MockMultipartFile(
            "file",
            "test.docx",
            "application/pdf",
            "PDF content".getBytes()
        );
        MultipartFile[] files = {file};

        FileValidationResult result = validationService.validateFiles(files);

        assertFalse(result.valid());
        assertEquals("INVALID_FILE_TYPE", result.errorCode());
    }

    @Test
    void testValidateFiles_FileTooLarge_ReturnsFILE_TOO_BIG() {
        byte[] largeContent = new byte[530000];
        MultipartFile file = new MockMultipartFile(
            "file",
            "large.pdf",
            "application/pdf",
            largeContent
        );
        MultipartFile[] files = {file};

        FileValidationResult result = validationService.validateFiles(files);

        assertFalse(result.valid());
        assertEquals("FILE_TOO_BIG", result.errorCode());
        assertEquals(HttpStatus.BAD_REQUEST, result.httpStatus());
        assertTrue(result.message().contains("too large"));
    }

    @Test
    void testValidateFiles_SingleValidPDF_ReturnsSuccess() {
        MultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "PDF content".getBytes()
        );
        MultipartFile[] files = {file};

        FileValidationResult result = validationService.validateFiles(files);

        assertTrue(result.valid());
        assertEquals("OK", result.errorCode());
        assertEquals(HttpStatus.OK, result.httpStatus());
    }

    @Test
    void testValidateFiles_MultipleValidPDFs_ReturnsSuccess() {
        MultipartFile[] files = new MultipartFile[3];
        for (int i = 0; i < 3; i++) {
            files[i] = new MockMultipartFile(
                "file" + i,
                "test" + i + ".pdf",
                "application/pdf",
                "PDF content".getBytes()
            );
        }

        FileValidationResult result = validationService.validateFiles(files);

        assertTrue(result.valid());
        assertEquals("OK", result.errorCode());
    }

    @Test
    void testValidateFiles_MaximumValidFiles_ReturnsSuccess() {
        MultipartFile[] files = new MultipartFile[6];
        for (int i = 0; i < 6; i++) {
            files[i] = new MockMultipartFile(
                "file" + i,
                "test" + i + ".pdf",
                "application/pdf",
                "PDF content".getBytes()
            );
        }

        FileValidationResult result = validationService.validateFiles(files);

        assertTrue(result.valid());
        assertEquals("OK", result.errorCode());
    }


    @Test
    void testValidateExpenses_RemovesNegativeValues() {
        List<Expense> expenses = new ArrayList<>();
        Expense expense1 = new Expense();
        expense1.setExpenseName("Valid Expense");
        expense1.setAmount(new BigDecimal("50.00"));
        expense1.setDate("15/03/2025");

        Expense expense2 = new Expense();
        expense2.setExpenseName("Negative Expense");
        expense2.setAmount(new BigDecimal("-10.00"));
        expense2.setDate("15/03/2025");

        expenses.add(expense1);
        expenses.add(expense2);

        List<Expense> result = validationService.validateExpenses(expenses);

        assertEquals(1, result.size());
        assertEquals("Valid Expense", result.getFirst().getExpenseName());
    }

    @Test
    void testValidateExpenses_RemovesZeroValues() {
        List<Expense> expenses = new ArrayList<>();
        Expense expense = new Expense();
        expense.setExpenseName("Zero Expense");
        expense.setAmount(BigDecimal.ZERO);
        expenses.add(expense);

        List<Expense> result = validationService.validateExpenses(expenses);

        assertEquals(0, result.size());
    }

    @Test
    void testValidateExpenses_RemovesEmptyTransactionNames() {
        List<Expense> expenses = new ArrayList<>();
        Expense expense = new Expense();
        expense.setExpenseName("");
        expense.setAmount(new BigDecimal("50.00"));
        expenses.add(expense);

        List<Expense> result = validationService.validateExpenses(expenses);

        assertEquals(0, result.size());
    }

    @Test
    void testValidateExpenses_PreClassifiesUberExpenses() {
        List<Expense> expenses = new ArrayList<>();
        Expense expense = new Expense();
        expense.setExpenseName("UBER TRIP 10/15");
        expense.setAmount(new BigDecimal("25.50"));
        expense.setDate("10/15/2024");
        expenses.add(expense);

        List<Expense> result = validationService.validateExpenses(expenses);

        assertEquals(1, result.size());
        assertEquals("Transporte / Auto", result.getFirst().getCategory());
    }

    @Test
    void testValidateExpenses_PreClassifiesIfoodExpenses() {
        List<Expense> expenses = new ArrayList<>();
        Expense expense = new Expense();
        expense.setExpenseName("IFOOD RESTAURANT");
        expense.setAmount(new BigDecimal("35.00"));
        expense.setDate("10/15/2024");
        expenses.add(expense);

        List<Expense> result = validationService.validateExpenses(expenses);

        assertEquals(1, result.size());
        assertEquals("Restaurante / Lanches", result.getFirst().getCategory());
    }

    @Test
    void testValidateExpenses_RemovesDateFormats() {
        List<Expense> expenses = new ArrayList<>();
        Expense expense = new Expense();
        expense.setExpenseName("PURCHASE 10/15 ITEM NAME");
        expense.setAmount(new BigDecimal("50.00"));
        expense.setDate("10/15/2024");
        expenses.add(expense);

        List<Expense> result = validationService.validateExpenses(expenses);

        assertEquals(1, result.size());
        assertFalse(result.getFirst().getExpenseName().contains("10/15"));
    }

    @Test
    void testValidateExpenses_LimitNameLength() {
        List<Expense> expenses = new ArrayList<>();
        Expense expense = new Expense();
        String longName = "A".repeat(100);
        expense.setExpenseName(longName);
        expense.setAmount(new BigDecimal("50.00"));
        expense.setDate("10/15/2024");
        expenses.add(expense);

        List<Expense> result = validationService.validateExpenses(expenses);

        assertEquals(1, result.size());
        assertTrue(result.getFirst().getExpenseName().length() <= 70);
    }

    @Test
    void testValidateExpenses_RemovesExtraSpaces() {
        List<Expense> expenses = new ArrayList<>();
        Expense expense = new Expense();
        expense.setExpenseName("PURCHASE   WITH   EXTRA   SPACES");
        expense.setAmount(new BigDecimal("50.00"));
        expense.setDate("10/15/2024");
        expenses.add(expense);

        List<Expense> result = validationService.validateExpenses(expenses);

        assertEquals(1, result.size());
        assertFalse(result.getFirst().getExpenseName().contains("   "));
    }

    @Test
    void testValidateExpenses_MultipleValidExpenses_ReturnsAll() {
        List<Expense> expenses = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Expense expense = new Expense();
            expense.setExpenseName("Expense " + i);
            expense.setAmount(new BigDecimal(String.valueOf(10 + i * 5)));
            expense.setDate("10/15/2024");
            expenses.add(expense);
        }

        List<Expense> result = validationService.validateExpenses(expenses);

        assertEquals(5, result.size());
    }

    @Test
    void testValidateExpenses_EmptyList_ReturnsEmptyList() {
        List<Expense> expenses = new ArrayList<>();

        List<Expense> result = validationService.validateExpenses(expenses);

        assertEquals(0, result.size());
    }
}
