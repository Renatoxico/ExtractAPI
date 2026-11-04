package io.github.renatoxico.extract.model;

import java.math.BigDecimal;

public record ExpenseData(
    Long expenseId,
    String expenseName,
    BigDecimal amount,
    String date,
    String category,
    String categorySource,
    String source
) {
    public ExpenseData(Long expenseId, String expenseName, BigDecimal amount, String date, String category) {
        this(expenseId, expenseName, amount, date, category, null, null);
    }
}
