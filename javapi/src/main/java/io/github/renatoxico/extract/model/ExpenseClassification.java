package io.github.renatoxico.extract.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "expense_classification")
public class ExpenseClassification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_name", nullable = false, unique = true)
    private String expenseName;

    private String category;

    @Enumerated(EnumType.STRING)
    private Source source;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ExpenseClassification() {
    }

    public ExpenseClassification(String expenseName) {
        this.expenseName = expenseName;
    }

    public Long getId() {
        return id;
    }

    public String getExpenseName() {
        return expenseName;
    }

    public String getCategory() {
        return category;
    }

    public Source getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public enum Source {
        AI,
        RULE,
        MANUAL
    }
}
