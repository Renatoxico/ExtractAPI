package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.api.v2.CategorySummaryResponse;
import io.github.renatoxico.extract.api.v2.DaySummaryResponse;
import io.github.renatoxico.extract.api.v2.ExpenseGroupResponse;
import io.github.renatoxico.extract.api.v2.ExpenseResponse;
import io.github.renatoxico.extract.api.v2.ReportHighlightsResponse;
import io.github.renatoxico.extract.api.v2.ReportResponse;
import io.github.renatoxico.extract.api.v2.ReportSummaryResponse;
import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.model.DaySummary;
import io.github.renatoxico.extract.model.ExpenseData;
import io.github.renatoxico.extract.model.ReportData;
import io.github.renatoxico.extract.model.ReportSummary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class V2ReportMapper {
    private static final DateTimeFormatter LEGACY_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    public ReportResponse toResponse(ReportData report) {
        return new ReportResponse(
            report.reportId(),
            report.createdAt(),
            report.expenses().stream().map(this::toExpense).toList(),
            report.expenseGroups().stream()
                .map(group -> new ExpenseGroupResponse(
                    group.expenseName(), group.totalAmount(), group.occurrenceCount(), group.category()))
                .toList(),
            report.categorySummaries().stream()
                .map(summary -> new CategorySummaryResponse(
                    summary.category(), summary.totalAmount(), summary.occurrenceCount()))
                .toList(),
            new ReportHighlightsResponse(
                toExpense(report.highlights().largestExpense()),
                toDay(report.highlights().mostActiveDay()),
                toDay(report.highlights().highestSpendingDay())
            )
        );
    }

    public ReportSummaryResponse toSummaryResponse(ReportSummary summary) {
        return new ReportSummaryResponse(
            summary.reportId(),
            summary.createdAt(),
            summary.total(),
            summary.countExpenses()
        );
    }

    LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date, LEGACY_DATE);
        } catch (DateTimeParseException | NullPointerException ex) {
            throw new ProcessingException(
                "Report contains an invalid expense date",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "REPORT_DATA_INVALID",
                ex
            );
        }
    }

    private ExpenseResponse toExpense(ExpenseData expense) {
        if (expense == null) {
            return null;
        }
        return new ExpenseResponse(
            expense.expenseId(), expense.expenseName(), expense.amount(), parseDate(expense.date()), expense.category());
    }

    private DaySummaryResponse toDay(DaySummary day) {
        if (day == null) {
            return null;
        }
        return new DaySummaryResponse(parseDate(day.date()), day.transactionCount(), day.totalAmount());
    }
}
