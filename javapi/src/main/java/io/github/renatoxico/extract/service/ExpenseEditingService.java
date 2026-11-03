package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.api.v2.CategoryCorrectionRequest;
import io.github.renatoxico.extract.api.v2.ManualExpenseRequest;
import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.model.Expense;
import io.github.renatoxico.extract.model.ExpenseCategoryChange;
import io.github.renatoxico.extract.model.ReportData;
import io.github.renatoxico.extract.repo.ExpenseCategoryChangeRepository;
import io.github.renatoxico.extract.repo.ExpenseReportRepository;
import io.github.renatoxico.extract.repo.ExpenseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class ExpenseEditingService {
    private final ExpenseReportAccessService accessService;
    private final ExpenseRepository expenses;
    private final ExpenseReportRepository reports;
    private final ExpenseCategoryChangeRepository changes;
    private final ExpenseReportingService reportingService;

    public ExpenseEditingService(ExpenseReportAccessService accessService, ExpenseRepository expenses,
                                 ExpenseReportRepository reports, ExpenseCategoryChangeRepository changes,
                                 ExpenseReportingService reportingService) {
        this.accessService = accessService;
        this.expenses = expenses;
        this.reports = reports;
        this.changes = changes;
        this.reportingService = reportingService;
    }

    @Transactional
    public ReportData correctCategory(String reportId, Long userId, CategoryCorrectionRequest request) {
        accessService.requireOwnership(reportId, userId);
        requireText(request.expenseName(), "expenseName");
        requireCategory(request.newCategory());
        // Exact merchant name within this report, as used by the report's grouped expenses.
        // Row locks serialize repeat edits so every audit entry has the actual previous value.
        var matches = expenses.lockByReportAndName(reportId, request.expenseName());
        if (matches.isEmpty()) throw expenseNotFound();
        for (Expense expense : matches) {
            changes.save(new ExpenseCategoryChange(expense, request.newCategory()));
            expense.setCategory(request.newCategory());
            expense.setCategorySource("USER");
        }
        expenses.flush();
        return reportingService.getReport(reportId);
    }

    @Transactional
    public ReportData removeExpense(String reportId, Long userId, Long expenseId) {
        accessService.requireOwnership(reportId, userId);
        if (expenseId <= 0) throw invalid("expenseId must be positive");
        expenses.lockByReportAndId(reportId, expenseId).orElseThrow(this::expenseNotFound);
        // Database cascades audit history; clear managed references to the deleted rows.
        expenses.deleteExpenseInReport(reportId, expenseId);
        return reportingService.getReport(reportId);
    }

    @Transactional
    public ReportData addExpense(String reportId, Long userId, ManualExpenseRequest request) {
        accessService.requireOwnership(reportId, userId);
        requireText(request.name(), "name");
        requireCategory(request.category());
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw invalid("amount must be greater than zero");
        }
        LocalDate date;
        try {
            if (request.date() == null || !request.date().matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw invalid("date must be a real date in yyyy-MM-dd format");
            }
            date = LocalDate.parse(request.date(), DateTimeFormatter.ISO_LOCAL_DATE);
            if (date.getYear() < 1) throw invalid("date year must be positive");
        } catch (DateTimeParseException ex) {
            throw invalid("date must be a real date in yyyy-MM-dd format");
        }
        Expense expense = new Expense(reports.getReferenceById(reportId), request.amount(),
            request.name().trim(), date.format(DateTimeFormatter.ofPattern("dd/MM/uuuu")), request.category());
        expense.setSource("MANUAL");
        expense.setCategorySource("USER");
        expenses.saveAndFlush(expense);
        return reportingService.getReport(reportId);
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw invalid(field + " is required");
    }

    private void requireCategory(String category) {
        if (!AiCategoryResponseParser.isValidCategory(category)) throw invalid("category must be an allowed category");
    }

    private ProcessingException invalid(String message) {
        return new ProcessingException(message, HttpStatus.BAD_REQUEST, "INVALID_EXPENSE");
    }

    private ProcessingException expenseNotFound() {
        return new ProcessingException("No expense found in the provided report", HttpStatus.NOT_FOUND, "EXPENSE_NOT_FOUND");
    }
}
