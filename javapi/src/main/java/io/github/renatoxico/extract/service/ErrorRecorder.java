package io.github.renatoxico.extract.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/** Only structural telemetry belongs here. Never pass text, filenames, amounts or exception messages. */
@Service
public class ErrorRecorder {
    public enum Stage { VALIDATION, REPORT_CREATION, PDF_EXTRACTION, PARSING, CATALOG, REPORTING, RAW_TEXT }
    private static final Logger LOG = LoggerFactory.getLogger(ErrorRecorder.class);
    private static final Set<String> CODES = Set.of("NO_FILES_PROVIDED", "TOO_MANY_FILES", "FILE_TOO_BIG",
        "INVALID_FILE_TYPE", "PDF_EXTRACTION_FAILED", "EMPTY_PDF_CONTENT", "NO_EXPENSES_EXTRACTED",
        "NO_VALID_EXPENSES", "FILE_PROCESSING_ERROR", "TEXT_EXTRACTION_ERROR", "NO_TEXT_EXTRACTED",
        "CATALOG_SYNC_FAILED", "REPORT_NOT_FOUND", "REPORT_DATA_INVALID");
    private static final Set<String> COUNT_KEYS = Set.of("fileCount", "candidateCount", "acceptedCount");
    static final String UPSERT = """
        INSERT INTO app_error (code, stage, report_id, correlation_id, metadata, fingerprint)
        VALUES (?, ?, ?, ?, CAST(? AS jsonb), ?)
        ON CONFLICT (fingerprint) DO UPDATE SET
            last_seen = clock_timestamp(), count = app_error.count + 1,
            report_id = EXCLUDED.report_id, correlation_id = EXCLUDED.correlation_id,
            metadata = EXCLUDED.metadata
        """;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final ObjectMapper mapper;

    public ErrorRecorder(JdbcTemplate jdbc, PlatformTransactionManager manager, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void record(String code, Stage stage, String reportId, UUID correlationId, Map<String, ?> metadata) {
        try {
            String safeCode = CODES.contains(code) ? code : "EXTRACTION_FAILED";
            Map<String, Integer> safeMetadata = new TreeMap<>();
            if (metadata != null) metadata.forEach((key, value) -> {
                if (COUNT_KEYS.contains(key) && value instanceof Integer count && count >= 0 && count <= 1000000) {
                    safeMetadata.put(key, count);
                }
            });
            // Group only by stable failure class, never by user/financial content or exception text.
            String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest((safeCode + ":" + stage.name()).getBytes(StandardCharsets.UTF_8)));
            String json = mapper.writeValueAsString(safeMetadata);
            String safeReport = reportId != null && reportId.matches("[a-z0-9_-]{32}") ? reportId : null;
            transaction.executeWithoutResult(status -> jdbc.update(UPSERT, safeCode, stage.name(), safeReport,
                correlationId == null ? UUID.randomUUID() : correlationId, json, fingerprint));
        } catch (Exception failure) {
            // Do not log the exception: JDBC diagnostics can contain parameter values.
            LOG.warn("Could not persist extraction error ({})", failure.getClass().getSimpleName());
        }
    }
}
