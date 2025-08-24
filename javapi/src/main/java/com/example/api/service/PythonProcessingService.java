package com.example.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PythonProcessingService {
    private static final Logger LOG = LoggerFactory.getLogger(PythonProcessingService.class);
    private static final String URL = "http://backend-python:9000/process";

    public String convertPDFtoJSON(MultipartFile file) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "multipart/form-data");

        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("file", file.getResource());

        HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(map, headers);

        ResponseEntity<String> resp = restTemplate.exchange(URL, HttpMethod.POST, req, String.class);
        return resp.getBody();
    }
}