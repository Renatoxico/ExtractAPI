package io.github.renatoxico.extract.model;

import org.springframework.http.HttpStatus;

public record FileValidationResult(
    boolean valid,
    String message,
    String errorCode,
    HttpStatus httpStatus
) {
    public FileValidationResult(boolean valid, String message) {
        this(
            valid,
            message,
            valid ? "OK" : "VALIDATION_FAILED",
            valid ? HttpStatus.OK : HttpStatus.BAD_REQUEST
        );
    }
}
