package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.kuocai.cdn.constant.StatisticsType.RESOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CdnDomainStatisticsServiceTest {

    @Test
    void includesVolcengineDomainWithoutDomainIdForStatistics() {
        CdnDomain domain = CdnDomain.builder()
                .route(CdnRoute.VOLCENGINE.getCode())
                .domainName("huoshan.example.com")
                .build();

        assertTrue(CdnDomainStatisticsService.shouldIncludeStatisticsDomain(domain, CdnRoute.VOLCENGINE.getCode()));
    }

    @Test
    void excludesDomainWhenRouteDoesNotMatch() {
        CdnDomain domain = CdnDomain.builder()
                .route(CdnRoute.VOLCENGINE.getCode())
                .domainName("huoshan.example.com")
                .build();

        assertFalse(CdnDomainStatisticsService.shouldIncludeStatisticsDomain(domain, CdnRoute.TENCENT.getCode()));
    }

    @Test
    void displaysCloudTrafficInDecimalGbWithoutChangingRawBytes() {
        long accessBytes = 603_750_000_000L;
        long originBytes = 505_920_000_000L;
        JSONObject resource = JSONObject.parseObject("{\"resource_summary\":{\"bw\":0,\"bs_bw\":0,\"flux\":"
                + accessBytes + ",\"bs_flux\":" + originBytes
                + "},\"resource_detail\":{\"bw\":[0],\"bs_bw\":[0],\"flux\":["
                + accessBytes + "],\"bs_flux\":[" + originBytes + "]}}");
        JSONObject result = new JSONObject();
        result.put(RESOURCE, resource);

        JSONObject converted = new CdnDomainStatisticsService().calcVirtualData(1L, result, RESOURCE)
                .getJSONObject(RESOURCE);
        JSONObject summary = converted.getJSONObject("resource_summary");
        JSONObject detail = converted.getJSONObject("resource_detail");

        assertEquals("603.75GB", summary.getString("flux"));
        assertEquals("505.92GB", summary.getString("bs_flux"));
        assertEquals(accessBytes, summary.getLongValue("access_flux_byte"));
        assertEquals(accessBytes, summary.getLongValue("flux_byte"));
        assertEquals("GB", detail.getJSONObject("flux").getString("unit"));
        assertEquals(new BigDecimal("603.75"), detail.getJSONObject("flux").getJSONArray("data").getBigDecimal(0));
    }
}
