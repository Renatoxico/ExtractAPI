package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.model.CategoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiCategoryResponseParserTest {

    private final AiCategoryResponseParser parser = new AiCategoryResponseParser();

    @Test
    void parsesValidLinesAndTrimsValues() {
        String response = """
            PAGAMENTO DE BOLETO ROCA ADMINISTRADORA DE IM|Moradia / Contas
              PIX ENVIADO Amazon Servicos de Varejo  |  E-commerce / Compras online
            IFD*JEFFERSON BORGES DE LIMA|Restaurante / Lanches
            """;

        List<CategoryMapper> result = parser.parse(response);

        assertEquals(3, result.size());
        assertEquals("PAGAMENTO DE BOLETO ROCA ADMINISTRADORA DE IM", result.get(0).getExpenseName());
        assertEquals("Moradia / Contas", result.get(0).getTransactionType());
        assertEquals("PIX ENVIADO Amazon Servicos de Varejo", result.get(1).getExpenseName());
        assertEquals("E-commerce / Compras online", result.get(1).getTransactionType());
        assertEquals("IFD*JEFFERSON BORGES DE LIMA", result.get(2).getExpenseName());
        assertEquals("Restaurante / Lanches", result.get(2).getTransactionType());
    }

    @Test
    void ignoresBlankMalformedAndUnknownCategoryLines() {
        String response = """

            Missing separator
            UNKNOWN|InvalidCategory
            COMPANY|SUBSIDIARY|Supermercado
            VALID EXPENSE|Supermercado
            """;

        List<CategoryMapper> result = parser.parse(response);

        assertEquals(1, result.size());
        assertEquals("VALID EXPENSE", result.getFirst().getExpenseName());
        assertEquals("Supermercado", result.getFirst().getTransactionType());
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
        List<CategoryMapper> result = parser.parse("EXPENSE|" + category);

        assertEquals(1, result.size());
        assertEquals(category, result.getFirst().getTransactionType());
    }

    @Test
    void returnsEmptyListForEmptyResponse() {
        assertTrue(parser.parse("").isEmpty());
    }
}
