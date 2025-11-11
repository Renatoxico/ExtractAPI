package com.example.api.service;

import com.example.api.model.Expense;
import com.example.api.repo.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ObjectifierService {
    private static final Logger LOG = LoggerFactory.getLogger(ObjectifierService.class);
    private static final Pattern datePattern = Pattern.compile("\\b(\\d{2}/\\d{2}(?:/\\d{4})?)\\b");
    private static final Pattern valuePattern = Pattern.compile("(-?\\d{1,3}(?:\\.\\d{3})*,\\d{2})");
    private final ExpenseRepository expenseRepo;
    private final ValidationService validationService;

    public ObjectifierService(ExpenseRepository expenseRepo, ValidationService validationService) {
        this.expenseRepo = expenseRepo;
        this.validationService = validationService;
    }

    public void process (String sessionId, String inputText) {
        LOG.info("Enter ObjectifierService.process");
        String[] expensesDoc = splitByLine(inputText);
        List<String> filteredExpenses = getFilterCharges(expensesDoc);
        List<Expense> expensesObj = objectifyExtract(sessionId,filteredExpenses);
        expensesObj = validationService.validateExpenses(expensesObj);
        expenseRepo.saveAll(expensesObj);
    }

    public String[] splitByLine (String extractText) {
        //LOG.info("Enter ObjectifierService.splitByLine");
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
                    if (!description.isEmpty()//no blank expenses
                            && value.compareTo(BigDecimal.ZERO) != 0//no expenses without value
                            && description.matches(".*[a-zA-Z].*") //must have letters
                            && !(description.contains("CREDITO") || description.contains("FATURA") || description.contains("SALDO"))){//ignore balance value
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

    public void mapToObj (String sessionId, List<Expense> expenses, BigDecimal amount, String transaction, String data){
        expenses.add(new Expense(sessionId, amount, transaction, data, ""));
    }

}
