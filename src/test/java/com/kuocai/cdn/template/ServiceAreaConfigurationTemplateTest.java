package com.kuocai.cdn.template;

import com.kuocai.cdn.service.CdnServiceAreaPolicyService;
import com.kuocai.cdn.vo.CdnServiceAreaOptionVo;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceAreaConfigurationTemplateTest {

    @Test
    void websiteSettingsContainsRouteAreaMatrix() throws IOException {
        String template = read("src/main/resources/templates/admin/settings/website-setting.html");

        assertTrue(template.contains("overseasEnabledTarget"));
        assertTrue(template.contains("globalEnabledTarget"));
        assertFalse(template.contains("平台账号标识"));
        assertTrue(template.contains("${serviceAreaOptions}"));
        assertTrue(template.contains("option.fixedArea == 'outside_mainland_china'"));
        assertTrue(template.contains("option.fixedArea == 'global'"));
        assertTrue(template.contains("加速区域线路"));
    }

    @Test
    void domainCreateUsesGlobalRouteAreaFlags() throws IOException {
        String template = read("src/main/resources/templates/admin/domain/domain-create.html");

        assertTrue(template.contains("${allowOverseas}"));
        assertTrue(template.contains("${allowGlobal}"));
        assertFalse(template.contains("${enableOverseas"));
        assertFalse(template.contains("${enableGlobal"));
    }

    @Test
    void websiteSettingsExposeAvatarAndAdminPathWithoutMonthlyBenefit() throws IOException {
        String template = read("src/main/resources/templates/admin/settings/website-setting.html");

        assertTrue(template.contains("id=\"updateDefaultAvatarFile\""));
        assertTrue(template.contains("id=\"adminPath\""));
        assertFalse(template.contains("monthlyBenefitSetting"));
        assertFalse(template.contains("每月福利赠送流量（GB）"));
    }

    @Test
    void websiteSettingsSaveAlwaysShowsProgressAndKeepsFailureVisible() throws IOException {
        String template = read("src/main/resources/templates/admin/settings/website-setting.html");
        String script = read("src/main/resources/static/common/custom_cmo.js");
        String uploadScript = read("src/main/resources/static/common/custom.js");
        String saveFunction = between(script, "async function saveWebsiteBaseConfig() {",
                "function appendWebsiteLogoFormData");

        assertTrue(template.contains("id=\"saveWebsiteBaseConfigButton\""));
        assertTrue(saveFunction.contains("正在保存"));
        assertTrue(saveFunction.contains("data && data.code === 'SUCCESS'"));
        assertFalse(saveFunction.contains("setTimeout(reload, 1000)"));
        assertTrue(uploadScript.contains("resolve({"));
        assertTrue(uploadScript.contains("请求超时，请检查服务器状态后重试"));
    }

    @Test
    void selfHostedNodeSettingsExposeDetectedDiskAndLowFrequencyCleanup() throws IOException {
        String template = read("src/main/resources/templates/admin/settings/self-hosted-node.html");

        assertTrue(template.contains("id=\"nodeCacheDisk\""));
        assertTrue(template.contains("id=\"nodeCacheMaxSizeGb\""));
        assertTrue(template.contains("id=\"nodeCacheCleanupAgeDays\""));
        assertTrue(template.contains("id=\"nodeCacheCleanupMinHits\""));
        assertTrue(template.contains("detectedDisks"));
        assertTrue(template.contains("cacheDiskMount"));
    }

    @Test
    void dashboardUsesConfiguredAdminPathForSessionExit() throws IOException {
        String dashboard = read("src/main/resources/templates/common/common-dashboard.html");
        String commonJs = read("src/main/resources/static/common/custom_ceo.js");

        assertTrue(dashboard.contains("window.kuocaiAdminPath"));
        assertTrue(dashboard.contains("websiteBaseConfig.adminPath"));
        assertTrue(commonJs.contains("window.kuocaiAdminPath || 'kuocaiadmin'"));
        assertTrue(commonJs.contains("window.kuocaiLoginRole === 'admin'"));
    }

    @Test
    void dashboardOmitsMonthlyBenefit() throws IOException {
        String dashboard = read("src/main/resources/templates/common/common-dashboard.html");

        assertFalse(dashboard.contains("websiteBaseConfig.monthGiftGb > 0"));
        assertFalse(dashboard.contains("claimMonthlyFreePackage"));
    }

    @Test
    void vendorAndSelfHostedRowsRenderWithoutAccountIdentifiers() throws IOException {
        String template = read("src/main/resources/templates/admin/settings/website-setting.html");
        String row = between(template,
                "<tr th:each=\"option : ${serviceAreaOptions}\">", "</tr>") + "</tr>";
        Context context = new Context();
        context.setVariable("serviceAreaOptions", Arrays.asList(
                CdnServiceAreaOptionVo.builder()
                        .targetKey("route:tencent_edgeone")
                        .routeName("腾讯云 EdgeOne")
                        .selectable(true)
                        .overseasEnabled(true)
                        .globalEnabled(false)
                        .build(),
                CdnServiceAreaOptionVo.builder()
                        .targetKey("route:self_hosted_overseas")
                        .routeName("海外自建 CDN")
                        .selectable(true)
                        .fixedArea(CdnServiceAreaPolicyService.OVERSEAS)
                        .overseasEnabled(true)
                        .globalEnabled(false)
                        .build()));

        String rendered = templateEngine().process(row, context);

        assertTrue(rendered.contains("腾讯云 EdgeOne"));
        assertTrue(rendered.contains("海外自建 CDN"));
        assertFalse(rendered.contains("ID 101"));
    }

    @Test
    void userProductConfigurationNoLongerContainsLegacyAreaSwitches() throws IOException {
        String template = read("src/main/resources/templates/admin/product/user-price-list.html");

        assertFalse(template.contains("name=\"enableOverseas\""));
        assertFalse(template.contains("name=\"enableGlobal\""));
        assertFalse(template.contains("row.user.enableOverseas"));
        assertFalse(template.contains("row.user.enableGlobal"));
        String columns = between(template, "columns: [", "columnDefs:");
        String headers = between(template, "<thead class=\"thead-light\">", "</thead>");
        assertEquals(11, count(columns, Pattern.compile("\\bdata\\s*:")));
        assertEquals(11, count(headers, Pattern.compile("<th(?:\\s|>)")));
    }

    private String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    private String between(String value, String start, String end) {
        int from = value.indexOf(start);
        int to = value.indexOf(end, from + start.length());
        return value.substring(from, to);
    }

    private int count(String value, Pattern pattern) {
        int total = 0;
        Matcher matcher = pattern.matcher(value);
        while (matcher.find()) {
            total++;
        }
        return total;
    }

    private TemplateEngine templateEngine() {
        TemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver());
        return engine;
    }
}
