package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.model.Expense;
import io.github.renatoxico.extract.model.ExpenseReport;
import io.github.renatoxico.extract.repo.ExpenseRepository;
import io.github.renatoxico.extract.exception.ProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

    public void process (ExpenseReport report, String inputText) {
        String reportId = report.getId();
        try {
            LOG.info("Enter ObjectifierService.process for report: {}", reportId);

            if (inputText == null || inputText.trim().isEmpty()) {
                LOG.warn("Empty input text provided for report: {}", reportId);
                return;
            }

            String[] expensesDoc = splitByLine(inputText);
            List<String> filteredExpenses = getFilterCharges(expensesDoc);

            if (filteredExpenses == null || filteredExpenses.isEmpty()) {
                LOG.warn("No candidate expense lines found after filtering for report: {}", reportId);
                throw new ProcessingException(
                    "No expenses could be extracted from the provided files",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "NO_EXPENSES_EXTRACTED"
                );
            }

            List<Expense> expensesObj = objectifyExtract(report, filteredExpenses);

            LOG.info("Extracted {} expenses from document for report: {}", expensesObj.size(), reportId);

            expensesObj = validationService.validateExpenses(expensesObj);

            LOG.info("Validated {} expenses, saving to database for report: {}", expensesObj.size(), reportId);
            expenseRepo.saveAll(expensesObj);

            LOG.info("Successfully processed {} expenses for report: {}", expensesObj.size(), reportId);
        } catch (Exception ex) {
            LOG.error("Error in ObjectifierService.process for report {}: {}", reportId, ex.getMessage(), ex);
            throw ex;
        }
    }

    public String[] splitByLine (String extractText) {
        extractText = extractText.replace("\\n", "\n");
        return extractText.split("\\n");

    }

    public List<Expense> objectifyExtract (ExpenseReport report,List<String> expenses) {
        String reportId = report.getId();
        LOG.info("Enter ObjectifierService.objectifyExtract for report: {}", reportId);
        List<Expense> expensesObj = new ArrayList<>();
        expenses.forEach(line -> {
            Matcher dateMatcher = datePattern.matcher(line);
            Matcher valueMatcher = valuePattern.matcher(line);

            if (dateMatcher.find() && valueMatcher.find()){
                String date = dateMatcher.group(1);
                String valueStr = valueMatcher.group(1)
                        .replace(".", "")
                        .replace(",", ".");
                BigDecimal value = new BigDecimal(valueStr);
                value = value.abs();
                if (!(valueMatcher.start()< dateMatcher.end())) {
                    String description = line.substring(dateMatcher.end(), valueMatcher.start())
                            .replace("|", "")
                            .replaceAll("\\d{4,}", "")
                            .trim();
                    if (!description.isEmpty()
                            && value.compareTo(BigDecimal.ZERO) != 0
                            && description.matches(".*[a-zA-Z].*")
                            && !(description.contains("CREDITO") || description.contains("FATURA") || description.contains("SALDO"))){
                        mapToObj(report, expensesObj, value, description, date);
                    }
                }
            }
        });
        LOG.info("Extracted {} valid expenses from {} lines for report: {}", expensesObj.size(), expenses.size(), reportId);
        return expensesObj;
    }
    
    public List<String> getFilterCharges (String[] inputText ) {
        LOG.info("Enter ObjectifierService.getFilterCharges");
        List<String> filteredCharges = new ArrayList<>();

        for (String line : inputText) {
            Matcher dateMatcher = datePattern.matcher(line);
            Matcher valueMatcher = valuePattern.matcher(line);

            if (dateMatcher.find() && valueMatcher.find()) {
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

    public void mapToObj (ExpenseReport report, List<Expense> expenses, BigDecimal amount, String expenseName, String date){
        expenses.add(new Expense(report, amount, expenseName, date, null));
    }

}
