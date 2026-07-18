package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuocai.cdn.dto.SelfHostedNodeSaveRequest;
import com.kuocai.cdn.dto.SelfHostedHeartbeatRequest;
import com.kuocai.cdn.dao.CdnDomainDao;
import com.kuocai.cdn.dao.SelfHostedCacheJobDao;
import com.kuocai.cdn.dao.SelfHostedCacheJobNodeDao;
import com.kuocai.cdn.dao.SelfHostedDomainConfigDao;
import com.kuocai.cdn.dao.SelfHostedGroupNodeDao;
import com.kuocai.cdn.dao.SelfHostedNodeDao;
import com.kuocai.cdn.dao.SelfHostedNodeGroupDao;
import com.kuocai.cdn.dao.SelfHostedPortForwardDao;
import com.kuocai.cdn.entity.SelfHostedNode;
import com.kuocai.cdn.entity.SelfHostedGroupNode;
import com.kuocai.cdn.entity.SelfHostedDomainConfig;
import com.kuocai.cdn.entity.SelfHostedNodeGroup;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.enumeration.domainmerage.route.CdnCacheSettingRoute;
import com.kuocai.cdn.enumeration.domainmerage.route.CdnOperationRoute;
import com.kuocai.cdn.enumeration.domainmerage.route.CdnStatisticsRoute;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

class SelfHostedCdnRouteTest {

    @Test
    void selfHostedRouteIsRegisteredAcrossAllCapabilities() {
        String route = CdnRoute.SELF_HOSTED.getCode();
        assertEquals("self_hosted", route);
        assertEquals(route, CdnOperationRoute.convert(route).getRoute());
        assertEquals(route, CdnCacheSettingRoute.convert(route).getRoute());
        assertEquals(route, CdnStatisticsRoute.convert(route).getRoute());
    }

    @Test
    void selfHostedProductRoutesMapToIndependentAreasAndCapabilities() {
        assertSelfHostedProductRoute(CdnRoute.SELF_HOSTED_MAINLAND.getCode(),
                "mainland", "mainland_china", "国内自建 CDN");
        assertSelfHostedProductRoute(CdnRoute.SELF_HOSTED_OVERSEAS.getCode(),
                "overseas", "outside_mainland_china", "海外自建 CDN");
        assertSelfHostedProductRoute(CdnRoute.SELF_HOSTED_GLOBAL.getCode(),
                "global", "global", "全球自建 CDN");
        assertTrue(CdnRoute.isSelfHosted(CdnRoute.SELF_HOSTED.getCode()));
        assertEquals(CdnRoute.SELF_HOSTED_OVERSEAS.getCode(),
                CdnRoute.selfHostedRouteForServiceArea("outside_mainland_china"));
        assertEquals(CdnRoute.SELF_HOSTED_OVERSEAS.getCode(),
                CdnRoute.selfHostedRouteForCoverage("overseas"));
        assertEquals(CdnRoute.SELF_HOSTED_OVERSEAS.getCode(),
                CdnRoute.resolveSelfHostedCreateRoute(
                        CdnRoute.SELF_HOSTED.getCode(), "outside_mainland_china"));
        assertEquals(CdnRoute.SELF_HOSTED_MAINLAND.getCode(),
                CdnRoute.resolveSelfHostedCreateRoute(
                        CdnRoute.SELF_HOSTED_MAINLAND.getCode(), "outside_mainland_china"));
    }

    @Test
    void cacheControllerDispatchesSelfHostedPreheatAndRefreshTasks() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/kuocai/cdn/controller/rest/CdnDomainCacheController.java")),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("selfHostedUrlList.add(url)"));
        assertTrue(source.contains("CACHE_PREHEATING_KEY, selfHostedUrlList"));
        assertTrue(source.contains("fileType, selfHostedUrlList"));
        assertTrue(source.contains("CdnRoute.SELF_HOSTED.getCode()"));
    }

    @Test
    void sshPasswordCanBeAcceptedButIsNeverSerializedIntoLogs() throws Exception {
        SelfHostedNodeSaveRequest request = new ObjectMapper().readValue(
                "{\"nodeName\":\"edge-1\",\"sshPassword\":\"secret-value\"}",
                SelfHostedNodeSaveRequest.class);
        assertEquals("secret-value", request.getSshPassword());
        assertFalse(JSON.toJSONString(request).contains("secret-value"));
        assertFalse(new ObjectMapper().writeValueAsString(request).contains("secret-value"));
    }

    @Test
    void nodeListNeverExposesEncryptedPasswordOrAgentTokenHash() {
        SelfHostedNodeDao nodeDao = mock(SelfHostedNodeDao.class);
        SelfHostedNodeGroupDao groupDao = mock(SelfHostedNodeGroupDao.class);
        SelfHostedGroupNodeDao groupNodeDao = mock(SelfHostedGroupNodeDao.class);
        when(nodeDao.selectList(any())).thenReturn(Collections.singletonList(SelfHostedNode.builder()
                .id(1L).nodeName("edge-1").host("192.0.2.10").sshPort(22)
                .sshPasswordCipher("encrypted-password").agentTokenHash("token-hash")
                .enabled(1).status("pending").build()));
        when(groupNodeDao.selectList(any())).thenReturn(java.util.Arrays.asList(
                SelfHostedGroupNode.builder().groupId(11L).nodeId(1L).build(),
                SelfHostedGroupNode.builder().groupId(12L).nodeId(1L).build()));
        when(groupDao.selectById(11L)).thenReturn(SelfHostedNodeGroup.builder()
                .id(11L).groupName("海外节点组").coverage("overseas").build());
        when(groupDao.selectById(12L)).thenReturn(SelfHostedNodeGroup.builder()
                .id(12L).groupName("全球节点组").coverage("global").build());

        SelfHostedCdnService service = new SelfHostedCdnService(nodeDao,
                groupDao, groupNodeDao,
                mock(SelfHostedDomainConfigDao.class), mock(SelfHostedCacheJobDao.class),
                mock(SelfHostedCacheJobNodeDao.class), mock(CdnDomainDao.class),
                mock(SelfHostedPortForwardDao.class));

        String json = service.listNodeViews().get(0).toJSONString();
        assertFalse(json.contains("encrypted-password"));
        assertFalse(json.contains("token-hash"));
        assertFalse(json.contains("sshPasswordCipher"));
        assertFalse(json.contains("agentTokenHash"));
        assertTrue(json.contains("海外节点组、全球节点组"));
        assertTrue(json.contains("\"groupIds\":[11,12]"));
    }

    @Test
    void agentConfigNeverIncludesEncryptedCertificateColumns() {
        SelfHostedGroupNodeDao groupNodeDao = mock(SelfHostedGroupNodeDao.class);
        SelfHostedDomainConfigDao domainConfigDao = mock(SelfHostedDomainConfigDao.class);
        CdnDomainDao domainDao = mock(CdnDomainDao.class);
        when(groupNodeDao.selectList(any())).thenReturn(Collections.singletonList(
                SelfHostedGroupNode.builder().groupId(2L).nodeId(1L).build()));
        when(domainConfigDao.selectList(any())).thenReturn(Collections.singletonList(
                SelfHostedDomainConfig.builder().id(3L).cdnDomainId(4L).nodeGroupId(2L)
                        .certificateCipher("certificate-ciphertext")
                        .privateKeyCipher("private-key-ciphertext")
                        .accessConfigCipher("access-config-ciphertext").status("enabled").build()));
        when(domainDao.selectById(4L)).thenReturn(CdnDomain.builder().id(4L).domainName("cdn.example.com")
                .route(CdnRoute.SELF_HOSTED_OVERSEAS.getCode()).domainStatus("online").build());
        SelfHostedCdnService service = new SelfHostedCdnService(mock(SelfHostedNodeDao.class),
                mock(SelfHostedNodeGroupDao.class), groupNodeDao, domainConfigDao,
                mock(SelfHostedCacheJobDao.class), mock(SelfHostedCacheJobNodeDao.class), domainDao,
                mock(SelfHostedPortForwardDao.class));

        String json = service.desiredConfig(SelfHostedNode.builder().id(1L).desiredConfigVersion(5L).build()).toJSONString();
        assertFalse(json.contains("certificate-ciphertext"));
        assertFalse(json.contains("private-key-ciphertext"));
        assertFalse(json.contains("certificateCipher"));
        assertFalse(json.contains("privateKeyCipher"));
        assertFalse(json.contains("access-config-ciphertext"));
        assertFalse(json.contains("accessConfigCipher"));
        assertTrue(json.contains("accessConfigJson"));
    }

    @Test
    void enablingHttpsWithoutNewPemKeepsTheStoredCertificate() throws Exception {
        SelfHostedDomainConfigDao domainConfigDao = mock(SelfHostedDomainConfigDao.class);
        SelfHostedGroupNodeDao groupNodeDao = mock(SelfHostedGroupNodeDao.class);
        when(groupNodeDao.selectList(any())).thenReturn(Collections.emptyList());
        SelfHostedCdnService service = new SelfHostedCdnService(mock(SelfHostedNodeDao.class),
                mock(SelfHostedNodeGroupDao.class), groupNodeDao, domainConfigDao,
                mock(SelfHostedCacheJobDao.class), mock(SelfHostedCacheJobNodeDao.class), mock(CdnDomainDao.class),
                mock(SelfHostedPortForwardDao.class));
        SelfHostedDomainConfig config = SelfHostedDomainConfig.builder().id(3L).nodeGroupId(2L)
                .certificateCipher("stored-certificate").privateKeyCipher("stored-private-key")
                .httpsEnabled(1).desiredConfigVersion(1L).build();

        service.saveCertificate(config, true, "", "", "off");

        assertEquals("stored-certificate", config.getCertificateCipher());
        assertEquals("stored-private-key", config.getPrivateKeyCipher());
    }

    @Test
    void heartbeatDoesNotDispatchCacheJobsBeforeLatestConfigIsApplied() {
        SelfHostedCacheJobNodeDao cacheJobNodeDao = mock(SelfHostedCacheJobNodeDao.class);
        SelfHostedCdnService service = new SelfHostedCdnService(mock(SelfHostedNodeDao.class),
                mock(SelfHostedNodeGroupDao.class), mock(SelfHostedGroupNodeDao.class),
                mock(SelfHostedDomainConfigDao.class), mock(SelfHostedCacheJobDao.class),
                cacheJobNodeDao, mock(CdnDomainDao.class), mock(SelfHostedPortForwardDao.class));
        SelfHostedNode node = SelfHostedNode.builder().id(1L).desiredConfigVersion(8L)
                .appliedConfigVersion(7L).enabled(1).build();
        SelfHostedHeartbeatRequest request = new SelfHostedHeartbeatRequest();
        request.setAppliedConfigVersion(7L);

        String json = service.heartbeat(node, request).toJSONString();

        assertTrue(json.contains("\"configChanged\":true"));
        assertTrue(json.contains("\"cacheJobs\":[]"));
        verify(cacheJobNodeDao, never()).selectList(any());
    }

    @Test
    void heartbeatMarksConfiguredDomainOnlineAfterNodeAppliesLatestVersion() {
        SelfHostedNodeDao nodeDao = mock(SelfHostedNodeDao.class);
        SelfHostedGroupNodeDao groupNodeDao = mock(SelfHostedGroupNodeDao.class);
        SelfHostedDomainConfigDao domainConfigDao = mock(SelfHostedDomainConfigDao.class);
        SelfHostedCacheJobNodeDao cacheJobNodeDao = mock(SelfHostedCacheJobNodeDao.class);
        CdnDomainDao domainDao = mock(CdnDomainDao.class);
        when(groupNodeDao.selectList(any())).thenReturn(Collections.singletonList(
                SelfHostedGroupNode.builder().groupId(2L).nodeId(1L).build()));
        when(domainConfigDao.selectList(any())).thenReturn(Collections.singletonList(
                SelfHostedDomainConfig.builder().cdnDomainId(4L).nodeGroupId(2L).status("enabled").build()));
        CdnDomain domain = CdnDomain.builder().id(4L).domainName("cdn.example.com")
                .route(CdnRoute.SELF_HOSTED.getCode()).cname("cdn.example.com.edge.test")
                .domainStatus("configuring").build();
        when(domainDao.selectById(4L)).thenReturn(domain);
        when(cacheJobNodeDao.selectList(any())).thenReturn(Collections.emptyList());
        SelfHostedCdnService service = new SelfHostedCdnService(nodeDao,
                mock(SelfHostedNodeGroupDao.class), groupNodeDao, domainConfigDao,
                mock(SelfHostedCacheJobDao.class), cacheJobNodeDao, domainDao, mock(SelfHostedPortForwardDao.class));
        SelfHostedNode node = SelfHostedNode.builder().id(1L).desiredConfigVersion(8L)
                .appliedConfigVersion(7L).enabled(1).build();
        SelfHostedHeartbeatRequest request = new SelfHostedHeartbeatRequest();
        request.setAppliedConfigVersion(8L);

        service.heartbeat(node, request);

        assertEquals("online", domain.getDomainStatus());
        verify(domainDao).updateById(domain);
    }

    @Test
    void domainIsReadyOnlyWhenARecentEnabledNodeHasAppliedLatestVersion() throws Exception {
        SelfHostedNodeDao nodeDao = mock(SelfHostedNodeDao.class);
        SelfHostedGroupNodeDao groupNodeDao = mock(SelfHostedGroupNodeDao.class);
        SelfHostedDomainConfigDao domainConfigDao = mock(SelfHostedDomainConfigDao.class);
        when(domainConfigDao.selectOne(any())).thenReturn(SelfHostedDomainConfig.builder()
                .cdnDomainId(4L).nodeGroupId(2L).status("enabled").build());
        when(groupNodeDao.selectList(any())).thenReturn(Collections.singletonList(
                SelfHostedGroupNode.builder().groupId(2L).nodeId(1L).build()));
        when(nodeDao.selectById(1L)).thenReturn(SelfHostedNode.builder().id(1L).enabled(1)
                .lastHeartbeat(new java.util.Date()).desiredConfigVersion(8L)
                .appliedConfigVersion(8L).build());
        SelfHostedCdnService service = new SelfHostedCdnService(nodeDao,
                mock(SelfHostedNodeGroupDao.class), groupNodeDao, domainConfigDao,
                mock(SelfHostedCacheJobDao.class), mock(SelfHostedCacheJobNodeDao.class),
                mock(CdnDomainDao.class), mock(SelfHostedPortForwardDao.class));

        assertTrue(service.isDomainConfigurationApplied(4L));
    }

    @Test
    void newAgentAppliesConfigurationBeforeProcessingCacheJobs() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/resources/self-hosted/kuocai-edge-agent.py")), StandardCharsets.UTF_8);

        assertTrue(source.indexOf("applied = apply_config(config, desired)")
                < source.indexOf("process_cache_jobs(config, response.get(\"cacheJobs\"))"));
        assertTrue(source.contains("\"agentVersion\": \"1.2.0\""));
    }

    @Test
    void agentGeneratesAllSelfHostedDomainConfigurationFamilies() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/resources/self-hosted/kuocai-edge-agent.py")), StandardCharsets.UTF_8);

        assertTrue(source.contains("proxy_force_ranges on"));
        assertTrue(source.contains("proxy_cache_revalidate on"));
        assertTrue(source.contains("valid_referers"));
        assertTrue(source.contains("secure_link_md5"));
        assertTrue(source.contains("proxy_set_header %s %s"));
        assertTrue(source.contains("add_header %s %s always"));
        assertTrue(source.contains("gzip on"));
        assertTrue(source.contains("listen [::]:80"));
        assertTrue(source.contains("backup;"));
        assertTrue(source.contains("followRedirectStatus"));
        assertTrue(source.contains("def write_port_forward_config"));
        assertTrue(source.contains("udp reuseport"));
        assertTrue(source.contains("stream {"));
    }

    private void assertSelfHostedProductRoute(String route, String coverage,
                                              String serviceArea, String name) {
        assertTrue(CdnRoute.isSelfHosted(route));
        assertEquals(coverage, CdnRoute.selfHostedCoverage(route));
        assertEquals(serviceArea, CdnRoute.selfHostedServiceArea(route));
        assertEquals(route, CdnOperationRoute.convert(route).getRoute());
        assertEquals(route, CdnCacheSettingRoute.convert(route).getRoute());
        assertEquals(route, CdnStatisticsRoute.convert(route).getRoute());
    }
}
