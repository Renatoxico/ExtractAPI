package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.model.ExpenseCategoryAssignment;
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

        List<ExpenseCategoryAssignment> result = parser.parse(response);

        assertEquals(3, result.size());
        assertEquals("PAGAMENTO DE BOLETO ROCA ADMINISTRADORA DE IM", result.get(0).expenseName());
        assertEquals("Moradia / Contas", result.get(0).category());
        assertEquals("PIX ENVIADO Amazon Servicos de Varejo", result.get(1).expenseName());
        assertEquals("E-commerce / Compras online", result.get(1).category());
        assertEquals("IFD*JEFFERSON BORGES DE LIMA", result.get(2).expenseName());
        assertEquals("Restaurante / Lanches", result.get(2).category());
    }

    @Test
    void ignoresBlankMalformedAndUnknownCategoryLines() {
        String response = """

            Missing separator
            UNKNOWN|InvalidCategory
            COMPANY|SUBSIDIARY|Supermercado
            VALID EXPENSE|Supermercado
            """;

        List<ExpenseCategoryAssignment> result = parser.parse(response);

        assertEquals(1, result.size());
        assertEquals("VALID EXPENSE", result.getFirst().expenseName());
        assertEquals("Supermercado", result.getFirst().category());
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
        List<ExpenseCategoryAssignment> result = parser.parse("EXPENSE|" + category);

        assertEquals(1, result.size());
        assertEquals(category, result.getFirst().category());
    }

    @Test
    void returnsEmptyListForEmptyResponse() {
        assertTrue(parser.parse("").isEmpty());
    }
}
