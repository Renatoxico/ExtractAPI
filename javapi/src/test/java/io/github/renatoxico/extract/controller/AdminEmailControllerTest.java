package io.github.renatoxico.extract.controller;

import io.github.renatoxico.extract.config.AdminEmailProperties;
import io.github.renatoxico.extract.service.AdminEmailService;
import io.github.renatoxico.extract.service.AdminEmailService.ResendResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.NoSuchElementException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminEmailController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminEmailControllerTest {
    private static final String ADMIN_KEY = "test-admin-email-api-key-1234567890";
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AdminEmailService adminEmailService;
    @MockitoBean
    private AdminEmailProperties properties;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        when(properties.getApiKey()).thenReturn(ADMIN_KEY);
    }

    @Test
    void acceptsResend() throws Exception {
        when(adminEmailService.resend(7L)).thenReturn(new ResendResult(8L, "RESENT"));

        mockMvc.perform(post("/api/admin/email-notifications/7/resend")
                .header("X-Admin-API-Key", ADMIN_KEY))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.notificationId").value(8))
            .andExpect(jsonPath("$.action").value("RESENT"));
    }

    @Test
    void returnsNotFoundForUnknownNotification() throws Exception {
        when(adminEmailService.resend(7L))
            .thenThrow(new NoSuchElementException("not found"));

        mockMvc.perform(post("/api/admin/email-notifications/7/resend")
                .header("X-Admin-API-Key", ADMIN_KEY))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("EMAIL_NOTIFICATION_NOT_FOUND"));
    }

    @Test
    void returnsConflictWhileNotificationIsSending() throws Exception {
        when(adminEmailService.resend(7L))
            .thenThrow(new IllegalStateException("busy"));

        mockMvc.perform(post("/api/admin/email-notifications/7/resend")
                .header("X-Admin-API-Key", ADMIN_KEY))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("EMAIL_NOTIFICATION_BUSY"));
    }

    @Test
    void rejectsMissingAdminKey() throws Exception {
        mockMvc.perform(post("/api/admin/email-notifications/7/resend"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("ADMIN_API_KEY_INVALID"));
    }
}
