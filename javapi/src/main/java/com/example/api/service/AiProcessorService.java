package com.example.api.service;

import com.example.api.model.CategoryMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiProcessorService {
    private static final Logger LOG = LoggerFactory.getLogger(AiProcessorService.class);
    private static final String URL = "http://192.168.15.9:11434/api/generate";
    private static final String PROMPT_TEMPLATE = """
            Preciso que você categorize/classifique cada uma delas em apenas uma das categorias a seguir:
     
                1. Supermercado
                2. Restaurante / Lanches
                3. Combustível / Transporte (Inclui coisas como Posto de abastecer, Uber, 99Taxi, Lyft blabla car e etc)
                4. Lazer / Entretenimento / Pets
                5. Saúde / Farmácia
                6. Moradia / Contas (Inclui boletos aqui, contas de luz/energia/agua/gas)
                7. Investimentos ( Inclui Aplicações financeiras)
                8. Roupas / Acessórios
                9. E-commerce (Coisas como compras on-line, Amazon, Mercado Livre, Aliexpress e etc)
                10. Outros / Diversos (Qualquer coisa que não tiver certza, usa essa classificação)
       
                Como retorno, por favor escreva exatamente o nome da despesa, seguido de um caractere pipe `|`, seguido pela categoria escolhida.
                Algumas despesas podem estar abreviadas ou faltando espaço, tente perceber e categorizar apropriadamente esses casos também.
                Por favor categorize cada uma das despesas, preciso de uma lista 1:1
       
                Exemplo do formato esperado:
       
                PAGAMENTO DE BOLETO ROCA ADMINISTRADORA DE IM|Supermercado / Alimentação
                PIX ENVIADO Companhia Paulista de For|Combustível / Transporte
                PIX ENVIADO Amazon Servicos de Varejo|E-commerce
       
                Despesas:
            """;

    public List<CategoryMapper> processWithAI(List<CategoryMapper> expenses) {
        RestTemplate restTemplate = new RestTemplate();
        StringBuilder prompt = new StringBuilder();
        prompt.append(PROMPT_TEMPLATE);

        for(CategoryMapper expense : expenses){
            prompt.append(expense.getExpenseName()).append(" | \n");
        }

        Map<String, Object> body = getRequestBody(prompt.toString());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        try {
            LOG.info("Calling LLM API");
            ResponseExtractor<String> extractor = response -> {
                StringBuilder resp = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                response.getBody(), StandardCharsets.UTF_8)
                        )
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
        body.put("model", "gemma3:4b");
        body.put("prompt", prompt);
        body.put("stream", true);
        body.put("options", options);
        return body;
    }

    private List<CategoryMapper> mapCategories(String aiResponse){
        List<CategoryMapper> res = new ArrayList<>();
        String[] lines = aiResponse.split("\\r?\\n");
        for(String line : lines){
            if(line.isBlank() || !line.contains("|")) continue;
            String[] parts = line.split("\\|");
            if(parts.length != 2) continue;
            res.add(new CategoryMapper(parts[0].trim(), parts[1].trim()));
        }
        return res;
    }
}
