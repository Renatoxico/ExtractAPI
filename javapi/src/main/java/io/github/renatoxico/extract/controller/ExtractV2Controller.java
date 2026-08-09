package io.github.renatoxico.extract.controller;

import io.github.renatoxico.extract.api.v2.ReportResponse;
import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.model.AuthenticatedUserPrincipal;
import io.github.renatoxico.extract.service.ExpenseReportAccessService;
import io.github.renatoxico.extract.service.ExpenseReportingService;
import io.github.renatoxico.extract.service.ExtractionFacade;
import io.github.renatoxico.extract.service.V2ReportMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v2/extract")
public class ExtractV2Controller {
    private final ExtractionFacade extractionFacade;
    private final ExpenseReportingService reportingService;
    private final ExpenseReportAccessService accessService;
    private final V2ReportMapper mapper;

    public ExtractV2Controller(
        ExtractionFacade extractionFacade,
        ExpenseReportingService reportingService,
        ExpenseReportAccessService accessService,
        V2ReportMapper mapper
    ) {
        this.extractionFacade = extractionFacade;
        this.reportingService = reportingService;
        this.accessService = accessService;
        this.mapper = mapper;
    }

    @PostMapping({"", "/"})
    public ReportResponse process(
        @RequestParam("file") MultipartFile[] files,
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return mapper.toResponse(extractionFacade.process(files, principal.localUserId()));
    }

    @GetMapping("/summary/{reportId}")
    public ReportResponse getReport(
        @PathVariable String reportId,
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        requireReportId(reportId);
        try {
            accessService.requireOwnership(reportId, principal.localUserId());
            return mapper.toResponse(reportingService.getReport(reportId));
        } catch (ProcessingException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProcessingException(
                "Error retrieving expense report",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "REPORT_RETRIEVAL_ERROR",
                ex
            );
        }
    }

    @GetMapping("/export/{reportId}")
    public ResponseEntity<ByteArrayResource> exportReport(
        @PathVariable String reportId,
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        requireReportId(reportId);
        accessService.requireOwnership(reportId, principal.localUserId());
        byte[] bytes = reportingService.exportReportCsvV2(reportId);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"expenses_" + reportId + ".csv\"");
        headers.add(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");
        return ResponseEntity.ok().headers(headers).contentLength(bytes.length)
            .body(new ByteArrayResource(bytes));
    }

    private void requireReportId(String reportId) {
        if (reportId == null || reportId.isBlank()) {
            throw new ProcessingException(
                "ReportId is required and cannot be empty",
                HttpStatus.BAD_REQUEST,
                "INVALID_REPORT_ID"
            );
        }
    }
}
