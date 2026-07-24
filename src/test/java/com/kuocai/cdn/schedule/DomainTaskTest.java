package com.kuocai.cdn.schedule;

import com.kuocai.cdn.api.DomainBasicInfo;
import com.kuocai.cdn.api.DomainConfig;
import com.kuocai.cdn.async.SmsAsync;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.service.CdnDomainService;
import com.kuocai.cdn.service.CdnDomainStatisticsService;
import com.kuocai.cdn.service.EdgeOneDomainQuotaService;
import com.kuocai.cdn.service.SysUserService;
import com.kuocai.cdn.service.domain.operation.AliyunDomainServiceImpl;
import com.kuocai.cdn.service.domain.operation.ICdnPlatformService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DomainTaskTest {

    @Test
    void oneProviderRuntimeFailureDoesNotAbortTheRestOfTheBatch() throws Exception {
        ICdnPlatformService platform = mock(ICdnPlatformService.class);
        when(platform.getDomainConfig("missing.example.com"))
                .thenThrow(new RuntimeException("NoSuchDomain: invalid domain"));
        when(platform.getDomainConfig("ready.example.com"))
                .thenReturn(DomainConfig.builder()
                        .domainBasicInfo(DomainBasicInfo.builder()
                                .domainName("ready.example.com")
                                .domainStatus("online")
                                .businessType("web")
                                .serviceArea("mainland_china")
                                .build())
                        .build());

        DomainTask task = task(platform);
        CdnDomainService domainService = mock(CdnDomainService.class);
        ReflectionTestUtils.setField(task, "cdnDomainService", domainService);
        ReflectionTestUtils.setField(task, "edgeOneDomainQuotaService", mock(EdgeOneDomainQuotaService.class));

        Date staleTime = new Date(System.currentTimeMillis() - 10 * 60 * 1000L);
        CdnDomain missing = CdnDomain.builder()
                .id(1L)
                .domainName("missing.example.com")
                .domainStatus("configuring")
                .route("baidu")
                .createTime(staleTime)
                .updateTime(staleTime)
                .build();
        CdnDomain ready = CdnDomain.builder()
                .id(2L)
                .domainName("ready.example.com")
                .domainStatus("configuring")
                .route("kingsoft")
                .createTime(staleTime)
                .updateTime(staleTime)
                .build();

        task.syncConfiguringDomainBatch(Arrays.asList(missing, ready));

        assertEquals("configure_failed", missing.getDomainStatus());
        assertTrue(missing.getFailureReason().contains("百度云"));
        assertEquals("online", ready.getDomainStatus());
        verify(domainService).save(missing);
        verify(domainService).save(ready);
    }

    private DomainTask task(ICdnPlatformService platform) {
        return new DomainTask(
                mock(AliyunDomainServiceImpl.class),
                mock(SmsAsync.class),
                mock(CdnDomainStatisticsService.class),
                mock(SysUserService.class),
                Runnable::run) {
            @Override
            ICdnPlatformService getCdnPlatform(String route) {
                return platform;
            }
        };
    }
}
