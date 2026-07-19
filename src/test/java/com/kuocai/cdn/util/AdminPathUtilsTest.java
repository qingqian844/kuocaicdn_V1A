package com.kuocai.cdn.util;

import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.vo.WebsiteBaseConfigVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminPathUtilsTest {
    private final WebsiteBaseConfigVo originalConfig = SystemConfig.websiteBaseConfig;

    @AfterEach
    void restoreConfig() {
        SystemConfig.websiteBaseConfig = originalConfig;
    }

    @Test
    void normalizesValidAdminPath() throws Exception {
        assertEquals("secure-admin_2", AdminPathUtils.normalize("/Secure-Admin_2/"));
        assertEquals(AdminPathUtils.DEFAULT_PATH, AdminPathUtils.normalize(""));
    }

    @Test
    void rejectsUnsafeOrReservedAdminPaths() {
        assertThrows(BusinessException.class, () -> AdminPathUtils.normalize("ab"));
        assertThrows(BusinessException.class, () -> AdminPathUtils.normalize("admin/path"));
        assertThrows(BusinessException.class, () -> AdminPathUtils.normalize("dashboard"));
    }

    @Test
    void onlyConfiguredSingleSegmentPathIsRecognized() {
        SystemConfig.websiteBaseConfig = WebsiteBaseConfigVo.builder().adminPath("secure-admin").build();

        assertTrue(AdminPathUtils.isConfiguredRequestPath("/secure-admin"));
        assertFalse(AdminPathUtils.isConfiguredRequestPath("/kuocaiadmin"));
        assertFalse(AdminPathUtils.isConfiguredRequestPath("/secure-admin/extra"));
    }
}
