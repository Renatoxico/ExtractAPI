package io.github.renatoxico.extract.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Component
@Validated
@ConfigurationProperties(prefix = "admin.email")
public class AdminEmailProperties {
    @NotBlank
    @Email
    private String sender;
    @NotEmpty
    @Valid
    private List<@NotBlank @Email String> recipients = new ArrayList<>();
    @NotBlank
    @Pattern(
        regexp = "^(?!replace-with-a-long-random-secret$).{32,}$",
        message = "must contain at least 32 characters and must not use the example placeholder"
    )
    private String apiKey;

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public List<String> getRecipients() {
        return List.copyOf(recipients);
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients == null ? new ArrayList<>() : new ArrayList<>(recipients);
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
