package io.github.renatoxico.extract.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.config.ClassificationProperties;
import io.github.renatoxico.extract.repo.ClassificationWorkRepository.ClaimedBatch;
import io.github.renatoxico.extract.repo.ClassificationWorkRepository.ApplyClaim;
import io.github.renatoxico.extract.repo.ExpenseClassificationRepository;
import java.util.Optional;
import io.github.renatoxico.extract.handler.GlobalExceptionHandler;
import io.github.renatoxico.extract.logging.ExpenseSource;
import io.github.renatoxico.extract.logging.LogFields;
import io.github.renatoxico.extract.model.Expense;
import io.github.renatoxico.extract.model.ExpenseReport;
import io.github.renatoxico.extract.repo.ExpenseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class LoggingDiagnosticsTest {
    private final Logger logger = (Logger) LoggerFactory.getLogger("io.github.renatoxico.extract");
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final ValidationService validation = new ValidationService();
    private final ExpenseReport report = new ExpenseReport("report-123", null);
    private Level previousLevel;

    @BeforeEach
    void capture() {
        previousLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void cleanup() {
        logger.detachAppender(logs);
        logger.setLevel(previousLevel);
        logs.stop();
    }

    @Test
    void shortDatesAndSuccessfulYearInferenceAreQuiet() {
        Expense shortDate = expense("02/01");
        validation.validateExpenses(new ArrayList<>(List.of(expense("01/01/2024"), shortDate)), Map.of());
        assertThat(shortDate.getDate()).isEqualTo("02/01/2024");
        assertThat(logs.list).isEmpty();
    }

    @Test
    void fallsBackFromUnusableCommonYearWithoutWarningForSuccessfulExpense() {
        String futureDate = "01/01/" + (LocalDate.now().getYear() + 1);
        Expense shortDate = expense("01/01");
        validation.validateExpenses(new ArrayList<>(List.of(expense(futureDate), shortDate)), Map.of());
        assertThat(shortDate.getDate()).isEqualTo("01/01/" + LocalDate.now().getYear());
        assertThat(logs.list).singleElement().satisfies(event ->
            assertThat(event.getFormattedMessage()).contains("originalDate=" + futureDate, "attemptedYears=[]"));
    }

    @Test
    void unresolvedDateLogsOnceAfterAllAttemptsAndRemainsUnchanged() {
        Expense expense = expense("32/13");
        ExpenseSource source = new ExpenseSource("statement.pdf", report.getId(), 7,
            "32/13 STORE 10,00", "32/13 STORE 10,00");
        validation.validateExpenses(new ArrayList<>(List.of(expense)), Map.of(expense, source));
        assertThat(expense.getDate()).isEqualTo("32/13");
        assertThat(logs.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getThrowableProxy()).isNull();
            assertThat(event.getFormattedMessage()).contains("filename=statement.pdf", "reportId=report-123",
                "lineNumber=7", "sourceLine=32/13 STORE 10,00", "originalDate=32/13",
                "attemptedYears=[" + LocalDate.now().getYear() + ", " + (LocalDate.now().getYear() - 1) + "]",
                "reason=No valid date after year inference");
        });
    }

    @Test
    void invalidFullDateIsDiagnosedWithoutChangingAcceptance() {
        Expense expense = expense("31/02/2024");
        assertThat(validation.validateExpenses(new ArrayList<>(List.of(expense)), Map.of())).containsExactly(expense);
        assertThat(expense.getDate()).isEqualTo("31/02/2024");
        assertThat(logs.list).singleElement().satisfies(event ->
            assertThat(event.getFormattedMessage()).contains("originalDate=31/02/2024", "attemptedYears=[]"));
    }

    @Test
    void twoCandidatesKeepOriginalLineAndLineNumber() {
        ExpenseRepository repository = mock(ExpenseRepository.class);
        String line = "32/13 FIRST 10,00 33/14 SECOND 20,00";
        new ObjectifierService(repository, validation).process(report, "HEADER\n" + line, "two.pdf");
        List<ILoggingEvent> warnings = logs.list.stream().filter(e -> e.getLevel() == Level.WARN).toList();
        assertThat(warnings).hasSize(2).allSatisfy(event ->
            assertThat(event.getFormattedMessage()).contains("sourceLine=" + line, "lineNumber=2", "filename=two.pdf"));
        assertThat(warnings.get(0).getFormattedMessage()).contains("candidate=32/13 FIRST 10,00");
        assertThat(warnings.get(1).getFormattedMessage()).contains("candidate= 33/14 SECOND 20,00");
        verify(repository).saveAll(anyList());
        assertThat(logs.list.stream().filter(e -> e.getLevel() == Level.INFO)).singleElement()
            .satisfies(event -> assertThat(event.getFormattedMessage()).contains("candidates=2", "savedExpenses=2"));
    }

    @Test
    void successfulSourceTextIsNotLogged() {
        new ObjectifierService(mock(ExpenseRepository.class), validation)
            .process(report, "01/01/2024 PRIVATE STORE 10,00", "good.pdf");
        assertThat(logs.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(event.getFormattedMessage()).doesNotContain("PRIVATE STORE", "sourceLine", "candidate=");
        });
    }

    @Test
    void fieldsAreEscapedAndBounded() {
        assertThat(LogFields.safe("a\r\n\t\u001b\u2028b")).isEqualTo("a\\r\\n\\t\\u001b\\u2028b");
        assertThat(LogFields.safe("x".repeat(5000))).hasSize(4096).endsWith("...[truncated]");
        assertThat(LogFields.safe("\n".repeat(5000))).hasSizeLessThanOrEqualTo(4096).endsWith("...[truncated]");
        assertThat(LogFields.safe("x".repeat(4096))).hasSize(4096).doesNotContain("truncated");
    }

    @Test
    void validationWarningEscapesSourceAndFilename() {
        Expense expense = expense("32/13");
        validation.validateExpenses(new ArrayList<>(List.of(expense)), Map.of(expense,
            new ExpenseSource("file\n.pdf", report.getId(), 1, "bad\rline\t" + "x".repeat(5000), "32/13")));
        assertThat(logs.list).singleElement().satisfies(event ->
            assertThat(event.getFormattedMessage()).contains("file\\n.pdf", "bad\\rline\\t", "...[truncated]")
                .doesNotContain("\n", "\r", "\t"));
    }

    @Test
    void wrappedDiagnosticsArePrivateAndTerminalErrorIsLoggedOnce() throws Exception {
        ProcessingException cause = new ProcessingException("Cannot process expense", HttpStatus.INTERNAL_SERVER_ERROR,
            "FILE_PROCESSING_ERROR", new IllegalArgumentException("bad date"))
            .withDiagnostic("sourceLine", "PRIVATE\nTRANSACTION").withDiagnostic("lineNumber", 5);
        ProcessingException wrapped = new ProcessingException("File failed", cause.getHttpStatus(), cause.getErrorCode(), cause)
            .withDiagnostic("filename", "file.pdf");
        var response = new GlobalExceptionHandler().handleProcessingException(wrapped);
        assertThat(logs.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getThrowableProxy()).isNotNull();
            assertThat(event.getFormattedMessage()).contains("sourceLine=PRIVATE\\nTRANSACTION", "lineNumber=5", "filename=file.pdf");
        });
        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(response.getBody());
        assertThat(json).doesNotContain("PRIVATE", "sourceLine", "lineNumber", "filename");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("File failed");
    }

    @Test
    void pdfFailureIsLoggedOnlyAtRequestBoundaryWithoutRoutineStackTrace() {
        ProcessingException failure = catchThrowableOfType(ProcessingException.class, () ->
            new ExtractorService().extractText(new MockMultipartFile("file", "broken.pdf", "application/pdf", new byte[]{1, 2})));
        assertThat(logs.list).isEmpty();
        new GlobalExceptionHandler().handleProcessingException(failure);
        assertThat(logs.list).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getThrowableProxy()).isNull();
            assertThat(event.getFormattedMessage()).contains("filename=broken.pdf", "PDF_EXTRACTION_FAILED");
        });
    }

    @Test
    void reportDateFailuresIncludeStoredIdentifiers() {
        ProcessingException failure = catchThrowableOfType(ProcessingException.class,
            () -> new V2ReportMapper().parseDate("32/13", "stored-report", 42L));
        assertThat(failure.getDiagnostics()).containsEntry("reportId", "stored-report")
            .containsEntry("expenseId", "42").containsEntry("date", "32/13");
        assertThat(failure.getMessage()).isEqualTo("Report contains an invalid expense date");
    }

    @Test
    void idleWorkersAndZeroCatalogChangesAreQuiet() {
        var transactions = mock(ClassificationTransactionService.class);
        var repository = mock(ExpenseClassificationRepository.class);
        var catalog = new ExpenseClassificationCatalogService(repository);
        var pipeline = new ClassificationPipelineService(transactions, new ClassificationProperties(),
            mock(AiProcessorService.class), catalog);
        new ScheduledTasksService(pipeline, mock(AdminEmailService.class)).registerMissingCatalogEntries();
        pipeline.createNextClassificationBatch();
        pipeline.processNextAiBatch();
        pipeline.applyReadyTasks();
        assertThat(logs.list).isEmpty();
        when(repository.populateMissing()).thenReturn(2);
        pipeline.registerMissingCatalogEntries();
        assertThat(logs.list).singleElement().satisfies(event ->
            assertThat(event.getFormattedMessage()).contains("Added 2 missing entries"));
    }

    @Test
    void workerFailuresRetainIdentifiersAttemptsAndCauses() {
        var transactions = mock(ClassificationTransactionService.class);
        var ai = mock(AiProcessorService.class);
        var pipeline = new ClassificationPipelineService(transactions, new ClassificationProperties(),
            ai, mock(ExpenseClassificationCatalogService.class));
        var aiClaim = new ClaimedBatch(91, 2, List.of());
        when(transactions.claimNextBatch()).thenReturn(Optional.of(aiClaim));
        when(ai.processWithGemini(anyList())).thenThrow(new IllegalStateException("provider unavailable"));
        pipeline.processNextAiBatch();
        var applyClaim = new ApplyClaim(42, 91, 5, "STORE", "Other", 3);
        when(transactions.claimNextApplyTask()).thenReturn(Optional.of(applyClaim), Optional.empty());
        doThrow(new IllegalStateException("database unavailable")).when(transactions).applyCatalogSuggestion(applyClaim);
        pipeline.applyReadyTasks();
        when(transactions.findExpenseIdsReadyForPropagation()).thenReturn(List.of(123L));
        doThrow(new IllegalStateException("propagation failed")).when(transactions).propagateCategoryToExpense(123L);
        pipeline.propagateCatalogCategories();
        assertThat(logs.list).hasSize(3).allSatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getThrowableProxy()).isNotNull();
        });
        assertThat(logs.list.get(0).getFormattedMessage()).contains("batch 91 attempt 2");
        assertThat(logs.list.get(1).getFormattedMessage()).contains("task 42 batch 91 attempt 3");
        assertThat(logs.list.get(2).getFormattedMessage()).contains("expense id 123");
    }

    private Expense expense(String date) {
        return new Expense(report, BigDecimal.TEN, "STORE", date, null);
    }
}
