package com.kuocai.cdn.controller.base;

import com.kuocai.cdn.vo.WebsiteBaseConfigVo;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BaseControllerLogoTest {

    @Test
    void dashboardUsesLogoFromWebsiteSettings() {
        BaseController controller = new BaseController();
        ReflectionTestUtils.setField(controller, "currentWebsiteBaseConfig",
                WebsiteBaseConfigVo.builder().websiteLogoImg("https://cdn.example.com/logo.png").build());

        String logo = ReflectionTestUtils.invokeMethod(controller, "resolveDashboardLogo", false);

        assertEquals("https://cdn.example.com/logo.png", logo);
    }
}
