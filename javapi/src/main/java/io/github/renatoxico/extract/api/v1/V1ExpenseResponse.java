package io.github.renatoxico.extract.api.v1;

import java.math.BigDecimal;

public record V1ExpenseResponse(String expenseName, BigDecimal value, String date, String category) {}
