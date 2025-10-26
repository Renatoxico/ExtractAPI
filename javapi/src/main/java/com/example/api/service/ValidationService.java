package com.example.api.service;

import com.example.api.model.Expense;
import com.example.api.model.ValidationResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ValidationService {
    private static final long MAX_SIZE_KB = 1024;

    public ValidationResponse validateFiles (MultipartFile[] files) {
        ValidationResponse res = new ValidationResponse(true, "DEFAULT_OK");
        if (files.length<1){
            res.setStatus(false);
            res.setMessage("No files found");
            return res;
        }
        if (files.length>6){
            res.setStatus(false);
            res.setMessage("Too many files");
            return res;
        }
        for (MultipartFile file : files) {
            long fileSizeKB = file.getSize() / 1024;
            String contentType = file.getContentType();
            String fileName = file.getOriginalFilename();

            boolean isValidType = "application/pdf".equals(contentType);
            boolean hasValidExtension = fileName != null && (fileName.toLowerCase().endsWith(".pdf"));

            if (fileSizeKB >= MAX_SIZE_KB / 2) {
                res.setStatus(false);
                res.setMessage("File too big");
                return res;
            }

            if (!isValidType || !hasValidExtension) {
                res.setStatus(false);
                res.setMessage("Invalid file type");
                return res;
            }

        }

        return res;
    }

    public List<Expense> validateExpenses (List<Expense> expenses) {
        expenses.removeIf(expense -> expense.getValue().doubleValue()<=0);
        //expenses.removeIf(expense -> !expense.getTransactionName().matches(".*[a-zA-Z].*"));
        expenses.removeIf(expense -> expense.getTransactionName().isEmpty());
        expenses.forEach(expense -> {
            expense.setTransactionName(expense.getTransactionName().replaceAll("\\d{2}/\\d{2}", "").trim());
            expense.setTransactionName(expense.getTransactionName().replaceAll("\\s+", " ").trim());
            expense.setTransactionName(expense.getTransactionName().replace(".","").trim());
            expense.setTransactionName(expense.getTransactionName().replaceAll("\\d{2}/\\d{2}", "")); // remove datas no formato dd/mm
            expense.setTransactionName(expense.getTransactionName().replaceAll("\\s+", " ").trim());
        });
        expenses = preClassifyExpenses(expenses);
        return expenses;
    }

    private List<Expense> preClassifyExpenses (List<Expense> expenses) {
        expenses.forEach(expense -> {
            String expName = expense.getTransactionName().toUpperCase();
            if (expName.contains("IFOOD") || expName.contains("IFD")) {
                expense.setTransactionType("Restaurante / Lanches");
            }
            else if (expName.contains("SERV BEM") || expName.contains("CARREFOUR") || expName.contains("JAU SERVE")){
                expense.setTransactionType("Supermercado");
            }

        });
        return expenses;
    }
}
