package com.example.api.service;

import com.example.api.model.AppUser;
import com.example.api.repo.AppUserRepository;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserService {
    private final AppUserRepository appUserRepository;

    public AppUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public AppUser synchronize(FirebaseToken firebaseToken) {
        appUserRepository.upsert(
            firebaseToken.getUid(),
            firebaseToken.getEmail(),
            firebaseToken.getName(),
            firebaseToken.getPicture()
        );

        return appUserRepository.findByFirebaseUid(firebaseToken.getUid())
            .orElseThrow(() -> new IllegalStateException("Synchronized app user was not found"));
    }
}
