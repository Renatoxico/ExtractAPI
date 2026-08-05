package io.github.renatoxico.extract.model;

import jakarta.persistence.Entity;

import java.math.BigDecimal;

public class NoteableDay {
    private String date;
    private long transactions;
    private BigDecimal total;

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public long getTransactions() {
        return transactions;
    }

    public void setTransactions(long transactions) {
        this.transactions = transactions;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public NoteableDay(String date, long transactions, BigDecimal total) {
        this.date = date;
        this.transactions = transactions;
        this.total = total;
    }
}
