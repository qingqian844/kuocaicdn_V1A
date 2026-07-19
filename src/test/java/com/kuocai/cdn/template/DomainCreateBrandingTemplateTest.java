package com.kuocai.cdn.template;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainCreateBrandingTemplateTest {

    @Test
    void serviceAreaHintsUseCurrentWebsiteNameWithoutExposingRouteMembers() throws IOException {
        Path templatePath = Paths.get(
                "src/main/resources/templates/admin/domain/domain-create.html");
        String template = new String(Files.readAllBytes(templatePath), StandardCharsets.UTF_8);

        assertTrue(template.contains("currentWebsiteName=${openAgent and agentConfig != null"));
        assertTrue(template.contains("th:title=\"|${currentWebsiteName}境内线路组|"));
        assertTrue(template.contains("th:title=\"|${currentWebsiteName}境外线路组|"));
        assertTrue(template.contains("th:title=\"|${currentWebsiteName}全球线路组|"));
        assertFalse(template.contains("'线路组：' + overseasRouteDescription"));
        assertFalse(template.contains("'线路组：' + globalRouteDescription"));
    }

    @Test
    void websiteNameExpressionRendersBrandedOverseasRouteGroup() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        String template = "<i "
                + "th:with=\"currentWebsiteName=${openAgent and agentConfig != null "
                + "and !#strings.isEmpty(agentConfig.websiteName) ? agentConfig.websiteName "
                + ": websiteBaseConfig.websiteName}\" "
                + "th:title=\"|${currentWebsiteName}境外线路组|\"></i>";

        Context mainSiteContext = context(false, "萝卜CDN", "代理品牌");
        assertTrue(engine.process(template, mainSiteContext)
                .contains("title=\"萝卜CDN境外线路组\""));

        Context agentContext = context(true, "萝卜CDN", "客户CDN");
        assertTrue(engine.process(template, agentContext)
                .contains("title=\"客户CDN境外线路组\""));
    }

    private Context context(boolean openAgent, String websiteName, String agentName) {
        Context context = new Context();
        context.setVariable("openAgent", openAgent);
        context.setVariable("websiteBaseConfig",
                Collections.singletonMap("websiteName", websiteName));
        context.setVariable("agentConfig",
                Collections.singletonMap("websiteName", agentName));
        return context;
    }
}
