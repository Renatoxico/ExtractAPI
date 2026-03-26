package com.example.api.service;

import com.example.api.exception.ProcessingException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtractorServiceTest {

    private final ExtractorService extractorService = new ExtractorService();

    private byte[] createPdfWithText(String... pages) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (String text : pages) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(50, 700);
                    content.showText(text);
                    content.endText();
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    @Test
    void shouldExtractTextFromValidPdf() throws IOException {
        byte[] pdf = createPdfWithText("Expense Report 2025");
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", pdf);

        String result = extractorService.extractText(file);

        assertThat(result).contains("Expense Report 2025");
    }

    @Test
    void shouldExtractTextFromMultiPagePdf() throws IOException {
        byte[] pdf = createPdfWithText("Page One Content", "Page Two Content");
        MockMultipartFile file = new MockMultipartFile("file", "multi.pdf", "application/pdf", pdf);

        String result = extractorService.extractText(file);

        assertThat(result).contains("Page One Content");
        assertThat(result).contains("Page Two Content");
    }

    @Test
    void shouldThrowProcessingExceptionForCorruptedPdf() {
        byte[] corrupted = "this is not a pdf".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "bad.pdf", "application/pdf", corrupted);

        assertThatThrownBy(() -> extractorService.extractText(file))
                .isInstanceOf(ProcessingException.class)
                .satisfies(ex -> {
                    ProcessingException pe = (ProcessingException) ex;
                    assertThat(pe.getErrorCode()).isEqualTo("PDF_EXTRACTION_FAILED");
                });
    }

    @Test
    void shouldHandleEmptyPdf() throws IOException {
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);

            MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", out.toByteArray());
            String result = extractorService.extractText(file);

            assertThat(result).isNotNull();
            assertThat(result.trim()).isEmpty();
        }
    }

    @Test
    void shouldExtractNumericExpenseData() throws IOException {
        byte[] pdf = createPdfWithText("15/03 SUPERMERCADO EXTRA 150,00");
        MockMultipartFile file = new MockMultipartFile("file", "expenses.pdf", "application/pdf", pdf);

        String result = extractorService.extractText(file);

        assertThat(result).contains("15/03");
        assertThat(result).contains("150,00");
        assertThat(result).contains("SUPERMERCADO EXTRA");
    }
}
