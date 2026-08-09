package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.model.FileValidationResult;
import io.github.renatoxico.extract.model.ReportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExtractionFacade {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractionFacade.class);
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
        ExpenseClassificationCatalogService catalogService
    ) {
        this.validationService = validationService;
        this.extractorService = extractorService;
        this.objectifierService = objectifierService;
        this.reportingService = reportingService;
        this.catalogService = catalogService;
    }

    public ReportData process(MultipartFile[] files, Long ownerId) {
        requireValid(files);
        String reportId = reportingService.createReport(ownerId);

        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            try {
                String pdfText = extractorService.extractText(file);
                if (pdfText == null || pdfText.isBlank()) {
                    throw new ProcessingException(
                        "No text extracted from file: " + fileName,
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "EMPTY_PDF_CONTENT"
                    );
                }
                objectifierService.process(reportId, pdfText);
            } catch (ProcessingException ex) {
                throw ex;
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
            LOG.error("Failed to synchronize classification catalog for report {}", reportId, ex);
        }
        return reportingService.getReport(reportId);
    }

    public void requireValid(MultipartFile[] files) {
        FileValidationResult validation = validationService.validateFiles(files);
        if (!validation.valid()) {
            throw new ProcessingException(
                validation.message(), validation.httpStatus(), validation.errorCode());
        }
    }
}
