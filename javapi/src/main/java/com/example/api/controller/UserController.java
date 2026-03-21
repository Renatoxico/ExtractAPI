package com.example.api.controller;

import com.example.api.model.User;
import com.example.api.service.UserService;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/status")
    public Map<String, Boolean> getStatus(JwtAuthenticationToken jwtAuth) {
        UUID userId = UUID.fromString(jwtAuth.getToken().getSubject());
        String email = jwtAuth.getToken().getClaimAsString("email");
        User user = userService.findOrCreate(userId, email);
        return Map.of("isPremium", user.isPremium());
    }
}
