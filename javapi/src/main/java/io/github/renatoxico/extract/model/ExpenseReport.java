package io.github.renatoxico.extract.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "expense_report")
public class ExpenseReport {
    @Id
    @Column(name = "id", length = 32)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExpenseReport() {
    }

    public ExpenseReport(String reportId, AppUser owner) {
        this.id = reportId;
        this.owner = owner;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public AppUser getOwner() {
        return owner;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
