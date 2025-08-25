package com.example.api.model;

import java.math.BigDecimal;

public class ExpenseDTO {
    private String expenseName;
    private BigDecimal value;
    private String date;
    private String category;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getExpenseName() {
        return expenseName;
    }

    public ExpenseDTO(String expenseName, BigDecimal value, String date, String category) {
        this.expenseName = expenseName;
        this.value = value;
        this.date = date;
        this.category = category;
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
