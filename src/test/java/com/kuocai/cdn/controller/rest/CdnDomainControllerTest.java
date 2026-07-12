package com.kuocai.cdn.controller.rest;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CdnDomainControllerTest {

    private final CdnDomainController controller = new CdnDomainController(null, null, null, null, null);

    @Test
    void tencentCdnRecordNotVerifiedRequiresDomainVerifyModal() {
        Boolean result = ReflectionTestUtils.invokeMethod(
                controller,
                "isDomainVerifyRequired",
                "创建时发生错误，域名解析未进行验证 (errorCode=UnauthorizedOperation.CdnDomainRecordNotVerified, requestId=req-test)"
        );

        assertTrue(result);
    }

    @Test
    void genericUnauthorizedStillDoesNotOpenDomainVerifyModal() {
        Boolean result = ReflectionTestUtils.invokeMethod(
                controller,
                "isDomainVerifyRequired",
                "UnauthorizedOperation.CdnCamUnauthorized"
        );

        assertFalse(result);
    }
}
