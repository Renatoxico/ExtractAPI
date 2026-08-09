package io.github.renatoxico.extract.api.v2;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(
    Long expenseId,
    String expenseName,
    BigDecimal amount,
    LocalDate date,
    String category
) {}
