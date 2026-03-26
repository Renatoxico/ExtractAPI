package com.example.api.service;

import com.example.api.model.Expense;
import com.example.api.model.ValidationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }

    // ========== File Validation Tests ==========

    @Test
    void testValidateFiles_NoFilesProvided_ReturnsNO_FILES_PROVIDED() {
        // Arrange
        MultipartFile[] files = new MultipartFile[0];

        // Act
        ValidationResponse result = validationService.validateFiles(files);

        // Assert
        assertFalse(result.getStatus());
        assertEquals("NO_FILES_PROVIDED", result.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, result.getHttpStatus());
        assertTrue(result.getMessage().contains("No files found"));
    }

    @Test
    void testValidateFiles_TooManyFiles_ReturnsTOO_MANY_FILES() {
        // Arrange
        MultipartFile[] files = new MultipartFile[7];
        for (int i = 0; i < 7; i++) {
            files[i] = new MockMultipartFile(
                "file" + i,
                "test" + i + ".pdf",
                "application/pdf",
                "PDF content".getBytes()
            );
        }

        // Act
        ValidationResponse result = validationService.validateFiles(files);

        // Assert
        assertFalse(result.getStatus());
        assertEquals("TOO_MANY_FILES", result.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, result.getHttpStatus());
        assertTrue(result.getMessage().contains("Too many files"));
    }

    @Test
    void testValidateFiles_InvalidFileType_ReturnsINVALID_FILE_TYPE() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "Text content".getBytes()
        );
        MultipartFile[] files = {file};

        // Act
        ValidationResponse result = validationService.validateFiles(files);

        // Assert
        assertFalse(result.getStatus());
        assertEquals("INVALID_FILE_TYPE", result.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, result.getHttpStatus());
        assertTrue(result.getMessage().contains("not a valid PDF"));
    }

    @Test
    void testValidateFiles_WrongExtension_ReturnsINVALID_FILE_TYPE() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "file",
            "test.docx",
            "application/pdf",  // Wrong: content type says PDF but extension is .docx
            "PDF content".getBytes()
        );
        MultipartFile[] files = {file};

        // Act
        ValidationResponse result = validationService.validateFiles(files);

        // Assert
        assertFalse(result.getStatus());
        assertEquals("INVALID_FILE_TYPE", result.getErrorCode());
    }

    @Test
    void testValidateFiles_FileTooLarge_ReturnsFILE_TOO_BIG() {
        // Arrange
        // Create a file larger than 512KB (524288 bytes)
        byte[] largeContent = new byte[530000];
        MultipartFile file = new MockMultipartFile(
            "file",
            "large.pdf",
            "application/pdf",
            largeContent
        );
        MultipartFile[] files = {file};

        // Act
        ValidationResponse result = validationService.validateFiles(files);

        // Assert
        assertFalse(result.getStatus());
        assertEquals("FILE_TOO_BIG", result.getErrorCode());
        assertEquals(HttpStatus.BAD_REQUEST, result.getHttpStatus());
        assertTrue(result.getMessage().contains("too large"));
    }

    @Test
    void testValidateFiles_SingleValidPDF_ReturnsSuccess() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "PDF content".getBytes()
        );
        MultipartFile[] files = {file};

        // Act
        ValidationResponse result = validationService.validateFiles(files);

        // Assert
        assertTrue(result.getStatus());
        assertEquals("OK", result.getErrorCode());
        assertEquals(HttpStatus.OK, result.getHttpStatus());
    }

    @Test
    void testValidateFiles_MultipleValidPDFs_ReturnsSuccess() {
        // Arrange
        MultipartFile[] files = new MultipartFile[3];
        for (int i = 0; i < 3; i++) {
            files[i] = new MockMultipartFile(
                "file" + i,
                "test" + i + ".pdf",
                "application/pdf",
                "PDF content".getBytes()
            );
        }

        // Act
        ValidationResponse result = validationService.validateFiles(files);

        // Assert
        assertTrue(result.getStatus());
        assertEquals("OK", result.getErrorCode());
    }

    @Test
    void testValidateFiles_MaximumValidFiles_ReturnsSuccess() {
        // Arrange: 6 files is the maximum
        MultipartFile[] files = new MultipartFile[6];
        for (int i = 0; i < 6; i++) {
            files[i] = new MockMultipartFile(
                "file" + i,
                "test" + i + ".pdf",
                "application/pdf",
                "PDF content".getBytes()
            );
        }

        // Act
        ValidationResponse result = validationService.validateFiles(files);

        // Assert
        assertTrue(result.getStatus());
        assertEquals("OK", result.getErrorCode());
    }

    // ========== Expense Validation Tests ==========

    @Test
    void testValidateExpenses_RemovesNegativeValues() {
        // Arrange
        List<Expense> expenses = new ArrayList<>();
        Expense expense1 = new Expense();
        expense1.setTransactionName("Valid Expense");
        expense1.setValue(new BigDecimal("50.00"));
        expense1.setDate("15/03/2025");

        Expense expense2 = new Expense();
        expense2.setTransactionName("Negative Expense");
        expense2.setValue(new BigDecimal("-10.00"));
        expense2.setDate("15/03/2025");

        expenses.add(expense1);
        expenses.add(expense2);

        // Act
        List<Expense> result = validationService.validateExpenses(expenses);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Valid Expense", result.getFirst().getTransactionName());
    }

    @Test
    void testValidateExpenses_RemovesZeroValues() {
        // Arrange
        List<Expense> expenses = new ArrayList<>();
        Expense expense = new Expense();
        expense.setTransactionName("Zero Expense");
        expense.setValue(BigDecimal.ZERO);
        expenses.add(expense);

        // Act
        List<Expense> result = validationService.validateExpenses(expenses);

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    void testValidateExpenses_RemovesEmptyTransactionNames() {
        // Arrange
        List<Expense> expenses = new ArrayList<>();
        Expense expense = new Expense();
        expense.setTransactionName("");
        expense.setValue(new BigDecimal("50.00"));
        expenses.add(expense);

        // Act
        List<Expense> result = validationService.validateExpenses(expenses);

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    void testValidateExpenses_PreClassifiesUberExpenses() {
        // Arrange
        List<Expense> expenses = new ArrayList<>();
        Expense expense = new Expense();
        expense.setTransactionName("UBER TRIP 10/15");
        expense.setValue(new BigDecimal("25.50"));
        expense.setDate("10/15/2024");
        expenses.add(expense);

        // Act
        List<Expense> result = validationService.validateExpenses(expenses);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Transporte / Auto", result.getFirst().getTransactionType());
    }

    @Test
    void testValidateExpenses_PreClassifiesIfoodExpenses() {
        // Arrange
        List<Expense> expenses = new ArrayList<>();
        Expense expense = new Expense();
        expense.setTransactionName("IFOOD RESTAURANT");
        expense.setValue(new BigDecimal("35.00"));
        expense.setDate("10/15/2024");
        expenses.add(expense);

        // Act
        List<Expense> result = validationService.validateExpenses(expenses);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Restaurante / Lanches", result.getFirst().getTransactionType());
    }

    @Test
    void testValidateExpenses_RemovesDateFormats() {
        // Arrange
        List<Expense> expenses = new ArrayList<>();
        Expense expense = new Expense();
        expense.setTransactionName("PURCHASE 10/15 ITEM NAME");
        expense.setValue(new BigDecimal("50.00"));
        expense.setDate("10/15/2024");
        expenses.add(expense);

        // Act
        List<Expense> result = validationService.validateExpenses(expenses);

        // Assert
        assertEquals(1, result.size());
        // Date pattern should be removed from name
        assertFalse(result.getFirst().getTransactionName().contains("10/15"));
    }

    @Test
    void testValidateExpenses_LimitNameLength() {
        // Arrange
        List<Expense> expenses = new ArrayList<>();
        Expense expense = new Expense();
        String longName = "A".repeat(100); // Create a name longer than 70 chars
        expense.setTransactionName(longName);
        expense.setValue(new BigDecimal("50.00"));
        expense.setDate("10/15/2024");
        expenses.add(expense);

        // Act
        List<Expense> result = validationService.validateExpenses(expenses);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.getFirst().getTransactionName().length() <= 70);
    }

    @Test
    void testValidateExpenses_RemovesExtraSpaces() {
        // Arrange
        List<Expense> expenses = new ArrayList<>();
        Expense expense = new Expense();
        expense.setTransactionName("PURCHASE   WITH   EXTRA   SPACES");
        expense.setValue(new BigDecimal("50.00"));
        expense.setDate("10/15/2024");
        expenses.add(expense);

        // Act
        List<Expense> result = validationService.validateExpenses(expenses);

        // Assert
        assertEquals(1, result.size());
        assertFalse(result.getFirst().getTransactionName().contains("   "));
    }

    @Test
    void testValidateExpenses_MultipleValidExpenses_ReturnsAll() {
        // Arrange
        List<Expense> expenses = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Expense expense = new Expense();
            expense.setTransactionName("Expense " + i);
            expense.setValue(new BigDecimal(String.valueOf(10 + i * 5)));
            expense.setDate("10/15/2024");
            expenses.add(expense);
        }

        // Act
        List<Expense> result = validationService.validateExpenses(expenses);

        // Assert
        assertEquals(5, result.size());
    }

    @Test
    void testValidateExpenses_EmptyList_ReturnsEmptyList() {
        // Arrange
        List<Expense> expenses = new ArrayList<>();

        // Act
        List<Expense> result = validationService.validateExpenses(expenses);

        // Assert
        assertEquals(0, result.size());
    }
}

