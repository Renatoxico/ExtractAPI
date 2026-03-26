package com.example.api.service;

import com.example.api.model.User;
import com.example.api.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // --- findOrCreate ---

    @Test
    void shouldReturnExistingUser() {
        UUID id = UUID.randomUUID();
        User existing = new User(id, "existing@email.com");
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));

        User result = userService.findOrCreate(id, "existing@email.com");

        assertThat(result).isEqualTo(existing);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldCreateNewUserWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.findOrCreate(id, "new@email.com");

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getEmail()).isEqualTo("new@email.com");
    }

    @Test
    void shouldUpdateEmailWhenExistingUserHasNoEmail() {
        UUID id = UUID.randomUUID();
        User existing = new User(id, null);
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.findOrCreate(id, "new@email.com");

        assertThat(result.getEmail()).isEqualTo("new@email.com");
        verify(userRepository).save(existing);
    }

    @Test
    void shouldNotOverwriteExistingEmail() {
        UUID id = UUID.randomUUID();
        User existing = new User(id, "original@email.com");
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));

        User result = userService.findOrCreate(id, "different@email.com");

        assertThat(result.getEmail()).isEqualTo("original@email.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldNotUpdateEmailWhenNewEmailIsNull() {
        UUID id = UUID.randomUUID();
        User existing = new User(id, null);
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));

        User result = userService.findOrCreate(id, null);

        assertThat(result.getEmail()).isNull();
        verify(userRepository, never()).save(any());
    }

    // --- handleRevenueCatEvent ---

    @Test
    void shouldSetPremiumOnInitialPurchase() {
        UUID id = UUID.randomUUID();
        User user = new User(id, null);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.handleRevenueCatEvent(id, "INITIAL_PURCHASE");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isPremium()).isTrue();
    }

    @Test
    void shouldSetPremiumOnRenewal() {
        UUID id = UUID.randomUUID();
        User user = new User(id, null);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.handleRevenueCatEvent(id, "RENEWAL");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isPremium()).isTrue();
    }

    @Test
    void shouldRevokePremiumOnCancellation() {
        UUID id = UUID.randomUUID();
        User user = new User(id, null);
        user.setPremium(true);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.handleRevenueCatEvent(id, "CANCELLATION");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isPremium()).isFalse();
    }

    @Test
    void shouldRevokePremiumOnExpiration() {
        UUID id = UUID.randomUUID();
        User user = new User(id, null);
        user.setPremium(true);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.handleRevenueCatEvent(id, "EXPIRATION");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isPremium()).isFalse();
    }

    @Test
    void shouldRevokePremiumOnBillingIssue() {
        UUID id = UUID.randomUUID();
        User user = new User(id, null);
        user.setPremium(true);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.handleRevenueCatEvent(id, "BILLING_ISSUE");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().isPremium()).isFalse();
    }

    @Test
    void shouldNotSaveOnUnknownEventType() {
        UUID id = UUID.randomUUID();
        User user = new User(id, null);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        userService.handleRevenueCatEvent(id, "UNKNOWN_EVENT");

        // save is called once during findById orElseGet — but since user exists, no save
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldCreateUserWhenNotFoundDuringWebhookEvent() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.handleRevenueCatEvent(id, "INITIAL_PURCHASE");

        // save called twice: once for creation, once for premium update
        verify(userRepository, times(2)).save(any());
    }
}
