package com.example.api.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ExtractorService {

    public String extractText(MultipartFile pdf) {
        String output;
        try {
            PDDocument pdfDoc = Loader.loadPDF(pdf.getBytes());
            PDFTextStripper ripper = new PDFTextStripper();
            output = ripper.getText(pdfDoc);
        }
        catch (IOException e){
            throw new RuntimeException("Failed to load PDF document: " + e.getMessage(), e);
        }
        return output;
    }
}
