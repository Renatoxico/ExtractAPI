package com.example.api.service;

import com.example.api.model.Expense;
import com.example.api.model.ExpenseDTO;
import com.example.api.model.ExpensesGroupedDTO;
import com.example.api.repo.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ObjectifierService {
    private static final Logger LOG = LoggerFactory.getLogger(ObjectifierService.class);
    private static final Pattern datePattern = Pattern.compile("\\b(\\d{2}/\\d{2}(?:/\\d{4})?)\\b");
    private static final Pattern valuePattern = Pattern.compile("(-?\\d{1,3}(?:\\.\\d{3})*,\\d{2})");
    private static final SecureRandom random = new SecureRandom();
    private final ExpenseRepository expenseRepo;

    public ObjectifierService(ExpenseRepository expenseRepo) {
        this.expenseRepo = expenseRepo;
    }

    public void process (String sessionId, String inputText) {
        LOG.info("Enter ObjectifierService.process");
        String[] expensesDoc = splitByLine(inputText);
        List<String> filteredExpenses = getFilterCharges(expensesDoc);
        List<Expense> expensesObj = objectifyExtract(sessionId,filteredExpenses);
        expenseRepo.saveAll(expensesObj);
    }

    public String[] splitByLine (String extractText) {
        LOG.info("Enter ObjectifierService.splitByLine");
        extractText = extractText.replace("\\n", "\n");
        return extractText.split("\\n");

    }

    public List<Expense> objectifyExtract (String sessionId,List<String> expenses) {
        LOG.info("Enter ObjectifierService.objectifyExtract");
        List<Expense> expensesObj = new ArrayList<>();
        expenses.forEach(line -> {
            Matcher dateMatcher = datePattern.matcher(line);
            Matcher valueMatcher = valuePattern.matcher(line);

            if (dateMatcher.find() && valueMatcher.find()){
                String date = dateMatcher.group(1); // Extract date
                String valueStr = valueMatcher.group(1)
                        .replace(".", "")//remove BR decimal
                        .replace(",", "."); // Convert to double format
                BigDecimal value = new BigDecimal(valueStr);
                value = value.abs();
                if (!(valueMatcher.start()< dateMatcher.end())) {
                    String description = line.substring(dateMatcher.end(), valueMatcher.start())
                            .replace("|", "")
                            .replaceAll("\\d{4,}", "")
                            .trim();
                    if (!description.isEmpty()
                            && value.compareTo(BigDecimal.ZERO) != 0
                            && !(description.contains("CREDITO") || description.contains("FATURA"))){
                        mapToObj(sessionId, expensesObj, value, description, date);
                    }
                }
            }
        });
        return expensesObj;
    }
    
    public List<String> getFilterCharges (String[] inputText ) {
        LOG.info("Enter ObjectifierService.getFilterCharges");
        List<String> filteredCharges = new ArrayList<>();

        for (String line : inputText) {
            Matcher dateMatcher = datePattern.matcher(line);
            Matcher valueMatcher = valuePattern.matcher(line);

            if (dateMatcher.find() && valueMatcher.find()) {
                //line has necessary items
                //filteredCharges.add(line);
                String firstMatch = line.substring(0, valueMatcher.end());
                String secondMatch = line.substring(valueMatcher.end());

                if (dateMatcher.find() && valueMatcher.find()) {
                    filteredCharges.add(firstMatch);
                    filteredCharges.add(secondMatch);
                }
                else
                    filteredCharges.add(line);
            }
        }
        return filteredCharges;
    }

//    public List<ExpensesGroupedDTO> getExpenseSummary (String sessionId) {
//        List<Object[]> mid = expenseRepo.getGroupedExpenses(sessionId);
//        List<ExpensesGroupedDTO> fin = mid.stream()
//                .map(obj -> new ExpensesGroupedDTO((String) obj[0], (Double) obj[1], (Long) obj[2]))
//                .collect(Collectors.toList());
//        return fin;
//    }

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

    public void mapToObj (String sessionId, List<Expense> expenses, BigDecimal amount, String transaction, String data){
        expenses.add(new Expense(sessionId, amount, transaction, data, ""));
    }

    public String generateId () {
        byte[] randomBytes = new byte[8];
        random.nextBytes(randomBytes);
        String session_id = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes).toLowerCase();
        LOG.info("Generated Session_id: {}", session_id);
        return session_id;
    }

}
