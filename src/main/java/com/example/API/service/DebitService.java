package com.example.API.service;

import com.example.API.model.Expense;
import com.example.API.model.ExpenseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DebitService {
    private static final Logger LOG = LoggerFactory.getLogger(DebitService.class);
    private List<Expense> expenses;

    public String getExtractDetails(String ext){
        LOG.info("getExtractDetails - start");
        List<ExpenseType> types = ExpenseType.load();
        StringBuilder res = new StringBuilder();
        List<String> patterns = List.of("(\\d{2}/\\d{2})\\s+.+?\\s+PIX\\s+([A-Z\\s]+)\\s+\\d+\\s+\\d+\\s+\\d+\\s+(\\d+,\\d{2})"
                ,"(\\d{2}/\\d{2}) \\d{4}\\.\\d{4} ([a-zA-Z0-9\\.\\s]+) (\\d+,\\d{2})"
                //"(\\d{2}/\\d{2}) [A-Z\\s]+ ([A-Z\\s]+) (\\d+,\\d{2})"
        );
        patterns.forEach(pattern -> mapExpenses(pattern, ext, res));
        //return ext;
        mapExpensesNoDate("([A-Za-z0-9\\s]+) - (\\d{1,5},\\d{2}) \\d{12}-\\d{12}-", ext, res);
        Map<String, Double> x = classify(expenses, types);
        return res.toString();
    }

    public void mapExpenses(String pattern, String str, StringBuilder output) {
        String ext = str.replaceAll("\\r\\n+", " ");
        Pattern pat = Pattern.compile(pattern);
        Matcher matcher = pat.matcher(ext);

        while (matcher.find()) {
            String data = matcher.group(1);
            String desc = matcher.group(2);
            String amount = matcher.group(3);
            mapToObj(Double.parseDouble(amount), desc);
            output.append("Data: ").append(data)
                    .append(", Descrição: ").append(desc)
                    .append(", R$: ").append(amount)
                    .append("\n");
        }
    }

    public void mapExpensesNoDate(String pattern, String str, StringBuilder output) {
        int mon = 0;
        String ext = str.replaceAll("\\r\\n+", " ");
        Pattern pat = Pattern.compile(pattern);
        Matcher matcher = pat.matcher(ext);

        while (matcher.find()) {
            String aux = matcher.group(1);
            String[] parts = aux.split(" ", 2);
            String data = parts[0];
            String desc = parts[1];
            String amount = matcher.group(2);
            mapToObj(Double.parseDouble(amount), desc);
            output.append("Data: ").append("01/").append(data)
                    .append(", Descrição: ").append(desc)
                    .append(", R$: ").append(amount)
                    .append("\n");
        }
    }

    public void mapToObj (Double amount, String transaction){
        expenses.add(new Expense(amount, transaction, 0));
    }

    public Map<String, Double> classify(List<Expense> expenses, List<ExpenseType> types){
        Map<String, Double> groupedExpenses = new HashMap<>();
        double nonClassifiedTotal = 0.0;

        for (Expense expense: expenses){
            boolean matched = false;
            for (ExpenseType type : types){
                if(expense.getTransactionName().toLowerCase().contains(type.getToken())){
                    groupedExpenses.put(type.getType(), groupedExpenses.getOrDefault(type.getType(), 0.0) + expense.getValue());
                    matched = true;
                }
            }
            if(!matched){
                nonClassifiedTotal += expense.getValue();
            }
        }
        if (nonClassifiedTotal > 0){
            groupedExpenses.put("NOT-CLASSIFIED", nonClassifiedTotal);
        }
        return groupedExpenses;
    }
}
