package com.example.api.model;

import java.math.BigDecimal;

public class ExpenseDTO {
    private String expenseName;
    private BigDecimal value;
    private String date;

    public String getExpenseName() {
        return expenseName;
    }

    public ExpenseDTO(String expenseName, BigDecimal value, String date) {
        this.expenseName = expenseName;
        this.value = value;
        this.date = date;
    }

    public void setExpenseName(String expenseName) {
        this.expenseName = expenseName;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
