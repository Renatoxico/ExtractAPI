package io.github.renatoxico.extract.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import io.github.renatoxico.extract.config.FirebaseAuthenticationEntryPoint;
import io.github.renatoxico.extract.config.SecurityConfig;
import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.handler.GlobalExceptionHandler;
import io.github.renatoxico.extract.model.AppUser;
import io.github.renatoxico.extract.service.AccountDeletionService;
import io.github.renatoxico.extract.service.AppUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, FirebaseAuthenticationEntryPoint.class, GlobalExceptionHandler.class})
class AccountControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FirebaseAuth firebaseAuth;

    @MockitoBean
    private AppUserService appUserService;

    @MockitoBean
    private AccountDeletionService accountDeletionService;

    @Test
    void deleteWithoutTokenIsUnauthorizedAndDoesNotDelete() throws Exception {
        mockMvc.perform(delete("/v2/account")
                .header("Origin", "http://localhost:5173"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_MISSING"))
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        verify(accountDeletionService, never()).deleteAccount(any());
    }

    @Test
    void deleteWithValidTokenReturnsNoContentOnlyAfterTheServiceAcceptsIt() throws Exception {
        authenticate();

        mockMvc.perform(delete("/v2/account")
                .header("Authorization", "Bearer valid-token")
                .header("Origin", "http://localhost:5173"))
            .andExpect(status().isNoContent())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        verify(accountDeletionService).deleteAccount(argThat(principal ->
            Long.valueOf(42L).equals(principal.localUserId())
                && "firebase-uid-123".equals(principal.uid())
        ));
    }

    @Test
    void firebaseFailureIsNotReportedAsSuccess() throws Exception {
        authenticate();
        doThrow(new ProcessingException(
            "Failed to delete the authentication account",
            HttpStatus.BAD_GATEWAY,
            "ACCOUNT_DELETION_FAILED"
        )).when(accountDeletionService).deleteAccount(any());

        mockMvc.perform(delete("/v2/account")
                .header("Authorization", "Bearer valid-token"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.errorCode").value("ACCOUNT_DELETION_FAILED"));
    }

    @Test
    void preflightAllowsDeleteFromTheConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/v2/account")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "DELETE")
                .header("Access-Control-Request-Headers", "Authorization"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
            .andExpect(header().string("Access-Control-Allow-Methods", containsString("DELETE")))
            .andExpect(header().string("Access-Control-Allow-Headers", containsString("Authorization")));
    }

    private void authenticate() throws Exception {
        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        AppUser appUser = mock(AppUser.class);
        when(appUser.getId()).thenReturn(42L);
        when(firebaseToken.getUid()).thenReturn("firebase-uid-123");
        when(firebaseToken.getEmail()).thenReturn("user@example.com");
        when(firebaseToken.getName()).thenReturn("Example User");
        when(firebaseToken.getPicture()).thenReturn(null);
        when(firebaseToken.isEmailVerified()).thenReturn(true);
        when(firebaseAuth.verifyIdToken("valid-token", true)).thenReturn(firebaseToken);
        when(appUserService.synchronize(firebaseToken)).thenReturn(appUser);
    }
}
