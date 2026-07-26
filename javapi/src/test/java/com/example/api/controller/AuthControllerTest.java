package com.example.api.controller;

import com.example.api.config.FirebaseAuthenticationEntryPoint;
import com.example.api.config.SecurityConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
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

@WebMvcTest({AuthController.class, PageController.class})
@AutoConfigureMockMvc
@Import({SecurityConfig.class, FirebaseAuthenticationEntryPoint.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FirebaseAuth firebaseAuth;

    @Test
    void termsIsPublic() throws Exception {
        mockMvc.perform(get("/terms"))
            .andExpect(status().isOk());
    }

    @Test
    void meWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_MISSING"));
    }

    @Test
    void extractEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/extract/"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void meWithInvalidTokenReturnsUnauthorized() throws Exception {
        FirebaseAuthException firebaseException = mock(FirebaseAuthException.class);
        when(firebaseAuth.verifyIdToken("invalid-token")).thenThrow(firebaseException);

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_INVALID"));
    }

    @Test
    void meWithValidTokenReturnsAuthenticatedUser() throws Exception {
        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        when(firebaseToken.getUid()).thenReturn("firebase-uid-123");
        when(firebaseToken.getEmail()).thenReturn("user@example.com");
        when(firebaseToken.getName()).thenReturn("Example User");
        when(firebaseToken.getPicture()).thenReturn("https://example.com/photo.jpg");
        when(firebaseToken.isEmailVerified()).thenReturn(true);
        when(firebaseAuth.verifyIdToken("valid-token")).thenReturn(firebaseToken);

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer valid-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.uid").value("firebase-uid-123"))
            .andExpect(jsonPath("$.email").value("user@example.com"))
            .andExpect(jsonPath("$.name").value("Example User"))
            .andExpect(jsonPath("$.emailVerified").value(true));
    }
}
