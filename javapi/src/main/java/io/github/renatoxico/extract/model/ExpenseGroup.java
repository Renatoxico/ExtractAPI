package io.github.renatoxico.extract.model;

import java.math.BigDecimal;

public record ExpenseGroup(
    String expenseName,
    BigDecimal totalAmount,
    Long occurrenceCount,
    String category
) {}
