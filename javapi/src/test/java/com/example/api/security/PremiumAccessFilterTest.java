package com.example.api.security;

import com.example.api.model.User;
import com.example.api.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PremiumAccessFilterTest {

    @Mock
    private UserService userService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private PremiumAccessFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void setJwtAuthentication(String subject, String email) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(subject)
                .claim("email", email)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Test
    void shouldSkipFilterForNonExtractPaths() throws ServletException, IOException {
        request.setServletPath("/api/user/status");
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldSkipFilterForWebhookPaths() throws ServletException, IOException {
        request.setServletPath("/api/webhooks/revenuecat");
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldReturn403WhenNoAuthentication() throws ServletException, IOException {
        request.setServletPath("/extract/");
        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Authentication required");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void shouldReturn403WhenSubjectIsNotValidUUID() throws ServletException, IOException {
        request.setServletPath("/extract/");
        setJwtAuthentication("not-a-uuid", "test@email.com");

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Invalid user identifier");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void shouldReturn403WhenUserIsNotPremium() throws ServletException, IOException {
        request.setServletPath("/extract/");
        UUID userId = UUID.randomUUID();
        setJwtAuthentication(userId.toString(), "test@email.com");

        User nonPremiumUser = new User(userId, "test@email.com");
        nonPremiumUser.setPremium(false);
        when(userService.findOrCreate(eq(userId), eq("test@email.com"))).thenReturn(nonPremiumUser);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Premium subscription required");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void shouldPassFilterWhenUserIsPremium() throws ServletException, IOException {
        request.setServletPath("/extract/");
        UUID userId = UUID.randomUUID();
        setJwtAuthentication(userId.toString(), "premium@email.com");

        User premiumUser = new User(userId, "premium@email.com");
        premiumUser.setPremium(true);
        when(userService.findOrCreate(eq(userId), eq("premium@email.com"))).thenReturn(premiumUser);

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldFilterExtractSubpaths() throws ServletException, IOException {
        request.setServletPath("/extract/summary/abc123");
        // No auth set — should get 403
        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void shouldPassCorrectParametersToUserService() throws ServletException, IOException {
        request.setServletPath("/extract/");
        UUID userId = UUID.randomUUID();
        String email = "user@example.com";
        setJwtAuthentication(userId.toString(), email);

        User premiumUser = new User(userId, email);
        premiumUser.setPremium(true);
        when(userService.findOrCreate(any(), any())).thenReturn(premiumUser);

        filter.doFilter(request, response, filterChain);

        verify(userService).findOrCreate(eq(userId), eq(email));
    }

    @Test
    void shouldHandleNullEmailInToken() throws ServletException, IOException {
        request.setServletPath("/extract/");
        UUID userId = UUID.randomUUID();
        setJwtAuthentication(userId.toString(), null);

        User premiumUser = new User(userId, null);
        premiumUser.setPremium(true);
        when(userService.findOrCreate(eq(userId), isNull())).thenReturn(premiumUser);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userService).findOrCreate(eq(userId), isNull());
    }
}
