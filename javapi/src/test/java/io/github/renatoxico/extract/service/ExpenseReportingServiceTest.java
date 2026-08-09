package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.model.AppUser;
import io.github.renatoxico.extract.model.CategorySummary;
import io.github.renatoxico.extract.model.Expense;
import io.github.renatoxico.extract.model.ExpenseData;
import io.github.renatoxico.extract.model.ExpenseGroup;
import io.github.renatoxico.extract.model.ExpenseReport;
import io.github.renatoxico.extract.model.ReportData;
import io.github.renatoxico.extract.repo.AppUserRepository;
import io.github.renatoxico.extract.repo.ExpenseReportRepository;
import io.github.renatoxico.extract.repo.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseReportingServiceTest {
    @Mock ExpenseRepository expenseRepository;
    @Mock ExpenseReportRepository reportRepository;
    @Mock AppUserRepository appUserRepository;
    @Mock AppUser owner;

    private ExpenseReportingService service;

    @BeforeEach
    void setUp() {
        service = new ExpenseReportingService(
            expenseRepository, reportRepository, appUserRepository, new V2ReportMapper());
    }

    @Test
    void createReportGeneratesOpaqueIdAndPersistsOwner() {
        when(appUserRepository.getReferenceById(42L)).thenReturn(owner);

        String reportId = service.createReport(42L);

        assertThat(reportId).hasSize(32).matches("[a-z0-9_-]{32}");
        ArgumentCaptor<ExpenseReport> captor = ArgumentCaptor.forClass(ExpenseReport.class);
        org.mockito.Mockito.verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(reportId);
        assertThat(captor.getValue().getOwner()).isSameAs(owner);
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void getReportBuildsSemanticHighlightsAndPreservesCategoryCounts() {
        String reportId = "report-123";
        ExpenseReport report = new ExpenseReport(reportId, owner);
        List<ExpenseData> expenses = List.of(
            new ExpenseData(1L, "MARKET", new BigDecimal("100.00"), "15/03/2025", "Supermercado"),
            new ExpenseData(2L, "CAFE", new BigDecimal("30.00"), "15/03/2025", null),
            new ExpenseData(3L, "RENT", new BigDecimal("90.00"), "16/03/2025", "Moradia / Contas")
        );
        when(reportRepository.findById(reportId)).thenReturn(Optional.of(report));
        when(expenseRepository.findExpenseDataByReportId(reportId)).thenReturn(expenses);
        when(expenseRepository.findExpenseGroupsByReportId(reportId)).thenReturn(List.of(
            new ExpenseGroup("MARKET", new BigDecimal("100.00"), 1L, "Supermercado")));
        when(expenseRepository.findCategorySummariesByReportId(reportId)).thenReturn(List.of(
            new CategorySummary("Supermercado", new BigDecimal("100.00"), 1L),
            new CategorySummary(null, new BigDecimal("30.00"), 1L),
            new CategorySummary("Moradia / Contas", new BigDecimal("90.00"), 1L)));

        ReportData result = service.getReport(reportId);

        assertThat(result.reportId()).isEqualTo(reportId);
        assertThat(result.categorySummaries()).extracting(CategorySummary::occurrenceCount)
            .containsExactly(1L, 1L, 1L);
        assertThat(result.categorySummaries()).extracting(CategorySummary::occurrenceCount)
            .satisfies(counts -> assertThat(counts.stream().mapToLong(Long::longValue).sum()).isEqualTo(3));
        assertThat(result.highlights().largestExpense().expenseId()).isEqualTo(1L);
        assertThat(result.highlights().mostActiveDay().date()).isEqualTo("15/03/2025");
        assertThat(result.highlights().mostActiveDay().transactionCount()).isEqualTo(2);
        assertThat(result.highlights().highestSpendingDay().date()).isEqualTo("15/03/2025");
    }

    @Test
    void csvContractsUseVersionSpecificHeadersAndDates() {
        Expense expense = new Expense(
            "report-123", new BigDecimal("99.90"), "UBER", "15/03/2025", "Transporte / Auto");
        when(expenseRepository.findAllByReportIdOrderByAmountDescIdAsc("report-123"))
            .thenReturn(List.of(expense));

        String v1 = new String(service.exportReportCsvV1("report-123"), StandardCharsets.UTF_8);
        String v2 = new String(service.exportReportCsvV2("report-123"), StandardCharsets.UTF_8);

        assertThat(v1).contains("transactionName", "transactionType", "value", "sessionId", "15/03/2025");
        assertThat(v2).contains("expenseId", "expenseName", "category", "amount", "reportId", "2025-03-15");
    }

    @Test
    void v2CsvRejectsInvalidHistoricalDate() {
        Expense expense = new Expense("report-123", BigDecimal.ONE, "TEST", "invalid", null);
        when(expenseRepository.findAllByReportIdOrderByAmountDescIdAsc("report-123"))
            .thenReturn(List.of(expense));

        assertThatThrownBy(() -> service.exportReportCsvV2("report-123"))
            .isInstanceOf(ProcessingException.class)
            .hasFieldOrPropertyWithValue("errorCode", "REPORT_DATA_INVALID");
    }
}
