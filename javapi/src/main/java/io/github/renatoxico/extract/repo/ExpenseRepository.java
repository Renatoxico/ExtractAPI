package io.github.renatoxico.extract.repo;

import io.github.renatoxico.extract.model.CategorySummary;
import io.github.renatoxico.extract.model.Expense;
import io.github.renatoxico.extract.model.ExpenseCategoryAssignment;
import io.github.renatoxico.extract.model.ExpenseData;
import io.github.renatoxico.extract.model.ExpenseGroup;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("""
        SELECT new io.github.renatoxico.extract.model.ExpenseData(
            expense.id,
            expense.expenseName,
            expense.amount,
            expense.date,
            expense.category
        )
        FROM Expense expense
        WHERE expense.reportId = :reportId
        ORDER BY expense.amount DESC, expense.id
        """)
    List<ExpenseData> findExpenseDataByReportId(@Param("reportId") String reportId);

    List<Expense> findAllByReportIdOrderByAmountDescIdAsc(String reportId);

    @Query("""
        SELECT new io.github.renatoxico.extract.model.ExpenseGroup(
            expense.expenseName,
            SUM(expense.amount),
            COUNT(expense),
            expense.category
        )
        FROM Expense expense
        WHERE expense.reportId = :reportId
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
        WHERE expense.reportId = :reportId
        GROUP BY expense.category
        ORDER BY SUM(expense.amount) DESC
        """)
    List<CategorySummary> findCategorySummariesByReportId(@Param("reportId") String reportId);

    @Query("""
        SELECT new io.github.renatoxico.extract.model.ExpenseCategoryAssignment(
            expense.expenseName,
            expense.category
        )
        FROM Expense expense
        WHERE expense.category IS NULL
        GROUP BY expense.expenseName, expense.category
        ORDER BY expense.expenseName
        """)
    List<ExpenseCategoryAssignment> findUnclassifiedExpenseNames();

    @Modifying
    @Transactional
    @Query("UPDATE Expense expense SET expense.category = :category WHERE expense.expenseName = :expenseName")
    void updateCategory(@Param("expenseName") String expenseName, @Param("category") String category);

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE expense target
        SET category = matched.category
        FROM expense matched
        WHERE target.category IS NULL
          AND matched.expense_name = target.expense_name
          AND matched.category IS NOT NULL
        """, nativeQuery = true)
    void updateMatchedExpenses();
}
