package com.kuocai.cdn.controller.rest;

import com.kuocai.cdn.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SysConfigControllerTest {

    @Test
    void acceptsHttpsLogoUrl() throws Exception {
        assertEquals("https://cdn.example.com/logo.png?v=2",
                SysConfigController.validateWebsiteLogoUrl("https://cdn.example.com/logo.png?v=2"));
    }

    @Test
    void acceptsSameOriginLogoPath() throws Exception {
        assertEquals("/image/logo.png",
                SysConfigController.validateWebsiteLogoUrl("/image/logo.png"));
    }

    @Test
    void rejectsUnsafeLogoProtocols() {
        assertThrows(BusinessException.class,
                () -> SysConfigController.validateWebsiteLogoUrl("javascript:alert(1)"));
        assertThrows(BusinessException.class,
                () -> SysConfigController.validateWebsiteLogoUrl("data:image/svg+xml,test"));
    }

    @Test
    void rejectsProtocolRelativeAndTraversalPaths() {
        assertThrows(BusinessException.class,
                () -> SysConfigController.validateWebsiteLogoUrl("//cdn.example.com/logo.png"));
        assertThrows(BusinessException.class,
                () -> SysConfigController.validateWebsiteLogoUrl("/images/../secret.png"));
    }

}
