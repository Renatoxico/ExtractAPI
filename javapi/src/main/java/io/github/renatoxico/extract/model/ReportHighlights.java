package io.github.renatoxico.extract.model;

public record ReportHighlights(
    ExpenseData largestExpense,
    DaySummary mostActiveDay,
    DaySummary highestSpendingDay
) {}
