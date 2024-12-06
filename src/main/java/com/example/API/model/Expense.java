package com.example.API.model;

public class Expense {
    private Double value = 0.00;

    private String transactionName = "";

    private String transactionType = "";

    private String date = "";

    public Expense (Double value, String name, String date, String type){
        this.transactionName = name;
        this.transactionType = type;
        this.value = value;
        this.date = date;
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

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
