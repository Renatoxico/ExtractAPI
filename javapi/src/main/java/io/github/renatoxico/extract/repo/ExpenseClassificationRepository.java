package io.github.renatoxico.extract.repo;

import io.github.renatoxico.extract.model.ExpenseClassification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseClassificationRepository extends JpaRepository<ExpenseClassification, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO expense_classification (
                expense_name,
                category,
                source,
                created_at,
                updated_at
            )
            SELECT DISTINCT
                expense_name,
                category,
                CASE
                    WHEN category IS NOT NULL THEN 'RULE'
                    ELSE NULL
                END,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            FROM expense expense
            WHERE report_id = :reportId
            ON CONFLICT (expense_name) DO NOTHING
            """, nativeQuery = true)
    int populateFromReport(@Param("reportId") String reportId);

    @Modifying
    @Query(value = """
            INSERT INTO expense_classification (
                expense_name,
                category,
                source,
                created_at,
                updated_at
            )
            SELECT DISTINCT
                expense.expense_name,
                expense.category,
                CASE
                    WHEN expense.category IS NOT NULL THEN 'RULE'
                    ELSE NULL
                END,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            FROM expense expense
            WHERE expense.expense_name IS NOT NULL
              AND NOT EXISTS (
                  SELECT 1
                  FROM expense_classification classification
                  WHERE classification.expense_name = expense.expense_name
              )
            ON CONFLICT (expense_name) DO NOTHING
            """, nativeQuery = true)
    int populateMissing();

    @Modifying
    @Query(value = """
            UPDATE expense AS expense
            SET category = classification.category
            FROM expense_classification classification
            WHERE classification.expense_name = expense.expense_name
              AND expense.report_id = :reportId
              AND expense.category IS NULL
              AND NULLIF(classification.category, '') IS NOT NULL
            """, nativeQuery = true)
    int applyCategoriesToReport(@Param("reportId") String reportId);

}
