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
                transaction_name,
                NULLIF(transaction_type, ''),
                CASE
                    WHEN NULLIF(transaction_type, '') IS NOT NULL THEN 'RULE'
                    ELSE NULL
                END,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            FROM tb_expense
            WHERE session_id = :reportId
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
                expense.transaction_name,
                NULLIF(expense.transaction_type, ''),
                CASE
                    WHEN NULLIF(expense.transaction_type, '') IS NOT NULL THEN 'RULE'
                    ELSE NULL
                END,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            FROM tb_expense expense
            WHERE expense.transaction_name IS NOT NULL
              AND NOT EXISTS (
                  SELECT 1
                  FROM expense_classification classification
                  WHERE classification.expense_name = expense.transaction_name
              )
            ON CONFLICT (expense_name) DO NOTHING
            """, nativeQuery = true)
    int populateMissing();

    @Modifying
    @Query(value = """
            UPDATE tb_expense expense
            SET transaction_type = classification.category
            FROM expense_classification classification
            WHERE classification.expense_name = expense.transaction_name
              AND expense.session_id = :reportId
              AND NULLIF(expense.transaction_type, '') IS NULL
              AND NULLIF(classification.category, '') IS NOT NULL
            """, nativeQuery = true)
    int applyCategoriesToReport(@Param("reportId") String reportId);

    @Modifying
    @Query(value = """
            UPDATE tb_expense expense
            SET transaction_type = classification.category
            FROM expense_classification classification
            WHERE classification.expense_name = expense.transaction_name
              AND NULLIF(expense.transaction_type, '') IS NULL
              AND NULLIF(classification.category, '') IS NOT NULL
            """, nativeQuery = true)
    int applyCategoriesToMissingExpenses();
}
