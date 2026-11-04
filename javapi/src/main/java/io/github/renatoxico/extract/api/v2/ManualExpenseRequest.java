package io.github.renatoxico.extract.api.v2;

import java.math.BigDecimal;

public record ManualExpenseRequest(String name, BigDecimal amount, String date, String category) {}
