package io.github.renatoxico.extract.controller;

import io.github.renatoxico.extract.model.AuthenticatedUserPrincipal;
import io.github.renatoxico.extract.service.AccountDeletionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v2/account")
public class AccountController {
    private final AccountDeletionService accountDeletionService;

    public AccountController(AccountDeletionService accountDeletionService) {
        this.accountDeletionService = accountDeletionService;
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(
        @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        accountDeletionService.deleteAccount(principal);
        return ResponseEntity.noContent().build();
    }
}
