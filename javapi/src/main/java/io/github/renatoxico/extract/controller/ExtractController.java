package io.github.renatoxico.extract.controller;

import io.github.renatoxico.extract.api.v1.V1ReportResponse;
import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.model.AuthenticatedUserPrincipal;
import io.github.renatoxico.extract.service.ExpenseReportAccessService;
import io.github.renatoxico.extract.service.ExpenseReportingService;
import io.github.renatoxico.extract.service.ExtractionFacade;
import io.github.renatoxico.extract.service.ExtractorService;
import io.github.renatoxico.extract.service.V1ReportMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Controller
@RequestMapping("/extract")
public class ExtractController {
    private final ExtractionFacade extractionFacade;
    private final ExpenseReportingService reportingService;
    private final ExpenseReportAccessService accessService;
    private final ExtractorService extractorService;
    private final V1ReportMapper mapper;

    public ExtractController(
        ExtractionFacade extractionFacade,
        ExpenseReportingService reportingService,
        ExpenseReportAccessService accessService,
        ExtractorService extractorService,
        V1ReportMapper mapper
    ) {
        this.extractionFacade = extractionFacade;
        this.reportingService = reportingService;
        this.accessService = accessService;
        this.extractorService = extractorService;
        this.mapper = mapper;
    }

    @GetMapping("/")
    public String home() {
        return "api-docs";
    }

    @PostMapping({"", "/"})
    public ResponseEntity<V1ReportResponse> process(
        @RequestParam("file") MultipartFile[] files,
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        try {
            return ResponseEntity.ok(mapper.toResponse(extractionFacade.process(files, principal.localUserId())));
        } catch (ProcessingException ex) {
            throw legacy(ex);
        } catch (Exception ex) {
            throw new ProcessingException(
                "An unexpected error occurred during processing",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "UNEXPECTED_ERROR",
                ex
            );
        }
    }

    @GetMapping("/summary/{sessionId}")
    public ResponseEntity<V1ReportResponse> getExpenseSummary(
        @PathVariable String sessionId,
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new ProcessingException(
                "SessionId is required and cannot be empty", HttpStatus.BAD_REQUEST, "INVALID_SESSION_ID");
        }
        try {
            accessService.requireOwnership(sessionId, principal.localUserId());
            return ResponseEntity.ok(mapper.toResponse(reportingService.getReport(sessionId)));
        } catch (ProcessingException ex) {
            throw legacy(ex);
        } catch (Exception ex) {
            throw new ProcessingException(
                "Error retrieving expense summary",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "SUMMARY_RETRIEVAL_ERROR",
                ex
            );
        }
    }

    @GetMapping("/export/{sessionId}")
    public ResponseEntity<ByteArrayResource> exportExpensesCsv(
        @PathVariable String sessionId,
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new ProcessingException(
                "SessionId is required and cannot be empty", HttpStatus.BAD_REQUEST, "INVALID_SESSION_ID");
        }
        try {
            accessService.requireOwnership(sessionId, principal.localUserId());
            byte[] bytes = reportingService.exportReportCsvV1(sessionId);
            return csv(bytes, "despesas_" + sessionId + ".csv");
        } catch (ProcessingException ex) {
            throw legacy(ex);
        } catch (Exception ex) {
            throw new ProcessingException(
                "Error exporting CSV", HttpStatus.INTERNAL_SERVER_ERROR, "CSV_EXPORT_ERROR", ex);
        }
    }

    @PostMapping("/raw-text/")
    public ResponseEntity<Map<String, String>> extractRawText(@RequestParam("file") MultipartFile[] files) {
        extractionFacade.requireValid(files);
        StringBuilder combinedText = new StringBuilder();
        for (MultipartFile file : files) {
            try {
                String pdfText = extractorService.extractText(file);
                if (pdfText != null && !pdfText.isBlank()) {
                    combinedText.append(pdfText).append('\n');
                }
            } catch (ProcessingException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new ProcessingException(
                    "Failed to extract text from file: " + file.getOriginalFilename(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "TEXT_EXTRACTION_ERROR",
                    ex
                );
            }
        }
        if (combinedText.isEmpty()) {
            throw new ProcessingException(
                "No text could be extracted from the provided files",
                HttpStatus.UNPROCESSABLE_ENTITY,
                "NO_TEXT_EXTRACTED"
            );
        }
        return ResponseEntity.ok(Map.of("extractedText", combinedText.toString()));
    }

    private ResponseEntity<ByteArrayResource> csv(byte[] bytes, String fileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
        headers.add(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");
        return ResponseEntity.ok().headers(headers).contentLength(bytes.length)
            .body(new ByteArrayResource(bytes));
    }

    private ProcessingException legacy(ProcessingException ex) {
        if ("REPORT_NOT_FOUND".equals(ex.getErrorCode())) {
            return new ProcessingException(
                "No data found for the provided session ID",
                ex.getHttpStatus(),
                "SESSION_NOT_FOUND",
                ex
            );
        }
        return ex;
    }
}
