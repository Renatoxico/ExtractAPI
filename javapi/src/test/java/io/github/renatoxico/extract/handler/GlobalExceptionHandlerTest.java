package io.github.renatoxico.extract.handler;

import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.model.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void processingExceptionPreservesStatusCodeAndMessage() {
        ProcessingException exception = new ProcessingException(
            "Invalid file type",
            HttpStatus.BAD_REQUEST,
            "INVALID_FILE_TYPE"
        );

        ResponseEntity<ErrorResponse> response = handler.handleProcessingException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_FILE_TYPE", response.getBody().getErrorCode());
        assertEquals("Invalid file type", response.getBody().getMessage());
    }

    @Test
    void ioExceptionReturnsSafeError() {
        ResponseEntity<ErrorResponse> response = handler.handleIOException(
            new IOException("Sensitive file-system detail")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("FILE_IO_ERROR", response.getBody().getErrorCode());
        assertEquals("Failed to process file", response.getBody().getMessage());
        assertNull(response.getBody().getDetails());
    }

    @Test
    void genericExceptionDoesNotLeakDetails() {
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(
            new RuntimeException("Sensitive internal algorithm detail")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getErrorCode());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
        assertNull(response.getBody().getDetails());
    }

    @Test
    void errorResponseIncludesTimestamp() {
        ProcessingException exception = new ProcessingException(
            "Test error",
            HttpStatus.BAD_REQUEST,
            "TEST_ERROR"
        );

        ErrorResponse body = handler.handleProcessingException(exception).getBody();

        assertNotNull(body);
        assertNotNull(body.getTimestamp());
    }

    @ParameterizedTest
    @EnumSource(
        value = HttpStatus.class,
        names = {"BAD_REQUEST", "UNPROCESSABLE_ENTITY", "NOT_FOUND", "INTERNAL_SERVER_ERROR"}
    )
    void processingExceptionUsesRelevantHttpStatus(HttpStatus status) {
        ProcessingException exception = new ProcessingException("Error", status, "TEST_ERROR");

        ResponseEntity<ErrorResponse> response = handler.handleProcessingException(exception);

        assertEquals(status, response.getStatusCode());
    }
}
