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
    private static final String URL = "http://host.docker.internal:11434/api/generate";
    private static final String PROMPT_TEMPLATE = """
            Preciso que você categorize/classifique algumas despesas financeiras, utilize as categorias abaixo:
            Observação: o que esta entre parênteses é apenas para te ajudar a entender melhor a categoria, não deve ser incluído na resposta.
     
                1. Supermercado
                   (Inclui supermercados, mercearias, frios, hortifrúti, padarias, açougues, bebidas e conveniências.)
            
                2. Restaurante / Lanches
                   (Inclui restaurantes, bares, lanchonetes, pizzarias, hamburguerias, e apps de delivery como iFood, UberEats, Rappi etc.)
            
                3. Transporte / Auto
                   (Inclui postos de combustível, oficinas, auto centers e transportes por app (Uber, 99, Lyft, BlaBlaCar etc).)
            
                4. Lazer / Entretenimento / Pets
                   (Inclui pet shops, serviços de streaming (YouTube, Netflix, Patreon, Kick, Spotify), jogos, hobbies, eventos e lazer em geral.)
            
                5. Saúde / Farmácia / Bem-estar
                   (Inclui farmácias, clínicas, planos de saúde, academias e serviços de bem-estar.)
            
                6. Moradia / Contas / Serviços
                   (Inclui boletos, aluguel, condomínio, energia, água, gás, telefone, internet, limpeza, dedetização e taxas bancárias.)
            
                7. Investimentos / Assinaturas profissionais
                   (Inclui aplicações financeiras, hospedagem (HostGator), Canva, domínios, softwares e outras plataformas profissionais.)
            
                8. Roupas / Acessórios
                   (Inclui lojas de roupas, calçados, shopping, artigos esportivos e acessórios.)
            
                9. E-commerce / Compras online
                   (Inclui Amazon, Mercado Livre, AliExpress e outras lojas virtuais ou intermediadores de pagamento (PagSeguro, Nuvei etc).)
            
                10. Outros / Transferências
                    (Inclui PIX entre pessoas, saques, transferências entre contas, doações e tudo que não se encaixa claramente nas categorias acima.)
       
                Como retorno, por favor escreva exatamente o nome da despesa, seguido de um caractere pipe `|`, seguido pela categoria apropriada.
                Algumas despesas podem estar abreviadas ou faltando espaço, tente perceber e categorizar apropriadamente esses casos também.
                Por favor categorize cada uma das despesas, preciso de uma lista 1:1
       
                Exemplo do formato esperado:
       
                PAGAMENTO DE BOLETO ROCA ADMINISTRADORA DE IM|Moradia / Contas
                PIX ENVIADO Amazon Servicos de Varejo|E-commerce
                IFD*JEFFERSON BORGES DE LIMA|Restaurante / Lanches
       
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
