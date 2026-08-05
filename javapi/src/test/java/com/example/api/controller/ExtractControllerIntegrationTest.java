package com.example.api.controller;

import com.example.api.service.*;
import com.example.api.exception.ProcessingException;
import com.example.api.model.AuthenticatedUserPrincipal;
import com.example.api.model.ReportExport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExtractController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExtractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ValidationService validationService;

    @MockitoBean
    private PythonProcessingService pyProcessor;

    @MockitoBean
    private ObjectifierService objService;

    @MockitoBean
    private ExpenseReportingService reportsService;

    @MockitoBean
    private ExtractorService javaProcessor;

    @MockitoBean
    private ExpenseReportAccessService reportAccessService;

    private MockMultipartFile validPdfFile;
    private MockMultipartFile invalidTypeFile;
    private AuthenticatedUserPrincipal principal;

    private static ReportExport emptyReport(String sessionId) {
        return new ReportExport(List.of(), List.of(), List.of(), List.of(), null, sessionId);
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

        invalidTypeFile = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "Text content".getBytes()
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ========== Home Endpoint Tests ==========

    @Test
    void testHome_Returns200() throws Exception {
        mockMvc.perform(get("/extract/"))
            .andExpect(status().isOk());
    }

    // ========== Process Endpoint - Happy Path ==========

    @Test
    void testProcess_WithValidFile_Returns200() throws Exception {
        // Arrange
        com.example.api.model.ValidationResponse validResponse =
            new com.example.api.model.ValidationResponse(true, "OK", "OK", HttpStatus.OK);
        when(validationService.validateFiles(any())).thenReturn(validResponse);
        when(reportsService.generateId()).thenReturn("session-123");
        when(javaProcessor.extractText(any())).thenReturn("Sample expense text");

        when(reportsService.getFullReport("session-123")).thenReturn(emptyReport("session-123"));

        doNothing().when(objService).process(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(multipart("/extract/")
            .file(validPdfFile))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionToken").value("session-123"));

        verify(validationService).validateFiles(any());
        verify(javaProcessor).extractText(any());
        verify(objService).process(anyString(), anyString());
        verify(reportAccessService).registerOwnership("session-123", 42L);
    }

    // ========== Process Endpoint - Validation Error Tests ==========

    @Test
    void testProcess_WithNoFiles_Returns400() throws Exception {
        // When no files are sent, Spring rejects the request before reaching the controller
        mockMvc.perform(multipart("/extract/"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testProcess_WithInvalidFileType_Returns400WithINVALID_FILE_TYPE() throws Exception {
        // Arrange
        com.example.api.model.ValidationResponse validResponse =
            new com.example.api.model.ValidationResponse(false, "not a valid PDF file", "INVALID_FILE_TYPE", HttpStatus.BAD_REQUEST);
        when(validationService.validateFiles(any())).thenReturn(validResponse);

        // Act & Assert
        MvcResult result = mockMvc.perform(multipart("/extract/")
            .file(invalidTypeFile))
            .andExpect(status().isBadRequest())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("INVALID_FILE_TYPE"));
    }

    @Test
    void testProcess_WithFileTooLarge_Returns400WithFILE_TOO_BIG() throws Exception {
        // Arrange
        com.example.api.model.ValidationResponse validResponse =
            new com.example.api.model.ValidationResponse(false, "File is too large", "FILE_TOO_BIG", HttpStatus.BAD_REQUEST);
        when(validationService.validateFiles(any())).thenReturn(validResponse);

        // Act & Assert
        MvcResult result = mockMvc.perform(multipart("/extract/")
            .file(validPdfFile))
            .andExpect(status().isBadRequest())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("FILE_TOO_BIG"));
    }

    @Test
    void testProcess_WithTooManyFiles_Returns400WithTOO_MANY_FILES() throws Exception {
        // Arrange
        com.example.api.model.ValidationResponse validResponse =
            new com.example.api.model.ValidationResponse(false, "Too many files", "TOO_MANY_FILES", HttpStatus.BAD_REQUEST);
        when(validationService.validateFiles(any())).thenReturn(validResponse);

        // Act & Assert
        MvcResult result = mockMvc.perform(multipart("/extract/")
            .file(validPdfFile)
            .file(validPdfFile)
            .file(validPdfFile)
            .file(validPdfFile)
            .file(validPdfFile)
            .file(validPdfFile)
            .file(validPdfFile))
            .andExpect(status().isBadRequest())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("TOO_MANY_FILES"));
    }

    // ========== Process Endpoint - PDF Processing Error Tests ==========

    @Test
    void testProcess_WithEmptyPDF_Returns422WithEMPTY_PDF_CONTENT() throws Exception {
        // Arrange
        com.example.api.model.ValidationResponse validResponse =
            new com.example.api.model.ValidationResponse(true, "OK", "OK", HttpStatus.OK);
        when(validationService.validateFiles(any())).thenReturn(validResponse);
        when(reportsService.generateId()).thenReturn("session-123");
        when(javaProcessor.extractText(any())).thenReturn("");  // Empty PDF

        // Act & Assert
        MvcResult result = mockMvc.perform(multipart("/extract/")
            .file(validPdfFile))
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("EMPTY_PDF_CONTENT"));
    }

    @Test
    void testProcess_WithNullPDFContent_Returns422WithEMPTY_PDF_CONTENT() throws Exception {
        // Arrange
        com.example.api.model.ValidationResponse validResponse =
            new com.example.api.model.ValidationResponse(true, "OK", "OK", HttpStatus.OK);
        when(validationService.validateFiles(any())).thenReturn(validResponse);
        when(reportsService.generateId()).thenReturn("session-123");
        when(javaProcessor.extractText(any())).thenReturn(null);  // Null PDF content

        // Act & Assert
        MvcResult result = mockMvc.perform(multipart("/extract/")
            .file(validPdfFile))
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("EMPTY_PDF_CONTENT"));
    }

    // ========== Process Endpoint - File Processing Error Tests ==========

    @Test
    void testProcess_WithProcessingError_Returns500WithFILE_PROCESSING_ERROR() throws Exception {
        // Arrange
        com.example.api.model.ValidationResponse validResponse =
            new com.example.api.model.ValidationResponse(true, "OK", "OK", HttpStatus.OK);
        when(validationService.validateFiles(any())).thenReturn(validResponse);
        when(reportsService.generateId()).thenReturn("session-123");
        when(javaProcessor.extractText(any())).thenReturn("Sample expense text");
        doThrow(new RuntimeException("Processing failed"))
            .when(objService).process(anyString(), anyString());

        // Act & Assert
        MvcResult result = mockMvc.perform(multipart("/extract/")
            .file(validPdfFile))
            .andExpect(status().isInternalServerError())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("FILE_PROCESSING_ERROR"));
    }

    @Test
    void testProcess_WithUnexpectedError_Returns500WithUNEXPECTED_ERROR() throws Exception {
        // Arrange
        when(validationService.validateFiles(any()))
            .thenThrow(new RuntimeException("Unexpected error in validation"));

        // Act & Assert
        MvcResult result = mockMvc.perform(multipart("/extract/")
            .file(validPdfFile))
            .andExpect(status().isInternalServerError())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("UNEXPECTED_ERROR"));
    }

    // ========== Summary Endpoint Tests ==========

    @Test
    void testGetSummary_WithValidSessionId_Returns200() throws Exception {
        // Arrange
        when(reportsService.getFullReport("session-123")).thenReturn(emptyReport("session-123"));

        // Act & Assert
        mockMvc.perform(get("/extract/summary/session-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionToken").value("session-123"));

        verify(reportsService).getFullReport("session-123");
        verify(reportAccessService).requireOwnership("session-123", 42L);
    }

    @Test
    void testGetSummary_OtherUsersSession_Returns404WithoutQueryingExpenses() throws Exception {
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
    void testExport_OtherUsersSession_Returns404WithoutExporting() throws Exception {
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
    void testGetSummary_WithBlankSessionId_Returns400WithINVALID_SESSION_ID() throws Exception {
        // Act & Assert
        MvcResult result = mockMvc.perform(get("/extract/summary/ "))
            .andExpect(status().isBadRequest())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("INVALID_SESSION_ID"));
    }

    @Test
    void testGetSummary_WithNonExistentSession_Returns404WithSESSION_NOT_FOUND() throws Exception {
        // Arrange
        when(reportsService.getFullReport("non-existent")).thenReturn(null);

        // Act & Assert
        MvcResult result = mockMvc.perform(get("/extract/summary/non-existent"))
            .andExpect(status().isNotFound())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("SESSION_NOT_FOUND"));
    }

    @Test
    void testGetSummary_WithRetrievalError_Returns500WithSUMMARY_RETRIEVAL_ERROR() throws Exception {
        // Arrange
        when(reportsService.getFullReport(anyString()))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        MvcResult result = mockMvc.perform(get("/extract/summary/session-123"))
            .andExpect(status().isInternalServerError())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("SUMMARY_RETRIEVAL_ERROR"));
    }

    // ========== Process Multiple Files Tests ==========

    @Test
    void testProcess_WithMultipleValidFiles_Returns200() throws Exception {
        // Arrange
        MockMultipartFile file1 = new MockMultipartFile("file", "test1.pdf", "application/pdf", "PDF 1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file", "test2.pdf", "application/pdf", "PDF 2".getBytes());

        com.example.api.model.ValidationResponse validResponse =
            new com.example.api.model.ValidationResponse(true, "OK", "OK", HttpStatus.OK);
        when(validationService.validateFiles(any())).thenReturn(validResponse);
        when(reportsService.generateId()).thenReturn("session-123");
        when(javaProcessor.extractText(any())).thenReturn("Sample expense text");

        when(reportsService.getFullReport("session-123")).thenReturn(emptyReport("session-123"));

        doNothing().when(objService).process(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(multipart("/extract/")
            .file(file1)
            .file(file2))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionToken").value("session-123"));

        verify(javaProcessor, times(2)).extractText(any());
        verify(objService, times(2)).process(anyString(), anyString());
    }
}

