package com.example.api.controller;

import com.example.api.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/webhooks/revenuecat")
public class RevenueCatWebhookController {

    @Value("${revenuecat.webhook.secret}")
    private String webhookSecret;

    private final UserService userService;

    public RevenueCatWebhookController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> payload) {

        if (authHeader == null || !authHeader.equals(webhookSecret)) {
            return ResponseEntity.status(401).build();
        }

        Object eventObj = payload.get("event");
        if (!(eventObj instanceof Map<?, ?> eventMap)) {
            return ResponseEntity.badRequest().build();
        }

        Object appUserIdObj = eventMap.get("app_user_id");
        Object eventTypeObj = eventMap.get("type");

        if (appUserIdObj == null || eventTypeObj == null) {
            return ResponseEntity.badRequest().build();
        }

        UUID userId;
        try {
            userId = UUID.fromString(appUserIdObj.toString());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        try {
            userService.handleRevenueCatEvent(userId, eventTypeObj.toString());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).build();
        }

        return ResponseEntity.ok().build();
    }
}
