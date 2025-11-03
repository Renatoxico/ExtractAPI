package com.example.api.service;

import com.example.api.model.*;
import com.example.api.repo.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        byte[] randomBytes = new byte[8];
        random.nextBytes(randomBytes);
        String session_id = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes).toLowerCase();
        LOG.info("Generated Session_id: {}", session_id);
        return session_id;
    }

    public Map<String, Object> getFullReport(String sessionId) {
        Map<String, Object> res = new HashMap<>();

        res.put("SmartGroupExpenselist", mapGroupedExpenses(expenseRepo.getGroupedExpenses(sessionId)));
        res.put("Top10Expenses", mapAllExpenses(expenseRepo.getTopExpenses(sessionId)));
        res.put("AllExpenses", mapAllExpenses(expenseRepo.getAllExpenses(sessionId)));
        res.put("ExpensesByCategory", mapByCategory(expenseRepo.getExpensesByType(sessionId)));

        List<Object[]> biggest = expenseRepo.getBiggestExpense(sessionId);
        if (!biggest.isEmpty()) {
            Object[] obj = biggest.getFirst();
            res.put("BiggestSingularExpense", new ExpenseDTO((String) obj[0], (BigDecimal) obj[2], (String) obj[1],(String) obj[3]));
        } else {
            res.put("BiggestSingularExpense", null);
        }

        return res;
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
        List<CategoryMapper> expenses = expenseRepo.getExpenseNames().stream()
                .map(obj -> new CategoryMapper((String) obj[0], (String) obj[1])).toList();
        return expenses;
    }

    private List<ExpensesGroupedDTO> mapGroupedExpenses(List<Object[]> list) {
        return list.stream()
                .map(obj -> new ExpensesGroupedDTO((String) obj[0], (BigDecimal) obj[1], (Long) obj[2], (String) obj[3]))
                .collect(Collectors.toList());
    }

    private List<ExpenseDTO> mapAllExpenses(List<Object[]> list) {
        return list.stream()
                .map(obj -> new ExpenseDTO((String) obj[0], (BigDecimal) obj[2], (String) obj[1], (String) obj[3]))
                .collect(Collectors.toList());
    }

    private List<ExpensesCategories> mapByCategory(List<Object[]> list) {
        return list.stream()
                .map(obj -> new ExpensesCategories((BigDecimal) obj[0], (String) obj[1]))
                .collect(Collectors.toList());
    }
}
