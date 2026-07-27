package com.example.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "expense_report")
public class ExpenseReport {
    @Id
    @Column(name = "session_id", length = 32)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser owner;

    protected ExpenseReport() {
    }

    public ExpenseReport(String sessionId, AppUser owner) {
        this.sessionId = sessionId;
        this.owner = owner;
    }

    public String getSessionId() {
        return sessionId;
    }

    public AppUser getOwner() {
        return owner;
    }
}
