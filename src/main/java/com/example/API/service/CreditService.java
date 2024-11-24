package com.example.API.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CreditService {
    private static final Logger LOG = LoggerFactory.getLogger(CreditService.class);

    public String getExtractDetails(String ext){
        LOG.info("getExtractDetails - start");
        StringBuilder res = new StringBuilder();
        List<String> patterns = List.of("(\\d{2}/\\d{2})\\s+([^0-9]+?)\\s+(\\d+,[0-9]{2})","(\\d{2}/\\d{2})\\s+([A-Za-z0-9 E*]+)\\s+\\d+/\\d+\\s+([0-9]+,[0-9]{2})");
        patterns.forEach(pattern -> mapExpenses(pattern, ext, res));
        return res.toString();
    }

    public void mapExpenses(String pattern, String ext, StringBuilder output) {
        Pattern pat = Pattern.compile(pattern);
        Matcher matcher = pat.matcher(ext);

        while (matcher.find()) {
            String data = matcher.group(1);
            String desc = matcher.group(2);
            String amount = matcher.group(3);

            ext = ext.substring(0, matcher.start()) + ext.substring(matcher.end());

            output.append("Data: ").append(data)
                    .append(", Descrição: ").append(desc)
                    .append(", R$: ").append(amount)
                    .append("\n");
        }
    }
}
