package io.github.renatoxico.extract.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ExpenseCategoryProtectionTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ExpenseClassificationRepository catalog;
    private ClassificationWorkRepository work;

    @BeforeEach
    void setUp() {
        work = new ClassificationWorkRepository(jdbc, new ObjectMapper());
        jdbc.update("INSERT INTO app_user(firebase_uid) VALUES ('protection-user')");
        Long user = jdbc.queryForObject("SELECT id FROM app_user WHERE firebase_uid = 'protection-user'", Long.class);
        jdbc.update("INSERT INTO expense_report(id, app_user_id, created_at) VALUES ('protection', ?, CURRENT_TIMESTAMP)", user);
        jdbc.update("INSERT INTO expense_classification(expense_name, category, source, created_at, updated_at) VALUES ('SHOP', 'Supermercado', 'AI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
    }

    @Test
    void workerSkipsUserSourceEvenWhenCategoryIsNullAndAClaimIsAlreadyQueued() {
        long user = expense("USER", null);
        long pending = expense("RULE", null);
        long chosen = expense("USER", "Moradia / Contas");
        assertThat(work.findExpenseIdsReadyForPropagation(50)).containsExactly(pending);
        assertThat(work.propagateCategoryToExpense(user)).isZero();
        assertThat(work.propagateCategoryToExpense(chosen)).isZero();
        // A previously selected candidate becomes USER before the worker writes.
        jdbc.update("UPDATE expense SET category_source = 'USER' WHERE id = ?", pending);
        assertThat(work.propagateCategoryToExpense(pending)).isZero();
        assertThat(category(user)).isNull();
        assertThat(category(chosen)).isEqualTo("Moradia / Contas");
    }

    @Test
    void workerStillFillsUntouchedExpenseAndRecordsCatalogProvenance() {
        long pending = expense("RULE", null);
        assertThat(work.propagateCategoryToExpense(pending)).isEqualTo(1);
        assertThat(category(pending)).isEqualTo("Supermercado");
        assertThat(jdbc.queryForObject("SELECT category_source FROM expense WHERE id = ?", String.class, pending)).isEqualTo("CATALOG");
        assertThat(work.propagateCategoryToExpense(pending)).isZero();
    }

    @Test
    void reportCatalogApplyAlsoGuardsUserSource() {
        long user = expense("USER", null);
        long pending = expense("RULE", null);
        assertThat(catalog.applyCategoriesToReport("protection")).isEqualTo(1);
        assertThat(category(user)).isNull();
        assertThat(category(pending)).isEqualTo("Supermercado");
    }

    private long expense(String source, String category) {
        jdbc.update("INSERT INTO expense(report_id, expense_name, category, category_source, amount, date) VALUES ('protection', 'SHOP', ?, ?, 10, '01/01/2025')", category, source);
        return jdbc.queryForObject("SELECT max(id) FROM expense", Long.class);
    }

    private String category(long id) {
        return jdbc.queryForObject("SELECT category FROM expense WHERE id = ?", String.class, id);
    }
}
