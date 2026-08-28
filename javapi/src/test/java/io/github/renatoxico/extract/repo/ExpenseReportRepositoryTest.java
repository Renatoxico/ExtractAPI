package io.github.renatoxico.extract.repo;

import io.github.renatoxico.extract.model.ReportSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ExpenseReportRepositoryTest {
    @Autowired ExpenseReportRepository reportRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private Long ownerId;
    private Long otherOwnerId;

    @BeforeEach
    void setUp() {
        ownerId = insertUser("history-owner");
        otherOwnerId = insertUser("history-other");

        insertReport("older-report", ownerId, "2025-03-17T10:15:30Z");
        insertExpense("older-report", "MARKET", "100.25");
        insertExpense("older-report", "CAFE", "50.00");

        insertReport("newer-report", ownerId, "2025-04-01T08:00:00Z");
        insertExpense("newer-report", "RENT", "900.00");

        insertReport("empty-report", ownerId, "2025-02-01T08:00:00Z");

        insertReport("other-report", otherOwnerId, "2025-05-01T08:00:00Z");
        insertExpense("other-report", "PRIVATE", "5000.00");
    }

    @Test
    void returnsOnlyOwnersReportsWithAggregatesOrderedByNewestFirst() {
        List<ReportSummary> summaries = reportRepository.findSummariesByOwnerId(ownerId);

        assertThat(summaries).extracting(ReportSummary::reportId)
            .containsExactly("newer-report", "older-report", "empty-report");
        assertThat(summaries.get(0).total()).isEqualByComparingTo(new BigDecimal("900.00"));
        assertThat(summaries.get(0).countExpenses()).isEqualTo(1L);
        assertThat(summaries.get(1).total()).isEqualByComparingTo(new BigDecimal("150.25"));
        assertThat(summaries.get(1).countExpenses()).isEqualTo(2L);
        assertThat(summaries.get(2).total()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summaries.get(2).countExpenses()).isZero();
    }

    @Test
    void returnsEmptyListForOwnerWithoutReports() {
        Long emptyOwnerId = insertUser("history-empty");

        assertThat(reportRepository.findSummariesByOwnerId(emptyOwnerId)).isEmpty();
    }

    private Long insertUser(String firebaseUid) {
        jdbcTemplate.update(
            "INSERT INTO app_user (firebase_uid, email) VALUES (?, ?)",
            firebaseUid,
            firebaseUid + "@example.com"
        );
        return jdbcTemplate.queryForObject(
            "SELECT id FROM app_user WHERE firebase_uid = ?",
            Long.class,
            firebaseUid
        );
    }

    private void insertReport(String reportId, Long appUserId, String createdAt) {
        jdbcTemplate.update(
            "INSERT INTO expense_report (id, app_user_id, created_at) VALUES (?, ?, ?)",
            reportId,
            appUserId,
            java.time.Instant.parse(createdAt)
        );
    }

    private void insertExpense(String reportId, String name, String amount) {
        jdbcTemplate.update(
            "INSERT INTO expense (report_id, expense_name, amount, date) VALUES (?, ?, ?, ?)",
            reportId,
            name,
            new BigDecimal(amount),
            "15/03/2025"
        );
    }
}
