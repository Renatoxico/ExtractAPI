package io.github.renatoxico.extract.service;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.flyway.enabled=true",
    "spring.config.import=",
    "classification.catalog-registration-cron=-",
    "classification.batch-creation-cron=-",
    "classification.ai-cron=-",
    "classification.ai-recovery-cron=-",
    "classification.apply-cron=-",
    "classification.apply-recovery-cron=-",
    "classification.propagation-cron=-",
    "ai.api-key=test-gemini-api-key-1234567890123456",
    "cors.allowed-origin=http://localhost:5173"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class AccountDeletionPostgresIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private FirebaseAuth firebaseAuth;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("""
            TRUNCATE TABLE
                admin_email_outbox,
                expense_classification_task,
                ai_classification_batch,
                expense_classification,
                expense,
                expense_report,
                app_user,
                users
            RESTART IDENTITY CASCADE
            """);
    }

    @Test
    void deleteAccountRemovesOnlyThatUsersDataRevokesFirebaseAndKeepsSharedRecords() throws Exception {
        long ownerId = insertUser("uid-owner", "owner@example.com");
        long otherId = insertUser("uid-other", "other@example.com");
        insertReport("owner-report", ownerId);
        insertExpense("owner-report", "PADARIA SAO JOSE");
        insertReport("other-report", otherId);
        insertExpense("other-report", "PRIVATE");
        insertCatalog("UBER TRIP");
        insertLegacyUser("owner@example.com");
        insertWeeklyOutbox(ownerId, "weekly-owner");
        stubToken("valid-token", "uid-owner", "owner@example.com");

        mockMvc.perform(delete("/v2/account").header("Authorization", "Bearer valid-token"))
            .andExpect(status().isNoContent());

        assertThat(count("SELECT COUNT(*) FROM app_user WHERE id = ?", ownerId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM expense_report WHERE id = 'owner-report'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM expense WHERE report_id = 'owner-report'")).isZero();
        assertThat(count("SELECT COUNT(*) FROM app_user WHERE id = ?", otherId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM expense_report WHERE id = 'other-report'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM expense WHERE report_id = 'other-report'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM expense_classification WHERE expense_name = 'UBER TRIP'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM users")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM admin_email_outbox WHERE deduplication_key = 'weekly-owner'")).isEqualTo(1);
        String payload = jdbc.queryForObject(
            "SELECT payload::text FROM admin_email_outbox WHERE deduplication_key = 'weekly-owner'",
            String.class
        );
        assertThat(payload).doesNotContain("owner@example.com").doesNotContain("\"Owner\"");
        assertThat(payload).contains("other@example.com").contains("Other");

        InOrder firebase = inOrder(firebaseAuth);
        firebase.verify(firebaseAuth).revokeRefreshTokens("uid-owner");
        firebase.verify(firebaseAuth).deleteUser("uid-owner");
        verify(firebaseAuth, never()).deleteUser("uid-other");
        verify(firebaseAuth, never()).revokeRefreshTokens("uid-other");
    }

    @Test
    void firebaseFailureKeepsTheAccountAndIsNotASuccessResponse() throws Exception {
        long ownerId = insertUser("uid-owner", "owner@example.com");
        insertReport("owner-report", ownerId);
        insertExpense("owner-report", "PADARIA");
        insertWeeklyOutbox(ownerId, "weekly-owner");
        stubToken("valid-token", "uid-owner", "owner@example.com");
        doThrow(authError(null)).when(firebaseAuth).deleteUser("uid-owner");

        mockMvc.perform(delete("/v2/account").header("Authorization", "Bearer valid-token"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.errorCode").value("ACCOUNT_DELETION_FAILED"));

        assertThat(count("SELECT COUNT(*) FROM app_user WHERE id = ?", ownerId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM expense_report WHERE id = 'owner-report'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM expense WHERE report_id = 'owner-report'")).isEqualTo(1);
        String payload = jdbc.queryForObject(
            "SELECT payload::text FROM admin_email_outbox WHERE deduplication_key = 'weekly-owner'",
            String.class
        );
        assertThat(payload).contains("owner@example.com");
        verify(firebaseAuth).revokeRefreshTokens("uid-owner");
        verify(firebaseAuth).deleteUser("uid-owner");
    }

    @Test
    void missingFirebaseUserStillDeletesLocalData() throws Exception {
        long ownerId = insertUser("uid-owner", "owner@example.com");
        insertReport("owner-report", ownerId);
        insertExpense("owner-report", "PADARIA");
        stubToken("valid-token", "uid-owner", "owner@example.com");
        doThrow(authError(AuthErrorCode.USER_NOT_FOUND)).when(firebaseAuth).revokeRefreshTokens("uid-owner");
        doThrow(authError(AuthErrorCode.USER_NOT_FOUND)).when(firebaseAuth).deleteUser("uid-owner");

        mockMvc.perform(delete("/v2/account").header("Authorization", "Bearer valid-token"))
            .andExpect(status().isNoContent());

        assertThat(count("SELECT COUNT(*) FROM app_user WHERE id = ?", ownerId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM expense WHERE report_id = 'owner-report'")).isZero();
    }

    @Test
    void deletingAUserCascadesReports() {
        String rule = jdbc.queryForObject("""
            SELECT confdeltype::text
            FROM pg_constraint
            WHERE conname = 'expense_report_app_user_id_fkey'
            """, String.class);

        assertThat(rule).isEqualTo("c");
    }

    private void stubToken(String rawToken, String uid, String email) throws Exception {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn(uid);
        when(token.getEmail()).thenReturn(email);
        when(token.getName()).thenReturn("Owner");
        when(token.getPicture()).thenReturn(null);
        when(token.isEmailVerified()).thenReturn(true);
        when(firebaseAuth.verifyIdToken(rawToken, true)).thenReturn(token);
    }

    private static FirebaseAuthException authError(AuthErrorCode code) {
        FirebaseAuthException exception = mock(FirebaseAuthException.class);
        when(exception.getAuthErrorCode()).thenReturn(code);
        return exception;
    }

    private long insertUser(String firebaseUid, String email) {
        Long id = jdbc.queryForObject("""
            INSERT INTO app_user (firebase_uid, email, display_name)
            VALUES (?, ?, ?)
            RETURNING id
            """, Long.class, firebaseUid, email, "Owner");
        return id == null ? 0 : id;
    }

    private void insertReport(String reportId, long ownerId) {
        jdbc.update("""
            INSERT INTO expense_report (id, app_user_id, created_at)
            VALUES (?, ?, CURRENT_TIMESTAMP)
            """, reportId, ownerId);
    }

    private void insertExpense(String reportId, String name) {
        jdbc.update("""
            INSERT INTO expense (report_id, expense_name, amount, date)
            VALUES (?, ?, ?, '2026-08-22')
            """, reportId, name, BigDecimal.TEN);
    }

    private void insertCatalog(String expenseName) {
        jdbc.update("""
            INSERT INTO expense_classification (expense_name, category, source)
            VALUES (?, 'Transporte', 'AI')
            """, expenseName);
    }

    private void insertLegacyUser(String email) {
        jdbc.update("""
            INSERT INTO users (id, email, is_premium)
            VALUES (?, ?, false)
            """, UUID.randomUUID(), email);
    }

    private void insertWeeklyOutbox(long ownerId, String deduplicationKey) {
        String payload = """
            {"schemaVersion":1,"perUser":[
              {"userId":%d,"email":"owner@example.com","displayName":"Owner","reports":2},
              {"userId":999999,"email":"other@example.com","displayName":"Other","reports":1}
            ]}
            """.formatted(ownerId);
        jdbc.update("""
            INSERT INTO admin_email_outbox (notification_type, deduplication_key, payload, status)
            VALUES ('WEEKLY_STATUS_REPORT', ?, CAST(? AS jsonb), 'SENT')
            """, deduplicationKey, payload);
    }

    private int count(String sql, Object... args) {
        Integer count = jdbc.queryForObject(sql, Integer.class, args);
        return count == null ? 0 : count;
    }
}
