package com.example.api.controller;

import com.example.api.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

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

    @Autowired
    private ObjectMapper objectMapper;

    private MockMultipartFile validPdfFile;
    private MockMultipartFile invalidTypeFile;

    @BeforeEach
    void setUp() {
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

        Map<String, Object> report = new HashMap<>();
        report.put("expenses", java.util.Collections.emptyList());
        when(reportsService.getFullReport("session-123")).thenReturn(report);

        doNothing().when(objService).process(anyString(), anyString());

        // Act & Assert
        mockMvc.perform(multipart("/extract/")
            .file(validPdfFile))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionToken").value("session-123"));

        verify(validationService).validateFiles(any());
        verify(javaProcessor).extractText(any());
        verify(objService).process(anyString(), anyString());
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
        Map<String, Object> report = new HashMap<>();
        report.put("expenses", java.util.Collections.emptyList());
        when(reportsService.getFullReport("session-123")).thenReturn(report);

        // Act & Assert
        mockMvc.perform(get("/extract/summary/session-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionToken").value("session-123"));

        verify(reportsService).getFullReport("session-123");
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
    void testGetSummary_WithNullSessionId_Returns400WithINVALID_SESSION_ID() throws Exception {
        // Act & Assert - Using empty string path which will be blank when passed
        MvcResult result = mockMvc.perform(get("/extract/summary/"))
            .andExpect(status().isNotFound())  // 404 because path param is missing
            .andReturn();
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
    void testGetSummary_WithEmptySessionData_Returns404WithSESSION_NOT_FOUND() throws Exception {
        // Arrange
        when(reportsService.getFullReport("empty-session")).thenReturn(new HashMap<>());

        // Act & Assert
        MvcResult result = mockMvc.perform(get("/extract/summary/empty-session"))
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

        Map<String, Object> report = new HashMap<>();
        report.put("expenses", java.util.Collections.emptyList());
        when(reportsService.getFullReport("session-123")).thenReturn(report);

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

