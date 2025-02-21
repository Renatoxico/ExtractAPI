package com.example.API.service;

import ch.qos.logback.core.net.SyslogOutputStream;
import com.example.API.model.Expense;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ObjectifierService {
    private static final Logger LOG = LoggerFactory.getLogger(ObjectifierService.class);
    private static final Pattern datePattern = Pattern.compile("\\b(\\d{2}/\\d{2}(?:/\\d{4})?)\\b");
    private static final Pattern valuePattern = Pattern.compile("(-?\\d{1,3}(?:\\.\\d{3})*,\\d{2})");

    public void process (String inputText) {
        String[] expensesDoc = splitByLine(inputText);
        List<String> filteredExpenses = getFilterCharges(expensesDoc);
        List<Expense> expensesObj = objectifyExtract(filteredExpenses);
        for (Expense e : expensesObj) {
            LOG.info(e.toString());
        }
    }

    public String[] splitByLine (String extractText) {
        extractText = extractText.replace("\\n", "\n");
        return extractText.split("\\n");

    }

    public List<Expense> objectifyExtract (List<String> expenses) {
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

                mapToObj(expensesObj, value, description, date);
            }
        });
        return expensesObj;
    }
    
    public List<String> getFilterCharges (String[] inputText ) {
        List<String> filteredCharges = new ArrayList<>();

        for (int i = 0; i < inputText.length; i++) {
            Matcher dateMatcher = datePattern.matcher(inputText[i]);
            Matcher valueMatcher = valuePattern.matcher(inputText[i]);

            if (dateMatcher.find() && valueMatcher.find() && inputText[i].charAt(0) == '|') {
                //line has necessary items
                filteredCharges.add(inputText[i]);
            }
        }
        return filteredCharges;
    }

    public void mapToObj (List<Expense> expenses, Double amount, String transaction, String data){
        expenses.add(new Expense(amount, transaction, data, ""));
    }
}
