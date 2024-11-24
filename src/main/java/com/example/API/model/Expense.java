package com.example.API.model;

public class Expense {
    private Double value = 0.00;

    private String transactionName = "";

    private Integer transactionType = 0;

    public Expense (Double value, String name, Integer type){
        this.transactionName = name;
        this.transactionType = type;
        this.value = value;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getTransactionName() {
        return transactionName;
    }

    public void setTransactionName(String transactionName) {
        this.transactionName = transactionName;
    }

    public Integer getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(Integer transactionType) {
        this.transactionType = transactionType;
    }
}
