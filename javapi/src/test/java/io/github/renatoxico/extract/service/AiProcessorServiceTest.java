package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.config.AiProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiProcessorServiceTest {

    private final AiProcessorService service = new AiProcessorService(
        new AiCategoryResponseParser(),
        testProperties()
    );

    @Test
    void buildsPromptWithStableTaskIds() {
        String prompt = service.buildPrompt(List.of(
            new AiProcessorService.RequestItem(41L, "PADARIA CENTRAL"),
            new AiProcessorService.RequestItem(42L, "MERCADO LOCAL")
        ));

        assertThat(prompt).contains("41|PADARIA CENTRAL");
        assertThat(prompt).contains("42|MERCADO LOCAL");
        assertThat(prompt).contains("identificador numérico");
    }

    @Test
    void buildsLocalOllamaRequestBody() {
        Map<String, Object> body = service.getLocalLlmRequestBody("Test prompt");

        assertThat(body)
            .containsEntry("model", "gemma3:4b")
            .containsEntry("prompt", "Test prompt")
            .containsEntry("stream", true);
        assertThat(body.get("options")).isEqualTo(Map.of(
            "num_ctx", 4096,
            "num_predict", 4096,
            "temperature", 0
        ));
    }

    private static AiProperties testProperties() {
        AiProperties properties = new AiProperties();
        properties.setApiKey("test-gemini-api-key-1234567890123456");
        return properties;
    }
}
