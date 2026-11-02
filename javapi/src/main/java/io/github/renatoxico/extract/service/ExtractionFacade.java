package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.model.FileValidationResult;
import io.github.renatoxico.extract.model.ExpenseReport;
import io.github.renatoxico.extract.model.ReportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.UUID;

@Service
public class ExtractionFacade {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractionFacade.class);
    private final ErrorRecorder errorRecorder;
    private final ValidationService validationService;
    private final ExtractorService extractorService;
    private final ObjectifierService objectifierService;
    private final ExpenseReportingService reportingService;
    private final ExpenseClassificationCatalogService catalogService;

    public ExtractionFacade(
        ValidationService validationService,
        ExtractorService extractorService,
        ObjectifierService objectifierService,
        ExpenseReportingService reportingService,
        ExpenseClassificationCatalogService catalogService,
        ErrorRecorder errorRecorder
    ) {
        this.errorRecorder = errorRecorder;
        this.validationService = validationService;
        this.extractorService = extractorService;
        this.objectifierService = objectifierService;
        this.reportingService = reportingService;
        this.catalogService = catalogService;
    }

    @Transactional
    public ReportData process(MultipartFile[] files, Long ownerId) {
        requireValid(files);
        UUID correlationId = UUID.randomUUID();
        String failedReportId = null;
        ErrorRecorder.Stage stage = ErrorRecorder.Stage.REPORT_CREATION;
        try {
            ExpenseReport report = reportingService.createReport(ownerId);
            String reportId = report.getId();
            failedReportId = reportId;

            for (MultipartFile file : files) {
                String fileName = file.getOriginalFilename();
                try {
                    stage = ErrorRecorder.Stage.PDF_EXTRACTION;
                    String pdfText = extractorService.extractText(file);
                    if (pdfText == null || pdfText.isBlank()) {
                        throw new ProcessingException(
                            "No text extracted from file: " + fileName,
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            "EMPTY_PDF_CONTENT"
                        );
                    }
                    stage = ErrorRecorder.Stage.PARSING;
                    objectifierService.process(report, pdfText);
                } catch (ProcessingException ex) {
                    throw new ProcessingException(
                        "Failed to process file '" + fileName + "': " + ex.getMessage(),
                        ex.getHttpStatus(),
                        ex.getErrorCode(),
                        ex
                    );
                } catch (Exception ex) {
                    throw new ProcessingException(
                        "Failed to process file: " + fileName,
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "FILE_PROCESSING_ERROR",
                        ex
                    );
                }
            }

            try {
                catalogService.populateFromReport(reportId);
                catalogService.applyCategoriesToReport(reportId);
            } catch (Exception ex) {
                errorRecorder.record("CATALOG_SYNC_FAILED", ErrorRecorder.Stage.CATALOG, reportId, correlationId, Map.of());
                LOG.warn("Failed to synchronize classification catalog for report {}", reportId);
            }
            stage = ErrorRecorder.Stage.REPORTING;
            return reportingService.getReport(reportId);
        } catch (RuntimeException ex) {
            String code = ex instanceof ProcessingException processing ? processing.getErrorCode() : "EXTRACTION_FAILED";
            errorRecorder.record(code, stage, failedReportId, correlationId, Map.of("fileCount", files.length));
            throw ex;
        }
    }

    public void requireValid(MultipartFile[] files) {
        FileValidationResult validation = validationService.validateFiles(files);
        if (!validation.valid()) {
            errorRecorder.record(validation.errorCode(), ErrorRecorder.Stage.VALIDATION, null,
                UUID.randomUUID(), Map.of("fileCount", files.length));
            throw new ProcessingException(
                validation.message(), validation.httpStatus(), validation.errorCode());
        }
    }
}
