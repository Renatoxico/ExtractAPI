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
import java.util.Map;
import java.util.IdentityHashMap;
import io.github.renatoxico.extract.logging.ExpenseSource;
import io.github.renatoxico.extract.logging.LogFields;
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

    public void process(ExpenseReport report, String inputText, String filename) {
        if (inputText == null || inputText.trim().isEmpty()) {
            LOG.debug("Empty input text reportId={} filename={}", report.getId(), LogFields.safe(filename));
            return;
        }
        List<ExpenseSource> candidates = filterSources(splitByLine(inputText), filename, report.getId());
        if (candidates.isEmpty()) {
            throw new ProcessingException("No expenses could be extracted from the provided files",
                HttpStatus.UNPROCESSABLE_ENTITY, "NO_EXPENSES_EXTRACTED");
        }
        Map<Expense, ExpenseSource> sources = new IdentityHashMap<>();
        List<Expense> expenses = objectifySources(report, candidates, sources);
        expenses = validationService.validateExpenses(expenses, sources);
        expenseRepo.saveAll(expenses);
        LOG.info("Expense file processed reportId={} filename={} candidates={} savedExpenses={}",
            report.getId(), LogFields.safe(filename), candidates.size(), expenses.size());
    }

    public String[] splitByLine (String extractText) {
        extractText = extractText.replace("\\n", "\n");
        return extractText.split("\\n");

    }

    private List<Expense> objectifySources(ExpenseReport report, List<ExpenseSource> candidates,
                                          Map<Expense, ExpenseSource> sources) {
        List<Expense> expensesObj = new ArrayList<>();
        candidates.forEach(source -> {
            String line = source.candidate();
            try {
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
                            Expense expense = new Expense(report, value, description, date, null);
                            expensesObj.add(expense);
                            sources.put(expense, source);
                        }
                    }
                }
            } catch (RuntimeException ex) {
                throw new ProcessingException("Failed to process file: " + source.filename(),
                    HttpStatus.INTERNAL_SERVER_ERROR, "FILE_PROCESSING_ERROR", ex)
                    .withDiagnostics(source.diagnostics());
            }
        });
        return expensesObj;
    }
    
    private List<ExpenseSource> filterSources(String[] inputText, String filename, String reportId) {
        List<ExpenseSource> filteredCharges = new ArrayList<>();
        for (int i = 0; i < inputText.length; i++) {
            String line = inputText[i];
            Matcher dateMatcher = datePattern.matcher(line);
            Matcher valueMatcher = valuePattern.matcher(line);

            if (dateMatcher.find() && valueMatcher.find()) {
                String firstMatch = line.substring(0, valueMatcher.end());
                String secondMatch = line.substring(valueMatcher.end());

                if (dateMatcher.find() && valueMatcher.find()) {
                    filteredCharges.add(new ExpenseSource(filename, reportId, i + 1, line, firstMatch));
                    filteredCharges.add(new ExpenseSource(filename, reportId, i + 1, line, secondMatch));
                }
                else
                    filteredCharges.add(new ExpenseSource(filename, reportId, i + 1, line, line));
            }
        }
        return filteredCharges;
    }

}
