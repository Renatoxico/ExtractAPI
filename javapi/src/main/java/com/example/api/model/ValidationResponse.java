package com.example.api.model;

import org.springframework.http.HttpStatus;

public class ValidationResponse {
    private boolean status;
    private String message;
    private String errorCode;
    private HttpStatus httpStatus;

    public ValidationResponse(boolean status, String message) {
        this.status = status;
        this.message = message;
        this.errorCode = status ? "OK" : "VALIDATION_FAILED";
        this.httpStatus = status ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
    }

    public ValidationResponse(boolean status, String message, String errorCode, HttpStatus httpStatus) {
        this.status = status;
        this.message = message;
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }
}
