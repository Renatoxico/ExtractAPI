package com.example.api.repo;

import com.example.api.model.Expense;
import com.example.api.model.ExpensesGroupedDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query(value = """
            SELECT
            	TRANSACTION_NAME,
            	DATE,
            	VALUE,
            	COALESCE(TRANSACTION_TYPE, '') AS TRANSACTION_TYPE
            FROM TB_EXPENSE
            WHERE SESSION_ID = :sessionId
            ORDER BY VALUE DESC
            """, nativeQuery = true)
    List<Object[]> getAllExpenses(@Param("sessionId") String sessionId);

    @Query(value = """
            SELECT DISTINCT (TRANSACTION_NAME),
            TRANSACTION_TYPE
            FROM TB_EXPENSE
            ORDER BY TRANSACTION_NAME
            LIMIT 50
            """, nativeQuery = true)
    List<Object[]> getExpenseNames();

    @Query(value = """
            WITH CLEANED_EXPENSES AS (SELECT
            	CASE
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%UBER%' THEN 'UBER'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%IFOOD%' OR UPPER(TRANSACTION_NAME) LIKE '%IFD%' THEN 'IFOOD'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%AMAZONMARKETPLACE%' THEN 'AMAZON PURCHASES'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%MERCADOLIVRE%' THEN 'MERCADO LIVRE PURCHASES'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%SERV%BEM%' THEN 'COMPRAS SERVE BEM'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%JAU%SERVE%' THEN 'COMPRAS JAU SERVE'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%CARREFOUR%' THEN 'COMPRAS CARREFOUR'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%NETFLIX%' THEN 'STREAMING SERVICE'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%HBO%' THEN 'STREAMING SERVICE'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%GLOBO%' THEN 'STREAMING SERVICE'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%DISNEY%' THEN 'STREAMING SERVICE'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%HULU%' THEN 'STREAMING SERVICE'
            		WHEN UPPER(TRANSACTION_NAME) LIKE '%AMAZON%PRIME%' THEN 'STREAMING SERVICE'
            		ELSE TRANSACTION_NAME
            	END AS EXPENSE_NAME,
            	ROUND(CAST(VALUE AS NUMERIC), 2) VALUE,
            	TRANSACTION_TYPE
            FROM TB_EXPENSE
            WHERE SESSION_ID = :sessionId)
            SELECT EXPENSE_NAME, SUM(VALUE) TOTAL, COUNT(1) INSTANCES, COALESCE(TRANSACTION_TYPE, '') AS TRANSACTION_TYPE
            FROM CLEANED_EXPENSES
            GROUP BY EXPENSE_NAME, TRANSACTION_TYPE
            HAVING COUNT(1) > 1
            ORDER BY TOTAL DESC, INSTANCES DESC, EXPENSE_NAME
            """, nativeQuery = true)
    List<Object[]> getGroupedExpenses(@Param("sessionId") String sessionId);

    @Query(value = """
            WITH CLEANED_EXPENSES AS (SELECT
                        	CASE
                        		WHEN UPPER(TRANSACTION_NAME) LIKE '%UBER%' THEN 'UBER'
                        		WHEN UPPER(TRANSACTION_NAME) LIKE '%IFOOD%' OR UPPER(TRANSACTION_NAME) LIKE '%IFD%' THEN 'IFOOD'
                        		WHEN UPPER(TRANSACTION_NAME) LIKE '%AMAZONMARKETPLACE%' THEN 'AMAZON PURCHASES'
                        		WHEN UPPER(TRANSACTION_NAME) LIKE '%MERCADOLIVRE%' THEN 'MERCADO LIVRE PURCHASES'
                        		WHEN UPPER(TRANSACTION_NAME) LIKE '%SERV%BEM%' THEN 'COMPRAS SERVE BEM'
                        		WHEN UPPER(TRANSACTION_NAME) LIKE '%JAU%SERVE%' THEN 'COMPRAS JAU SERVE'
                        		WHEN UPPER(TRANSACTION_NAME) LIKE '%CARREFOUR%' THEN 'COMPRAS CARREFOUR'
                        		WHEN UPPER(TRANSACTION_NAME) LIKE '%NETFLIX%' THEN 'STREAMING SERVICE'
                        		WHEN UPPER(TRANSACTION_NAME) LIKE '%HBO%' THEN 'STREAMING SERVICE'
                        		WHEN UPPER(TRANSACTION_NAME) LIKE '%GLOBO%' THEN 'STREAMING SERVICE'
                        		WHEN UPPER(TRANSACTION_NAME) LIKE '%DISNEY%' THEN 'STREAMING SERVICE'
                        		WHEN UPPER(TRANSACTION_NAME) LIKE '%HULU%' THEN 'STREAMING SERVICE'
                        		WHEN UPPER(TRANSACTION_NAME) LIKE '%AMAZON%PRIME%' THEN 'STREAMING SERVICE'
                        		ELSE TRANSACTION_NAME
                        	END AS EXPENSE_NAME,
                        	ROUND(CAST(VALUE AS NUMERIC), 2) VALUE,
                        	TRANSACTION_TYPE
                        FROM TB_EXPENSE
                        WHERE SESSION_ID = :sessionId)
                        SELECT EXPENSE_NAME, SUM(VALUE) TOTAL, COUNT(1) INSTANCES, COALESCE(TRANSACTION_TYPE, '') AS TRANSACTION_TYPE
                        FROM CLEANED_EXPENSES
                        GROUP BY EXPENSE_NAME, TRANSACTION_TYPE
                        ORDER BY TOTAL DESC, INSTANCES DESC, EXPENSE_NAME
                        LIMIT 10
            """, nativeQuery = true)
    List<Object[]> getTopExpenses(@Param("sessionId")String sessionId);

    @Query(value = """
            SELECT
            	TRANSACTION_NAME,
            	DATE,
            	VALUE,
            	COALESCE(TRANSACTION_TYPE, '') AS TRANSACTION_TYPE
            FROM TB_EXPENSE
            WHERE SESSION_ID = :sessionId
            ORDER BY VALUE DESC
            LIMIT 1
            """, nativeQuery = true)
    List<Object[]> getBiggestExpense(@Param("sessionId") String sessionId);
}
