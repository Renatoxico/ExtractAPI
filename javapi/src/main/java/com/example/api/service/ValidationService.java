package com.example.api.service;

import com.example.api.model.ValidationResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ValidationService {
    private static final long MAX_SIZE_KB = 1024;

    public ValidationResponse validateFiles (MultipartFile[] files) {
        ValidationResponse res = new ValidationResponse(true, "DEFAULT_OK");
        if (files.length<1){
            res.setStatus(false);
            res.setMessage("No files found");
            return res;
        }
        if (files.length>6){
            res.setStatus(false);
            res.setMessage("Too many files");
            return res;
        }
        for (MultipartFile file : files) {
            long fileSizeKB = file.getSize() / 1024;
            String contentType = file.getContentType();
            String fileName = file.getOriginalFilename();

            boolean isValidType = "application/pdf".equals(contentType);
            boolean hasValidExtension = fileName != null && (fileName.toLowerCase().endsWith(".pdf"));

            if (fileSizeKB >= MAX_SIZE_KB / 2) {
                res.setStatus(false);
                res.setMessage("File too big");
                return res;
            }

            if (!isValidType || !hasValidExtension) {
                res.setStatus(false);
                res.setMessage("Invalid file type");
                return res;
            }

        }

        return res;
    }
}
