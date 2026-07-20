package com.kuocai.cdn.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdgeOneFailureReasonFormatterTest {

    @Test
    void unfiledDomainUsesOverseasAccelerationHint() {
        String reason = EdgeOneFailureReasonFormatter.format(
                "The domain has no ICP filing",
                "InvalidParameter.NoIcp",
                "request-icp",
                "mainland_china");

        assertTrue(reason.contains("未完成 ICP 备案"));
        assertTrue(reason.contains("请使用海外加速"));
        assertTrue(reason.contains("InvalidParameter.NoIcp"));
        assertTrue(reason.contains("request-icp"));
    }

    @Test
    void domainOccupiedByAnotherAccountUsesRecoveryHint() {
        String reason = EdgeOneFailureReasonFormatter.format(
                "Zone is already used by other account",
                "ResourceInUse.Zone",
                "request-owner",
                "outside_mainland_china");

        assertTrue(reason.contains("其他账号或现有站点占用"));
        assertTrue(reason.contains("域名取回"));
        assertTrue(reason.contains("ResourceInUse.Zone"));
        assertTrue(reason.contains("request-owner"));
    }

    @Test
    void genericTencentFailureKeepsErrorCodeAndRequestId() {
        String reason = EdgeOneFailureReasonFormatter.format(
                "upstream service is busy",
                "InternalError",
                "request-generic",
                "outside_mainland_china");

        assertTrue(reason.contains("upstream service is busy"));
        assertTrue(reason.contains("InternalError"));
        assertTrue(reason.contains("request-generic"));
    }

    @Test
    void wrappedBusinessFailureExtractsTencentDiagnosticsFromMessage() {
        String reason = EdgeOneFailureReasonFormatter.format(
                "创建站点失败：InvalidParameter.NoIcp：domain is not filed，RequestId：request-wrapped",
                null,
                null,
                "mainland_china");

        assertTrue(reason.contains("InvalidParameter.NoIcp"));
        assertTrue(reason.contains("request-wrapped"));
        assertTrue(reason.contains("请使用海外加速"));
    }

    @Test
    void overseasFallbackDoesNotClaimIcpIsRequired() {
        String reason = EdgeOneFailureReasonFormatter.defaultReason("outside_mainland_china");

        assertFalse(reason.contains("备案"));
        assertTrue(reason.contains("其他 EdgeOne 账号"));
    }

    @Test
    void persistedReasonIsBoundedByDatabaseColumnLength() {
        String reason = EdgeOneFailureReasonFormatter.format(
                repeat('x', 1500), null, null, "mainland_china");

        assertTrue(reason.length() <= EdgeOneFailureReasonFormatter.MAX_REASON_LENGTH);
    }

    private String repeat(char value, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            result.append(value);
        }
        return result.toString();
    }
}
