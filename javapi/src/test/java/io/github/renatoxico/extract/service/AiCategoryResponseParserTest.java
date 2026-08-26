package io.github.renatoxico.extract.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiCategoryResponseParserTest {

    private final AiCategoryResponseParser parser = new AiCategoryResponseParser();

    @Test
    void parsesValidLinesAndTrimsValues() {
        String response = """
            101|Moradia / Contas
              102  |  E-commerce / Compras online
            103|Restaurante / Lanches
            """;

        AiCategoryResponseParser.ParseResult result = parser.parse(response, Set.of(101L, 102L, 103L));

        assertEquals(3, result.acceptedItems().size());
        assertEquals(101L, result.acceptedItems().get(0).taskId());
        assertEquals("Moradia / Contas", result.acceptedItems().get(0).category());
        assertEquals(102L, result.acceptedItems().get(1).taskId());
        assertEquals("E-commerce / Compras online", result.acceptedItems().get(1).category());
        assertEquals(103L, result.acceptedItems().get(2).taskId());
        assertEquals("Restaurante / Lanches", result.acceptedItems().get(2).category());
    }

    @Test
    void ignoresBlankMalformedAndUnknownCategoryLines() {
        String response = """

            Missing separator
            abc|Supermercado
            999|Supermercado
            101|InvalidCategory
            102|Supermercado
            102|Moradia / Contas
            """;

        AiCategoryResponseParser.ParseResult result = parser.parse(response, Set.of(101L, 102L));

        assertTrue(result.acceptedItems().isEmpty());
        assertEquals(5, result.diagnostics().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
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
    })
    void acceptsEverySupportedCategory(String category) {
        AiCategoryResponseParser.ParseResult result = parser.parse("1|" + category, Set.of(1L));

        assertEquals(1, result.acceptedItems().size());
        assertEquals(category, result.acceptedItems().getFirst().category());
    }

    @Test
    void returnsEmptyListForEmptyResponse() {
        assertTrue(parser.parse("", Set.of(1L)).acceptedItems().isEmpty());
    }
}
