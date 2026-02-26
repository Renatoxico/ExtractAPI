package com.example.api.controller;

import com.example.api.model.Expense;
import com.example.api.repo.ExpenseRepository;
import com.example.api.service.ExtractorService;
import com.example.api.service.ObjectifierService;
import com.example.api.service.PythonProcessingService;
import com.example.api.service.ValidationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import static org.mockito.Mockito.mock;

public class ExtractControllerTestOld {
    static String filePath = "src/test/resources/testing/FaturaNovembro.pdf";
    static MultipartFile file;
    private static ObjectifierService service;

    @BeforeAll
    static void setup() throws IOException {
        Path path = Paths.get(filePath);
        byte[] content = Files.readAllBytes(path);
        file = new MockMultipartFile("file", path.getFileName().toString(), "application/pdf", content);
        //ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        ValidationService validationService = new ValidationService();
        service = new ObjectifierService(null, validationService);
    }

    @Test
    void testPythonProcessor() {
        PythonProcessingService pyProcessor = new PythonProcessingService();
        long startTime = System.currentTimeMillis();
        String response = pyProcessor.convertPDFtoJSON(file);
        long endTime = System.currentTimeMillis();
        System.out.println("Python backend response time: " + (endTime - startTime));
        //System.out.println("Response: " + response);
        String [] lines = service.splitByLine(response);
        var filteredCharges = service.getFilterCharges(lines);
        var expenses = service.objectifyExtract("testSession", filteredCharges);
        System.out.println("Extracted Expenses count: " + expenses.size());
        expenses.sort(Comparator.comparing(Expense::getTransactionName));
        System.out.println(expenses);
    }

    @Test
    void testJavaProcessor() {
        ExtractorService javaExtractor = new ExtractorService();
        long startTime = System.currentTimeMillis();
        String text = javaExtractor.extractText(file);
        long endTime = System.currentTimeMillis();
        System.out.println("Java extractor response time: " + (endTime - startTime));
        //System.out.println("Extracted Text: " + text);
        String [] lines = service.splitByLine(text);
        var filteredCharges = service.getFilterCharges(lines);
        var expenses = service.objectifyExtract("testSession", filteredCharges);
        System.out.println("Extracted Expenses count: " + expenses.size());
        expenses.sort(Comparator.comparing(Expense::getTransactionName));
        System.out.println(expenses);
    }
}
