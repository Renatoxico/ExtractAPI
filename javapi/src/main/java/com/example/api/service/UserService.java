package com.example.api.service;

import com.example.api.model.User;
import com.example.api.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User findOrCreate(UUID id, String email) {
        User user = userRepository.findById(id).orElseGet(() -> {
            User newUser = new User(id, email);
            return userRepository.save(newUser);
        });

        if (user.getEmail() == null && email != null) {
            user.setEmail(email);
            userRepository.save(user);
        }

        return user;
    }

    @Transactional
    public void handleRevenueCatEvent(UUID userId, String eventType) {
        User user = userRepository.findById(userId)
                .orElseGet(() -> userRepository.save(new User(userId, null)));

        switch (eventType) {
            case "INITIAL_PURCHASE", "RENEWAL" -> user.setPremium(true);
            case "CANCELLATION", "EXPIRATION", "BILLING_ISSUE" -> user.setPremium(false);
            default -> { return; }
        }

        userRepository.save(user);
    }
}
