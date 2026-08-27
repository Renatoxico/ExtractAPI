package io.github.renatoxico.extract.controller;

import io.github.renatoxico.extract.model.ErrorResponse;
import io.github.renatoxico.extract.config.AdminEmailProperties;
import io.github.renatoxico.extract.service.AdminEmailService;
import io.github.renatoxico.extract.service.AdminEmailService.ResendResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/admin/email-notifications")
public class AdminEmailController {
    private static final String ADMIN_KEY_HEADER = "X-Admin-API-Key";
    private final AdminEmailService adminEmailService;
    private final AdminEmailProperties properties;

    public AdminEmailController(
        AdminEmailService adminEmailService,
        AdminEmailProperties properties
    ) {
        this.adminEmailService = adminEmailService;
        this.properties = properties;
    }

    @PostMapping("/{notificationId}/resend")
    public ResponseEntity<?> resend(
        @PathVariable long notificationId,
        @RequestHeader(value = ADMIN_KEY_HEADER, required = false) String suppliedKey
    ) {
        if (!validAdminKey(suppliedKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new ErrorResponse("ADMIN_API_KEY_INVALID", "A valid admin API key is required")
            );
        }
        try {
            ResendResult result = adminEmailService.resend(notificationId);
            return ResponseEntity.accepted().body(
                new ResendResponse(result.notificationId(), result.action())
            );
        } catch (NoSuchElementException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponse("EMAIL_NOTIFICATION_NOT_FOUND", exception.getMessage())
            );
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse("EMAIL_NOTIFICATION_BUSY", exception.getMessage())
            );
        }
    }

    private boolean validAdminKey(String suppliedKey) {
        return suppliedKey != null && MessageDigest.isEqual(
            suppliedKey.getBytes(StandardCharsets.UTF_8),
            properties.getApiKey().getBytes(StandardCharsets.UTF_8)
        );
    }

    public record ResendResponse(long notificationId, String action) {
    }
}
