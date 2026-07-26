package com.example.api.controller;

import com.example.api.model.AuthenticatedUserResponse;
import com.example.api.model.AuthenticatedUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponse> me(
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        AuthenticatedUserResponse response = new AuthenticatedUserResponse(
            principal.localUserId(),
            principal.uid(),
            principal.email(),
            principal.name(),
            principal.picture(),
            principal.emailVerified()
        );

        return ResponseEntity.ok(response);
    }
}
