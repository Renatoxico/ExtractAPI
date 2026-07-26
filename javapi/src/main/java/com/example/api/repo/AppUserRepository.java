package com.example.api.repo;

import com.example.api.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByFirebaseUid(String firebaseUid);

    @Modifying
    @Query(value = """
        INSERT INTO app_user (firebase_uid, email, display_name, picture_url)
        VALUES (:firebaseUid, :email, :displayName, :pictureUrl)
        ON CONFLICT (firebase_uid) DO UPDATE SET
            email = EXCLUDED.email,
            display_name = EXCLUDED.display_name,
            picture_url = EXCLUDED.picture_url
        """, nativeQuery = true)
    void upsert(
        @Param("firebaseUid") String firebaseUid,
        @Param("email") String email,
        @Param("displayName") String displayName,
        @Param("pictureUrl") String pictureUrl
    );
}
