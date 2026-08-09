package io.github.renatoxico.extract.api.v1;

import java.math.BigDecimal;

public record V1DaySummaryResponse(String date, long transactions, BigDecimal total) {}
