package io.github.renatoxico.extract.controller;

import io.github.renatoxico.extract.service.LegalDocumentService;
import io.github.renatoxico.extract.service.RenderedLegalDocument;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    private final LegalDocumentService legalDocuments;

    public PageController(LegalDocumentService legalDocuments) {
        this.legalDocuments = legalDocuments;
    }

    @GetMapping("/terms")
    public String terms(Model model) {
        return render(model, "Termos de Uso — Somaí", legalDocuments.terms());
    }

    @GetMapping("/privacy")
    public String privacy(Model model) {
        return render(model, "Política de Privacidade — Somaí", legalDocuments.privacy());
    }

    private String render(Model model, String title, RenderedLegalDocument document) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("documentHtml", document.html());
        model.addAttribute("placeholderCount", document.placeholderCount());
        return "legal";
    }
}
