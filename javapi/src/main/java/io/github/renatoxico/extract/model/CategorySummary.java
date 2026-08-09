package io.github.renatoxico.extract.model;

import java.math.BigDecimal;

public record CategorySummary(
    String category,
    BigDecimal totalAmount,
    Long occurrenceCount
) {}
