package com.example.api.service;

import com.example.api.model.Expense;
import com.example.api.repo.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        String[] expensesDoc = splitByLine(inputText);
        List<String> filteredExpenses = getFilterCharges(expensesDoc);
        List<Expense> expensesObj = objectifyExtract(sessionId,filteredExpenses);
        expenseRepo.saveAll(expensesObj);
    }

    public String[] splitByLine (String extractText) {
        extractText = extractText.replace("\\n", "\n");
        return extractText.split("\\n");

    }

    public List<Expense> objectifyExtract (String sessionId,List<String> expenses) {
        List<Expense> expensesObj = new ArrayList<>();

        expenses.forEach(line -> {
            Matcher dateMatcher = datePattern.matcher(line);
            Matcher valueMatcher = valuePattern.matcher(line);

            if (dateMatcher.find() && valueMatcher.find()){
                String date = dateMatcher.group(1); // Extract date
                String valueStr = valueMatcher.group(1)
                        .replace(".", "")//remove BR decimal
                        .replace(",", "."); // Convert to double format
                double value = Double.parseDouble(valueStr);
                value = Math.abs(value);
                String description = line.substring(dateMatcher.end(), valueMatcher.start())
                        .replace("|", "")
                        .trim();

                mapToObj(sessionId, expensesObj, value, description, date);
            }
        });
        return expensesObj;
    }
    
    public List<String> getFilterCharges (String[] inputText ) {
        List<String> filteredCharges = new ArrayList<>();

        for (String line : inputText) {
            Matcher dateMatcher = datePattern.matcher(line);
            Matcher valueMatcher = valuePattern.matcher(line);

            if (dateMatcher.find() && valueMatcher.find() && line.charAt(0) == '|') {
                //line has necessary items
                filteredCharges.add(line);
            }
        }
        return filteredCharges;
    }

    public void mapToObj (String sessionId, List<Expense> expenses, Double amount, String transaction, String data){
        expenses.add(new Expense(sessionId, amount, transaction, data, ""));
    }

    public String generateId () {
        byte[] randomBytes = new byte[8];
        random.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes).toLowerCase();
    }
}
