package com.example.api.controller;


import com.example.api.model.ValidationResponse;
import com.example.api.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/extract")
public class ExtractController {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractController.class);
    private final PythonProcessingService pyProcessor;
    private final ObjectifierService objService;
    private final ExpenseReportingService reportsService;
    private final ValidationService validationService;
    private static final String PATH = System.getProperty("user.dir") + "\\tmp\\";

    public ExtractController(ValidationService validationService, PythonProcessingService pyProcessor, ObjectifierService objService, ExpenseReportingService reportsService) {
        this.pyProcessor = pyProcessor;
        this.objService = objService;
        this.validationService = validationService;
        this.reportsService = reportsService;
    }

    @GetMapping("/")
    public ResponseEntity<?> home(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        LOG.info("Home API called from IP: " + ip);
        return ResponseEntity.ok("Ip Address: " + ip);
    }

    @PostMapping("/")
    public ResponseEntity<?> pythonProcessor(@RequestParam("file") MultipartFile[] files){
        LOG.info("Python Processor API");
        String sessionId = reportsService.generateId();
        ValidationResponse isValid = validationService.validateFiles(files);
        if (!isValid.getStatus()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(isValid.getMessage());
        }
        for (MultipartFile file : files) {
            //String filepath = saveFileTemp(file);
            String pdfText = pyProcessor.convertPDFtoJSON(file);
            objService.process(sessionId, pdfText);
        }
        Map<String,Object> expensesGrouped = reportsService.getFullReport(sessionId);
        expensesGrouped.put("sessionToken", sessionId);
        return ResponseEntity.ok(expensesGrouped);
    }

    @GetMapping("/summary/{sessionId}")
    public ResponseEntity<?> getExpenseSummary(@PathVariable String sessionId) {
        if(sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No sessionId");
        }
        Map<String,Object> expensesGrouped = reportsService.getFullReport(sessionId);
        expensesGrouped.put("sessionToken", sessionId);
        return ResponseEntity.status(HttpStatus.OK).body(expensesGrouped);
    }

    //@GetMapping("/test/{sessionId}")
    public ResponseEntity<?> testEndpoint(@PathVariable String sessionId) {
        return ResponseEntity.status(HttpStatus.OK).body(
                reportsService.updateExpenses(sessionId)
        );
    }

    private String saveFileTemp(MultipartFile file){
        try {
            Files.createDirectories(Paths.get(PATH));
            String filePath = PATH + file.getOriginalFilename();
            file.transferTo(new File(filePath));
            return filePath;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
