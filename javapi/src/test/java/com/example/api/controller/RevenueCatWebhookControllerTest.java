package com.example.api.controller;

import com.example.api.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RevenueCatWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "revenuecat.webhook.secret=test-webhook-secret")
class RevenueCatWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String WEBHOOK_URL = "/api/webhooks/revenuecat";
    private static final String VALID_SECRET = "test-webhook-secret";

    private Map<String, Object> validPayload(String eventType) {
        UUID userId = UUID.randomUUID();
        return validPayload(userId, eventType);
    }

    private Map<String, Object> validPayload(UUID userId, String eventType) {
        return Map.of("event", Map.of(
                "app_user_id", userId.toString(),
                "type", eventType
        ));
    }

    @Test
    void shouldReturn401WhenNoAuthorizationHeader() throws Exception {
        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(validPayload("INITIAL_PURCHASE"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenWrongSecret() throws Exception {
        mockMvc.perform(post(WEBHOOK_URL)
                        .header("Authorization", "wrong-secret")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(validPayload("INITIAL_PURCHASE"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn400WhenNoEventInPayload() throws Exception {
        mockMvc.perform(post(WEBHOOK_URL)
                        .header("Authorization", VALID_SECRET)
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of("something", "else"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenEventIsNotAMap() throws Exception {
        mockMvc.perform(post(WEBHOOK_URL)
                        .header("Authorization", VALID_SECRET)
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(Map.of("event", "not-a-map"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenMissingAppUserId() throws Exception {
        Map<String, Object> payload = Map.of("event", Map.of("type", "INITIAL_PURCHASE"));

        mockMvc.perform(post(WEBHOOK_URL)
                        .header("Authorization", VALID_SECRET)
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenMissingEventType() throws Exception {
        Map<String, Object> payload = Map.of("event", Map.of("app_user_id", UUID.randomUUID().toString()));

        mockMvc.perform(post(WEBHOOK_URL)
                        .header("Authorization", VALID_SECRET)
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenAppUserIdIsNotUUID() throws Exception {
        Map<String, Object> payload = Map.of("event", Map.of(
                "app_user_id", "not-a-uuid",
                "type", "INITIAL_PURCHASE"
        ));

        mockMvc.perform(post(WEBHOOK_URL)
                        .header("Authorization", VALID_SECRET)
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenUserServiceThrows() throws Exception {
        doThrow(new IllegalArgumentException("User not found"))
                .when(userService).handleRevenueCatEvent(any(), any());

        mockMvc.perform(post(WEBHOOK_URL)
                        .header("Authorization", VALID_SECRET)
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(validPayload("INITIAL_PURCHASE"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn200ForValidInitialPurchase() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post(WEBHOOK_URL)
                        .header("Authorization", VALID_SECRET)
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(validPayload(userId, "INITIAL_PURCHASE"))))
                .andExpect(status().isOk());

        verify(userService).handleRevenueCatEvent(eq(userId), eq("INITIAL_PURCHASE"));
    }

    @Test
    void shouldReturn200ForCancellation() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post(WEBHOOK_URL)
                        .header("Authorization", VALID_SECRET)
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(validPayload(userId, "CANCELLATION"))))
                .andExpect(status().isOk());

        verify(userService).handleRevenueCatEvent(eq(userId), eq("CANCELLATION"));
    }

    @Test
    void shouldReturn200ForRenewal() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post(WEBHOOK_URL)
                        .header("Authorization", VALID_SECRET)
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(validPayload(userId, "RENEWAL"))))
                .andExpect(status().isOk());

        verify(userService).handleRevenueCatEvent(eq(userId), eq("RENEWAL"));
    }
}
