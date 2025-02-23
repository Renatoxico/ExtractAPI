package com.example.api.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpHeaders;

@Service
public class PythonProcessingService {
    private static final String URL = "http://localhost:5000/process";

    public String convertPDFtoJSON(String file) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");
//        String reqBody = "{\"filepath\": \"" + file + "\"}";
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("filepath", file);
        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(map, headers);

        ResponseEntity<String> resp = restTemplate.exchange(URL, HttpMethod.POST, req, String.class);
        return resp.getBody();
    }
}