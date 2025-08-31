package com.example.api.service;

import com.example.api.model.CategoryMapper;
import com.example.api.model.ExpenseDTO;
import com.example.api.model.ExpensesGroupedDTO;
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
    private final AiProcessorService aiProcessorService;

    public ExpenseReportingService(ExpenseRepository expenseRepo, AiProcessorService aiProcessorService) {
        this.expenseRepo = expenseRepo;
        this.aiProcessorService = aiProcessorService;
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
        res.put("Top10Expenses", mapGroupedExpenses(expenseRepo.getTopExpenses(sessionId)));
        res.put("AllExpenses", mapAllExpenses(expenseRepo.getAllExpenses(sessionId)));

        List<Object[]> biggest = expenseRepo.getBiggestExpense(sessionId);
        if (!biggest.isEmpty()) {
            Object[] obj = biggest.getFirst();
            res.put("BiggestSingularExpense", new ExpenseDTO((String) obj[0], (BigDecimal) obj[2], (String) obj[1],(String) obj[3]));
        } else {
            res.put("BiggestSingularExpense", null);
        }

        return res;
    }

    public void getAiEnrichedReport() {
        List<CategoryMapper> enrichedExpenses = getExpenseNames();
        try {
            enrichedExpenses = aiProcessorService.processWithAI(enrichedExpenses);
            for (CategoryMapper cm : enrichedExpenses) {
                expenseRepo.updateTransactionType(cm.getExpenseName(), cm.getTransactionType());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
}
