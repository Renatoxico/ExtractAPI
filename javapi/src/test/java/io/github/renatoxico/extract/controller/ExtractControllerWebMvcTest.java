package io.github.renatoxico.extract.controller;

import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.model.AuthenticatedUserPrincipal;
import io.github.renatoxico.extract.model.ReportExport;
import io.github.renatoxico.extract.model.ValidationResponse;
import io.github.renatoxico.extract.service.ExpenseReportAccessService;
import io.github.renatoxico.extract.service.ExpenseReportingService;
import io.github.renatoxico.extract.service.ExtractorService;
import io.github.renatoxico.extract.service.ObjectifierService;
import io.github.renatoxico.extract.service.ValidationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExtractController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExtractControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ValidationService validationService;

    @MockitoBean
    private ObjectifierService objService;

    @MockitoBean
    private ExpenseReportingService reportsService;

    @MockitoBean
    private ExtractorService javaProcessor;

    @MockitoBean
    private ExpenseReportAccessService reportAccessService;

    private MockMultipartFile validPdfFile;
    private AuthenticatedUserPrincipal principal;

    private static ReportExport emptyReport(String sessionId) {
        return new ReportExport(List.of(), List.of(), List.of(), List.of(), null, sessionId);
    }

    private static ValidationResponse validResponse() {
        return new ValidationResponse(true, "OK", "OK", HttpStatus.OK);
    }

    private static Stream<Arguments> validationErrors() {
        return Stream.of(
            Arguments.of("INVALID_FILE_TYPE", "File is not a valid PDF"),
            Arguments.of("FILE_TOO_BIG", "File is too large"),
            Arguments.of("TOO_MANY_FILES", "Too many files")
        );
    }

    @BeforeEach
    void setUp() {
        principal = new AuthenticatedUserPrincipal(
            42L,
            "firebase-uid-123",
            "user@example.com",
            "Example User",
            null,
            true
        );
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );

        validPdfFile = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "PDF content".getBytes()
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void homeReturnsOk() throws Exception {
        mockMvc.perform(get("/extract/"))
            .andExpect(status().isOk());
    }

    @Test
    void processReturnsReportAndCreatesSessionForOwner() throws Exception {
        when(validationService.validateFiles(any())).thenReturn(validResponse());
        when(reportsService.generateId(42L)).thenReturn("session-123");
        when(javaProcessor.extractText(any())).thenReturn("Sample expense text");

        when(reportsService.getFullReport("session-123")).thenReturn(emptyReport("session-123"));

        mockMvc.perform(multipart("/extract/")
            .file(validPdfFile))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionToken").value("session-123"));

        verify(validationService).validateFiles(any());
        verify(javaProcessor).extractText(any());
        verify(objService).process(anyString(), anyString());
        verify(reportsService).generateId(42L);
    }

    @Test
    void processWithoutFilesReturnsBadRequest() throws Exception {
        mockMvc.perform(multipart("/extract/"))
            .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("validationErrors")
    void processReturnsStructuredValidationError(String errorCode, String message) throws Exception {
        when(validationService.validateFiles(any())).thenReturn(
            new ValidationResponse(false, message, errorCode, HttpStatus.BAD_REQUEST)
        );

        mockMvc.perform(multipart("/extract/").file(validPdfFile))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value(errorCode))
            .andExpect(jsonPath("$.message").value(message));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void processRejectsPdfWithoutExtractedText(String extractedText) throws Exception {
        when(validationService.validateFiles(any())).thenReturn(validResponse());
        when(reportsService.generateId(42L)).thenReturn("session-123");
        when(javaProcessor.extractText(any())).thenReturn(extractedText);

        mockMvc.perform(multipart("/extract/")
            .file(validPdfFile))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errorCode").value("EMPTY_PDF_CONTENT"));
    }

    @Test
    void processMapsFileFailureToProcessingError() throws Exception {
        when(validationService.validateFiles(any())).thenReturn(validResponse());
        when(reportsService.generateId(42L)).thenReturn("session-123");
        when(javaProcessor.extractText(any())).thenReturn("Sample expense text");
        doThrow(new RuntimeException("Processing failed"))
            .when(objService).process(anyString(), anyString());

        mockMvc.perform(multipart("/extract/")
            .file(validPdfFile))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.errorCode").value("FILE_PROCESSING_ERROR"));
    }

    @Test
    void processMapsUnexpectedFailureToSafeError() throws Exception {
        when(validationService.validateFiles(any()))
            .thenThrow(new RuntimeException("Unexpected error in validation"));

        mockMvc.perform(multipart("/extract/")
            .file(validPdfFile))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.errorCode").value("UNEXPECTED_ERROR"));
    }

    @Test
    void summaryReturnsOwnedReport() throws Exception {
        when(reportsService.getFullReport("session-123")).thenReturn(emptyReport("session-123"));

        mockMvc.perform(get("/extract/summary/session-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionToken").value("session-123"));

        verify(reportsService).getFullReport("session-123");
        verify(reportAccessService).requireOwnership("session-123", 42L);
    }

    @Test
    void summaryForAnotherUserReturnsNotFoundWithoutQueryingExpenses() throws Exception {
        doThrow(new ProcessingException(
            "No report found for the provided session ID",
            HttpStatus.NOT_FOUND,
            "SESSION_NOT_FOUND"
        )).when(reportAccessService).requireOwnership("other-session", 42L);

        mockMvc.perform(get("/extract/summary/other-session"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("SESSION_NOT_FOUND"));

        verify(reportsService, never()).getFullReport("other-session");
    }

    @Test
    void exportForAnotherUserReturnsNotFoundWithoutExporting() throws Exception {
        doThrow(new ProcessingException(
            "No report found for the provided session ID",
            HttpStatus.NOT_FOUND,
            "SESSION_NOT_FOUND"
        )).when(reportAccessService).requireOwnership("other-session", 42L);

        mockMvc.perform(get("/extract/export/other-session"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("SESSION_NOT_FOUND"));

        verify(reportsService, never()).exportReportCSV("other-session");
    }

    @Test
    void summaryRejectsBlankSessionId() throws Exception {
        mockMvc.perform(get("/extract/summary/ "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("INVALID_SESSION_ID"));
    }

    @Test
    void summaryReturnsNotFoundWhenReportDoesNotExist() throws Exception {
        when(reportsService.getFullReport("non-existent")).thenReturn(null);

        mockMvc.perform(get("/extract/summary/non-existent"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("SESSION_NOT_FOUND"));
    }

    @Test
    void summaryMapsRetrievalFailureToSafeError() throws Exception {
        when(reportsService.getFullReport(anyString()))
            .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/extract/summary/session-123"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.errorCode").value("SUMMARY_RETRIEVAL_ERROR"));
    }

    @Test
    void processHandlesEveryUploadedFile() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("file", "test1.pdf", "application/pdf", "PDF 1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file", "test2.pdf", "application/pdf", "PDF 2".getBytes());

        when(validationService.validateFiles(any())).thenReturn(validResponse());
        when(reportsService.generateId(42L)).thenReturn("session-123");
        when(javaProcessor.extractText(any())).thenReturn("Sample expense text");

        when(reportsService.getFullReport("session-123")).thenReturn(emptyReport("session-123"));

        mockMvc.perform(multipart("/extract/")
            .file(file1)
            .file(file2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionToken").value("session-123"));

        verify(javaProcessor, times(2)).extractText(any());
        verify(objService, times(2)).process(anyString(), anyString());
    }
}
