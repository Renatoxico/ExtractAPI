package com.example.API.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.springframework.http.HttpHeaders;

@Service
public class PythonProcessingService {
    private static String pythonProcessor = "pythonProcessor.py";
    private static String URL = "http://localhost:5000/process";

    public void processFile(String file) {
        try {
            ProcessBuilder pb = new ProcessBuilder("python3",pythonProcessor, file);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null){
                output.append(line);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

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

//String fetching = "python " + "c:\\Fetch.py \"" + songDetails + "\"";
//String[] commandToExecute = new String[]{"cmd.exe", "/c", fetching};
//Runtime.getRuntime().exec(commandToExecute);