package com.example.api.controller;


import com.example.api.exception.ProcessingException;
import com.example.api.model.ValidationResponse;
import com.example.api.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@Controller
@RequestMapping("/extract")
public class ExtractController {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractController.class);
    private final PythonProcessingService pyProcessor;
    private final ObjectifierService objService;
    private final ExpenseReportingService reportsService;
    private final ValidationService validationService;
    private final ExtractorService javaProcessor;
    private static final String PATH = System.getProperty("user.dir") + "\\tmp\\";

    public ExtractController(ValidationService validationService, PythonProcessingService pyProcessor, ObjectifierService objService, ExpenseReportingService reportsService, ExtractorService javaProcessor) {
        this.pyProcessor = pyProcessor;
        this.objService = objService;
        this.validationService = validationService;
        this.reportsService = reportsService;
        this.javaProcessor = javaProcessor;
    }

    @GetMapping("/")
    public String home(HttpServletRequest request) {
        return "api-docs";
    }

    @PostMapping("/")
    public ResponseEntity<?> process(@RequestParam("file") MultipartFile[] files){
        LOG.info("Java Processor API - Processing {} file(s)", files.length);

        try {
            // Validate files
            ValidationResponse isValid = validationService.validateFiles(files);
            if (!isValid.getStatus()){
                LOG.warn("File validation failed [{}]: {}", isValid.getErrorCode(), isValid.getMessage());
                throw new ProcessingException(
                    isValid.getMessage(),
                    isValid.getHttpStatus(),
                    isValid.getErrorCode()
                );
            }

            // Generate session ID
            String sessionId = reportsService.generateId();
            LOG.info("Generated session ID: {}", sessionId);

            // Process each file
            for (MultipartFile file : files) {
                try {
                    LOG.info("Processing file: {}", file.getOriginalFilename());
                    String pdfText = javaProcessor.extractText(file);

                    if (pdfText == null || pdfText.trim().isEmpty()) {
                        throw new ProcessingException(
                            "No text extracted from file: " + file.getOriginalFilename(),
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "EMPTY_PDF_CONTENT"
                        );
                    }

                    objService.process(sessionId, pdfText);
                    LOG.info("Successfully processed file: {}", file.getOriginalFilename());
                } catch (ProcessingException ex) {
                    throw ex;
                } catch (Exception ex) {
                    LOG.error("Error processing file {}: {}", file.getOriginalFilename(), ex.getMessage(), ex);
                    throw new ProcessingException(
                        "Failed to process file: " + file.getOriginalFilename(),
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "FILE_PROCESSING_ERROR",
                        ex
                    );
                }
            }

            // Generate report
            Map<String,Object> expensesGrouped = reportsService.getFullReport(sessionId);
            expensesGrouped.put("sessionToken", sessionId);
            LOG.info("Process completed successfully for session: {}", sessionId);
            return ResponseEntity.ok(expensesGrouped);

        } catch (ProcessingException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.error("Unexpected error in process endpoint: {}", ex.getMessage(), ex);
            throw new ProcessingException(
                "An unexpected error occurred during processing",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "UNEXPECTED_ERROR",
                ex
            );
        }
    }

    @GetMapping("/summary/{sessionId}")
    public ResponseEntity<?> getExpenseSummary(@PathVariable String sessionId) {
        if(sessionId == null || sessionId.isBlank()) {
            LOG.warn("Invalid sessionId provided");
            throw new ProcessingException(
                "SessionId is required and cannot be empty",
                HttpStatus.BAD_REQUEST,
                "INVALID_SESSION_ID"
            );
        }

        try {
            LOG.info("Fetching summary for session: {}", sessionId);
            Map<String,Object> expensesGrouped = reportsService.getFullReport(sessionId);

            if (expensesGrouped == null || expensesGrouped.isEmpty()) {
                LOG.warn("No data found for session: {}", sessionId);
                throw new ProcessingException(
                    "No data found for the provided session ID",
                    HttpStatus.NOT_FOUND,
                    "SESSION_NOT_FOUND"
                );
            }

            expensesGrouped.put("sessionToken", sessionId);
            LOG.info("Successfully retrieved summary for session: {}", sessionId);
            return ResponseEntity.status(HttpStatus.OK).body(expensesGrouped);
        } catch (ProcessingException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.error("Error retrieving summary for session {}: {}", sessionId, ex.getMessage(), ex);
            throw new ProcessingException(
                "Error retrieving expense summary",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "SUMMARY_RETRIEVAL_ERROR",
                ex
            );
        }
    }

    //@GetMapping("/test/{sessionId}")
    public ResponseEntity<?> testEndpoint(@PathVariable String sessionId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                reportsService.updateExpenses(sessionId)
        );
    }

    @GetMapping("/export/{sessionId}")
    public ResponseEntity<ByteArrayResource> exportExpensesCsv(@PathVariable String sessionId) {
        if(sessionId == null || sessionId.isBlank()) {
            LOG.warn("Invalid sessionId provided for export");
            throw new ProcessingException(
                "SessionId is required and cannot be empty",
                HttpStatus.BAD_REQUEST,
                "INVALID_SESSION_ID"
            );
        }

        try {
            LOG.info("Exporting CSV for session: {}", sessionId);
            byte[] bytes = reportsService.exportReportCSV(sessionId);
            ByteArrayResource resource = new ByteArrayResource(bytes);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"despesas_" + sessionId + ".csv\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(bytes.length)
                    .body(resource);

        } catch (ProcessingException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.error("Error exporting CSV for session {}: {}", sessionId, ex.getMessage(), ex);
            throw new ProcessingException(
                "Error exporting CSV",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "CSV_EXPORT_ERROR",
                ex
            );
        }
    }

    @PostMapping("/raw-text/")
    public ResponseEntity<?> extractRawText(@RequestParam("file") MultipartFile[] files) {
        LOG.info("Extract Raw Text API - Processing {} file(s)", files.length);

        try {
            ValidationResponse isValid = validationService.validateFiles(files);
            if (!isValid.getStatus()){
                LOG.warn("File validation failed [{}]: {}", isValid.getErrorCode(), isValid.getMessage());
                throw new ProcessingException(
                    isValid.getMessage(),
                    isValid.getHttpStatus(),
                    isValid.getErrorCode()
                );
            }

            StringBuilder combinedText = new StringBuilder();
            for (MultipartFile file : files) {
                try {
                    LOG.info("Extracting text from file: {}", file.getOriginalFilename());
                    String pdfText = javaProcessor.extractText(file);

                    if (pdfText == null || pdfText.trim().isEmpty()) {
                        LOG.warn("No text extracted from file: {}", file.getOriginalFilename());
                        continue;
                    }

                    combinedText.append(pdfText).append("\n");
                    LOG.info("Successfully extracted text from file: {}", file.getOriginalFilename());
                } catch (ProcessingException ex) {
                    throw ex;
                } catch (Exception ex) {
                    LOG.error("Error extracting text from file {}: {}", file.getOriginalFilename(), ex.getMessage(), ex);
                    throw new ProcessingException(
                        "Failed to extract text from file: " + file.getOriginalFilename(),
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "TEXT_EXTRACTION_ERROR",
                        ex
                    );
                }
            }

            if (combinedText.isEmpty()) {
                LOG.warn("No text extracted from any of the provided files");
                throw new ProcessingException(
                    "No text could be extracted from the provided files",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "NO_TEXT_EXTRACTED"
                );
            }

            LOG.info("Successfully extracted text from all files");
            return ResponseEntity.ok(Map.of("extractedText", combinedText.toString()));
        } catch (ProcessingException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.error("Unexpected error in extractRawText endpoint: {}", ex.getMessage(), ex);
            throw new ProcessingException(
                "An unexpected error occurred during text extraction",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "UNEXPECTED_ERROR",
                ex
            );
        }
    }

    private String saveFileTemp(MultipartFile file){
        try {
            Files.createDirectories(Paths.get(PATH));
            String filePath = PATH + file.getOriginalFilename();
            file.transferTo(new File(filePath));
            return filePath;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
