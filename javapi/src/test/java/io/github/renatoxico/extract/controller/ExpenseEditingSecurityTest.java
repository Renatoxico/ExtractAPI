package io.github.renatoxico.extract.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import io.github.renatoxico.extract.config.FirebaseAuthenticationEntryPoint;
import io.github.renatoxico.extract.config.SecurityConfig;
import io.github.renatoxico.extract.model.AppUser;
import io.github.renatoxico.extract.model.ReportData;
import io.github.renatoxico.extract.model.ReportHighlights;
import io.github.renatoxico.extract.service.AppUserService;
import io.github.renatoxico.extract.service.ExpenseEditingService;
import io.github.renatoxico.extract.service.V2ReportMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExpenseEditingController.class)
@Import({SecurityConfig.class, FirebaseAuthenticationEntryPoint.class, V2ReportMapper.class})
class ExpenseEditingSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean FirebaseAuth firebaseAuth;
    @MockitoBean AppUserService users;
    @MockitoBean ExpenseEditingService editing;

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PATCH", "DELETE"})
    void everyEditingEndpointRequiresAuthentication(String method) throws Exception {
        var request = switch (method) {
            case "POST" -> post("/v2/reports/own/expenses");
            case "PATCH" -> patch("/v2/reports/own/expenses/category");
            default -> delete("/v2/reports/own/expenses/1");
        };
        mvc.perform(request.contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_MISSING"))
            .andExpect(jsonPath("$.message").isString())
            .andExpect(jsonPath("$.timestamp").exists());
        verifyNoInteractions(editing);
    }

    @Test
    void firebasePrincipalIsPassedToAllEditingMethods() throws Exception {
        FirebaseToken token = mock(FirebaseToken.class);
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(42L);
        when(token.getUid()).thenReturn("uid");
        when(firebaseAuth.verifyIdToken("valid", true)).thenReturn(token);
        when(users.synchronize(token)).thenReturn(user);
        ReportData report = new ReportData("own", Instant.now(), List.of(), List.of(), List.of(), new ReportHighlights(null, null, null));
        when(editing.addExpense(eq("own"), eq(42L), any())).thenReturn(report);
        when(editing.correctCategory(eq("own"), eq(42L), any())).thenReturn(report);
        when(editing.removeExpense("own", 42L, 1L)).thenReturn(report);
        mvc.perform(post("/v2/reports/own/expenses").header("Authorization", "Bearer valid")
            .contentType("application/json").content("{}"))
            .andExpect(status().isCreated());
        mvc.perform(patch("/v2/reports/own/expenses/category").header("Authorization", "Bearer valid")
            .contentType("application/json").content("{}"))
            .andExpect(status().isOk());
        mvc.perform(delete("/v2/reports/own/expenses/1").header("Authorization", "Bearer valid"))
            .andExpect(status().isOk());
        verify(editing).addExpense(eq("own"), eq(42L), any());
        verify(editing).correctCategory(eq("own"), eq(42L), any());
        verify(editing).removeExpense("own", 42L, 1L);
    }

    @Test
    void preflightAllowsPatch() throws Exception {
        mvc.perform(options("/v2/reports/own/expenses/category")
            .header("Origin", "http://localhost:5173")
            .header("Access-Control-Request-Method", "PATCH")
            .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Methods", containsString("PATCH")));
    }
}
