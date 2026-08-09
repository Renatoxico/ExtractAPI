package io.github.renatoxico.extract.api.v2;

import java.math.BigDecimal;

public record CategorySummaryResponse(
    String category,
    BigDecimal totalAmount,
    Long occurrenceCount
) {}
