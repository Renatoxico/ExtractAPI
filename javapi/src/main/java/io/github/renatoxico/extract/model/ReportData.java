package io.github.renatoxico.extract.model;

import java.time.Instant;
import java.util.List;

public record ReportData(
    String reportId,
    Instant createdAt,
    List<ExpenseData> expenses,
    List<ExpenseGroup> expenseGroups,
    List<CategorySummary> categorySummaries,
    ReportHighlights highlights
) {}
