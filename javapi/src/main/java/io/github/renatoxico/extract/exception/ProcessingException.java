package io.github.renatoxico.extract.exception;

import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.renatoxico.extract.logging.LogFields;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProcessingException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String errorCode;
    private final Map<String, String> diagnostics = new LinkedHashMap<>();

    public ProcessingException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public ProcessingException(String message, HttpStatus httpStatus, String errorCode, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        if (cause instanceof ProcessingException processing) {
            diagnostics.putAll(processing.getDiagnostics());
        }
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public ProcessingException withDiagnostic(String key, Object value) {
        diagnostics.put(key, LogFields.safe(value));
        return this;
    }

    public ProcessingException withDiagnostics(Map<String, ?> fields) {
        fields.forEach(this::withDiagnostic);
        return this;
    }

    @JsonIgnore
    public Map<String, String> getDiagnostics() {
        return Collections.unmodifiableMap(diagnostics);
    }
}

