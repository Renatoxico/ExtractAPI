package com.example.api.repo;

import com.example.api.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query(value = """
            WITH CLEANED_EXPENSES AS (SELECT
            	CASE
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%UBER%' THEN 'UBER'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%IFOOD%' OR UPPER(TRANSACTION_NAME) LIKE '%IFD%' THEN 'IFOOD'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%AMAZONMARKETPLACE%' THEN 'AMAZON PURCHASES'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%MERCADOLIVRE%' THEN 'MERCADO LIVRE PURCHASES'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%SERV BEM%' THEN 'COMPRAS SERVE BEM'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%JAU SERVE%' THEN 'COMPRAS JAU SERVE'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%CARREFOUR%' THEN 'COMPRAS CARREFOUR'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%NETFLIX%' THEN 'STREAMING SERVICE'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%HBO%' THEN 'STREAMING SERVICE'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%GLOBO%' THEN 'STREAMING SERVICE'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%DISNEY%' THEN 'STREAMING SERVICE'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%HULU%' THEN 'STREAMING SERVICE'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%AMAZON%PRIME%' THEN 'STREAMING SERVICE'
            		ELSE TRANSACTION_NAME
            	END AS EXPENSE_NAME,
            	VALUE
            FROM TB_EXPENSE
            WHERE SESSION_ID = :sessionId)
            SELECT EXPENSE_NAME, SUM(VALUE) TOTAL, COUNT(1) INSTANCES
            FROM CLEANED_EXPENSES
            GROUP BY EXPENSE_NAME
            ORDER BY TOTAL desc, INSTANCES desc, EXPENSE_NAME
            """, nativeQuery = true)
    List<Object[]> getGroupedExpenses(@Param("sessionId") String sessionId);
}
