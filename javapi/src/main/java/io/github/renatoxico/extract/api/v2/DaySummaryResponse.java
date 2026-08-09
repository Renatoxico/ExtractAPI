package io.github.renatoxico.extract.api.v2;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DaySummaryResponse(
    LocalDate date,
    long transactionCount,
    BigDecimal totalAmount
) {}
