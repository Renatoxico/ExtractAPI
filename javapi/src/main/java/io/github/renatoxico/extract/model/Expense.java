package io.github.renatoxico.extract.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @Column(name = "category_source", nullable = false, columnDefinition = "varchar(16) default 'RULE'")
    private String categorySource = "RULE";

    @Column(name = "source", nullable = false, columnDefinition = "varchar(16) default 'EXTRACTED'")
    private String source = "EXTRACTED";

    @Column(name = "date")
    private String date;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private ExpenseReport report;

    public Expense (ExpenseReport report, BigDecimal amount, String expenseName, String date, String category){
        this.expenseName = expenseName;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.report = report;
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

    public String getCategorySource() {
        return categorySource;
    }

    public void setCategorySource(String categorySource) {
        this.categorySource = categorySource;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getReportId() {
        return report == null ? null : report.getId();
    }

    public ExpenseReport getReport() {
        return report;
    }

    public void setReport(ExpenseReport report) {
        this.report = report;
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
