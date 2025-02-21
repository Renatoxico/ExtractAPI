package com.example.API.service;

import com.example.API.model.Expense;
import com.example.API.model.ExpenseType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class ExtractService {
    private final FileService fs;
    private final DebitService debitService;
    private final CreditService creditService;
    private final List<Expense> expenses;


    public ExtractService(FileService fs, DebitService debitService, CreditService creditService, List<Expense> expenses) {
        this.fs = fs;
        this.debitService = debitService;
        this.creditService = creditService;
        this.expenses = expenses;
    }

    public List<Expense> processDocument(MultipartFile iFile, String type){

        String fileStr = fs.getContent(iFile);
        //List<ExpenseType> types = ExpenseType.load();

        return switch (type) {
            case "debit" -> debitService.getExtractDetails(fileStr);
            case "credit" -> creditService.getExtractDetails(fileStr);
            default -> null;
        };
    }

    public List<Expense> processDocument2(MultipartFile iFile, List<Expense> expensesObj){
        //MAINTAINS EXTRACTION LOGIC TO 1 CLASS
        String fileStr = fs.getContent(iFile);//crazy shit is going on here nigga, you gotta pay attention
//        fileStr = fileStr.substring(fileStr.indexOf("SALDO EM"));
//        fileStr = fileStr.substring(fileStr.indexOf("\r\n"));
        /*fileStr = fileStr.replace("PIX ENVIADO", "");*/
        return getExtractDetails(fileStr, expensesObj);

    }

    public List<Expense> getExtractDetails(String ext, List<Expense> expensesObj){
        List<ExpenseType> types = ExpenseType.load();
        StringBuilder res = new StringBuilder();
        List<String> patterns = List.of(
                //"(\\d{2}/\\d{2})\\s+([A-Za-z0-9\\s]+?)\\s+(\\d{1,3}(?:\\.\\d{3})*,\\d{2}-)"
                "(\\d{2}/\\d{2})\\s+.+?\\s+PIX\\s+([A-Z\\s]+)\\s+\\d+\\s+\\d+\\s+\\d+\\s+(\\d+,\\d{2})"
                ,"(\\d{2}/\\d{2}) \\d{4}\\.\\d{4} ([a-zA-Z0-9\\.\\s]+) (\\d+,\\d{2})"
                ,"(\\d{2}/\\d{2})\\s+([^0-9]+?)\\s+(\\d+,[0-9]{2})"
                ,"(\\d{2}/\\d{2})\\s+([A-Za-z0-9 E*]+)\\s+\\d+/\\d+\\s+([0-9]+,[0-9]{2})"
                //"(\\d{2}/\\d{2}) [A-Z\\s]+ ([A-Z\\s]+) (\\d+,\\d{2})"
        );
        patterns.forEach(pattern -> mapExpenses(pattern, ext, res, expensesObj));

        //mapExpensesNoDate("([A-Za-z0-9\\s]+) - (\\d{1,5},\\d{2}) \\d{12}-\\d{12}-", ext, res);
        Map<String, Double> x = classify(expensesObj, types);// obj with classified results
        //return res.toString();
        return expensesObj;
    }

    public void mapExpenses(String pattern, String str, StringBuilder output, List<Expense> expensesObj) {
        String ext = str.replaceAll("\\r\\n+", " ");
        Pattern pat = Pattern.compile(pattern);
        Matcher matcher = pat.matcher(ext);

        while (matcher.find()) {
            String data = matcher.group(1);
            String desc = matcher.group(2);
            String amount = matcher.group(3);

            String x =  amount.replace("-", "");
            x = x.replace(".", "");
            x = x.replace(",", ".");
            expensesObj.add(new Expense(Double.parseDouble(x), desc, data, ""));
            ext = ext.substring(0, matcher.start()) + ext.substring(matcher.end());
            str = ext;

            output.append("Data: ").append(data)
                    .append(", Descrição: ").append(desc)
                    .append(", R$: ").append(amount)
                    .append("\n");
            matcher = pat.matcher(ext);
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

            String x =  amount.replace(",", ".");
            mapToObj(Double.parseDouble(x), desc, "01/" + data);
            ext = ext.substring(0, matcher.start()) + ext.substring(matcher.end());

            output.append("Data: ").append("01/").append(data)
                    .append(", Descrição: ").append(desc)
                    .append(", R$: ").append(amount)
                    .append("\n");
        }
    }

    public void mapToObj (Double amount, String transaction, String data){
        expenses.add(new Expense(amount, transaction, data, ""));
    }

    public Map<String, Double> classify(List<Expense> expenses, List<ExpenseType> types){
        Map<String, Double> groupedExpenses = new HashMap<>();
        double nonClassifiedTotal = 0.0;

        for (Expense expense: expenses){
            boolean matched = false;
            for (ExpenseType type : types){
                if(expense.getTransactionName().toLowerCase().contains(type.getToken())){
                    groupedExpenses.put(type.getType(), groupedExpenses.getOrDefault(type.getType(), 0.0) + expense.getValue());
                    //expense.setTransactionName(type.getToken());
                    expense.setTransactionType(type.getType());
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

    public List<Expense> batchProcess (MultipartFile [] files ){
        List<Expense> expensesObj = new ArrayList<Expense>();
        for(MultipartFile ext : files){
            processDocument2(ext, expensesObj);
        }
        return expensesObj;
    }

    public void expenseClear () {
        expenses.clear();
    }
}
