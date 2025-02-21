package com.example.API.controller;

import com.example.API.model.Expense;
import com.example.API.service.*;
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
    private final ExtractService exService;
    private final PythonProcessingService pyProcessor;
    private final ObjectifierService objService;
    private static final String PATH = System.getProperty("user.dir") + "\\tmp\\";

    public ExtractController(ExtractService exService, DebitService debitService, FileService fs, PythonProcessingService pyProcessor, ObjectifierService objService) {
        this.exService = exService;
        this.pyProcessor = pyProcessor;
        this.objService = objService;
    }

    @PostMapping("/single/")
    public ResponseEntity<?> processExtract(@RequestParam("ext") MultipartFile ext, @RequestParam("type") String type){
        LOG.info("Single file processing");
        if (ext.isEmpty() || type == null || type.isBlank()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing data in request");
        }
        try {
            //String output = exService.processDocument(ext, type);
            //return ResponseEntity.ok(output);
            return ResponseEntity.ok(exService.processDocument(ext, type));
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/batch/")
    public ResponseEntity<?> processBatchExtract(@RequestParam MultipartFile[] files){
        LOG.info("Batch processing");
        if (files.length<1){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No files.");
        }
        try {
            List<Expense> output = exService.batchProcess(files);
            exService.expenseClear();
            return ResponseEntity.ok(output);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
            //return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process request.");
        }
    }

    @PostMapping("/")
    public ResponseEntity<?> pythonProcessor(@RequestParam("file") MultipartFile iFile){
        if (iFile.isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing data in request");
        }
        String filepath = saveFileTemp(iFile);
        String pdfText = pyProcessor.convertPDFtoJSON(filepath);
        objService.process(pdfText);
        return ResponseEntity.ok(pdfText);
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
