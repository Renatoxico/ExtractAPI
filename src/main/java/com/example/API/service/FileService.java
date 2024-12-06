package com.example.API.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.List;

@Service
public class FileService {
    private final String newLine = System.lineSeparator();
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
            String xxx = reader.getText(doc);
            readPDF(doc);
            return reader.getText(doc);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read PDF " + e.getMessage(), e);
        }
    }

    public void readPDF(PDDocument doc){
        StringBuilder x = new StringBuilder();
        StringBuilder xxx = new StringBuilder();
        for(PDPage page : doc.getPages()){
            try {
                PDFStreamParser parser = new PDFStreamParser(page);
                List<Object> aux = parser.parse();
                xxx.append(parsePDF(aux));
                for(Object token : aux){
                    if (token instanceof Operator){
                        Operator op = (Operator) token;
                        x.append(op.getName());
                    } else if (token instanceof COSString){
                        COSString str = (COSString) token;
                        x.append(str);
                    }
                    else {
                        x.append(token);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        x.toString();
    }

    public String parsePDF (List<Object> aux) {
        StringBuilder xxx = new StringBuilder();
        for(Object token : aux){
            if(token.toString().contains("Movimenta")){
                token.toString();
            }
            switch (token) {
                case COSFloat f -> {
                    float fValue = f.floatValue();
                    if (fValue < -30) {
                        xxx.append(newLine);
                    } else if (fValue  > 10000) {
                        xxx.append(" ");
                    } else if (fValue  > 0 && fValue  < 5) {
                        xxx.append(" ");
                    }

                }
                case COSArray a -> {
                    for (COSBase cosBase : a){
                        switch (cosBase) {
                            case COSString a_String -> xxx.append(a_String.getString());
                            case COSInteger a_int -> {
                                if (a_int.intValue() < -200) {
                                    xxx.append(" ");
                                }
                                if (a_int.intValue() == 0) {
                                    xxx.append(" ");
                                }
                            }
                            default -> {

                            }
                        }
                    }
                }
                case Operator o -> {
                    break;// x.append(o.getName());
                }
                case COSInteger i -> {
                    if (i.intValue() == 34 ) {
                        xxx.append(newLine);
                    }
                    if (i.intValue() == 0 ) {
                        xxx.append(" ");
                    }
                }
                default -> {
                    break;
                }
            }
        }
        return xxx.toString();
    }
}
