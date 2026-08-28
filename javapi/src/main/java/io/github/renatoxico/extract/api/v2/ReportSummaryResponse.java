package io.github.renatoxico.extract.api.v2;

import java.math.BigDecimal;
import java.time.Instant;

public record ReportSummaryResponse(
    String reportId,
    Instant createdAt,
    BigDecimal total,
    Long countExpenses
) {}
