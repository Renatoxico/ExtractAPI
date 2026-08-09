package io.github.renatoxico.extract.api.v2;

import java.time.Instant;
import java.util.List;

public record ReportResponse(
    String reportId,
    Instant createdAt,
    List<ExpenseResponse> expenses,
    List<ExpenseGroupResponse> expenseGroups,
    List<CategorySummaryResponse> categorySummaries,
    ReportHighlightsResponse highlights
) {}
