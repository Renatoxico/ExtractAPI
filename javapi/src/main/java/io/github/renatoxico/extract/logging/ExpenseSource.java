package io.github.renatoxico.extract.logging;

import java.util.LinkedHashMap;
import java.util.Map;

/** Request-local provenance; never persisted or included in an API response. */
public record ExpenseSource(String filename, String reportId, int lineNumber,
                            String sourceLine, String candidate) {
    public Map<String, Object> diagnostics() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("filename", filename);
        fields.put("reportId", reportId);
        fields.put("lineNumber", lineNumber);
        fields.put("sourceLine", sourceLine);
        fields.put("candidate", candidate);
        return fields;
    }
}
