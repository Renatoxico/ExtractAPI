package io.github.renatoxico.extract.handler;

import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.model.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }


    @Test
    void testHandleProcessingException_WithBAD_REQUEST_ReturnsCorrectStatus() {
        ProcessingException exception = new ProcessingException(
            "Invalid file type",
            HttpStatus.BAD_REQUEST,
            "INVALID_FILE_TYPE"
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProcessingException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_FILE_TYPE", response.getBody().getErrorCode());
        assertEquals("Invalid file type", response.getBody().getMessage());
    }

    @Test
    void testHandleProcessingException_WithUNPROCESSABLE_ENTITY_ReturnsCorrectStatus() {
        ProcessingException exception = new ProcessingException(
            "No text extracted from PDF",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "EMPTY_PDF_CONTENT"
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProcessingException(exception);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("EMPTY_PDF_CONTENT", response.getBody().getErrorCode());
    }

    @Test
    void testHandleProcessingException_WithINTERNAL_SERVER_ERROR_ReturnsCorrectStatus() {
        ProcessingException exception = new ProcessingException(
            "Database connection failed",
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR"
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProcessingException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getErrorCode());
    }

    @Test
    void testHandleProcessingException_WithNOT_FOUND_ReturnsCorrectStatus() {
        ProcessingException exception = new ProcessingException(
            "Session not found",
            HttpStatus.NOT_FOUND,
            "SESSION_NOT_FOUND"
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProcessingException(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SESSION_NOT_FOUND", response.getBody().getErrorCode());
    }

    @Test
    void testHandleProcessingException_IncludesErrorCode() {
        ProcessingException exception = new ProcessingException(
            "Test error",
            HttpStatus.BAD_REQUEST,
            "TEST_ERROR_CODE"
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProcessingException(exception);

        assertEquals("TEST_ERROR_CODE", response.getBody().getErrorCode());
    }

    @Test
    void testHandleProcessingException_IncludesMessage() {
        String testMessage = "This is a test error message";
        ProcessingException exception = new ProcessingException(
            testMessage,
            HttpStatus.BAD_REQUEST,
            "TEST_ERROR"
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProcessingException(exception);

        assertEquals(testMessage, response.getBody().getMessage());
    }

    @Test
    void testHandleProcessingException_WithCause_PreservesErrorCode() {
        Exception cause = new Exception("Root cause");
        ProcessingException exception = new ProcessingException(
            "Processing failed",
            HttpStatus.INTERNAL_SERVER_ERROR,
            "PROCESSING_ERROR",
            cause
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProcessingException(exception);

        assertEquals("PROCESSING_ERROR", response.getBody().getErrorCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testHandleProcessingException_IncludesTimestamp() {
        ProcessingException exception = new ProcessingException(
            "Test error",
            HttpStatus.BAD_REQUEST,
            "TEST_ERROR"
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProcessingException(exception);

        assertNotNull(response.getBody().getTimestamp());
    }


    @Test
    void testHandleIOException_Returns500() {
        IOException exception = new IOException("File read error");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIOException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testHandleIOException_IncludesFILE_IO_ERROR_Code() {
        IOException exception = new IOException("File not found");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIOException(exception);

        assertEquals("FILE_IO_ERROR", response.getBody().getErrorCode());
    }

    @Test
    void testHandleIOException_IncludesExceptionMessage() {
        String errorMessage = "Disk I/O error";
        IOException exception = new IOException(errorMessage);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIOException(exception);

        assertTrue(response.getBody().getMessage().contains("Failed to process file"));
    }

    @Test
    void testHandleIOException_DoesNotLeakExceptionDetails() {
        IOException exception = new IOException("File read error");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIOException(exception);

        assertNull(response.getBody().getDetails());
    }

    @Test
    void testHandleIOException_IncludesTimestamp() {
        IOException exception = new IOException("Test error");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIOException(exception);

        assertNotNull(response.getBody().getTimestamp());
    }


    @Test
    void testHandleGenericException_Returns500() {
        Exception exception = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void testHandleGenericException_IncludesINTERNAL_SERVER_ERROR_Code() {
        Exception exception = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception);

        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getErrorCode());
    }

    @Test
    void testHandleGenericException_IncludesSafeMessage() {
        Exception exception = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception);

        assertTrue(response.getBody().getMessage().contains("unexpected error occurred"));
    }

    @Test
    void testHandleGenericException_DoesNotExposeInternalDetails() {
        Exception exception = new RuntimeException("Internal algorithm details");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception);

        assertNull(response.getBody().getDetails());
    }

    @Test
    void testHandleGenericException_IncludesTimestamp() {
        Exception exception = new RuntimeException("Test error");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception);

        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void testHandleGenericException_WithNullException_HandledGracefully() {
        Exception exception = new Exception((String) null);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getErrorCode());
    }


    @Test
    void testErrorResponse_ContainsAllRequiredFields() {
        ProcessingException exception = new ProcessingException(
            "Test error",
            HttpStatus.BAD_REQUEST,
            "TEST_ERROR"
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleProcessingException(exception);
        ErrorResponse body = response.getBody();

        assertNotNull(body.getErrorCode());
        assertNotNull(body.getMessage());
        assertNotNull(body.getTimestamp());
    }

}
