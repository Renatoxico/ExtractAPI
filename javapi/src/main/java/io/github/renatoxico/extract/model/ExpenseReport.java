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
    @Column(name = "session_id", length = 32)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser owner;

    @Column(name = "creation_date", nullable = false, updatable = false)
    private Instant creationDate;

    protected ExpenseReport() {
    }

    public ExpenseReport(String sessionId, AppUser owner) {
        this.sessionId = sessionId;
        this.owner = owner;
        this.creationDate = Instant.now();
    }

    public String getSessionId() {
        return sessionId;
    }

    public AppUser getOwner() {
        return owner;
    }

    public Instant getCreationDate() {
        return creationDate;
    }
}
