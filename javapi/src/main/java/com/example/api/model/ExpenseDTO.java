package com.example.api.model;

public class ExpenseDTO {
    private String expenseName;
    private Double value;
    private String date;

    public String getExpenseName() {
        return expenseName;
    }

    public ExpenseDTO(String expenseName, Double value, String date) {
        this.expenseName = expenseName;
        this.value = value;
        this.date = date;
    }

    public void setExpenseName(String expenseName) {
        this.expenseName = expenseName;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
