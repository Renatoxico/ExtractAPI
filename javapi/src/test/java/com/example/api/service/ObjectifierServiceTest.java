package com.example.api.service;


import com.example.api.repo.ExpenseRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.mock;

class ObjectifierServiceTest {
    static String pythonOutput;

    private ObjectifierService service;

    @BeforeAll
    static void loadPythonOutput() {
        try (InputStream is = ObjectifierServiceTest.class.getResourceAsStream("/testing/pythonOutputExample.txt")) {
            if (is == null) throw new IllegalArgumentException("File not found!");
            pythonOutput = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setup() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        ValidationService validationService = new ValidationService();
        service = new ObjectifierService(expenseRepository, validationService);
    }

    @Test
    void testProcess() {
        String fakeToken = "fakeToken123";
        long startTime = System.currentTimeMillis();
        service.process(fakeToken, pythonOutput);
        long endTime = System.currentTimeMillis();
        System.out.println("Processing time: " + (endTime - startTime) + " ms");
    }

    @Test
    void testGetFilterCharges() {
        String[] lines = service.splitByLine(pythonOutput);
        long startTime = System.currentTimeMillis();
        var filteredCharges = service.getFilterCharges(lines);
        long endTime = System.currentTimeMillis();
        System.out.println("Filtered Charges count: " + filteredCharges.size());
        System.out.println("Filtering time: " + (endTime - startTime) + " ms");
    }

    @Test
    void testSplitByLine() {
        long startTime = System.currentTimeMillis();
        String[] lines = service.splitByLine(pythonOutput);
        long endTime = System.currentTimeMillis();
        System.out.println("Total lines: " + lines.length);
        System.out.println("Splitting time: " + (endTime - startTime) + " ms");
    }

    @Test
    void testObjectifyExtract() {
        String[] lines = service.splitByLine(pythonOutput);
        var filteredCharges = service.getFilterCharges(lines);
        long startTime = System.currentTimeMillis();
        var expenses = service.objectifyExtract("fakeToken123", filteredCharges);
        long endTime = System.currentTimeMillis();
        System.out.println("Objectified Expenses count: " + expenses.size());
        System.out.println("Objectification time: " + (endTime - startTime) + " ms");
    }
}
