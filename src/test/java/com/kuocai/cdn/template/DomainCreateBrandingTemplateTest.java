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
    void mainlandServiceAreaHintUsesCurrentWebsiteName() throws IOException {
        Path templatePath = Paths.get(
                "src/main/resources/templates/admin/domain/domain-create.html");
        String template = new String(Files.readAllBytes(templatePath), StandardCharsets.UTF_8);

        assertTrue(template.contains("currentWebsiteName=${openAgent and agentConfig != null"));
        assertTrue(template.contains("th:title=\"|所有用户均使用${currentWebsiteName}中国境内节点"));
        assertFalse(template.contains("所有用户均使用括彩云中国境内节点"));
    }

    @Test
    void websiteNameExpressionRendersMainSiteAndAgentBrands() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        String template = "<i "
                + "th:with=\"currentWebsiteName=${openAgent and agentConfig != null "
                + "and !#strings.isEmpty(agentConfig.websiteName) ? agentConfig.websiteName "
                + ": websiteBaseConfig.websiteName}\" "
                + "th:title=\"|所有用户均使用${currentWebsiteName}中国境内节点|\"></i>";

        Context mainSiteContext = context(false, "萝卜CDN", "代理品牌");
        assertTrue(engine.process(template, mainSiteContext)
                .contains("title=\"所有用户均使用萝卜CDN中国境内节点\""));

        Context agentContext = context(true, "萝卜CDN", "客户CDN");
        assertTrue(engine.process(template, agentContext)
                .contains("title=\"所有用户均使用客户CDN中国境内节点\""));
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
