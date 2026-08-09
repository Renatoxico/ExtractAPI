package io.github.renatoxico.extract.model;

import java.math.BigDecimal;

public record DaySummary(
    String date,
    long transactionCount,
    BigDecimal totalAmount
) {}
