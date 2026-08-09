package io.github.renatoxico.extract.model;

import java.math.BigDecimal;

public record ExpenseData(
    Long expenseId,
    String expenseName,
    BigDecimal amount,
    String date,
    String category
) {}
