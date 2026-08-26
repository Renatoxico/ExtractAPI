package io.github.renatoxico.extract.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    public ParseResult parse(String aiResponse, Set<Long> expectedTaskIds) {
        List<String> diagnostics = new ArrayList<>();
        Map<Long, String> candidates = new HashMap<>();
        Set<Long> duplicateIds = new HashSet<>();

        for (String line : aiResponse.split("\\r?\\n")) {
            if (line.isBlank()) {
                continue;
            }

            String[] parts = line.split("\\|", -1);
            if (parts.length != 2) {
                diagnostics.add("Malformed response line ignored");
                continue;
            }

            long taskId;
            try {
                taskId = Long.parseLong(parts[0].trim());
            } catch (NumberFormatException exception) {
                diagnostics.add("Response line had a non-numeric task id");
                continue;
            }

            if (!expectedTaskIds.contains(taskId)) {
                diagnostics.add("Response contained an unexpected task id");
                continue;
            }

            String category = parts[1].trim();
            if (!VALID_CATEGORIES.contains(category)) {
                diagnostics.add("Response contained an unsupported category for task " + taskId);
                continue;
            }

            if (candidates.putIfAbsent(taskId, category) != null) {
                duplicateIds.add(taskId);
            }
        }

        duplicateIds.forEach(candidates::remove);
        duplicateIds.forEach(taskId ->
            diagnostics.add("Response contained duplicate values for task " + taskId));

        List<ParsedItem> accepted = candidates.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> new ParsedItem(entry.getKey(), entry.getValue()))
            .toList();
        return new ParseResult(accepted, List.copyOf(diagnostics));
    }

    public record ParsedItem(long taskId, String category) {
    }

    public record ParseResult(List<ParsedItem> acceptedItems, List<String> diagnostics) {
    }
}
