package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.model.CategoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AiProcessorService Tests")
class AiProcessorServiceTest {

    private AiProcessorService aiProcessorService;

    @BeforeEach
    void setUp() {
        aiProcessorService = new AiProcessorService();
    }

    @Test
    @DisplayName("Should map categories correctly from valid AI response")
    void testMapCategoriesWithValidResponse() {
        String aiResponse = """
                PAGAMENTO DE BOLETO ROCA ADMINISTRADORA DE IM|Moradia / Contas
                PIX ENVIADO Amazon Servicos de Varejo|E-commerce / Compras online
                IFD*JEFFERSON BORGES DE LIMA|Restaurante / Lanches
                """;

        List<CategoryMapper> result = callMapCategories(aiResponse);

        assertEquals(3, result.size());
        assertEquals("PAGAMENTO DE BOLETO ROCA ADMINISTRADORA DE IM", result.get(0).getExpenseName());
        assertEquals("Moradia / Contas", result.get(0).getTransactionType());
        assertEquals("PIX ENVIADO Amazon Servicos de Varejo", result.get(1).getExpenseName());
        assertEquals("E-commerce / Compras online", result.get(1).getTransactionType());
        assertEquals("IFD*JEFFERSON BORGES DE LIMA", result.get(2).getExpenseName());
        assertEquals("Restaurante / Lanches", result.get(2).getTransactionType());
    }

    @Test
    @DisplayName("Should ignore lines without pipe character")
    void testMapCategoriesIgnoresInvalidFormat() {
        String aiResponse = """
                VALID LINE|Supermercado
                This line has no pipe
                ANOTHER VALID|Restaurante / Lanches
                """;

        List<CategoryMapper> result = callMapCategories(aiResponse);

        assertEquals(2, result.size());
        assertEquals("VALID LINE", result.get(0).getExpenseName());
        assertEquals("ANOTHER VALID", result.get(1).getExpenseName());
    }

    @Test
    @DisplayName("Should ignore lines with invalid categories")
    void testMapCategoriesIgnoresInvalidCategories() {
        String aiResponse = """
                VALID EXPENSE|Supermercado
                INVALID CATEGORY|InvalidCategory
                ANOTHER VALID|Lazer / Entretenimento / Pets
                """;

        List<CategoryMapper> result = callMapCategories(aiResponse);

        assertEquals(2, result.size());
        assertEquals("VALID EXPENSE", result.get(0).getExpenseName());
        assertEquals("ANOTHER VALID", result.get(1).getExpenseName());
    }

    @Test
    @DisplayName("Should handle empty lines in AI response")
    void testMapCategoriesHandlesEmptyLines() {
        String aiResponse = """
                EXPENSE ONE|Supermercado
                
                
                EXPENSE TWO|Restaurante / Lanches
                """;

        List<CategoryMapper> result = callMapCategories(aiResponse);

        assertEquals(2, result.size());
        assertEquals("EXPENSE ONE", result.get(0).getExpenseName());
        assertEquals("EXPENSE TWO", result.get(1).getExpenseName());
    }

    @Test
    @DisplayName("Should trim whitespace from expense names and categories")
    void testMapCategoriesTrimmsWhitespace() {
        String aiResponse = """
                EXPENSE WITH SPACES   |   Supermercado
                """;

        List<CategoryMapper> result = callMapCategories(aiResponse);

        assertEquals(1, result.size());
        assertEquals("EXPENSE WITH SPACES", result.getFirst().getExpenseName());
        assertEquals("Supermercado", result.getFirst().getTransactionType());
    }

    @Test
    @DisplayName("Should return empty list for empty response")
    void testMapCategoriesEmptyResponse() {
        String aiResponse = "";

        List<CategoryMapper> result = callMapCategories(aiResponse);

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should return empty list when response has only invalid lines")
    void testMapCategoriesOnlyInvalidLines() {
        String aiResponse = """
                Invalid format without pipe
                Another invalid line
                
                """;

        List<CategoryMapper> result = callMapCategories(aiResponse);

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should handle all valid categories")
    void testMapCategoriesAllValidCategories() {
        String aiResponse = """
                Expense1|Roupas / Acessórios
                Expense2|E-commerce / Compras online
                Expense3|Restaurante / Lanches
                Expense4|Investimentos / Assinaturas profissionais
                Expense5|Saúde / Farmácia / Bem-estar
                Expense6|Transporte / Auto
                Expense7|Lazer / Entretenimento / Pets
                Expense8|Supermercado
                Expense9|Outros / Transferências
                Expense10|Moradia / Contas
                """;

        List<CategoryMapper> result = callMapCategories(aiResponse);

        assertEquals(10, result.size());
        for (int i = 0; i < 10; i++) {
            assertNotNull(result.get(i).getTransactionType());
            assertFalse(result.get(i).getTransactionType().isBlank());
        }
    }

    @Test
    @DisplayName("Should handle expenses with multiple pipes")
    void testMapCategoriesMultiplePipes() {
        String aiResponse = """
                EXPENSE|WITH|PIPES|Supermercado
                NORMAL EXPENSE|Restaurante / Lanches
                """;

        List<CategoryMapper> result = callMapCategories(aiResponse);

        // Should only include the normal expense (first one has too many pipes)
        assertEquals(1, result.size());
        assertEquals("NORMAL EXPENSE", result.getFirst().getExpenseName());
    }

    @Test
    @DisplayName("Should create valid request body")
    void testGetRequestBody() {
        String prompt = "Test prompt";

        Map<String, Object> body = callGetRequestBody(prompt);

        assertNotNull(body);
        assertEquals("gemma3:4b", body.get("model"));
        assertEquals(prompt, body.get("prompt"));
        assertEquals(true, body.get("stream"));

        @SuppressWarnings("unchecked")
        Map<String, Object> options = (Map<String, Object>) body.get("options");
        assertNotNull(options);
        assertEquals(4096, options.get("num_ctx"));
        assertEquals(4096, options.get("num_predict"));
        assertEquals(0, options.get("temperature"));
    }

    @Test
    @DisplayName("Should handle multiple pipes in expense name correctly")
    void testMapCategoriesComplexExpenseNames() {
        String aiResponse = """
                COMPANY | SUBSIDIARY | PRODUCT|Supermercado
                """;

        List<CategoryMapper> result = callMapCategories(aiResponse);

        // Should be ignored because it has more than 1 pipe before category
        assertEquals(0, result.size());
    }

    // ===== Helper Methods =====

    /**
     * Uses reflection to call the private mapCategories method
     */
    @SuppressWarnings("unchecked")
    private List<CategoryMapper> callMapCategories(String aiResponse) {
        try {
            return (List<CategoryMapper>) ReflectionTestUtils.invokeMethod(
                    aiProcessorService, "mapCategories", aiResponse);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke mapCategories", e);
        }
    }

    /**
     * Uses reflection to call the private getRequestBody method
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> callGetRequestBody(String prompt) {
        try {
            return (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                    aiProcessorService, "getRequestBody", prompt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke getRequestBody", e);
        }
    }

}









