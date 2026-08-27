package io.github.renatoxico.extract.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@Validated
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    @NotBlank
    @Pattern(
        regexp = "^(?!X+$).{32,}$",
        message = "must contain at least 32 characters and must not use the example placeholder"
    )
    private String apiKey;
    @NotBlank
    private String geminiModel = "gemini-3-flash-preview";
    @NotNull
    private Duration geminiTimeout = Duration.ofMinutes(3);
    @NotNull
    private Duration defaultTimeout = Duration.ofSeconds(90);

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getGeminiModel() {
        return geminiModel;
    }

    public void setGeminiModel(String geminiModel) {
        this.geminiModel = geminiModel;
    }

    public Duration getGeminiTimeout() {
        return geminiTimeout;
    }

    public void setGeminiTimeout(Duration geminiTimeout) {
        this.geminiTimeout = geminiTimeout;
    }

    public Duration getDefaultTimeout() {
        return defaultTimeout;
    }

    public void setDefaultTimeout(Duration defaultTimeout) {
        this.defaultTimeout = defaultTimeout;
    }

    @AssertTrue(message = "AI timeouts must be positive and fit the SDK millisecond range")
    public boolean isTimeoutConfigurationValid() {
        return validTimeout(geminiTimeout) && validTimeout(defaultTimeout);
    }

    private boolean validTimeout(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return false;
        }
        try {
            return timeout.toMillis() <= Integer.MAX_VALUE;
        } catch (ArithmeticException exception) {
            return false;
        }
    }
}
