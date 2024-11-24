package com.example.API.controller;

import com.example.API.service.DebitService;
import com.example.API.service.ExtractService;
import com.example.API.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/extract")
public class ExtractController {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractController.class);
    private final ExtractService exService;

    public ExtractController(ExtractService exService, DebitService debitService, FileService fs) {
        this.exService = exService;
    }

    @PostMapping("/")
    public ResponseEntity<String> processExtract(@RequestParam("ext") MultipartFile ext, @RequestParam("type") String type){
        LOG.info("This is the final one.");
        if (ext.isEmpty() || type == null || type.isBlank()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Request missing data");
        }
        try {
            //String output = exService.processDocument(ext, type);
            String output = exService.processDocument2(ext);
            return ResponseEntity.ok(output);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }
    /*TODO
        CREATE METHOD FOR PROCESSING DOC INDEPENDENT OF TYPE
        CREATE METHOD FOR SEARCHING SESSION_ID
     */


}
