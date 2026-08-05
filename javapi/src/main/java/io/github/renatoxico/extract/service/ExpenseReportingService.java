package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.model.*;
import io.github.renatoxico.extract.repo.ExpenseRepository;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
public class ExpenseReportingService {
    private static final Logger LOG = LoggerFactory.getLogger(ExpenseReportingService.class);
    private final ExpenseRepository expenseRepo;
    private static final SecureRandom random = new SecureRandom();
    private final ValidationService validationService;

    public ExpenseReportingService(ExpenseRepository expenseRepo, ValidationService validationService) {
        this.expenseRepo = expenseRepo;
        this.validationService = validationService;
    }

    public String generateId () {
        byte[] randomBytes = new byte[24];
        random.nextBytes(randomBytes);
        String session_id = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes).toLowerCase();
        LOG.info("Generated Session_id: {}", session_id);
        return session_id;
    }

    public ReportExport getFullReport(String sessionId) {
        try {
            LOG.info("Generating full report for session: {}", sessionId);
            List<ExpenseDTO> allExpenses = expenseRepo.getAllExpenses(sessionId);

            if (allExpenses == null || allExpenses.isEmpty()) {
                LOG.warn("No data found for session: {}", sessionId);
                throw new ProcessingException(
                        "No data found for the provided session ID",
                        HttpStatus.NOT_FOUND,
                        "SESSION_NOT_FOUND"
                );
            }

            ReportExport res = new ReportExport(
                expenseRepo.getGroupedExpenses(sessionId),
                expenseRepo.getNoteableDays(sessionId),
                allExpenses,
                expenseRepo.getExpensesByType(sessionId),
                allExpenses.isEmpty() ? null : allExpenses.getFirst(),
                sessionId);

            LOG.info("Successfully generated full report for session: {}", sessionId);
            return res;
        } catch (Exception ex) {
            LOG.error("Error generating report for session {}: {}", sessionId, ex.getMessage(), ex);
            throw ex;
        }
    }

    public List<Expense> updateExpenses(String SessionId) {
        List<Expense> expenses = expenseRepo.getAllExpenses2(SessionId);
        expenses = validationService.validateExpenses(expenses);
        for (Expense expense : expenses) {
            expense = expenseRepo.save(expense);
        }
        return expenses;
    }
    public List<Expense> getAllExpensesEntities(String sessionId) {
        return expenseRepo.getAllExpenses2(sessionId);
    }

    public List<CategoryMapper> getExpenseNames() {
        List<CategoryMapper> expenses = expenseRepo.getExpenseNames();
        return expenses;
    }

    public byte[] exportReportCSV(String sessionId) {
        List<Expense> expenses = expenseRepo.getAllExpenses2(sessionId);
        if (expenses == null || expenses.isEmpty()) {
            LOG.warn("No data found for session (export): {}", sessionId);
            throw new ProcessingException(
                    "No data found for the provided session ID",
                    HttpStatus.NOT_FOUND,
                    "SESSION_NOT_FOUND"
            );
        }
        String[] header = {"id", "transactionName", "transactionType", "value", "date", "sessionId"};
        StringWriter sw = new StringWriter();
        try (CSVWriter csvWriter = new CSVWriter(sw)) {
            csvWriter.writeNext(header);
            for (io.github.renatoxico.extract.model.Expense e : expenses) {
                String[] row = new String[]{
                        e.getId() == null ? "" : e.getId().toString(),
                        e.getTransactionName() == null ? "" : e.getTransactionName(),
                        e.getTransactionType() == null ? "" : e.getTransactionType(),
                        e.getValue() == null ? "" : e.getValue().toString(),
                        e.getDate() == null ? "" : e.getDate(),
                        e.getSessionId() == null ? "" : e.getSessionId()
                };
                csvWriter.writeNext(row);
            }
        } catch (Exception ex) {
            LOG.error("Error exporting CSV for session {}: {}", sessionId, ex.getMessage(), ex);
            throw new ProcessingException(
                    "Error exporting CSV",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "CSV_EXPORT_ERROR",
                    ex
            );
        }

        return sw.toString().getBytes(StandardCharsets.UTF_8);

    }
}
