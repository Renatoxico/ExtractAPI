package io.github.renatoxico.extract.service;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders the checked-in legal drafts. Wording is kept as written, including
 * every [PREENCHER] and [CONFIRMAR] blank. Those blanks are wrapped so they
 * can be found on the page. The draft's privacy-policy link target
 * {@code PREENCHER-link} is pointed at {@code /privacy} and left visible.
 */
@Service
public class LegalDocumentService {
    private static final Pattern PRIVACY_LINK = Pattern.compile(
        "<a href=\"PREENCHER-link\">([\\s\\S]*?)</a>"
    );
    private static final Pattern BRACKET_PLACEHOLDER = Pattern.compile(
        "\\[(?:PREENCHER|CONFIRMAR)[^\\]]*\\]"
    );
    private static final Pattern UNNUMBERED_MARK = Pattern.compile(
        "<mark class=\"placeholder\">([\\s\\S]*?)</mark>"
    );

    private final RenderedLegalDocument terms;
    private final RenderedLegalDocument privacy;

    public LegalDocumentService() {
        List<Extension> extensions = List.of(TablesExtension.create());
        Parser parser = Parser.builder().extensions(extensions).build();
        HtmlRenderer renderer = HtmlRenderer.builder()
            .extensions(extensions)
            .escapeHtml(true)
            .build();
        this.terms = render(parser, renderer, "legal/termos.md");
        this.privacy = render(parser, renderer, "legal/privacidade.md");
    }

    public RenderedLegalDocument terms() {
        return terms;
    }

    public RenderedLegalDocument privacy() {
        return privacy;
    }

    private static RenderedLegalDocument render(Parser parser, HtmlRenderer renderer, String classpathLocation) {
        Node document = parser.parse(read(classpathLocation));
        String html = renderer.render(document);
        html = PRIVACY_LINK.matcher(html).replaceAll(
            "<a href=\"/privacy\">$1</a> <mark class=\"placeholder\">PREENCHER-link</mark>"
        );
        html = wrapBracketPlaceholders(html);
        return numberPlaceholders(html);
    }

    private static String wrapBracketPlaceholders(String html) {
        Matcher matcher = BRACKET_PLACEHOLDER.matcher(html);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(
                "<mark class=\"placeholder\">" + matcher.group() + "</mark>"
            ));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private static RenderedLegalDocument numberPlaceholders(String html) {
        Matcher matcher = UNNUMBERED_MARK.matcher(html);
        StringBuilder rendered = new StringBuilder();
        int count = 0;
        while (matcher.find()) {
            count++;
            String replacement = "<mark class=\"placeholder\" id=\"placeholder-" + count + "\">"
                + matcher.group(1)
                + "</mark>";
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rendered);
        return new RenderedLegalDocument(rendered.toString(), count);
    }

    private static String read(String classpathLocation) {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        try (InputStream input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Missing legal document " + classpathLocation, exception);
        }
    }
}
