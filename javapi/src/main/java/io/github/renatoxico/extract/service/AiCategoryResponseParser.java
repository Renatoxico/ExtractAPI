package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.model.CategoryMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class AiCategoryResponseParser {

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

    public List<CategoryMapper> parse(String aiResponse) {
        List<CategoryMapper> categories = new ArrayList<>();

        for (String line : aiResponse.split("\\r?\\n")) {
            if (line.isBlank() || !line.contains("|")) {
                continue;
            }

            String[] parts = line.split("\\|");
            if (parts.length != 2) {
                continue;
            }

            String category = parts[1].trim();
            if (!VALID_CATEGORIES.contains(category)) {
                continue;
            }

            categories.add(new CategoryMapper(parts[0].trim(), category));
        }

        return categories;
    }
}
