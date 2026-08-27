package io.github.renatoxico.extract.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.io.IOException;

@Configuration
public class FirebaseAdminConfig {

    @Bean
    @Lazy
    public FirebaseApp firebaseApp(AiProperties properties) throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            int timeoutMillis = Math.toIntExact(properties.getDefaultTimeout().toMillis());
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .setConnectTimeout(timeoutMillis)
                .setReadTimeout(timeoutMillis)
                .setWriteTimeout(timeoutMillis)
                .build();

            return FirebaseApp.initializeApp(options);
        }

        return FirebaseApp.getInstance();
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }
}
