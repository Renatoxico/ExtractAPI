package io.github.renatoxico.extract.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationPropertiesValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsReleaseTimeoutsAndAdminConfiguration() {
        AiProperties ai = new AiProperties();
        ai.setApiKey("test-gemini-api-key-1234567890123456");
        ai.setGeminiTimeout(Duration.ofMinutes(3));
        ai.setDefaultTimeout(Duration.ofSeconds(90));

        AdminEmailProperties admin = validAdminProperties();

        assertThat(validator.validate(ai)).isEmpty();
        assertThat(validator.validate(admin)).isEmpty();
    }

    @Test
    void rejectsUnsafeAdminApiKeys() {
        AdminEmailProperties placeholder = validAdminProperties();
        placeholder.setApiKey("replace-with-a-long-random-secret");
        AdminEmailProperties shortKey = validAdminProperties();
        shortKey.setApiKey("too-short");

        assertThat(validator.validate(placeholder)).isNotEmpty();
        assertThat(validator.validate(shortKey)).isNotEmpty();
    }

    @Test
    void rejectsGeminiPlaceholderKey() {
        AiProperties ai = new AiProperties();
        ai.setApiKey("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

        assertThat(validator.validate(ai)).isNotEmpty();
    }

    @Test
    void rejectsNonPositiveExternalTimeouts() {
        AiProperties ai = new AiProperties();
        ai.setApiKey("test-gemini-api-key-1234567890123456");
        ai.setGeminiTimeout(Duration.ZERO);
        ai.setDefaultTimeout(Duration.ofSeconds(-1));

        assertThat(validator.validate(ai))
            .anyMatch(violation -> violation.getPropertyPath().toString()
                .equals("timeoutConfigurationValid"));
    }

    private AdminEmailProperties validAdminProperties() {
        AdminEmailProperties properties = new AdminEmailProperties();
        properties.setSender("sender@example.com");
        properties.setRecipients(List.of("admin@example.com"));
        properties.setApiKey("test-admin-email-api-key-1234567890");
        return properties;
    }
}
