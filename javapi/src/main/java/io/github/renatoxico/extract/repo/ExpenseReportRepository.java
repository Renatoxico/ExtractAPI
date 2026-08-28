package io.github.renatoxico.extract.repo;

import io.github.renatoxico.extract.model.ExpenseReport;
import io.github.renatoxico.extract.model.ReportSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseReportRepository extends JpaRepository<ExpenseReport, String> {
    boolean existsByIdAndOwnerId(String reportId, Long ownerId);

    @Query("""
        SELECT new io.github.renatoxico.extract.model.ReportSummary(
            report.id,
            report.createdAt,
            COALESCE(SUM(expense.amount), 0),
            COUNT(expense)
        )
        FROM ExpenseReport report
        LEFT JOIN Expense expense ON expense.report = report
        WHERE report.owner.id = :ownerId
        GROUP BY report.id, report.createdAt
        ORDER BY report.createdAt DESC, report.id
        """)
    List<ReportSummary> findSummariesByOwnerId(@Param("ownerId") Long ownerId);
}
