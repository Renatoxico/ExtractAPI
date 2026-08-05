package io.github.renatoxico.extract.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/terms")
    public String terms() {
        return "terms";
    }
}
