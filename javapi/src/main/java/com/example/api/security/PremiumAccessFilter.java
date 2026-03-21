package com.example.api.security;

import com.example.api.model.User;
import com.example.api.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class PremiumAccessFilter extends OncePerRequestFilter {

    private static final String PROTECTED_PATH = "/extract/**";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final UserService userService;

    public PremiumAccessFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !PATH_MATCHER.match(PROTECTED_PATH, request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            sendForbidden(response, "Authentication required");
            return;
        }

        String sub = jwtAuth.getToken().getSubject();
        String email = jwtAuth.getToken().getClaimAsString("email");

        UUID userId;
        try {
            userId = UUID.fromString(sub);
        } catch (IllegalArgumentException e) {
            sendForbidden(response, "Invalid user identifier in token");
            return;
        }

        User user = userService.findOrCreate(userId, email);

        if (!user.isPremium()) {
            sendForbidden(response, "Premium subscription required");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
