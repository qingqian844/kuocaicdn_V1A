package com.kuocai.cdn.controller.login;

import com.kuocai.cdn.async.SmsAsync;
import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.service.InstallationStateService;
import com.kuocai.cdn.service.SysUserService;
import com.kuocai.cdn.vo.WebsiteBaseConfigVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class LoginControllerAdminPathTest {
    private final WebsiteBaseConfigVo originalConfig = SystemConfig.websiteBaseConfig;

    @AfterEach
    void restoreConfig() {
        SystemConfig.websiteBaseConfig = originalConfig;
    }

    @Test
    void customPathReplacesDefaultAdminLoginPath() {
        SystemConfig.websiteBaseConfig = WebsiteBaseConfigVo.builder().adminPath("secure-admin").build();
        LoginController controller = new LoginController(mock(SysUserService.class),
                mock(SmsAsync.class), mock(InstallationStateService.class));

        assertEquals("admin/login", controller.customAdminLogin("secure-admin", new HashMap<>()));
        assertThrows(ResponseStatusException.class,
                () -> controller.adminLogin(new HashMap<>()));
        assertThrows(ResponseStatusException.class,
                () -> controller.customAdminLogin("another-admin", new HashMap<>()));
    }
}
