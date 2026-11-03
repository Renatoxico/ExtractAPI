package io.github.renatoxico.extract.repo;

import io.github.renatoxico.extract.model.CategorySummary;
import io.github.renatoxico.extract.model.Expense;
import io.github.renatoxico.extract.model.ExpenseData;
import io.github.renatoxico.extract.model.ExpenseGroup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Expense e WHERE e.report.id = :reportId AND e.expenseName = :name ORDER BY e.id")
    List<Expense> lockByReportAndName(@Param("reportId") String reportId, @Param("name") String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Expense e WHERE e.report.id = :reportId AND e.id = :expenseId")
    Optional<Expense> lockByReportAndId(@Param("reportId") String reportId, @Param("expenseId") Long expenseId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Expense e WHERE e.report.id = :reportId AND e.id = :expenseId")
    int deleteExpenseInReport(@Param("reportId") String reportId, @Param("expenseId") Long expenseId);

    @Query("""
        SELECT new io.github.renatoxico.extract.model.ExpenseData(
            expense.id,
            expense.expenseName,
            expense.amount,
            expense.date,
            expense.category,
            expense.categorySource,
            expense.source
        )
        FROM Expense expense
        WHERE expense.report.id = :reportId
        ORDER BY expense.amount DESC, expense.id
        """)
    List<ExpenseData> findExpenseDataByReportId(@Param("reportId") String reportId);

    List<Expense> findAllByReport_IdOrderByAmountDescIdAsc(String reportId);

    @Query("""
        SELECT new io.github.renatoxico.extract.model.ExpenseGroup(
            expense.expenseName,
            SUM(expense.amount),
            COUNT(expense),
            expense.category
        )
        FROM Expense expense
        WHERE expense.report.id = :reportId
        GROUP BY expense.expenseName, expense.category
        ORDER BY SUM(expense.amount) DESC, COUNT(expense) DESC, expense.expenseName
        """)
    List<ExpenseGroup> findExpenseGroupsByReportId(@Param("reportId") String reportId);

    @Query("""
        SELECT new io.github.renatoxico.extract.model.CategorySummary(
            expense.category,
            SUM(expense.amount),
            COUNT(expense)
        )
        FROM Expense expense
        WHERE expense.report.id = :reportId
        GROUP BY expense.category
        ORDER BY SUM(expense.amount) DESC
        """)
    List<CategorySummary> findCategorySummariesByReportId(@Param("reportId") String reportId);

}
