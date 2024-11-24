package com.example.API.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Service
public class FileService {
    public PDDocument loadFile(MultipartFile iFile) {
        try {
            File file = File.createTempFile("tmp", iFile.getOriginalFilename());
            iFile.transferTo(file);
            return toPdfObj(file);
        } catch (IOException e) {
            throw new RuntimeException("Error while creating or transferring file: " + e.getMessage(), e);
        }
    }

    public PDDocument toPdfObj(File iFile) {
        try {
            PDDocument doc = new PDDocument();
            doc = Loader.loadPDF(iFile);
            return doc;
        } catch (IOException e) {//fails here if cant read text?
            throw new RuntimeException("Error while converting to PDDocument: " + e.getMessage(), e);
        }
    }

    public String getContent(MultipartFile pdf){
        PDDocument doc = loadFile(pdf);
        PDFTextStripper reader = new PDFTextStripper();
        try {
            return reader.getText(doc);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read PDF " + e.getMessage(), e);
        }
    }
}
