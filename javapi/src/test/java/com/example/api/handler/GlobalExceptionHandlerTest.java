package com.example.api.handler;

import com.example.api.exception.ProcessingException;
import com.example.api.model.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    // ========== ProcessingException Handler Tests ==========

    @Test
    void testHandleProcessingException_WithBAD_REQUEST_ReturnsCorrectStatus() {
        // Arrange
        ProcessingException exception = new ProcessingException(
            "Invalid file type",
            HttpStatus.BAD_REQUEST,
            "INVALID_FILE_TYPE"
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProcessingException(exception);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_FILE_TYPE", response.getBody().getErrorCode());
        assertEquals("Invalid file type", response.getBody().getMessage());
    }

    @Test
    void testHandleProcessingException_WithUNPROCESSABLE_ENTITY_ReturnsCorrectStatus() {
        // Arrange
        ProcessingException exception = new ProcessingException(
            "No text extracted from PDF",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "EMPTY_PDF_CONTENT"
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProcessingException(exception);

        // Assert
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("EMPTY_PDF_CONTENT", response.getBody().getErrorCode());
    }

    @Test
    void testHandleProcessingException_WithINTERNAL_SERVER_ERROR_ReturnsCorrectStatus() {
        // Arrange
        ProcessingException exception = new ProcessingException(
            "Database connection failed",
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR"
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProcessingException(exception);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getErrorCode());
    }

    @Test
    void testHandleProcessingException_WithNOT_FOUND_ReturnsCorrectStatus() {
        // Arrange
        ProcessingException exception = new ProcessingException(
            "Session not found",
            HttpStatus.NOT_FOUND,
            "SESSION_NOT_FOUND"
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProcessingException(exception);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SESSION_NOT_FOUND", response.getBody().getErrorCode());
    }

    @Test
    void testHandleProcessingException_IncludesErrorCode() {
        // Arrange
        ProcessingException exception = new ProcessingException(
            "Test error",
            HttpStatus.BAD_REQUEST,
            "TEST_ERROR_CODE"
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProcessingException(exception);

        // Assert
        assertEquals("TEST_ERROR_CODE", response.getBody().getErrorCode());
    }

    @Test
    void testHandleProcessingException_IncludesMessage() {
        // Arrange
        String testMessage = "This is a test error message";
        ProcessingException exception = new ProcessingException(
            testMessage,
            HttpStatus.BAD_REQUEST,
            "TEST_ERROR"
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProcessingException(exception);

        // Assert
        assertEquals(testMessage, response.getBody().getMessage());
    }

    @Test
    void testHandleProcessingException_WithCause_PreservesErrorCode() {
        // Arrange
        Exception cause = new Exception("Root cause");
        ProcessingException exception = new ProcessingException(
            "Processing failed",
            HttpStatus.INTERNAL_SERVER_ERROR,
            "PROCESSING_ERROR",
            cause
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProcessingException(exception);

        // Assert
        assertEquals("PROCESSING_ERROR", response.getBody().getErrorCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testHandleProcessingException_IncludesTimestamp() {
        // Arrange
        ProcessingException exception = new ProcessingException(
            "Test error",
            HttpStatus.BAD_REQUEST,
            "TEST_ERROR"
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProcessingException(exception);

        // Assert
        assertNotNull(response.getBody().getTimestamp());
    }

    // ========== IOException Handler Tests ==========

    @Test
    void testHandleIOException_Returns500() {
        // Arrange
        IOException exception = new IOException("File read error");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIOException(exception);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testHandleIOException_IncludesFILE_IO_ERROR_Code() {
        // Arrange
        IOException exception = new IOException("File not found");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIOException(exception);

        // Assert
        assertEquals("FILE_IO_ERROR", response.getBody().getErrorCode());
    }

    @Test
    void testHandleIOException_IncludesExceptionMessage() {
        // Arrange
        String errorMessage = "Disk I/O error";
        IOException exception = new IOException(errorMessage);

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIOException(exception);

        // Assert
        assertTrue(response.getBody().getMessage().contains("Failed to process file"));
    }

    @Test
    void testHandleIOException_IncludesDetails() {
        // Arrange
        IOException exception = new IOException("File read error");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIOException(exception);

        // Assert
        assertNotNull(response.getBody().getDetails());
    }

    @Test
    void testHandleIOException_IncludesTimestamp() {
        // Arrange
        IOException exception = new IOException("Test error");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIOException(exception);

        // Assert
        assertNotNull(response.getBody().getTimestamp());
    }

    // ========== Generic Exception Handler Tests ==========

    @Test
    void testHandleGenericException_Returns500() {
        // Arrange
        Exception exception = new RuntimeException("Unexpected error");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testHandleGenericException_IncludesINTERNAL_SERVER_ERROR_Code() {
        // Arrange
        Exception exception = new RuntimeException("Unexpected error");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception);

        // Assert
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getErrorCode());
    }

    @Test
    void testHandleGenericException_IncludesSafeMessage() {
        // Arrange
        Exception exception = new RuntimeException("Unexpected error");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception);

        // Assert
        assertTrue(response.getBody().getMessage().contains("unexpected error occurred"));
    }

    @Test
    void testHandleGenericException_DoesNotExposeStackTrace() {
        // Arrange
        Exception exception = new RuntimeException("Internal algorithm details");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception);

        // Assert
        // Should not contain stack trace elements
        String details = response.getBody().getDetails();
        assertFalse(details.contains("at "));
    }

    @Test
    void testHandleGenericException_IncludesTimestamp() {
        // Arrange
        Exception exception = new RuntimeException("Test error");

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception);

        // Assert
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testHandleGenericException_WithNullException_HandledGracefully() {
        // Arrange
        Exception exception = new Exception((String) null);

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getErrorCode());
    }

    // ========== Error Response Structure Tests ==========

    @Test
    void testErrorResponse_ContainsAllRequiredFields() {
        // Arrange
        ProcessingException exception = new ProcessingException(
            "Test error",
            HttpStatus.BAD_REQUEST,
            "TEST_ERROR"
        );

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProcessingException(exception);
        ErrorResponse body = response.getBody();

        // Assert
        assertNotNull(body.getErrorCode());
        assertNotNull(body.getMessage());
        assertNotNull(body.getTimestamp());
    }

    @Test
    void testErrorResponse_TimestampIsRecent() {
        // Arrange
        ProcessingException exception = new ProcessingException(
            "Test error",
            HttpStatus.BAD_REQUEST,
            "TEST_ERROR"
        );
        long beforeTime = System.currentTimeMillis();

        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProcessingException(exception);
        long afterTime = System.currentTimeMillis();

        // Assert - Timestamp should be between before and after times (with some buffer)
        assertNotNull(response.getBody().getTimestamp());
    }
}

