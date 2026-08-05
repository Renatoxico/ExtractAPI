package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.model.CategoryMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AiProcessorService {
    private static final Logger LOG = LoggerFactory.getLogger(AiProcessorService.class);
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String OLLAMA_MODEL = "gemma3:4b";

    @Value("${api.key}")
    private String API_KEY;


    public String getPrompt() {
        try {
            InputStream promptStream = this.getClass().getResourceAsStream("/cat-prompt.txt");
            return new String( promptStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            LOG.error("Error loading prompt template: {}", ex.getMessage(), ex);
            throw new ProcessingException(
                    "Error loading prompt template",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "PROMPT_LOAD_ERROR",
                    ex
            );
        }
    }

    private static final Set<String> VALID_CATEGORIES = Set.of(
            "Roupas / Acessórios",
            "E-commerce / Compras online",
            "Restaurante / Lanches",
            "Investimentos / Assinaturas profissionais",
            "Saúde / Farmácia / Bem-estar",
            "Transporte / Auto",
            "Lazer / Entretenimento / Pets",
            "Supermercado",
            "Outros / Transferências",
            "Moradia / Contas"
    );

    public List<CategoryMapper> processWithGemini(List<CategoryMapper> expenses) {
        String prompt = buildPrompt(expenses);
        try {
            Client client = Client.builder().apiKey(API_KEY).build();

            GenerateContentResponse response = client.models.generateContent(
            "gemini-3-flash-preview",
            prompt,
            null);

            String aiResponse = response.text();
            if (aiResponse != null && !aiResponse.isBlank()) {
                LOG.info("Received response from Gemini");
                return mapCategories(aiResponse);
            }
        } catch (Exception ex) {
            LOG.error("Error calling Gemini API: {}", ex.getMessage(), ex);
                throw new ProcessingException(
                        "Error calling Gemini API",
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "GEMINI_API_ERROR",
                        ex
                );

        }
        return expenses;
    }

    private String buildPrompt(List<CategoryMapper> expenses) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(getPrompt());

        for(CategoryMapper expense : expenses){
            prompt.append(expense.getExpenseName()).append(" | \n");
        }
        return prompt.toString();
    }

    public List<CategoryMapper> processWithLocalLLM(List<CategoryMapper> expenses) {
        RestTemplate restTemplate = new RestTemplate();
        String prompt = buildPrompt(expenses);
        Map<String, Object> body = getLocalLlmRequestBody(prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            LOG.info("Calling local Ollama API");
            ResponseExtractor<String> responseExtractor = response -> {
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) {
                            continue;
                        }
                        Map<?, ?> chunk = new ObjectMapper().readValue(line, Map.class);
                        Object responseContent = chunk.get("response");
                        if (responseContent != null) {
                            content.append(responseContent);
                        }
                    }
                }
                return content.toString();
            };

            String response = restTemplate.execute(OLLAMA_URL, HttpMethod.POST, request -> {
                request.getHeaders().putAll(requestEntity.getHeaders());
                new ObjectMapper().writeValue(request.getBody(), requestEntity.getBody());
            }, responseExtractor);

            LOG.info("Local Ollama API call completed");
            if (response != null) {
                return mapCategories(response);
            }
        } catch (Exception ex) {
            LOG.error("Error calling local Ollama API", ex);
        }
        return expenses;
    }

    private Map<String, Object> getLocalLlmRequestBody(String prompt) {
        Map<String, Object> options = new HashMap<>();
        options.put("num_ctx", 4096);
        options.put("num_predict", 4096);
        options.put("temperature", 0);

        Map<String, Object> body = new HashMap<>();
        body.put("model", OLLAMA_MODEL);
        body.put("prompt", prompt);
        body.put("stream", true);
        body.put("options", options);
        return body;
    }

    private List<CategoryMapper> mapCategories(String aiResponse){
        List<CategoryMapper> res = new ArrayList<>();
        String[] lines = aiResponse.split("\\r?\\n");
        for(String line : lines){
            if(line.isBlank() || !line.contains("|"))
                continue;

            String[] parts = line.split("\\|");

            if(parts.length != 2)
                continue;

            if(!VALID_CATEGORIES.contains(parts[1].trim()))
                continue;

            res.add(new CategoryMapper(parts[0].trim(), parts[1].trim()));
        }
        return res;
    }
}
