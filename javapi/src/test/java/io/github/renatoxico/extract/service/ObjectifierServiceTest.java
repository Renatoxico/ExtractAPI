package io.github.renatoxico.extract.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.model.Expense;
import io.github.renatoxico.extract.model.ExpenseReport;
import io.github.renatoxico.extract.repo.ExpenseRepository;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ObjectifierServiceTest {
    private final ExpenseRepository repo = mock(ExpenseRepository.class);
    private final ObjectifierService parser = new ObjectifierService(repo,
        new ValidationService(Clock.fixed(Instant.parse("2025-01-15T12:00:00Z"), ZoneOffset.UTC), 1048576));
    private final ExpenseReport report = new ExpenseReport("syntheticreport000000000000000000", null);

    @ParameterizedTest
    @ValueSource(strings = {"nubank", "itau", "bradesco", "banco-do-brasil", "inter", "santander"})
    void matchesGoldenTextAndGeneratedPdf(String bank) throws Exception {
        String prefix = "/fixtures/" + bank + "/";
        String text = new String(getClass().getResourceAsStream(prefix + "statement.txt").readAllBytes(), StandardCharsets.UTF_8);
        JsonNode golden = new ObjectMapper().readTree(getClass().getResourceAsStream(prefix + "expected.json"));
        assertThat(text).isEqualTo(golden.get("rawText").asText());
        assertGolden(text, golden);
        reset(repo);
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            pdf.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                content.newLineAtOffset(30, 750);
                for (String line : text.split("\n")) {
                    content.showText(line);
                    content.newLineAtOffset(0, -15);
                }
                content.endText();
            }
            pdf.save(bytes);
            String extracted = new ExtractorService().extractText(new MockMultipartFile("file", "synthetic.pdf", "application/pdf", bytes.toByteArray()));
            assertGolden(extracted, golden);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void assertGolden(String text, JsonNode golden) {
        parser.process(report, text);
        ArgumentCaptor<Iterable<Expense>> saved = ArgumentCaptor.forClass((Class) Iterable.class);
        verify(repo).saveAll(saved.capture());
        List<Expense> expenses = (List<Expense>) saved.getValue();
        assertThat(expenses).hasSize(golden.get("transactions").size());
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < expenses.size(); i++) {
            JsonNode expected = golden.get("transactions").get(i);
            Expense expense = expenses.get(i);
            assertThat(expense.getDate()).isEqualTo(expected.get("date").asText());
            assertThat(expense.getExpenseName()).isEqualTo(expected.get("description").asText());
            assertThat(expense.getAmount()).isEqualByComparingTo(expected.get("amount").asText());
            total = total.add(new BigDecimal(expected.get("amount").asText()));
        }
        assertThat(expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo(total);
    }

    @ParameterizedTest
    @ValueSource(strings = {"31/02/2024 LOJA 10,00", "29/02/2023 LOJA 10,00", "15/13 LOJA 10,00", "10/01 LOJA 0,00", "10/01 123 10,00"})
    void allDiscardedHasSpecific422(String text) {
        assertThatThrownBy(() -> parser.process(report, text)).isInstanceOfSatisfying(ProcessingException.class, ex -> {
            assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(ex.getErrorCode()).isEqualTo("NO_VALID_EXPENSES");
        });
        verifyNoInteractions(repo);
    }

    @Test
    void noCandidatesHasSeparateCode() {
        assertThatThrownBy(() -> parser.process(report, "unrelated text")).hasFieldOrPropertyWithValue("errorCode", "NO_EXPENSES_EXTRACTED");
    }

    @Test
    void installmentDoesNotSplitSecondTransactionOnSameLine() {
        List<String> rows = parser.getFilterCharges(new String[]{"10/01 LOJA PARC 03/10 1234,56 11/01 OUTRA 20,00"});
        assertThat(parser.objectifyExtract(report, rows)).extracting(Expense::getExpenseName).containsExactly("LOJA PARC 03/10", "OUTRA");
    }
}
