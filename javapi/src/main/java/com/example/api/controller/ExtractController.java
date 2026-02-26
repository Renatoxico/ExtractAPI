package com.example.api.controller;


import com.example.api.exception.ProcessingException;
import com.example.api.model.ValidationResponse;
import com.example.api.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@RestController
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
    public ResponseEntity<?> home(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        LOG.info("Home API called from IP: " + ip);
        return ResponseEntity.ok("Ip Address: " + ip);
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
