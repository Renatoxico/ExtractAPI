package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.config.AiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AiProcessorService {
    private static final Logger LOG = LoggerFactory.getLogger(AiProcessorService.class);
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String OLLAMA_MODEL = "gemma3:4b";

    private final AiCategoryResponseParser categoryResponseParser;
    private final AiProperties properties;
    private final RestTemplate localLlmClient;

    public AiProcessorService(
        AiCategoryResponseParser categoryResponseParser,
        AiProperties properties
    ) {
        this.categoryResponseParser = categoryResponseParser;
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getDefaultTimeout());
        requestFactory.setReadTimeout(properties.getDefaultTimeout());
        this.localLlmClient = new RestTemplate(requestFactory);
    }

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

    public AiResponse processWithGemini(List<RequestItem> expenses) {
        String prompt = buildPrompt(expenses);
        try (Client client = Client.builder()
            .apiKey(properties.getApiKey())
            .httpOptions(HttpOptions.builder()
                .timeout(timeoutMillis(properties.getGeminiTimeout()))
                .build())
            .build()) {
            GenerateContentResponse response = client.models.generateContent(
                properties.getGeminiModel(),
                prompt,
                null
            );

            String rawResponse = response.text();
            if (rawResponse == null || rawResponse.isBlank()) {
                throw new ProcessingException(
                    "Gemini returned an empty response",
                    HttpStatus.BAD_GATEWAY,
                    "EMPTY_GEMINI_RESPONSE"
                );
            }
            LOG.info("Received response from Gemini for {} classification tasks", expenses.size());
            return parseResponse(rawResponse, expenses);
        } catch (Exception ex) {
            if (ex instanceof ProcessingException processingException) {
                throw processingException;
            }
            LOG.error("Error calling Gemini API: {}", ex.getMessage(), ex);
            throw new ProcessingException(
                "Error calling Gemini API",
                HttpStatus.BAD_GATEWAY,
                "GEMINI_API_ERROR",
                ex
            );
        }
    }

    public AiResponse processWithLocalLLM(List<RequestItem> expenses) {
        ObjectMapper objectMapper = new ObjectMapper();
        String prompt = buildPrompt(expenses);
        Map<String, Object> body = getLocalLlmRequestBody(prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            LOG.info("Calling local Ollama API for {} classification tasks", expenses.size());
            ResponseExtractor<String> responseExtractor = response -> {
                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) {
                            continue;
                        }
                        Map<?, ?> chunk = objectMapper.readValue(line, Map.class);
                        Object responseContent = chunk.get("response");
                        if (responseContent != null) {
                            content.append(responseContent);
                        }
                    }
                }
                return content.toString();
            };

            String rawResponse = localLlmClient.execute(OLLAMA_URL, HttpMethod.POST, request -> {
                request.getHeaders().putAll(requestEntity.getHeaders());
                objectMapper.writeValue(request.getBody(), requestEntity.getBody());
            }, responseExtractor);

            if (rawResponse == null || rawResponse.isBlank()) {
                throw new ProcessingException(
                    "Ollama returned an empty response",
                    HttpStatus.BAD_GATEWAY,
                    "EMPTY_OLLAMA_RESPONSE"
                );
            }
            LOG.info("Local Ollama API completed for {} classification tasks", expenses.size());
            return parseResponse(rawResponse, expenses);
        } catch (Exception exception) {
            if (exception instanceof ProcessingException processingException) {
                throw processingException;
            }
            LOG.error("Error calling local Ollama API: {}", exception.getMessage(), exception);
            throw new ProcessingException(
                "Error calling local Ollama API",
                HttpStatus.BAD_GATEWAY,
                "OLLAMA_API_ERROR",
                exception
            );
        }
    }

    String buildPrompt(List<RequestItem> expenses) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(getPrompt());

        for (RequestItem expense : expenses) {
            prompt.append(expense.taskId())
                .append('|')
                .append(expense.expenseName())
                .append('\n');
        }
        return prompt.toString();
    }

    Map<String, Object> getLocalLlmRequestBody(String prompt) {
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

    private AiResponse parseResponse(String rawResponse, List<RequestItem> expenses) {
        Set<Long> expectedTaskIds = expenses.stream()
            .map(RequestItem::taskId)
            .collect(Collectors.toSet());
        return new AiResponse(
            rawResponse,
            categoryResponseParser.parse(rawResponse, expectedTaskIds)
        );
    }

    private static int timeoutMillis(java.time.Duration timeout) {
        return Math.toIntExact(timeout.toMillis());
    }

    public record RequestItem(long taskId, String expenseName) {
    }

    public record AiResponse(
        String rawResponse,
        AiCategoryResponseParser.ParseResult parseResult
    ) {
    }
}
