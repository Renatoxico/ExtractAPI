package com.example.api.config;

import com.example.api.model.AppUser;
import com.example.api.model.AuthenticatedUserPrincipal;
import com.example.api.service.AppUserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class FirebaseAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final FirebaseAuth firebaseAuth;
    private final FirebaseAuthenticationEntryPoint authenticationEntryPoint;
    private final AppUserService appUserService;

    public FirebaseAuthenticationFilter(
        FirebaseAuth firebaseAuth,
        FirebaseAuthenticationEntryPoint authenticationEntryPoint,
        AppUserService appUserService
    ) {
        this.firebaseAuth = firebaseAuth;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.appUserService = appUserService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            rejectInvalidToken(request, response);
            return;
        }

        String idToken = authorization.substring(BEARER_PREFIX.length()).trim();
        if (idToken.isEmpty()) {
            rejectInvalidToken(request, response);
            return;
        }

        try {
            FirebaseToken firebaseToken = firebaseAuth.verifyIdToken(idToken);
            AppUser appUser = appUserService.synchronize(firebaseToken);
            AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                appUser.getId(),
                firebaseToken.getUid(),
                firebaseToken.getEmail(),
                firebaseToken.getName(),
                firebaseToken.getPicture(),
                firebaseToken.isEmailVerified()
            );
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (FirebaseAuthException exception) {
            SecurityContextHolder.clearContext();
            rejectInvalidToken(request, response);
        }
    }

    private void rejectInvalidToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        authenticationEntryPoint.commence(
            request,
            response,
            new BadCredentialsException("Firebase ID Token is invalid")
        );
    }
}
