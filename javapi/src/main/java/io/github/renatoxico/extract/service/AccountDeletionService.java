package io.github.renatoxico.extract.service;

import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.model.AuthenticatedUserPrincipal;
import io.github.renatoxico.extract.repo.AdminEmailOutboxRepository;
import io.github.renatoxico.extract.repo.AppUserRepository;
import io.github.renatoxico.extract.repo.ExpenseReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes one signed-in account.
 *
 * <p>Removed in the same transaction, before Firebase is contacted: the owner's
 * expense reports and their transactions (the expense rows cascade from the
 * report delete), the {@code app_user} row, and the email and display name of
 * that user inside admin weekly-report copies still stored in
 * {@code admin_email_outbox}. After that commit is ready, the Firebase refresh
 * tokens are revoked and the Firebase Auth user is deleted. A Firebase failure
 * rolls the database work back, so the API does not report success while the
 * account still exists.
 *
 * <p>Kept on purpose: the shared expense-classification catalog and its AI task
 * history (descriptions only, no account id), outbox rows themselves and any
 * message already sent to administrators, server logs, database backups, and
 * the legacy {@code users} table. That table has no foreign key and no stable
 * link to {@code app_user}, so deleting from it by email could hit the wrong
 * row. The numeric id may remain inside a redacted outbox copy.
 */
@Service
public class AccountDeletionService {
    private static final Logger LOG = LoggerFactory.getLogger(AccountDeletionService.class);

    private final ExpenseReportRepository expenseReportRepository;
    private final AppUserRepository appUserRepository;
    private final AdminEmailOutboxRepository adminEmailOutboxRepository;
    private final FirebaseAuth firebaseAuth;

    public AccountDeletionService(
        ExpenseReportRepository expenseReportRepository,
        AppUserRepository appUserRepository,
        AdminEmailOutboxRepository adminEmailOutboxRepository,
        FirebaseAuth firebaseAuth
    ) {
        this.expenseReportRepository = expenseReportRepository;
        this.appUserRepository = appUserRepository;
        this.adminEmailOutboxRepository = adminEmailOutboxRepository;
        this.firebaseAuth = firebaseAuth;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(AuthenticatedUserPrincipal principal) {
        if (principal == null || principal.localUserId() == null
            || principal.uid() == null || principal.uid().isBlank()) {
            throw new ProcessingException(
                "Authentication is required",
                HttpStatus.UNAUTHORIZED,
                "AUTH_TOKEN_MISSING"
            );
        }

        Long userId = principal.localUserId();
        expenseReportRepository.deleteByOwnerId(userId);
        adminEmailOutboxRepository.redactUser(userId);
        appUserRepository.findById(userId).ifPresent(appUserRepository::delete);
        appUserRepository.flush();
        deleteFirebaseIdentity(principal.uid());
        LOG.info("Deleted account for local user {}", userId);
    }

    private void deleteFirebaseIdentity(String uid) {
        try {
            firebaseAuth.revokeRefreshTokens(uid);
        } catch (FirebaseAuthException exception) {
            if (!isAlreadyAbsent(exception)) {
                throw deletionFailed(exception);
            }
        }

        try {
            firebaseAuth.deleteUser(uid);
        } catch (FirebaseAuthException exception) {
            if (!isAlreadyAbsent(exception)) {
                throw deletionFailed(exception);
            }
        }
    }

    private static boolean isAlreadyAbsent(FirebaseAuthException exception) {
        return exception.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND;
    }

    private static ProcessingException deletionFailed(FirebaseAuthException exception) {
        return new ProcessingException(
            "Failed to delete the authentication account",
            HttpStatus.BAD_GATEWAY,
            "ACCOUNT_DELETION_FAILED",
            exception
        );
    }
}
