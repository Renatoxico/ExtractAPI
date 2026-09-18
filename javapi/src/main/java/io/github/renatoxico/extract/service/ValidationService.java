package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.model.Expense;
import io.github.renatoxico.extract.model.FileValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import io.github.renatoxico.extract.logging.ExpenseSource;
import io.github.renatoxico.extract.logging.LogFields;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ValidationService {
    private static final Logger LOG = LoggerFactory.getLogger(ValidationService.class);
    private static final long MAX_SIZE_KB = 1024;

    public FileValidationResult validateFiles (MultipartFile[] files) {
        if (files.length < 1) {
            return new FileValidationResult(false, "No files found", "NO_FILES_PROVIDED", HttpStatus.BAD_REQUEST);
        }
        if (files.length > 6) {
            return new FileValidationResult(false, "Too many files (maximum 6 allowed)", "TOO_MANY_FILES", HttpStatus.BAD_REQUEST);
        }

        for (MultipartFile file : files) {
            long fileSizeKB = file.getSize() / 1024;
            String contentType = file.getContentType();
            String fileName = file.getOriginalFilename();

            LOG.debug("Validating file: {} ({}KB, type: {})", LogFields.safe(fileName), fileSizeKB, LogFields.safe(contentType));

            boolean isValidType = "application/pdf".equals(contentType);
            boolean hasValidExtension = fileName != null && (fileName.toLowerCase().endsWith(".pdf"));

            if (fileSizeKB >= MAX_SIZE_KB / 2) {
                return new FileValidationResult(false,
                    "File '" + fileName + "' is too large: " + fileSizeKB + "KB (maximum 512KB)",
                    "FILE_TOO_BIG",
                    HttpStatus.BAD_REQUEST);
            }

            if (!isValidType || !hasValidExtension) {
                return new FileValidationResult(false,
                    "File '" + fileName + "' is not a valid PDF file",
                    "INVALID_FILE_TYPE",
                    HttpStatus.BAD_REQUEST);
            }
        }
        LOG.debug("All {} files passed validation", files.length);
        return new FileValidationResult(true, "All files validated successfully", "OK", HttpStatus.OK);
    }

    public List<Expense> validateExpenses(List<Expense> expenses, Map<Expense, ExpenseSource> sources) {
        expenses.removeIf(expense -> expense.getAmount().doubleValue()<=0);
        expenses.removeIf(expense -> expense.getExpenseName().isEmpty());
        expenses.forEach(expense -> {
            expense.setExpenseName(expense.getExpenseName().replaceAll("\\d{2}/\\d{2}", "").trim());// remove datas no formato dd/mm
            expense.setExpenseName(expense.getExpenseName().replaceAll("\\s+", " ").trim());// remove espaços extras
            expense.setExpenseName(expense.getExpenseName().replace("."," ").trim());// remove pontos
            expense.setExpenseName(expense.getExpenseName().replace("*"," ").trim());// remove asteriscos;
            expense.setExpenseName(expense.getExpenseName().substring(0,Math.min(expense.getExpenseName().length(), 70)).trim());// limita tamanho do nome
            expense = preClassifyExpenses(expense);
        });
        setLongDate(expenses, sources);
        return expenses;
    }

    private Expense preClassifyExpenses (Expense expense) {
            String expName = expense.getExpenseName().toUpperCase();
            if(expName.contains("UBER") || expName.contains("CLICKBUS")){
                expense.setCategory("Transporte / Auto");
            }
            else if (expName.contains("IFOOD") || expName.contains("IFD")) {
                expense.setCategory("Restaurante / Lanches");
            }
            else if (expName.contains("SERV BEM") || expName.contains("CARREFOUR")
                    || expName.contains("JAU SERVE")){
                expense.setCategory("Supermercado");
            }
            else if (expName.contains("NETFLIX") || expName.contains("HBOMAX")
                    || expName.contains("AMAZONPRIME") || expName.contains("PATREON")
                    || expName.contains("PAG*STEAM") || expName.contains("PET SHOP") || expName.contains("PETSHOP")
                    || expName.contains("KICKSTREAMING") || expName.contains("YOUTUBE")
                    || expName.contains("MELIMAIS") || expName.contains("DISNEY")) {
                expense.setCategory("Lazer / Entretenimento / Pets");
            }
            else if (expName.contains("AMAZON") || expName.contains("MERCADOLIVRE")
                    || expName.contains("MERCADOPAGO") || expName.contains("KABUM")
                    || expName.contains("HAVAN")){
                expense.setCategory("E-commerce / Compras online");
            }
            else if (expName.contains("PAGAMENTO DE BOLETO") || expName.contains("TARIFA MENSALIDADE")
                    || expName.contains("PGTO CONTA") || expName.contains("MENSALIDADE DE SEGURO")
                    || expName.contains("HOSTGATOR") || expName.contains("SEGURO VIDA") || expName.contains("SEGUROVIDA")
                    || expName.contains("APPLECOM/BILL")){
                expense.setCategory("Moradia / Contas");
            }
            else if (expName.contains("RENNER") || expName.contains("HERING")
                    || expName.contains("NIKEECOMMER")){
                expense.setCategory("Roupas / Acessórios");
            }
            else if (expName.contains("SAUDE") || expName.contains("FARMACIA")){
                expense.setCategory("Saúde / Farmácia / Bem-estar");
            }
            else if (//expName.contains("PIX ") || expName.contains("PIX*") || expName.contains("PIX-")|| 
                    expName.contains("TRANSFERENCIA") || expName.contains("TRANSFERÊNCIA")
                    || expName.contains("TED ") || expName.contains("TED*") || expName.contains("TED-")
                    || expName.contains("DOC ") || expName.contains("DOC*") || expName.contains("DOC-")){
                expense.setCategory("Outros / Transferências");
            }
            return expense;
    }

    private void setLongDate(List<Expense> expenses, Map<Expense, ExpenseSource> sources) {
        int currentYear = LocalDate.now().getYear();
        Integer commonYear = mostCommonYear(expenses);
        for (Expense expense : expenses) {
            String originalDate = expense.getDate();
            List<Integer> attemptedYears = new ArrayList<>();
            if (originalDate != null && originalDate.matches("\\d{2}/\\d{2}")) {
                List<Integer> years = new ArrayList<>();
                if (commonYear != null) years.add(commonYear);
                years.add(currentYear);
                years.add(currentYear - 1);
                for (Integer year : years) {
                    attemptedYears.add(year);
                    String candidateDate = originalDate + "/" + year;
                    if (validateDate(candidateDate)) {
                        expense.setDate(candidateDate);
                        break;
                    }
                }
            }
            if (!validateDate(expense.getDate())) {
                Map<String, Object> fields = new LinkedHashMap<>();
                ExpenseSource source = sources.get(expense);
                if (source != null) fields.putAll(source.diagnostics());
                fields.put("reportId", expense.getReportId());
                fields.put("expenseId", expense.getId());
                fields.put("originalDate", originalDate);
                fields.put("attemptedYears", attemptedYears);
                fields.put("reason", attemptedYears.isEmpty()
                    ? "Invalid, missing, or future date" : "No valid date after year inference");
                Map<String, String> safeFields = new LinkedHashMap<>();
                fields.forEach((key, value) -> safeFields.put(key, LogFields.safe(value)));
                LOG.warn("Expense date validation failed context={}", safeFields);
            }
        }
    }

    private static Integer extractYear(String date) {
        if (date == null) return null;
        String trimmed = date.trim();
        // Short dates are expected and have no explicit year to contribute.
        if (trimmed.length() < 4 || !trimmed.substring(trimmed.length() - 4).matches("\\d{4}")) {
            return null;
        }
        int year = Integer.parseInt(trimmed.substring(trimmed.length() - 4));
        return year > 2000 && year < 2100 ? year : null;
    }

    private Integer mostCommonYear (List<Expense> expenses) {
        if(expenses == null || expenses.isEmpty())
            return null;
        Map<Integer, Integer> freqMap = new HashMap<>();
        for(Expense expense : expenses) {
            Integer year = extractYear(expense.getDate());
            if(year != null) {
                freqMap.merge(year, 1, Integer::sum);
            }
        }
        if (freqMap.isEmpty())
            return null;
        return freqMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private boolean validateDate(String date) {
        if (date == null || !date.matches("\\d{2}/\\d{2}/\\d{4}")) {
            return false;
        }
        String[] parts = date.split("/");
        int day = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int year = Integer.parseInt(parts[2]);

        if (month < 1 || month > 12) {
            return false;
        }

        int[] daysInMonth = {31, (isLeapYear(year) ? 29 : 28), 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (day < 1 || day > daysInMonth[month - 1]) {
            return false;
        }

        try {
            LocalDate parsed = LocalDate.of(year, month, day);
            LocalDate now = LocalDate.now();
            if (parsed.isAfter(now)) {
                return false;
            }
        } catch (java.time.DateTimeException e) {
            return false;
        }

        return true;
    }

    private boolean isLeapYear(int year) {
        if (year <= 0)
            return false;
        return (year % 4 == 0) && (year % 100 != 0 || year % 400 == 0);
    }
}
