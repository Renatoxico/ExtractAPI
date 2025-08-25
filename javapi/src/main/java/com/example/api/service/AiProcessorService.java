package com.example.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class AiProcessorService {
    private static final Logger LOG = LoggerFactory.getLogger(AiProcessorService.class);
    private static final String URL = "http://192.168.15.9:11434/v1/completions";

    private static final String PROMPT_TEMPLATE = """
            Usando apenas estas categorias:
            - iFood / Delivery
            - Streaming
            - E-commerce
            - Supermercado / Alimentação
            - Combustível / Transporte
            - Farmácia / Saúde
            - Cartão de crédito / Pagamentos bancários
            - Lazer / Entretenimento
            - Assinaturas / Softwares
            - Viagens / Hospedagem
            - Serviços / Contas (internet, energia, telefone)
            - Educação / Cursos / Livros
            - Roupas / Vestuário
            - Outros / Diversos
            Preencha o JSON abaixo no campo "categoria" onde está vazio. \s
            Não invente categorias; se não souber, use "Outros / Diversos". \s
            O output deve ser apenas JSON, no mesmo formato do input, pronto para uso.
        %s
        """;

    public String processWithAI(String pdfText) {
        RestTemplate restTemplate = new RestTemplate();
        //setting prompt
        String prompt = String.format(PROMPT_TEMPLATE, pdfText);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "mistral");
        body.put("prompt", prompt);
        body.put("temperature", 0.0);
        body.put("max_tokens",2000);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> res = restTemplate.exchange(URL, HttpMethod.POST, req, Map.class);
            if(res.getStatusCode() == HttpStatus.OK && res.getBody() != null){
                ArrayList<Object> llmResponse;
                llmResponse = (ArrayList<Object>) res.getBody().get("choices");
                if (llmResponse != null){
                    LOG.info(llmResponse.get(0).toString());
                    return llmResponse.get(0).toString();
                }
            }
        } catch (Exception e) {
            LOG.error("error calling LLM", e);
        }

        return "[]"; // fallback empty JSON array
    }
}
