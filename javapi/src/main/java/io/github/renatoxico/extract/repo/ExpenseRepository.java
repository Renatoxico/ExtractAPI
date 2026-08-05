package io.github.renatoxico.extract.repo;

import io.github.renatoxico.extract.model.CategoryMapper;
import io.github.renatoxico.extract.model.Expense;
import io.github.renatoxico.extract.model.ExpenseDTO;
import io.github.renatoxico.extract.model.ExpensesCategories;
import io.github.renatoxico.extract.model.ExpensesGroupedDTO;
import io.github.renatoxico.extract.model.NoteableDay;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("""
            SELECT
                e.transactionName,
                e.value,
                e.date,
                COALESCE(e.transactionType, 'Outros / Transferências')
            
            FROM Expense e
            WHERE e.sessionId = :sessionId
            ORDER BY e.value DESC
            """)
    List<ExpenseDTO> getAllExpenses(@Param("sessionId") String sessionId);

    @Query(value = """
            SELECT
                ID,
                SESSION_ID,
                TRANSACTION_NAME,
                DATE,
                VALUE,
                COALESCE(TRANSACTION_TYPE, 'Outros / Transferências') AS TRANSACTION_TYPE
            FROM TB_EXPENSE
            WHERE SESSION_ID = :sessionId
            ORDER BY VALUE DESC
            """, nativeQuery = true)
    List<Expense> getAllExpenses2(@Param("sessionId") String sessionId);

    @Query(value = """
            (SELECT TE.DATE, COUNT(1) AS TRANSACIONS, SUM(TE.VALUE) AS TOTAL
            FROM TB_EXPENSE TE
            WHERE TE.SESSION_ID = :sessionId
            GROUP BY TE.DATE
            ORDER BY COUNT(1) DESC, TOTAL DESC
            LIMIT 1)
            UNION ALL
            (SELECT TE.DATE, COUNT(1) AS TRANSACIONS, SUM(TE.VALUE) AS TOTAL
            FROM TB_EXPENSE TE
            WHERE TE.SESSION_ID = :sessionId
            GROUP BY TE.DATE
            ORDER BY SUM(TE.VALUE ) DESC
            LIMIT 1)""", nativeQuery = true)
    List<NoteableDay> getNoteableDays(@Param("sessionId") String sessionId);

    @Query(value = """
            SELECT DISTINCT (TRANSACTION_NAME),
            TRANSACTION_TYPE
            FROM TB_EXPENSE
            WHERE TRANSACTION_TYPE IS NULL OR TRANSACTION_TYPE = ''
            ORDER BY TRANSACTION_NAME
            LIMIT 50
            """, nativeQuery = true)
    List<CategoryMapper> getExpenseNames();

    @Query(value = """
            SELECT
                    TRANSACTION_NAME,
                    SUM(VALUE) TOTAL,
                    COUNT(1) INSTANCES,
                    COALESCE(TRANSACTION_TYPE, 'Outros / Transferências') AS TRANSACTION_TYPE
                FROM TB_EXPENSE TE
                WHERE TE.SESSION_ID = :sessionId
                GROUP BY TRANSACTION_NAME, TRANSACTION_TYPE
                ORDER BY TOTAL DESC, INSTANCES DESC, TRANSACTION_NAME
            """, nativeQuery = true)
    List<ExpensesGroupedDTO> getGroupedExpenses(@Param("sessionId") String sessionId);

    @Query(value = """
            SELECT SUM(TE.VALUE ), TE.TRANSACTION_TYPE
            FROM TB_EXPENSE TE
            WHERE SESSION_ID = :sessionId
            AND TE.TRANSACTION_TYPE IS NOT NULL AND TE.TRANSACTION_TYPE <> ''
            GROUP BY TE.TRANSACTION_TYPE
            ORDER BY 1
            """, nativeQuery = true)
    List<ExpensesCategories> getExpensesByType(@Param("sessionId") String sessionId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE TB_EXPENSE SET TRANSACTION_TYPE = :type WHERE TRANSACTION_NAME = :name", nativeQuery = true)
    void updateTransactionType(@Param("name") String name, @Param("type") String type);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE TB_EXPENSE TE SET TRANSACTION_TYPE = (SELECT DISTINCT(TE2.TRANSACTION_TYPE)  FROM TB_EXPENSE TE2 WHERE TE2.TRANSACTION_NAME = TE.TRANSACTION_NAME AND te2.TRANSACTION_TYPE <> '' LIMIT 1)
            WHERE TE.TRANSACTION_TYPE = ''
            AND EXISTS (SELECT 1 FROM TB_EXPENSE TE2 WHERE TE2.TRANSACTION_NAME = TE.TRANSACTION_NAME AND te2.TRANSACTION_TYPE <> '')
            """, nativeQuery = true)
    void updateMatchedExpenses();
}
