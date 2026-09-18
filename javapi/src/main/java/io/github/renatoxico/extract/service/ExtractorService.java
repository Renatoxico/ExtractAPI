package io.github.renatoxico.extract.service;

import io.github.renatoxico.extract.exception.ProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import io.github.renatoxico.extract.logging.LogFields;

@Service
public class ExtractorService {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractorService.class);

    public String extractText(MultipartFile pdf) {
        String output;
        try {
            LOG.debug("Extracting text from PDF: {}", LogFields.safe(pdf.getOriginalFilename()));
            PDDocument pdfDoc = Loader.loadPDF(pdf.getBytes());
            PDFTextStripper ripper = new PDFTextStripper();
            output = ripper.getText(pdfDoc);
            pdfDoc.close();
            LOG.debug("Successfully extracted {} characters from PDF", output.length());
            return output;
        } catch (IOException e){
            throw new ProcessingException(
                "Failed to extract text from PDF: " + e.getMessage(),
                HttpStatus.UNPROCESSABLE_ENTITY,
                "PDF_EXTRACTION_FAILED",
                e
            ).withDiagnostic("filename", pdf.getOriginalFilename());
        }
    }
}
