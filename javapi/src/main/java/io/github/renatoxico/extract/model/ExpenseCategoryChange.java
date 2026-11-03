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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Table(name = "expense_category_change")
public class ExpenseCategoryChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ExpenseReport report;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AppUser user;

    @Column(name = "old_category")
    private String oldCategory;

    @Column(name = "new_category", nullable = false)
    private String newCategory;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected ExpenseCategoryChange() {}

    public ExpenseCategoryChange(Expense expense, String newCategory) {
        this.expense = expense;
        this.report = expense.getReport();
        this.user = report.getOwner();
        this.oldCategory = expense.getCategory();
        this.newCategory = newCategory;
        this.changedAt = Instant.now();
    }
}
