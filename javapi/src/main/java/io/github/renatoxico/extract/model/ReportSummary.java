package io.github.renatoxico.extract.model;

import java.math.BigDecimal;
import java.time.Instant;

public record ReportSummary(
    String reportId,
    Instant createdAt,
    BigDecimal total,
    Long countExpenses
) {}
