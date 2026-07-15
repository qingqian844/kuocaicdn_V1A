package com.kuocai.cdn.template;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminThemeTemplateTest {

    @Test
    void hiddenTemplatesMustDeclareThemeConfiguration() throws IOException {
        Path templateRoot = Paths.get("src/main/resources/templates");
        List<String> invalidTemplates = new ArrayList<>();

        try (Stream<Path> files = Files.walk(templateRoot)) {
            files.filter(path -> path.toString().endsWith(".html"))
                    .forEach(path -> inspectTemplate(path, invalidTemplates));
        }

        assertTrue(invalidTemplates.isEmpty(),
                "Templates hide the page without defining window.hs_config: " + invalidTemplates);
    }

    @Test
    void themeLoaderMustRevealThePageWhenStylesheetLoadingFails() throws IOException {
        Path scriptPath = Paths.get(
                "src/main/resources/static/dashboard/assets/js/hs.theme-appearance.js");
        String script = new String(Files.readAllBytes(scriptPath), StandardCharsets.UTF_8);

        assertTrue(script.contains("$styleNode.addEventListener('error', settleOnce"));
        assertTrue(script.contains("window.setTimeout(finish, 3000)"));
        assertTrue(script.contains("document.body.style.opacity = '1'"));
    }

    private void inspectTemplate(Path path, List<String> invalidTemplates) {
        try {
            String template = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            boolean hidesBody = template.contains("data-hs-appearance-onload-styles")
                    && template.matches("(?s).*opacity\\s*:\\s*0.*");
            boolean loadsTheme = template.contains("hs.theme-appearance.js");
            boolean declaresConfig = template.contains("window.hs_config =");
            if (hidesBody && loadsTheme && !declaresConfig) {
                invalidTemplates.add(path.toString());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to inspect template " + path, e);
        }
    }
}
