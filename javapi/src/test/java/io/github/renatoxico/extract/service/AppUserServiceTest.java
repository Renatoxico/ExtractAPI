package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.model.AppUser;
import io.github.renatoxico.extract.repo.AppUserRepository;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {
    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private AppUserService appUserService;

    @Test
    void synchronizeUpsertsAndReturnsLocalUser() {
        FirebaseToken firebaseToken = mock(FirebaseToken.class);
        AppUser appUser = mock(AppUser.class);
        when(firebaseToken.getUid()).thenReturn("firebase-uid-123");
        when(firebaseToken.getEmail()).thenReturn("user@example.com");
        when(firebaseToken.getName()).thenReturn("Example User");
        when(firebaseToken.getPicture()).thenReturn("https://example.com/photo.jpg");
        when(appUserRepository.findByFirebaseUid("firebase-uid-123")).thenReturn(Optional.of(appUser));

        AppUser result = appUserService.synchronize(firebaseToken);

        assertSame(appUser, result);
        verify(appUserRepository).upsert(
            "firebase-uid-123",
            "user@example.com",
            "Example User",
            "https://example.com/photo.jpg"
        );
    }
}
