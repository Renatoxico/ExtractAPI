package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.repo.AppUserRepository;
import io.github.renatoxico.extract.repo.ExpenseReportRepository;
import io.github.renatoxico.extract.repo.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import io.github.renatoxico.extract.model.ReportData;

@org.springframework.boot.context.properties.EnableConfigurationProperties(org.springframework.boot.autoconfigure.web.servlet.MultipartProperties.class)
@DataJpaTest
@Import({
    ExtractionFacade.class,
    ExpenseReportingService.class,
    ObjectifierService.class,
    ValidationService.class,
    V2ReportMapper.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ExtractionFacadeTransactionTest {
    @Autowired ExtractionFacade extractionFacade;
    @Autowired ExpenseRepository expenseRepository;
    @Autowired ExpenseReportRepository reportRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ExpenseReportingService reportingService;

    @MockitoBean io.github.renatoxico.extract.service.ErrorRecorder errorRecorder;
    @MockitoBean ExtractorService extractorService;
    @MockitoBean ExpenseClassificationCatalogService catalogService;

    private Long ownerId;

    @BeforeEach
    void createOwner() {
        expenseRepository.deleteAll();
        reportRepository.deleteAll();
        appUserRepository.deleteAll();
        reset(errorRecorder, extractorService);
        jdbcTemplate.update(
            "INSERT INTO app_user (firebase_uid, email) VALUES (?, ?)",
            "rollback-test-user",
            "rollback@example.com"
        );
        ownerId = jdbcTemplate.queryForObject(
            "SELECT id FROM app_user WHERE firebase_uid = ?",
            Long.class,
            "rollback-test-user"
        );
    }

    @Test
    void rollsBackReportAndExpensesWhenSecondFileFails() {
        MockMultipartFile first = pdf("first.pdf");
        MockMultipartFile second = pdf("second.pdf");
        when(extractorService.extractText(first)).thenReturn("15/03 MERCADO 10,00");
        when(extractorService.extractText(second)).thenThrow(new ProcessingException(
            "Unreadable PDF",
            HttpStatus.UNPROCESSABLE_ENTITY,
            "PDF_EXTRACTION_FAILED"
        ));

        assertThatThrownBy(() -> extractionFacade.process(
            new MockMultipartFile[]{first, second}, ownerId))
            .isInstanceOf(ProcessingException.class)
            .hasMessageContaining("second.pdf")
            .hasFieldOrPropertyWithValue("errorCode", "PDF_EXTRACTION_FAILED");

        verify(errorRecorder).record(eq("PDF_EXTRACTION_FAILED"), eq(ErrorRecorder.Stage.PDF_EXTRACTION),
            anyString(), any(UUID.class), eq(Map.of("fileCount", 2)));
        assertThat(reportRepository.count()).isZero();
        assertThat(expenseRepository.count()).isZero();
        assertThat(appUserRepository.count()).isEqualTo(1);
    }

    @Test
    void allInvalidCandidatesReturns422BeforeReportLookupAndRollsBack() {
        MockMultipartFile file = pdf("invalid.pdf");
        when(extractorService.extractText(file)).thenReturn("31/02/2024 LOJA FICTICIA 10,00");
        assertThatThrownBy(() -> extractionFacade.process(new MockMultipartFile[]{file}, ownerId))
            .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.UNPROCESSABLE_ENTITY)
            .hasFieldOrPropertyWithValue("errorCode", "NO_VALID_EXPENSES");
        verify(errorRecorder).record(eq("NO_VALID_EXPENSES"), eq(ErrorRecorder.Stage.PARSING),
            anyString(), any(UUID.class), anyMap());
        assertThat(reportRepository.count()).isZero();
        assertThat(expenseRepository.count()).isZero();
    }

    @Test
    void creditsReduceReportGroupsCategoriesSummaryAndCsvTotals() {
        MockMultipartFile file = pdf("signed.pdf");
        when(extractorService.extractText(file)).thenReturn("10/01/2025 LOJA FICTICIA 100,00\n11/01/2025 LOJA FICTICIA -25,00");
        ReportData report = extractionFacade.process(new MockMultipartFile[]{file}, ownerId);
        assertThat(report.expenses()).hasSize(2);
        assertThat(report.expenseGroups().getFirst().totalAmount()).isEqualByComparingTo("75.00");
        assertThat(report.categorySummaries().getFirst().totalAmount()).isEqualByComparingTo("75.00");
        assertThat(reportingService.getReportSummaries(ownerId).getFirst().total()).isEqualByComparingTo("75.00");
        assertThat(new String(reportingService.exportReportCsvV2(report.reportId()), java.nio.charset.StandardCharsets.UTF_8)).contains("-25.00");
        assertThat(expenseRepository.findAll()).extracting(e -> e.getAmount()).contains(new BigDecimal("-25.00"));
    }

    private static MockMultipartFile pdf(String fileName) {
        return new MockMultipartFile("file", fileName, "application/pdf", "pdf".getBytes());
    }
}
