package io.github.renatoxico.extract.exception;

import org.springframework.http.HttpStatus;

public class ProcessingException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String errorCode;

    public ProcessingException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public ProcessingException(String message, HttpStatus httpStatus, String errorCode, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

