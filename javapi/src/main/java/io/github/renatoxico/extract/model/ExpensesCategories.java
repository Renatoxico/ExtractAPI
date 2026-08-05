package io.github.renatoxico.extract.model;

import java.math.BigDecimal;

public class ExpensesCategories {
    private BigDecimal value;
    private String category;

    public ExpensesCategories(BigDecimal value, String category) {
        this.value = value;
        this.category = category;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
