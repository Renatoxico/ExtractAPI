package io.github.renatoxico.extract.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "expense")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "expense_name")
    private String expenseName;

    @Column(name = "category")
    private String category;

    @Column(name = "date")
    private String date;

    @Column(name = "report_id", nullable = false, length = 32)
    private String reportId;

    public Expense (String reportId, BigDecimal amount, String expenseName, String date, String category){
        this.expenseName = expenseName;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.reportId = reportId;
    }

    public Expense() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getExpenseName() {
        return expenseName;
    }

    public void setExpenseName(String expenseName) {
        this.expenseName = expenseName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    @Override
    public String toString() {
        return "Expense{" +
                "amount=" + amount +
                ", expenseName='" + expenseName + '\'' +
                ", date='" + date + '\'' +
                '}';
    }
}
