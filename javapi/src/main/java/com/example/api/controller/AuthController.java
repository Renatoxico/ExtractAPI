package com.example.api.controller;

import com.example.api.model.AuthenticatedUserResponse;
import com.example.api.model.ErrorResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String BEARER_PREFIX = "Bearer ";

    private final FirebaseAuth firebaseAuth;

    public AuthController(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        if (authorization == null || authorization.isBlank()) {
            return unauthorized("AUTH_TOKEN_MISSING", "Authorization header is required");
        }

        if (!authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return unauthorized("AUTH_TOKEN_INVALID_FORMAT", "Authorization header must use the Bearer scheme");
        }

        String idToken = authorization.substring(BEARER_PREFIX.length()).trim();
        if (idToken.isEmpty()) {
            return unauthorized("AUTH_TOKEN_INVALID_FORMAT", "Bearer token is required");
        }

        try {
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
            AuthenticatedUserResponse response = new AuthenticatedUserResponse(
                decodedToken.getUid(),
                decodedToken.getEmail(),
                decodedToken.getName(),
                decodedToken.getPicture(),
                decodedToken.isEmailVerified()
            );

            return ResponseEntity.ok(response);
        } catch (FirebaseAuthException ex) {
            return unauthorized("AUTH_TOKEN_INVALID", "Firebase ID Token is invalid or expired");
        }
    }

    private ResponseEntity<ErrorResponse> unauthorized(String errorCode, String message) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse(errorCode, message));
    }
}
