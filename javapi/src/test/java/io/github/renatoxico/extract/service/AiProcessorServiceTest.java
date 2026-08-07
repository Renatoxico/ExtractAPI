package io.github.renatoxico.extract.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AiProcessorServiceTest {

    private final AiProcessorService service = new AiProcessorService(new AiCategoryResponseParser());

    @Test
    void buildsLocalLlmRequestBody() {
        String prompt = "Test prompt";

        Map<String, Object> body = service.getLocalLlmRequestBody(prompt);

        assertEquals("gemma3:4b", body.get("model"));
        assertEquals(prompt, body.get("prompt"));
        assertEquals(true, body.get("stream"));

        @SuppressWarnings("unchecked")
        Map<String, Object> options = (Map<String, Object>) body.get("options");
        assertNotNull(options);
        assertEquals(4096, options.get("num_ctx"));
        assertEquals(4096, options.get("num_predict"));
        assertEquals(0, options.get("temperature"));
    }
}
