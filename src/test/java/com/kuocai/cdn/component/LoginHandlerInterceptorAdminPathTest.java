package com.kuocai.cdn.component;

import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.vo.WebsiteBaseConfigVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginHandlerInterceptorAdminPathTest {
    private final WebsiteBaseConfigVo originalConfig = SystemConfig.websiteBaseConfig;

    @AfterEach
    void restoreConfig() {
        SystemConfig.websiteBaseConfig = originalConfig;
    }

    @Test
    void configuredAdminPathIsAllowedWithoutUserToken() throws Exception {
        SystemConfig.websiteBaseConfig = WebsiteBaseConfigVo.builder().adminPath("secure-admin").build();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/secure-admin");
        request.setRequestURI("/secure-admin");

        assertTrue(new LoginHandlerInterceptor().preHandle(
                request, new MockHttpServletResponse(), new Object()));
    }
}
