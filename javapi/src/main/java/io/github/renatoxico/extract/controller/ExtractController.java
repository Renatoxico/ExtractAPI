package io.github.renatoxico.extract.controller;

import io.github.renatoxico.extract.exception.ProcessingException;
import io.github.renatoxico.extract.service.ExtractionFacade;
import io.github.renatoxico.extract.service.ExtractorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Controller
@RequestMapping("/extract")
public class ExtractController {
    private final ExtractionFacade extractionFacade;
    private final ExtractorService extractorService;

    public ExtractController(ExtractionFacade extractionFacade, ExtractorService extractorService) {
        this.extractionFacade = extractionFacade;
        this.extractorService = extractorService;
    }

    @GetMapping("/")
    public String home() {
        return "api-docs";
    }

    @PostMapping("/raw-text/")
    public ResponseEntity<Map<String, String>> extractRawText(@RequestParam("file") MultipartFile[] files) {
        extractionFacade.requireValid(files);
        StringBuilder combinedText = new StringBuilder();
        for (MultipartFile file : files) {
            try {
                String pdfText = extractorService.extractText(file);
                if (pdfText != null && !pdfText.isBlank()) {
                    combinedText.append(pdfText).append('\n');
                }
            } catch (ProcessingException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new ProcessingException(
                    "Failed to extract text from file: " + file.getOriginalFilename(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "TEXT_EXTRACTION_ERROR",
                    ex
                );
            }
        }
        if (combinedText.isEmpty()) {
            throw new ProcessingException(
                "No text could be extracted from the provided files",
                HttpStatus.UNPROCESSABLE_ENTITY,
                "NO_TEXT_EXTRACTED"
            );
        }
        return ResponseEntity.ok(Map.of("extractedText", combinedText.toString()));
    }
}
