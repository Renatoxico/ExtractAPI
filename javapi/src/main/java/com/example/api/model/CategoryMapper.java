package com.example.api.model;

public class CategoryMapper {
    private String expenseName;
    private String transactionType;

    public String getExpenseName() {
        return expenseName;
    }

    public CategoryMapper(String expenseName, String transactionType) {
        this.expenseName = expenseName;
        this.transactionType = transactionType;
    }

    public void setExpenseName(String expenseName) {
        this.expenseName = expenseName;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
}
