package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.model.Expense;
import io.github.renatoxico.extract.repo.ExpenseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ParserSafetyNetTest {
    private final ValidationService validation = new ValidationService(
        Clock.fixed(Instant.parse("2025-01-15T12:00:00Z"), ZoneOffset.UTC), 1048576);

    @ParameterizedTest
    @CsvSource({"PAG*STEAM,Lazer / Entretenimento / Pets", "HOSTGATOR,Investimentos / Assinaturas profissionais"})
    void classifiesUnmodifiedDescriptions(String name, String category) {
        Expense expense = expense("01/01/2025", name);
        validation.validateExpenses(new ArrayList<>(List.of(expense)));
        assertThat(expense.getExpenseName()).isEqualTo(name);
        assertThat(expense.getCategory()).isEqualTo(category);
    }

    @Test
    void rolloverWorksWithDecemberAnchorAndClockFallbackIncludingLeapDay() {
        List<Expense> anchored = new ArrayList<>(List.of(expense("31/12/2024", "LOJA"), expense("02/01", "LOJA")));
        assertThat(validation.validateExpenses(anchored)).extracting(Expense::getDate).containsExactly("31/12/2024", "02/01/2025");
        List<Expense> fallback = new ArrayList<>(List.of(expense("31/12", "LOJA"), expense("02/01", "LOJA"), expense("29/02", "LOJA")));
        assertThat(validation.validateExpenses(fallback)).extracting(Expense::getDate).containsExactly("31/12/2024", "02/01/2025", "29/02/2024");
    }

    @Test
    void mapperRejectsInvalidFullDatesRatherThanCoercingThem() {
        assertThatThrownBy(() -> new V2ReportMapper().parseDate("31/02/2024"))
            .hasFieldOrPropertyWithValue("errorCode", "REPORT_DATA_INVALID");
        assertThat(new V2ReportMapper().parseDate("29/02/2024")).isEqualTo(LocalDate.of(2024, 2, 29));
    }

    @ParameterizedTest
    @CsvSource({"1234,1234.56", "1.234,1234.56", "1234567,1234567.56", "1.234.567,1234567.56", "-1234,-1234.56", "-1.234,-1234.56"})
    void parsesEntireAmount(String integral, String expected) {
        ObjectifierService parser = new ObjectifierService(mock(ExpenseRepository.class), validation);
        List<Expense> rows = parser.objectifyExtract(new io.github.renatoxico.extract.model.ExpenseReport("a".repeat(32), null), List.of("01/01/2025 LOJA " + integral + ",56"));
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().getAmount()).isEqualByComparingTo(expected);
    }

    @Test
    void sizeUsesExactSpringConfiguredBoundary() {
        MultipartProperties properties = new MultipartProperties();
        properties.setMaxFileSize(DataSize.ofKilobytes(64));
        ValidationService service = new ValidationService(properties);
        assertThat(service.validateFiles(new MockMultipartFile[]{pdf(65536)}).valid()).isTrue();
        assertThat(service.validateFiles(new MockMultipartFile[]{pdf(65537)}).errorCode()).isEqualTo("FILE_TOO_BIG");
    }

    private static MockMultipartFile pdf(int bytes) {
        return new MockMultipartFile("file", "synthetic.pdf", "application/pdf", new byte[bytes]);
    }

    private static Expense expense(String date, String name) {
        return new Expense(null, BigDecimal.TEN, name, date, null);
    }
}
