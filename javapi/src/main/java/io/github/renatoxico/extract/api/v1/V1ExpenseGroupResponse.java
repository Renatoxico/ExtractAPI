package io.github.renatoxico.extract.api.v1;

import java.math.BigDecimal;

public record V1ExpenseGroupResponse(String expenseName, BigDecimal total, Long instances, String category) {}
