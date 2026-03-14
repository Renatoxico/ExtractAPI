package com.example.api.service;

import com.example.api.exception.ProcessingException;
import com.example.api.model.CategoryMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AiProcessorService {
    private static final Logger LOG = LoggerFactory.getLogger(AiProcessorService.class);
    private static final String URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "gemma3:4b";

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

            if (response.text() != null) {
                String aiResponse = response.text();
                if (aiResponse != null) {
                    LOG.info("Received response from Gemini");
                    return mapCategories(aiResponse);
                }
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

    public List<CategoryMapper> processWithAI(List<CategoryMapper> expenses) {
        RestTemplate restTemplate = new RestTemplate();
        String prompt = buildPrompt(expenses);

        Map<String, Object> body = getRequestBody(prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        try {
            LOG.info("Calling LLM API");
            ResponseExtractor<String> extractor = response -> {
                StringBuilder resp = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))
                ) {
                    String line;
                    while((line = reader.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        Map<?, ?> chunk = new ObjectMapper().readValue(line, Map.class);
                        Object jsonResp = chunk.get("response");
                        if (jsonResp != null)
                            resp.append(jsonResp.toString());
                    }
                    return resp.toString();
                }
            };

            String fullResponse = restTemplate.execute(URL, HttpMethod.POST, request -> {
                request.getHeaders().putAll(req.getHeaders());
                new ObjectMapper().writeValue(request.getBody(), req.getBody());
            }, extractor );
            LOG.info("LLM API call completed");
            //LOG.info("Full response: {}", fullResponse);
            if (fullResponse != null) {
                return mapCategories(fullResponse);
            }
        } catch (Exception e) {
            LOG.error("error calling LLM", e);
        }
        return expenses; // fallback empty JSON array
    }

    private Map<String, Object> getRequestBody(String prompt){
        Map<String, Object> options = new HashMap<>();
        options.put("num_ctx", 4096);
        options.put("num_predict", 4096);
        options.put("temperature", 0);

        Map<String, Object> body = new HashMap<>();
        body.put("model", MODEL);
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
                continue;//invalid line -> skip

            String[] parts = line.split("\\|");

            if(parts.length != 2)
                continue;//invalid format -> skip

            if(!VALID_CATEGORIES.contains(parts[1].trim()))
                continue;//invalid category -> skip

            res.add(new CategoryMapper(parts[0].trim(), parts[1].trim()));
        }
        return res;
    }
}
