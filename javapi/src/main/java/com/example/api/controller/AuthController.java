package com.example.api.controller;

import com.example.api.model.AuthenticatedUserResponse;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponse> me(@AuthenticationPrincipal FirebaseToken firebaseToken) {
        AuthenticatedUserResponse response = new AuthenticatedUserResponse(
            firebaseToken.getUid(),
            firebaseToken.getEmail(),
            firebaseToken.getName(),
            firebaseToken.getPicture(),
            firebaseToken.isEmailVerified()
        );

        return ResponseEntity.ok(response);
    }
}
