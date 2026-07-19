package com.kuocai.cdn.service.domain.operation;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.DomainConfig;
import com.kuocai.cdn.api.tencent.dns.dto.CreateRecordDTO;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.SelfHostedDomainConfig;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.SelfHostedCdnService;
import com.kuocai.cdn.service.SysUserService;
import com.kuocai.cdn.vo.DomainOriginSettingVo;
import com.kuocai.cdn.vo.IgnoreQueryStringDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void cacheParameterFilterDoesNotOverwriteExistingCacheRules() throws Exception {
        SelfHostedCdnService cdnService = mock(SelfHostedCdnService.class);
        SelfHostedDomainConfig config = SelfHostedDomainConfig.builder().id(2L).cdnDomainId(1L)
                .cacheConfigJson("{\"defaultTtl\":3600,\"cacheRules\":[{\"match_type\":\"all\",\"ttl\":60}]}" ).build();
        when(cdnService.getDomainConfig(1L)).thenReturn(config);
        SelfHostedDomainServiceImpl service = new SelfHostedDomainServiceImpl(cdnService, mock(SysUserService.class));
        IgnoreQueryStringDTO filter = new IgnoreQueryStringDTO();
        filter.setEnable("on");
        filter.setType("allow");
        filter.setHashKeyArgs("id,token");

        service.saveIgnoreQueryString(CdnDomain.builder().id(1L).build(), filter);

        JSONObject saved = JSON.parseObject(config.getCacheConfigJson());
        assertEquals(3600, saved.getIntValue("defaultTtl"));
        assertEquals(1, saved.getJSONArray("cacheRules").size());
        assertEquals("id,token", saved.getJSONObject("ignoreQueryString").getString("hashKeyArgs"));
        verify(cdnService).updateDomainConfig(config);
    }

    @Test
    void originSwitchDoesNotOverwriteStandbyOrOtherOriginSettings() throws Exception {
        SelfHostedCdnService cdnService = mock(SelfHostedCdnService.class);
        SelfHostedDomainConfig config = SelfHostedDomainConfig.builder().id(2L).cdnDomainId(1L)
                .originConfigJson("{\"standby\":{\"ipOrDomain\":\"192.0.2.20\"},\"etagStatus\":\"on\"}").build();
        when(cdnService.getDomainConfig(1L)).thenReturn(config);
        SelfHostedDomainServiceImpl service = new SelfHostedDomainServiceImpl(cdnService, mock(SysUserService.class));

        service.saveRangeSwitch(CdnDomain.builder().id(1L).build(),
                DomainOriginSettingVo.builder().status("on").build());

        JSONObject saved = JSON.parseObject(config.getOriginConfigJson());
        assertEquals("on", saved.getString("rangeStatus"));
        assertEquals("on", saved.getString("etagStatus"));
        assertNotNull(saved.getJSONObject("standby"));
        assertEquals("192.0.2.20", saved.getJSONObject("standby").getString("ipOrDomain"));
        verify(cdnService).updateDomainConfig(config);
    }

    @Test
    void explicitSelfHostedRouteRejectsMismatchedArea() {
        SelfHostedDomainServiceImpl service = new SelfHostedDomainServiceImpl(
                mock(SelfHostedCdnService.class), mock(SysUserService.class));

        assertThrows(BusinessException.class, () -> service.createForRoute(
                "self_hosted_overseas", 1L, "cdn.example.com", "web", "mainland_china",
                "ipaddr", "192.0.2.10", "http", 80, 443,
                "cdn.example.com", 100));
    }

    @Test
    void multiCdnChildReadsConfigurationByParentId() throws Exception {
        SelfHostedCdnService cdnService = mock(SelfHostedCdnService.class);
        SelfHostedDomainConfig config = SelfHostedDomainConfig.builder()
                .id(2L).cdnDomainId(1L).originType("ipaddr").originAddress("192.0.2.10")
                .originProtocol("http").httpPort(80).httpsPort(443).originHost("cdn.example.com")
                .status("enabled").build();
        when(cdnService.getDomainConfig(1L)).thenReturn(config);
        when(cdnService.isDomainConfigurationApplied(1L)).thenReturn(true);
        SelfHostedDomainServiceImpl service = new SelfHostedDomainServiceImpl(
                cdnService, mock(SysUserService.class));
        CdnDomain child = CdnDomain.builder().id(1L).domainName("cdn.example.com")
                .route("self_hosted_mainland").domainStatus("configuring")
                .businessType("web").serviceArea("mainland_china").build();

        DomainConfig result = service.getDomainConfig(child);

        assertEquals("online", result.getDomainBasicInfo().getDomainStatus());
        assertEquals("mainland_china", result.getDomainBasicInfo().getServiceArea());
    }
}
