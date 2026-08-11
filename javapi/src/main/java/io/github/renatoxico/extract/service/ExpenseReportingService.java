package io.github.renatoxico.extract.service;

import com.opencsv.CSVWriter;
import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.model.AppUser;
import io.github.renatoxico.extract.model.CategorySummary;
import io.github.renatoxico.extract.model.DaySummary;
import io.github.renatoxico.extract.model.Expense;
import io.github.renatoxico.extract.model.ExpenseCategoryAssignment;
import io.github.renatoxico.extract.model.ExpenseData;
import io.github.renatoxico.extract.model.ExpenseReport;
import io.github.renatoxico.extract.model.ReportData;
import io.github.renatoxico.extract.model.ReportHighlights;
import io.github.renatoxico.extract.repo.AppUserRepository;
import io.github.renatoxico.extract.repo.ExpenseReportRepository;
import io.github.renatoxico.extract.repo.ExpenseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExpenseReportingService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter V2_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ExpenseRepository expenseRepository;
    private final ExpenseReportRepository reportRepository;
    private final AppUserRepository appUserRepository;
    private final V2ReportMapper v2ReportMapper;

    public ExpenseReportingService(
        ExpenseRepository expenseRepository,
        ExpenseReportRepository reportRepository,
        AppUserRepository appUserRepository,
        V2ReportMapper v2ReportMapper
    ) {
        this.expenseRepository = expenseRepository;
        this.reportRepository = reportRepository;
        this.appUserRepository = appUserRepository;
        this.v2ReportMapper = v2ReportMapper;
    }

    @Transactional
    public String createReport(Long ownerId) {
        byte[] randomBytes = new byte[24];
        RANDOM.nextBytes(randomBytes);
        String reportId = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes).toLowerCase();
        AppUser owner = appUserRepository.getReferenceById(ownerId);
        reportRepository.save(new ExpenseReport(reportId, owner));
        return reportId;
    }

    @Transactional(readOnly = true)
    public ReportData getReport(String reportId) {
        ExpenseReport report = reportRepository.findById(reportId)
            .orElseThrow(this::reportNotFound);
        List<ExpenseData> expenses = expenseRepository.findExpenseDataByReportId(reportId);
        if (expenses.isEmpty()) {
            throw reportNotFound();
        }

        return new ReportData(
            reportId,
            report.getCreatedAt(),
            expenses,
            expenseRepository.findExpenseGroupsByReportId(reportId),
            expenseRepository.findCategorySummariesByReportId(reportId),
            buildHighlights(expenses)
        );
    }

    public List<ExpenseCategoryAssignment> getUnclassifiedExpenseNames() {
        return expenseRepository.findUnclassifiedExpenseNames().stream().distinct().limit(50).toList();
    }

    @Transactional(readOnly = true)
    public byte[] exportReportCsvV2(String reportId) {
        List<Expense> expenses = requireExpenses(reportId);
        String[] header = {"expenseId", "expenseName", "category", "amount", "date", "reportId"};
        return writeCsv(header, expenses.stream().map(expense -> new String[]{
            value(expense.getId()),
            value(expense.getExpenseName()),
            value(expense.getCategory()),
            value(expense.getAmount()),
            V2_DATE.format(v2ReportMapper.parseDate(expense.getDate())),
            value(expense.getReportId())
        }).toList());
    }

    private ReportHighlights buildHighlights(List<ExpenseData> expenses) {
        Map<String, DayAccumulator> days = new LinkedHashMap<>();
        for (ExpenseData expense : expenses) {
            days.computeIfAbsent(expense.date(), ignored -> new DayAccumulator())
                .add(expense.amount());
        }
        List<DaySummary> summaries = days.entrySet().stream()
            .map(entry -> new DaySummary(entry.getKey(), entry.getValue().count, entry.getValue().total))
            .toList();

        DaySummary mostActive = summaries.stream()
            .max(Comparator.comparingLong(DaySummary::transactionCount)
                .thenComparing(DaySummary::totalAmount))
            .orElse(null);
        DaySummary highestSpending = summaries.stream()
            .max(Comparator.comparing(DaySummary::totalAmount)
                .thenComparingLong(DaySummary::transactionCount))
            .orElse(null);
        return new ReportHighlights(expenses.getFirst(), mostActive, highestSpending);
    }

    private List<Expense> requireExpenses(String reportId) {
        List<Expense> expenses = expenseRepository.findAllByReportIdOrderByAmountDescIdAsc(reportId);
        if (expenses.isEmpty()) {
            throw reportNotFound();
        }
        return expenses;
    }

    private byte[] writeCsv(String[] header, List<String[]> rows) {
        StringWriter writer = new StringWriter();
        try (CSVWriter csvWriter = new CSVWriter(writer)) {
            csvWriter.writeNext(header);
            csvWriter.writeAll(rows);
        } catch (Exception ex) {
            throw new ProcessingException(
                "Error exporting CSV", HttpStatus.INTERNAL_SERVER_ERROR, "CSV_EXPORT_ERROR", ex);
        }
        return writer.toString().getBytes(StandardCharsets.UTF_8);
    }

    private ProcessingException reportNotFound() {
        return new ProcessingException(
            "No report found for the provided report ID", HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND");
    }

    private static String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private static final class DayAccumulator {
        private long count;
        private BigDecimal total = BigDecimal.ZERO;

        private void add(BigDecimal amount) {
            count++;
            total = total.add(amount);
        }
    }
}
