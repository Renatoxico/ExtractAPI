package com.example.api.service;

import com.example.api.model.ExpenseDTO;
import com.example.api.model.ExpensesGroupedDTO;
import com.example.api.repo.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExpenseReportingService {
    private static final Logger LOG = LoggerFactory.getLogger(ExpenseReportingService.class);
    private final ExpenseRepository expenseRepo;

    public ExpenseReportingService(ExpenseRepository expenseRepo) {
        this.expenseRepo = expenseRepo;
    }

    public Map<String,Object> getFullReport (String sessionId) {
        Map<String,Object> res = new HashMap<>();
        List<Object[]> mid = expenseRepo.getGroupedExpenses(sessionId);
        res.put("SmartGroupExpenselist", mid.stream().map(
                obj -> {
                    BigDecimal val = (BigDecimal) obj[1];
                    Double value = val.doubleValue();
                    return new ExpensesGroupedDTO((String) obj[0], val, (Long) obj[2]);
                }
        ).collect(Collectors.toList()));
        mid.clear();

        mid = expenseRepo.getTopExpenses(sessionId);
        res.put("Top10Expenses",mid.stream().map(
                obj -> {
                    BigDecimal val = (BigDecimal) obj[1];
                    Double value = val.doubleValue();
                    return new ExpensesGroupedDTO((String) obj[0], val, (Long) obj[2]);
                }
        ).collect(Collectors.toList()));
        mid.clear();

        mid = expenseRepo.getAllExpenses(sessionId);
        res.put("AllExpenses", mid.stream().map(
                obj -> new ExpenseDTO((String) obj[0], (BigDecimal) obj[2], (String) obj[1])
        ).collect(Collectors.toList()));
        mid.clear();

        mid = expenseRepo.getBiggestExpense(sessionId);
        res.put("BiggestSingularExpense", mid.stream().map(
                obj -> new ExpenseDTO((String) obj[0], (BigDecimal) obj[2], (String) obj[1])
        ).collect(Collectors.toList()));//this shouldn't be a list
        mid.clear();

        return res;
    }
}
