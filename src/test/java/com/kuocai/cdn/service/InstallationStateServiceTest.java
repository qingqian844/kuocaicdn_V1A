package com.kuocai.cdn.service;

import com.kuocai.cdn.constant.ConfigBizTypeConstants;
import com.kuocai.cdn.vo.InstallationStateVo;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InstallationStateServiceTest {

    @Test
    void completionIsWrittenInsideTransaction() throws Exception {
        assertNotNull(InstallationStateService.class.getMethod("complete", Long.class)
                .getAnnotation(Transactional.class));
    }

    @Test
    void missingStateKeepsExistingInstallationsCompleted() {
        SysConfigService configService = mock(SysConfigService.class);
        when(configService.getPlainConfigContentVo(InstallationStateVo.class,
                ConfigBizTypeConstants.INSTALLATION_STATE)).thenReturn(null);

        InstallationStateService service = new InstallationStateService(configService);

        assertFalse(service.isPending());
        assertEquals(InstallationStateVo.COMPLETED, service.getState().getStatus());
    }

    @Test
    void pendingStatePersistsProgressUpdates() {
        SysConfigService configService = mock(SysConfigService.class);
        InstallationStateVo state = InstallationStateVo.pending();
        when(configService.getPlainConfigContentVo(InstallationStateVo.class,
                ConfigBizTypeConstants.INSTALLATION_STATE)).thenReturn(state);
        InstallationStateService service = new InstallationStateService(configService);

        service.update(value -> {
            value.setAdminConfigured(true);
            value.setCurrentStep(3);
        }, 1L);

        assertTrue(service.isPending());
        assertTrue(service.getState().getAdminConfigured());
        assertEquals(3, service.getState().getCurrentStep());
        verify(configService).savePlainConfig(eq(state), eq(ConfigBizTypeConstants.INSTALLATION_STATE), eq(1L));
    }
}
