package com.example.api.config;

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

    public FirebaseAuthenticationFilter(
        FirebaseAuth firebaseAuth,
        FirebaseAuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.firebaseAuth = firebaseAuth;
        this.authenticationEntryPoint = authenticationEntryPoint;
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
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(firebaseToken, null, List.of());
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
