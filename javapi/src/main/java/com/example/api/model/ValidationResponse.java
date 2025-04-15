package com.example.api.model;

public class ValidationResponse {
    private boolean status;
    private String message;

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

    public ValidationResponse(boolean status, String message) {
        this.status = status;
        this.message = message;
    }
}
