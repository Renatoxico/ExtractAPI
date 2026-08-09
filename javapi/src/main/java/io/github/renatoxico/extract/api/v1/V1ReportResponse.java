package io.github.renatoxico.extract.api.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record V1ReportResponse(
    @JsonProperty("SmartGroupExpenselist") List<V1ExpenseGroupResponse> expenseGroups,
    @JsonProperty("NotableDays") List<V1DaySummaryResponse> notableDays,
    @JsonProperty("AllExpenses") List<V1ExpenseResponse> expenses,
    @JsonProperty("ExpensesByCategory") List<V1CategorySummaryResponse> categorySummaries,
    @JsonProperty("BiggestSingularExpense") V1ExpenseResponse largestExpense,
    @JsonProperty("sessionToken") String reportId
) {}
