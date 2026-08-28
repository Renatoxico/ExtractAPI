package io.github.renatoxico.extract.controller;

import io.github.renatoxico.extract.model.AuthenticatedUserPrincipal;
import io.github.renatoxico.extract.model.CategorySummary;
import io.github.renatoxico.extract.model.DaySummary;
import io.github.renatoxico.extract.model.ExpenseData;
import io.github.renatoxico.extract.model.ExpenseGroup;
import io.github.renatoxico.extract.model.ReportData;
import io.github.renatoxico.extract.model.ReportHighlights;
import io.github.renatoxico.extract.model.ReportSummary;
import io.github.renatoxico.extract.service.ExpenseReportAccessService;
import io.github.renatoxico.extract.service.ExpenseReportingService;
import io.github.renatoxico.extract.service.ExtractionFacade;
import io.github.renatoxico.extract.service.ExtractorService;
import io.github.renatoxico.extract.service.V2ReportMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ExtractController.class, ExtractV2Controller.class})
@Import(V2ReportMapper.class)
@AutoConfigureMockMvc(addFilters = false)
class ExtractControllerWebMvcTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean ExtractionFacade extractionFacade;
    @MockitoBean ExpenseReportingService reportingService;
    @MockitoBean ExpenseReportAccessService accessService;
    @MockitoBean ExtractorService extractorService;

    private AuthenticatedUserPrincipal principal;
    private MockMultipartFile pdf;

    @BeforeEach
    void setUp() {
        principal = new AuthenticatedUserPrincipal(
            42L, "firebase-uid", "user@example.com", "User", null, true);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        pdf = new MockMultipartFile("file", "test.pdf", "application/pdf", "pdf".getBytes());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void v2SummaryUsesOnlySemanticCamelCaseContract() throws Exception {
        when(reportingService.getReport("report-123")).thenReturn(report());

        mockMvc.perform(get("/v2/extract/summary/report-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reportId").value("report-123"))
            .andExpect(jsonPath("$.createdAt").value("2025-03-17T10:15:30Z"))
            .andExpect(jsonPath("$.expenses[0].expenseId").value(1))
            .andExpect(jsonPath("$.expenses[0].expenseName").value("MARKET"))
            .andExpect(jsonPath("$.expenses[0].amount").value(100.25))
            .andExpect(jsonPath("$.expenses[0].date").value("2025-03-15"))
            .andExpect(jsonPath("$.expenses[1].category").doesNotExist())
            .andExpect(jsonPath("$.expenseGroups[0].occurrenceCount").value(2))
            .andExpect(jsonPath("$.categorySummaries[0].totalAmount").value(100.25))
            .andExpect(jsonPath("$.categorySummaries[0].occurrenceCount").value(1))
            .andExpect(jsonPath("$.categorySummaries[1].category").doesNotExist())
            .andExpect(jsonPath("$.highlights.largestExpense.expenseId").value(1))
            .andExpect(jsonPath("$.highlights.mostActiveDay.transactionCount").value(2))
            .andExpect(jsonPath("$.highlights.highestSpendingDay.totalAmount").value(150.25))
            .andExpect(jsonPath("$.sessionToken").doesNotExist())
            .andExpect(jsonPath("$.SmartGroupExpenselist").doesNotExist());

        verify(accessService).requireOwnership("report-123", 42L);
    }

    @Test
    void v2UploadUsesSharedFacade() throws Exception {
        when(extractionFacade.process(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(42L)))
            .thenReturn(report());

        mockMvc.perform(multipart("/v2/extract").file(pdf))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reportId").value("report-123"));
    }

    @Test
    void v2ReportsReturnsAuthenticatedUsersSummaries() throws Exception {
        when(reportingService.getReportSummaries(42L)).thenReturn(List.of(
            new ReportSummary(
                "report-123",
                Instant.parse("2025-03-17T10:15:30Z"),
                new BigDecimal("150.25"),
                2L
            )
        ));

        mockMvc.perform(get("/v2/extract/reports"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].reportId").value("report-123"))
            .andExpect(jsonPath("$[0].createdAt").value("2025-03-17T10:15:30Z"))
            .andExpect(jsonPath("$[0].total").value(150.25))
            .andExpect(jsonPath("$[0].countExpenses").value(2));

        verify(reportingService).getReportSummaries(42L);
    }

    @Test
    void v2ReportsReturnsEmptyArrayWhenUserHasNoReports() throws Exception {
        when(reportingService.getReportSummaries(42L)).thenReturn(List.of());

        mockMvc.perform(get("/v2/extract/reports"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @Test
    void removedV1ReportRoutesReturnNotFound() throws Exception {
        mockMvc.perform(get("/extract/summary/report-123"))
            .andExpect(status().isNotFound());
        mockMvc.perform(get("/extract/export/report-123"))
            .andExpect(status().isNotFound());
    }

    @Test
    void v2ExportUsesNewCsvContract() throws Exception {
        when(reportingService.exportReportCsvV2("report-123"))
            .thenReturn("expenseId,expenseName,category,amount,date,reportId".getBytes());

        mockMvc.perform(get("/v2/extract/export/report-123"))
            .andExpect(status().isOk())
            .andExpect(content().string("expenseId,expenseName,category,amount,date,reportId"));
    }

    private static ReportData report() {
        ExpenseData largest = new ExpenseData(
            1L, "MARKET", new BigDecimal("100.25"), "15/03/2025", "Supermercado");
        ExpenseData pending = new ExpenseData(
            2L, "PIX JOAO", new BigDecimal("50.00"), "15/03/2025", null);
        DaySummary day = new DaySummary("15/03/2025", 2, new BigDecimal("150.25"));
        return new ReportData(
            "report-123",
            Instant.parse("2025-03-17T10:15:30Z"),
            List.of(largest, pending),
            List.of(new ExpenseGroup("MARKET", new BigDecimal("150.25"), 2L, "Supermercado")),
            List.of(
                new CategorySummary("Supermercado", new BigDecimal("100.25"), 1L),
                new CategorySummary(null, new BigDecimal("50.00"), 1L)),
            new ReportHighlights(largest, day, day)
        );
    }
}
