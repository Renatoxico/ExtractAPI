package com.example.api.controller;

import com.example.api.model.ExpensesGroupedDTO;
import com.example.api.model.ValidationResponse;
import com.example.api.service.*;
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
import java.util.List;

@RestController
@RequestMapping("/extract")
public class ExtractController {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractController.class);
    private final PythonProcessingService pyProcessor;
    private final ObjectifierService objService;
    private final ValidationService validationService;
    private static final String PATH = System.getProperty("user.dir") + "\\tmp\\";

    public ExtractController( ValidationService validationService, PythonProcessingService pyProcessor, ObjectifierService objService) {
        this.pyProcessor = pyProcessor;
        this.objService = objService;
        this.validationService = validationService;
    }

    @PostMapping("/")
    public ResponseEntity<?> pythonProcessor(@RequestParam("file") MultipartFile[] files){
        LOG.info("Python Processor API");
        String sessionId = objService.generateId();
        ValidationResponse isValid = validationService.validateFiles(files);
        if (!isValid.getStatus()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(isValid.getMessage());
        }
        for (MultipartFile file : files) {
            String filepath = saveFileTemp(file);
            String pdfText = pyProcessor.convertPDFtoJSON(filepath);
            objService.process(sessionId, pdfText);
        }
        return ResponseEntity.ok(sessionId);
    }

    @GetMapping("/summary/{sessionId}")
    public ResponseEntity<?> getExpenseSummery(@PathVariable String sessionId) {
        if(sessionId.isEmpty() || sessionId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No sessionId");
        }
        List<ExpensesGroupedDTO> expensesGrouped = objService.getExpenseSummary(sessionId);
        return ResponseEntity.status(HttpStatus.OK).body(expensesGrouped);
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
    /*TODO
        CREATE METHOD FOR SEARCHING SESSION_ID
     */
}
