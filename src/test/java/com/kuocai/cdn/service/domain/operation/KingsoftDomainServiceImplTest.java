package com.kuocai.cdn.service.domain.operation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KingsoftDomainServiceImplTest {

    @Test
    void lockedDomainBecomesTerminalFailureWithAnActionableReason() {
        assertEquals("configure_failed", KingsoftDomainServiceImpl.mapKingsoftStatusToSystem("locked"));
        assertEquals("域名已被金山云锁定，请联系管理员或金山云处理",
                KingsoftDomainServiceImpl.kingsoftFailureReason("locked"));
    }

    @Test
    void icpStatesAndUnknownStatesDoNotStayConfiguringForever() {
        assertEquals("configuring", KingsoftDomainServiceImpl.mapKingsoftStatusToSystem("icp_checking"));
        assertEquals("configure_failed", KingsoftDomainServiceImpl.mapKingsoftStatusToSystem("icp_check_failed"));
        assertEquals("configure_failed", KingsoftDomainServiceImpl.mapKingsoftStatusToSystem("unexpected"));
        assertNull(KingsoftDomainServiceImpl.kingsoftFailureReason("online"));
    }
}
