package io.github.renatoxico.extract.api.v2;

import java.math.BigDecimal;

public record ExpenseGroupResponse(
    String expenseName,
    BigDecimal totalAmount,
    Long occurrenceCount,
    String category
) {}
