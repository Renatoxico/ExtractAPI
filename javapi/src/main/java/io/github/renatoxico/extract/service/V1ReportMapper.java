package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.api.v1.V1CategorySummaryResponse;
import io.github.renatoxico.extract.api.v1.V1DaySummaryResponse;
import io.github.renatoxico.extract.api.v1.V1ExpenseGroupResponse;
import io.github.renatoxico.extract.api.v1.V1ExpenseResponse;
import io.github.renatoxico.extract.api.v1.V1ReportResponse;
import io.github.renatoxico.extract.model.DaySummary;
import io.github.renatoxico.extract.model.ExpenseData;
import io.github.renatoxico.extract.model.ReportData;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Comparator;

@Component
public class V1ReportMapper {
    private static final String FALLBACK_CATEGORY = "Outros / Transferências";

    public V1ReportResponse toResponse(ReportData report) {
        List<V1DaySummaryResponse> notableDays = List.of(
            toDay(report.highlights().mostActiveDay()),
            toDay(report.highlights().highestSpendingDay())
        );
        return new V1ReportResponse(
            report.expenseGroups().stream()
                .map(group -> new V1ExpenseGroupResponse(
                    group.expenseName(), group.totalAmount(), group.occurrenceCount(), displayCategory(group.category())))
                .toList(),
            notableDays,
            report.expenses().stream().map(this::toExpense).toList(),
            report.categorySummaries().stream()
                .filter(summary -> summary.category() != null)
                .sorted(Comparator.comparing(summary -> summary.totalAmount()))
                .map(summary -> new V1CategorySummaryResponse(summary.totalAmount(), summary.category()))
                .toList(),
            toExpense(report.highlights().largestExpense()),
            report.reportId()
        );
    }

    private V1ExpenseResponse toExpense(ExpenseData expense) {
        return new V1ExpenseResponse(
            expense.expenseName(), expense.amount(), expense.date(), displayCategory(expense.category()));
    }

    private V1DaySummaryResponse toDay(DaySummary day) {
        return new V1DaySummaryResponse(day.date(), day.transactionCount(), day.totalAmount());
    }

    private String displayCategory(String category) {
        return category == null || category.isBlank() ? FALLBACK_CATEGORY : category;
    }
}
