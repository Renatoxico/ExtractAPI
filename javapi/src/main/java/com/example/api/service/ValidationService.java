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
        preClassifyExpenses(expenses);
        return expenses;
    }

    private void preClassifyExpenses (List<Expense> expenses) {
        expenses.forEach(expense -> {
            String expName = expense.getTransactionName().toUpperCase();
            if(expName.contains("UBER") || expName.contains("CLICKBUS")){
                expense.setTransactionType("Transporte / Auto");
            }
            else if (expName.contains("IFOOD") || expName.contains("IFD")) {
                expense.setTransactionType("Restaurante / Lanches");
            }
            else if (expName.contains("SERV BEM") || expName.contains("CARREFOUR")
                    || expName.contains("JAU SERVE")){
                expense.setTransactionType("Supermercado");
            }
            else if (expName.contains("AMAZON") || expName.contains("MERCADOLIVRE")
                    || expName.contains("MERCADOPAGO") || expName.contains("KABUM")
                    || expName.contains("HAVAN")){
                expense.setTransactionType("E-commerce / Compras online");
            }
            else if (expName.contains("NETFLIX") || expName.contains("HBOMAX")
                    || expName.contains("AMAZONPRIME") || expName.contains("PATREON")
                    || expName.contains("PAG*STEAM") || expName.contains("PET SHOP") || expName.contains("PETSHOP")
                    || expName.contains("KICKSTREAMING") || expName.contains("YOUTUBE")
                    || expName.contains("MELIMAIS") || expName.contains("DISNEY")) {
                expense.setTransactionType("Lazer / Entretenimento / Pets");
            }
            else if (expName.contains("PAGAMENTO DE BOLETO") || expName.contains("TARIFA MENSALIDADE")
                    || expName.contains("PGTO CONTA") || expName.contains("MENSALIDADE DE SEGURO")
                    || expName.contains("HOSTGATOR") || expName.contains("SEGURO VIDA") || expName.contains("SEGUROVIDA")
                    || expName.contains("APPLECOM/BILL")){
                expense.setTransactionType("Moradia / Contas");
            }
            else if (expName.contains("RENNER") || expName.contains("HERING")
                    || expName.contains("NIKEECOMMER")){
                expense.setTransactionType("Roupas / Acessórios");
            }
            else if (expName.contains("SAUDE") || expName.contains("FARMACIA")){
                expense.setTransactionType("Saúde / Farmácia / Bem-estar");
            }
        });
    }
}
