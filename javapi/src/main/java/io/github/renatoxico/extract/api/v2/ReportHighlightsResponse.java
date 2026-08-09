package io.github.renatoxico.extract.api.v2;

public record ReportHighlightsResponse(
    ExpenseResponse largestExpense,
    DaySummaryResponse mostActiveDay,
    DaySummaryResponse highestSpendingDay
) {}
