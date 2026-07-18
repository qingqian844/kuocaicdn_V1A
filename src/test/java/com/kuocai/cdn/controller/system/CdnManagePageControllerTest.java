package com.kuocai.cdn.controller.system;

import com.kuocai.cdn.entity.SelfHostedNodeGroup;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.service.SelfHostedCdnService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CdnManagePageControllerTest {

    @Test
    void legacySelfHostedRouteUsesEnabledDefaultGroupCoverage() throws Exception {
        CdnManagePageController controller = new CdnManagePageController();
        SelfHostedCdnService selfHostedCdnService = mock(SelfHostedCdnService.class);
        ReflectionTestUtils.setField(controller, "selfHostedCdnService", selfHostedCdnService);
        when(selfHostedCdnService.defaultGroup(CdnRoute.SELF_HOSTED.getCode()))
                .thenReturn(SelfHostedNodeGroup.builder().coverage("overseas").build());

        assertEquals(CdnRoute.SELF_HOSTED_OVERSEAS.getCode(),
                controller.resolveDomainCreateRoute(CdnRoute.SELF_HOSTED.getCode()));
        assertEquals(CdnRoute.TENCENT.getCode(),
                controller.resolveDomainCreateRoute(CdnRoute.TENCENT.getCode()));
    }
}
