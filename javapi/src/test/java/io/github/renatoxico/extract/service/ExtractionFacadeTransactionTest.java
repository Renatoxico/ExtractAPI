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
import static org.mockito.Mockito.when;

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

    @MockitoBean ExtractorService extractorService;
    @MockitoBean ExpenseClassificationCatalogService catalogService;

    private Long ownerId;

    @BeforeEach
    void createOwner() {
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

        assertThat(reportRepository.count()).isZero();
        assertThat(expenseRepository.count()).isZero();
        assertThat(appUserRepository.count()).isEqualTo(1);
    }

    private static MockMultipartFile pdf(String fileName) {
        return new MockMultipartFile("file", fileName, "application/pdf", "pdf".getBytes());
    }
}
