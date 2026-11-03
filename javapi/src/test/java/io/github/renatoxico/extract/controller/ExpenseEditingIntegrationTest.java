package io.github.renatoxico.extract.controller;

import io.github.renatoxico.extract.handler.GlobalExceptionHandler;
import io.github.renatoxico.extract.model.AuthenticatedUserPrincipal;
import io.github.renatoxico.extract.repo.ExpenseRepository;
import io.github.renatoxico.extract.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DataJpaTest
@org.springframework.boot.autoconfigure.ImportAutoConfiguration(org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class)
@Import({ExpenseEditingService.class, ExpenseReportAccessService.class, ExpenseReportingService.class,
    V2ReportMapper.class, ExpenseEditingController.class})
class ExpenseEditingIntegrationTest {
    @Autowired ExpenseEditingController controller;
    @Autowired ExpenseEditingService editing;
    @Autowired ExpenseReportingService reporting;
    @Autowired ExpenseRepository expenses;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private MockMvc mvc;
    private Long userId;
    private Long first;
    private Long second;
    private Long foreign;
    private Long sameOwnerOtherReport;
    private static final String BASE = "/v2/reports/own/expenses";
    private static final String VALID = """
        {"name":" MANUAL UBER ","amount":12.34,"date":"2024-02-29","category":"Supermercado"}
        """;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
            .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
        userId = user("owner");
        Long other = user("other");
        report("own", userId);
        report("same-owner", userId);
        report("foreign", other);
        first = expense("own", "SHOP", "10.00");
        second = expense("own", "SHOP", "20.00");
        foreign = expense("foreign", "SHOP", "100.00");
        sameOwnerOtherReport = expense("same-owner", "SHOP", "200.00");
        var principal = new AuthenticatedUserPrincipal(userId, "owner", "owner@example.com", "Owner", null, true);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of()));
    }

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void repeatedCorrectionAuditsEveryMatchingRowAndRefreshesGroupsWithoutTouchingOtherReports() throws Exception {
        mvc.perform(patch(BASE + "/category").contentType("application/json").content(correction("Supermercado")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expenses[0].categorySource").value("USER"))
            .andExpect(jsonPath("$.expenses[0].source").value("EXTRACTED"))
            .andExpect(jsonPath("$.categorySummaries[0].totalAmount").value(30))
            .andExpect(jsonPath("$.expenseGroups[0].occurrenceCount").value(2));
        mvc.perform(patch(BASE + "/category").contentType("application/json").content(correction("Moradia / Contas")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.expenses[0].category").value("Moradia / Contas"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM expense_category_change", Long.class)).isEqualTo(4);
        assertThat(jdbc.queryForList("SELECT old_category FROM expense_category_change ORDER BY id", String.class))
            .containsExactly("Transporte / Auto", "Transporte / Auto", "Supermercado", "Supermercado");
        assertThat(jdbc.queryForList("SELECT new_category FROM expense_category_change ORDER BY id", String.class))
            .containsExactly("Supermercado", "Supermercado", "Moradia / Contas", "Moradia / Contas");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM expense_category_change WHERE app_user_id = ? AND report_id = 'own' AND changed_at IS NOT NULL", Long.class, userId)).isEqualTo(4);
        assertThat(jdbc.queryForList("SELECT category FROM expense WHERE report_id <> 'own'", String.class))
            .containsOnly("Transporte / Auto");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM expense_classification", Long.class)).isZero();
    }

    @Test
    void removeCascadesAuditAndRecalculatesTotalsHighlightsAndEmptyReport() throws Exception {
        editing.correctCategory("own", userId, new io.github.renatoxico.extract.api.v2.CategoryCorrectionRequest("SHOP", "Supermercado"));
        mvc.perform(delete(BASE + "/" + second)).andExpect(status().isOk())
            .andExpect(jsonPath("$.expenses.length()").value(1))
            .andExpect(jsonPath("$.categorySummaries[0].totalAmount").value(10))
            .andExpect(jsonPath("$.highlights.largestExpense.expenseId").value(first))
            .andExpect(jsonPath("$.highlights.mostActiveDay.transactionCount").value(1));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM expense_category_change", Long.class)).isEqualTo(1);
        mvc.perform(delete(BASE + "/" + first)).andExpect(status().isOk())
            .andExpect(jsonPath("$.expenses").isEmpty())
            .andExpect(jsonPath("$.expenseGroups").isEmpty())
            .andExpect(jsonPath("$.categorySummaries").isEmpty())
            .andExpect(jsonPath("$.highlights.largestExpense").isEmpty());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM expense_category_change", Long.class)).isZero();
        var summary = reporting.getReportSummaries(userId).stream().filter(r -> r.reportId().equals("own")).findFirst().orElseThrow();
        assertThat(summary.total()).isZero();
        assertThat(summary.countExpenses()).isZero();
        assertThat(reporting.getReport("own").expenses()).isEmpty();
        assertThat(new String(reporting.exportReportCsvV2("own"))).contains("expenseId");
    }

    @Test
    void addManualUsesUserCategoryPreservesDateAndRefreshesTotals() throws Exception {
        mvc.perform(post(BASE).contentType("application/json").content(VALID))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.expenses.length()").value(3))
            .andExpect(jsonPath("$.expenses[1].expenseName").value("MANUAL UBER"))
            .andExpect(jsonPath("$.expenses[1].date").value("2024-02-29"))
            .andExpect(jsonPath("$.expenses[1].category").value("Supermercado"))
            .andExpect(jsonPath("$.expenses[1].categorySource").value("USER"))
            .andExpect(jsonPath("$.expenses[1].source").value("MANUAL"));
        var summary = reporting.getReportSummaries(userId).stream().filter(r -> r.reportId().equals("own")).findFirst().orElseThrow();
        assertThat(summary.total()).isEqualByComparingTo("42.34");
        assertThat(summary.countExpenses()).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM expense_classification", Long.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"expenseName\":\" \",\"newCategory\":\"Supermercado\"}",
        "{\"expenseName\":\"SHOP\",\"newCategory\":\"invalid\"}", "{\"expenseName\":\"SHOP\"}", "null", "{"})
    void correctionValidationUsesNormalError(String body) throws Exception {
        normalError(mvc.perform(patch(BASE + "/category").contentType("application/json").content(body)), 400);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM expense_category_change", Long.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{", "null", "{\"amount\":\"abc\"}",
        "{\"name\":\" \",\"amount\":1,\"date\":\"2025-01-01\",\"category\":\"Supermercado\"}",
        "{\"name\":\"X\",\"amount\":0,\"date\":\"2025-01-01\",\"category\":\"Supermercado\"}",
        "{\"name\":\"X\",\"amount\":-1,\"date\":\"2025-01-01\",\"category\":\"Supermercado\"}",
        "{\"name\":\"X\",\"date\":\"2025-01-01\",\"category\":\"Supermercado\"}",
        "{\"name\":\"X\",\"amount\":1,\"date\":\"2025-02-29\",\"category\":\"Supermercado\"}",
        "{\"name\":\"X\",\"amount\":1,\"date\":\"31/02/2025\",\"category\":\"Supermercado\"}",
        "{\"name\":\"X\",\"amount\":1,\"category\":\"Supermercado\"}",
        "{\"name\":\"X\",\"amount\":1,\"date\":\"2025-01-01\",\"category\":\"invalid\"}"})
    void manualValidationUsesNormalErrorAndDoesNotInsert(String body) throws Exception {
        normalError(mvc.perform(post(BASE).contentType("application/json").content(body)), 400);
        assertThat(expenses.count()).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "foreign"})
    void allEndpointsHideMissingOrOtherUsersReports(String reportId) throws Exception {
        String base = "/v2/reports/" + reportId + "/expenses";
        normalError(mvc.perform(post(base).contentType("application/json").content(VALID)), 404)
            .andExpect(jsonPath("$.errorCode").value("REPORT_NOT_FOUND"));
        normalError(mvc.perform(patch(base + "/category").contentType("application/json").content(correction("Supermercado"))), 404);
        normalError(mvc.perform(delete(base + "/" + foreign)), 404);
        assertThat(expenses.count()).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM expense_category_change", Long.class)).isZero();
    }

    @Test
    void correctionRequiresNameInPathReport() throws Exception {
        jdbc.update("UPDATE expense SET expense_name = 'PRIVATE' WHERE id = ?", foreign);
        normalError(mvc.perform(patch(BASE + "/category").contentType("application/json")
            .content("{\"expenseName\":\"PRIVATE\",\"newCategory\":\"Supermercado\"}")), 404)
            .andExpect(jsonPath("$.errorCode").value("EXPENSE_NOT_FOUND"));
    }

    @Test
    void deleteRequiresExpenseInPathReportEvenForSameOwner() throws Exception {
        for (Long id : java.util.List.of(999999L, foreign, sameOwnerOtherReport)) {
            normalError(mvc.perform(delete(BASE + "/" + id)), 404)
                .andExpect(jsonPath("$.errorCode").value("EXPENSE_NOT_FOUND"));
        }
        assertThat(expenses.count()).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-1", "999999999999999999999999"})
    void invalidDeleteIdUsesNormalError(String id) throws Exception {
        normalError(mvc.perform(delete(BASE + "/" + id)), 400);
    }

    private ResultActions normalError(ResultActions result, int statusCode) throws Exception {
        return result.andExpect(status().is(statusCode)).andExpect(jsonPath("$.errorCode").isString())
            .andExpect(jsonPath("$.message").isString()).andExpect(jsonPath("$.timestamp").exists());
    }

    private String correction(String category) {
        return "{\"expenseName\":\"SHOP\",\"newCategory\":\"" + category + "\"}";
    }

    private Long user(String uid) {
        jdbc.update("INSERT INTO app_user(firebase_uid) VALUES (?)", uid);
        return jdbc.queryForObject("SELECT id FROM app_user WHERE firebase_uid = ?", Long.class, uid);
    }

    private void report(String id, Long owner) {
        jdbc.update("INSERT INTO expense_report(id, app_user_id, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)", id, owner);
    }

    private Long expense(String report, String name, String amount) {
        jdbc.update("INSERT INTO expense(report_id, expense_name, amount, date, category) VALUES (?, ?, ?, '01/01/2025', 'Transporte / Auto')", report, name, new java.math.BigDecimal(amount));
        return jdbc.queryForObject("SELECT max(id) FROM expense", Long.class);
    }
}
