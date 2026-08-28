package io.github.renatoxico.extract.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import io.github.renatoxico.extract.config.AdminEmailProperties;
import io.github.renatoxico.extract.config.FirebaseAuthenticationEntryPoint;
import io.github.renatoxico.extract.config.SecurityConfig;
import io.github.renatoxico.extract.model.AppUser;
import io.github.renatoxico.extract.service.AdminEmailService;
import io.github.renatoxico.extract.service.AppUserService;
import io.github.renatoxico.extract.service.ExpenseReportAccessService;
import io.github.renatoxico.extract.service.ExpenseReportingService;
import io.github.renatoxico.extract.service.ExtractionFacade;
import io.github.renatoxico.extract.service.ExtractorService;
import io.github.renatoxico.extract.service.V2ReportMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@AutoConfigureMockMvc
@Import({SecurityConfig.class, FirebaseAuthenticationEntryPoint.class})
class SecurityConfigWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FirebaseAuth firebaseAuth;

    @MockitoBean
    private AppUserService appUserService;

    @MockitoBean
    private AdminEmailService adminEmailService;

    @MockitoBean
    private AdminEmailProperties adminEmailProperties;

    @MockitoBean
    private ExtractionFacade extractionFacade;

    @MockitoBean
    private ExtractorService extractorService;

    @MockitoBean
    private ExpenseReportingService expenseReportingService;

    @MockitoBean
    private ExpenseReportAccessService expenseReportAccessService;

    @MockitoBean
    private V2ReportMapper v2ReportMapper;

    @Test
    void termsIsPublic() throws Exception {
        mockMvc.perform(get("/terms"))
            .andExpect(status().isOk());
    }

    @Test
    void reportEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/v2/extract/reports"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_MISSING"));
    }

    @Test
    void reportEndpointsRejectInvalidTokens() throws Exception {
        FirebaseAuthException firebaseException = mock(FirebaseAuthException.class);
        when(firebaseAuth.verifyIdToken("invalid-token")).thenThrow(firebaseException);

        mockMvc.perform(get("/v2/extract/reports")
                .header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_INVALID"));
    }

    @Test
    void removedAuthenticationDiagnosticReturnsNotFound() throws Exception {
        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(42L);
        when(firebaseToken.getUid()).thenReturn("firebase-uid-123");
        when(firebaseAuth.verifyIdToken("valid-token")).thenReturn(firebaseToken);
        when(appUserService.synchronize(firebaseToken)).thenReturn(appUser);

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer valid-token"))
            .andExpect(status().isNotFound());
    }
}
