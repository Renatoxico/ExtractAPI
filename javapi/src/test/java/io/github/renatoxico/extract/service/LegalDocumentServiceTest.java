package io.github.renatoxico.extract.service;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class LegalDocumentServiceTest {
    private static final Pattern BRACKET = Pattern.compile("\\[(?:PREENCHER|CONFIRMAR)[^\\]]*\\]");
    private static final Pattern MARK = Pattern.compile(
        "<mark class=\"placeholder\" id=\"placeholder-(\\d+)\">([\\s\\S]*?)</mark>"
    );

    private final LegalDocumentService documents = new LegalDocumentService();

    @Test
    void termsRenderPublishedVersion() {
        RenderedLegalDocument terms = documents.terms();
        String text = stripTags(terms.html());

        assertThat(text).contains("Termos de Uso do Somai");
        assertThat(text).contains("O Somai lê extratos bancários e faturas de cartão de crédito em PDF enviados por você.");
        assertThat(text).doesNotContain("RASCUNHO");
        assertThat(text).contains("comarca de São Carlos/SP");
        assertThat(text).doesNotContain("contato@extractapp.com.br");
        assertThat(terms.html()).contains("href=\"/privacy\"");
        assertThat(terms.html()).doesNotContain("href=\"PREENCHER-link\"");
        assertPlaceholdersMatchSource("legal/termos.md", terms);
    }

    @Test
    void privacyRendersPublishedVersion() {
        RenderedLegalDocument privacy = documents.privacy();
        String text = stripTags(privacy.html());

        assertThat(text).contains("Política de Privacidade do Somai");
        assertThat(text).contains("Não tratamos dados de menores de 18 anos. O serviço é destinado apenas a maiores de idade.");
        assertThat(text).contains("Lei nº 13.709/2018");
        assertThat(privacy.html()).contains("<table");
        assertThat(text).doesNotContain("RASCUNHO");
        assertThat(text).contains("diasrenato2@outlook.com");
        assertPlaceholdersMatchSource("legal/privacidade.md", privacy);
    }

    private static void assertPlaceholdersMatchSource(String classpathLocation, RenderedLegalDocument document) {
        String source = read(classpathLocation);
        int expected = count(BRACKET, source) + countLiteral(source, "PREENCHER-link");
        assertThat(document.placeholderCount()).isEqualTo(expected);
        assertThat(count(MARK, document.html())).isEqualTo(expected);

        Matcher marks = MARK.matcher(document.html());
        int index = 0;
        while (marks.find()) {
            index++;
            assertThat(marks.group(1)).isEqualTo(Integer.toString(index));
            String visible = stripTags(marks.group(2)).strip();
            assertThat(visible).matches("(\\[(?:PREENCHER|CONFIRMAR).*\\])|PREENCHER-link");
        }
    }

    private static int count(Pattern pattern, String text) {
        int count = 0;
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static int countLiteral(String text, String literal) {
        int count = 0;
        int from = 0;
        while (true) {
            int found = text.indexOf(literal, from);
            if (found < 0) {
                return count;
            }
            count++;
            from = found + literal.length();
        }
    }

    private static String stripTags(String html) {
        return html
            .replaceAll("<[^>]+>", "")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">");
    }

    private static String read(String classpathLocation) {
        try (var input = LegalDocumentServiceTest.class.getClassLoader().getResourceAsStream(classpathLocation)) {
            if (input == null) {
                throw new IllegalStateException("Missing " + classpathLocation);
            }
            return new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
