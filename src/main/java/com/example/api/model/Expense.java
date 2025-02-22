package com.example.api.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tbExpense")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double value;

    private String transactionName;

    private String transactionType;

    private String date;

    private String sessionId;

    public Expense (String sessionId, Double value, String name, String date, String type){
        this.transactionName = name;
        this.transactionType = type;
        this.value = value;
        this.date = date;
        this.sessionId = sessionId;
    }

    public Expense() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String toString() {
        return "Expense{" +
                "value=" + value +
                ", transactionName='" + transactionName + '\'' +
                ", date='" + date + '\'' +
                '}';
    }
}
