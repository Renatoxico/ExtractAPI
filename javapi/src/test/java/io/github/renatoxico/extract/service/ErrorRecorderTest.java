package io.github.renatoxico.extract.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.*;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ErrorRecorderTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private final TransactionStatus status = mock(TransactionStatus.class);
    private final ErrorRecorder recorder = new ErrorRecorder(jdbc, manager, new ObjectMapper());

    @Test
    void sanitizesAndUpsertsInOwnTransactionWithStableFingerprint() {
        when(manager.getTransaction(any())).thenReturn(status);
        UUID correlation = UUID.randomUUID();
        String report = "a".repeat(32);
        recorder.record("NO_VALID_EXPENSES", ErrorRecorder.Stage.PARSING, report, correlation,
            Map.of("fileCount", 2, "text", "PRIVATE", "amount", 123, "candidateCount", "PRIVATE", "acceptedCount", -1));
        ArgumentCaptor<TransactionDefinition> definition = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(manager).getTransaction(definition.capture());
        assertThat(definition.getValue().getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        ArgumentCaptor<String> fingerprint = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(eq(ErrorRecorder.UPSERT), eq("NO_VALID_EXPENSES"), eq("PARSING"), eq(report), eq(correlation), eq("{\"fileCount\":2}"), fingerprint.capture());
        verify(manager).commit(status);
        reset(jdbc);
        recorder.record("NO_VALID_EXPENSES", ErrorRecorder.Stage.PARSING, null, UUID.randomUUID(), Map.of());
        verify(jdbc).update(eq(ErrorRecorder.UPSERT), eq("NO_VALID_EXPENSES"), eq("PARSING"), isNull(), any(UUID.class), eq("{}"), eq(fingerprint.getValue()));
        assertThat(fingerprint.getValue()).matches("[0-9a-f]{64}");
        assertThat(ErrorRecorder.UPSERT).contains("ON CONFLICT (fingerprint)", "app_error.count + 1");
    }

    @Test
    void swallowsStartWriteAndCommitFailures() {
        when(manager.getTransaction(any())).thenThrow(new IllegalStateException("private"));
        assertThatCode(() -> recorder.record("PDF_EXTRACTION_FAILED", ErrorRecorder.Stage.PDF_EXTRACTION, null, null, Map.of())).doesNotThrowAnyException();
        doReturn(status).when(manager).getTransaction(any());
        when(jdbc.update(anyString(), any(Object[].class))).thenThrow(new IllegalStateException("private"));
        assertThatCode(() -> recorder.record("PDF_EXTRACTION_FAILED", ErrorRecorder.Stage.PDF_EXTRACTION, null, null, Map.of())).doesNotThrowAnyException();
        verify(manager).rollback(status);
        reset(jdbc);
        doThrow(new IllegalStateException("private")).when(manager).commit(status);
        assertThatCode(() -> recorder.record("PDF_EXTRACTION_FAILED", ErrorRecorder.Stage.PDF_EXTRACTION, null, null, Map.of())).doesNotThrowAnyException();
    }

    @Test
    void rejectsUntrustedCodesIdentifiersAndMetadataValues() {
        when(manager.getTransaction(any())).thenReturn(status);
        recorder.record("PRIVATE STATEMENT", ErrorRecorder.Stage.RAW_TEXT, "PRIVATE", null, Map.of("fileCount", "PRIVATE", "pdf", new byte[]{1}));
        verify(jdbc).update(eq(ErrorRecorder.UPSERT), eq("EXTRACTION_FAILED"), eq("RAW_TEXT"), isNull(), any(UUID.class), eq("{}"), anyString());
    }
}
