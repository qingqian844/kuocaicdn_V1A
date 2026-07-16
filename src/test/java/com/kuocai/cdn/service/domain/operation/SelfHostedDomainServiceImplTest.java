package com.kuocai.cdn.service.domain.operation;

import com.kuocai.cdn.api.tencent.dns.dto.CreateRecordDTO;
import com.kuocai.cdn.service.SelfHostedCdnService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SelfHostedDomainServiceImplTest {

    @Test
    void customerCnameUsesDnsPlanCompatibleTtl() {
        CreateRecordDTO record = SelfHostedDomainServiceImpl.buildCustomerCnameRecord(
                "cdn.example.com", "kuocaidns.com", "edge.kuocaidns.com");

        assertEquals("kuocaidns.com", record.getDomain());
        assertEquals("edge.kuocaidns.com", record.getValue());
        assertEquals("CNAME", record.getRecordType());
        assertEquals(SelfHostedCdnService.DNS_RECORD_TTL_SECONDS, record.getTTL());
        assertEquals(600L, record.getTTL());
    }
}
