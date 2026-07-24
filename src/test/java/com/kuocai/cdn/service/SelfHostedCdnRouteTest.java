package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuocai.cdn.dto.SelfHostedNodeSaveRequest;
import com.kuocai.cdn.dto.SelfHostedHeartbeatRequest;
import com.kuocai.cdn.dto.SelfHostedDiskInfo;
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
import java.math.BigDecimal;
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
                        .originType("ipaddr").originAddress("192.0.2.10")
                        .originConfigJson("{}")
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
    void invalidHistoricalOriginIsQuarantinedWithoutEnteringNodeConfig() {
        SelfHostedGroupNodeDao groupNodeDao = mock(SelfHostedGroupNodeDao.class);
        SelfHostedDomainConfigDao domainConfigDao = mock(SelfHostedDomainConfigDao.class);
        CdnDomainDao domainDao = mock(CdnDomainDao.class);
        SelfHostedDomainConfig invalid = SelfHostedDomainConfig.builder()
                .id(3L).cdnDomainId(4L).nodeGroupId(2L).originType("ipaddr")
                .originAddress("225.667.21").originConfigJson("{}").status("enabled").build();
        when(groupNodeDao.selectList(any())).thenReturn(Collections.singletonList(
                SelfHostedGroupNode.builder().groupId(2L).nodeId(1L).build()));
        when(domainConfigDao.selectList(any())).thenReturn(Collections.singletonList(invalid));
        CdnDomain domain = CdnDomain.builder().id(4L).domainName("cdn.example.com")
                .route(CdnRoute.SELF_HOSTED_OVERSEAS.getCode()).domainStatus("online").build();
        when(domainDao.selectById(4L)).thenReturn(domain);
        SelfHostedCdnService service = new SelfHostedCdnService(mock(SelfHostedNodeDao.class),
                mock(SelfHostedNodeGroupDao.class), groupNodeDao, domainConfigDao,
                mock(SelfHostedCacheJobDao.class), mock(SelfHostedCacheJobNodeDao.class), domainDao,
                mock(SelfHostedPortForwardDao.class));

        String json = service.desiredConfig(
                SelfHostedNode.builder().id(1L).desiredConfigVersion(5L).build()).toJSONString();

        assertTrue(json.contains("\"domains\":[]"));
        assertEquals("configure_failed", domain.getDomainStatus());
        verify(domainConfigDao).update(any(), any());
        verify(domainDao).updateById(domain);
    }

    @Test
    void wildcardDomainUsesLegacyCompatibleNginxNameWithoutBlockingOtherDomains() {
        SelfHostedGroupNodeDao groupNodeDao = mock(SelfHostedGroupNodeDao.class);
        SelfHostedDomainConfigDao domainConfigDao = mock(SelfHostedDomainConfigDao.class);
        CdnDomainDao domainDao = mock(CdnDomainDao.class);
        SelfHostedDomainConfig wildcardConfig = SelfHostedDomainConfig.builder()
                .id(3L).cdnDomainId(4L).nodeGroupId(2L).originType("ipaddr")
                .originAddress("192.0.2.10").originHost("*.example.com")
                .originConfigJson("{}").status("enabled").build();
        SelfHostedDomainConfig regularConfig = SelfHostedDomainConfig.builder()
                .id(5L).cdnDomainId(6L).nodeGroupId(2L).originType("ipaddr")
                .originAddress("192.0.2.11").originHost("cdn.example.net")
                .originConfigJson("{}").status("enabled").build();
        when(groupNodeDao.selectList(any())).thenReturn(Collections.singletonList(
                SelfHostedGroupNode.builder().groupId(2L).nodeId(1L).build()));
        when(domainConfigDao.selectList(any())).thenReturn(java.util.Arrays.asList(wildcardConfig, regularConfig));
        when(domainDao.selectById(4L)).thenReturn(CdnDomain.builder().id(4L).domainName("*.Example.COM")
                .route(CdnRoute.SELF_HOSTED_OVERSEAS.getCode()).domainStatus("online").build());
        when(domainDao.selectById(6L)).thenReturn(CdnDomain.builder().id(6L).domainName("cdn.example.net")
                .route(CdnRoute.SELF_HOSTED_OVERSEAS.getCode()).domainStatus("online").build());
        SelfHostedCdnService service = new SelfHostedCdnService(mock(SelfHostedNodeDao.class),
                mock(SelfHostedNodeGroupDao.class), groupNodeDao, domainConfigDao,
                mock(SelfHostedCacheJobDao.class), mock(SelfHostedCacheJobNodeDao.class), domainDao,
                mock(SelfHostedPortForwardDao.class));

        String json = service.desiredConfig(
                SelfHostedNode.builder().id(1L).desiredConfigVersion(5L).build()).toJSONString();

        assertTrue(json.contains("\"domainName\":\".example.com\""));
        assertTrue(json.contains("\"originHost\":\"example.com\""));
        assertTrue(json.contains("\"domainName\":\"cdn.example.net\""));
        verify(domainConfigDao, never()).update(any(), any());
    }

    @Test
    void invalidHistoricalDomainIsQuarantinedWithoutBlockingValidDomain() {
        SelfHostedGroupNodeDao groupNodeDao = mock(SelfHostedGroupNodeDao.class);
        SelfHostedDomainConfigDao domainConfigDao = mock(SelfHostedDomainConfigDao.class);
        CdnDomainDao domainDao = mock(CdnDomainDao.class);
        SelfHostedDomainConfig invalidConfig = SelfHostedDomainConfig.builder()
                .id(3L).cdnDomainId(4L).nodeGroupId(2L).originType("ipaddr")
                .originAddress("192.0.2.10").originConfigJson("{}").status("enabled").build();
        SelfHostedDomainConfig validConfig = SelfHostedDomainConfig.builder()
                .id(5L).cdnDomainId(6L).nodeGroupId(2L).originType("ipaddr")
                .originAddress("192.0.2.11").originConfigJson("{}").status("enabled").build();
        when(groupNodeDao.selectList(any())).thenReturn(Collections.singletonList(
                SelfHostedGroupNode.builder().groupId(2L).nodeId(1L).build()));
        when(domainConfigDao.selectList(any())).thenReturn(java.util.Arrays.asList(invalidConfig, validConfig));
        CdnDomain invalidDomain = CdnDomain.builder().id(4L).domainName("https://bad.example.com/path")
                .route(CdnRoute.SELF_HOSTED_OVERSEAS.getCode()).domainStatus("online").build();
        when(domainDao.selectById(4L)).thenReturn(invalidDomain);
        when(domainDao.selectById(6L)).thenReturn(CdnDomain.builder().id(6L).domainName("cdn.example.net")
                .route(CdnRoute.SELF_HOSTED_OVERSEAS.getCode()).domainStatus("online").build());
        SelfHostedCdnService service = new SelfHostedCdnService(mock(SelfHostedNodeDao.class),
                mock(SelfHostedNodeGroupDao.class), groupNodeDao, domainConfigDao,
                mock(SelfHostedCacheJobDao.class), mock(SelfHostedCacheJobNodeDao.class), domainDao,
                mock(SelfHostedPortForwardDao.class));

        String json = service.desiredConfig(
                SelfHostedNode.builder().id(1L).desiredConfigVersion(5L).build()).toJSONString();

        assertFalse(json.contains("https://bad.example.com/path"));
        assertTrue(json.contains("\"domainName\":\"cdn.example.net\""));
        assertEquals("configure_failed", invalidDomain.getDomainStatus());
        verify(domainConfigDao).update(any(), any());
        verify(domainDao).updateById(invalidDomain);
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
    void agentConfigIncludesNodeCacheDiskAndCleanupPolicy() {
        SelfHostedGroupNodeDao groupNodeDao = mock(SelfHostedGroupNodeDao.class);
        when(groupNodeDao.selectList(any())).thenReturn(Collections.emptyList());
        SelfHostedCdnService service = new SelfHostedCdnService(mock(SelfHostedNodeDao.class),
                mock(SelfHostedNodeGroupDao.class), groupNodeDao, mock(SelfHostedDomainConfigDao.class),
                mock(SelfHostedCacheJobDao.class), mock(SelfHostedCacheJobNodeDao.class),
                mock(CdnDomainDao.class), mock(SelfHostedPortForwardDao.class));
        SelfHostedNode node = SelfHostedNode.builder().id(1L).desiredConfigVersion(5L)
                .cacheDiskMount("/data").cacheMaxSizeGb(200)
                .cacheCleanupEnabled(1).cacheCleanupAgeDays(14).cacheCleanupMinHits(3).build();

        String json = service.desiredConfig(node).toJSONString();

        assertTrue(json.contains("\"diskMount\":\"/data\""));
        assertTrue(json.contains("\"directory\":\"/data/kuocai-cdn-cache\""));
        assertTrue(json.contains("\"maxSizeGb\":200"));
        assertTrue(json.contains("\"cleanupAgeDays\":14"));
        assertTrue(json.contains("\"cleanupMinHits\":3"));
        assertEquals("/var/cache/kuocai-cdn", SelfHostedCdnService.cacheDirectory("/"));
    }

    @Test
    void heartbeatStoresDetectedCacheDisksForAdminSelection() {
        SelfHostedNodeDao nodeDao = mock(SelfHostedNodeDao.class);
        SelfHostedCdnService service = new SelfHostedCdnService(nodeDao,
                mock(SelfHostedNodeGroupDao.class), mock(SelfHostedGroupNodeDao.class),
                mock(SelfHostedDomainConfigDao.class), mock(SelfHostedCacheJobDao.class),
                mock(SelfHostedCacheJobNodeDao.class), mock(CdnDomainDao.class),
                mock(SelfHostedPortForwardDao.class));
        SelfHostedNode node = SelfHostedNode.builder().id(1L).desiredConfigVersion(8L)
                .appliedConfigVersion(7L).enabled(1).build();
        SelfHostedDiskInfo disk = new SelfHostedDiskInfo();
        disk.setDevice("/dev/vdb1");
        disk.setMountPath("/data");
        disk.setFsType("xfs");
        disk.setTotalBytes(500L * 1024 * 1024 * 1024);
        disk.setAvailableBytes(400L * 1024 * 1024 * 1024);
        disk.setUsedPercent(new BigDecimal("20.00"));
        disk.setWritable(true);
        SelfHostedHeartbeatRequest request = new SelfHostedHeartbeatRequest();
        request.setAppliedConfigVersion(7L);
        request.setDisks(Collections.singletonList(disk));

        service.heartbeat(node, request);

        assertTrue(node.getDetectedDisksJson().contains("/dev/vdb1"));
        assertTrue(node.getDetectedDisksJson().contains("/data"));
        verify(nodeDao).updateById(node);
    }

    @Test
    void heartbeatCalculatesNetworkRatesAndRecordsTelemetry() {
        SelfHostedNodeDao nodeDao = mock(SelfHostedNodeDao.class);
        SelfHostedNodeTelemetryService telemetryService = mock(SelfHostedNodeTelemetryService.class);
        SelfHostedCdnService service = new SelfHostedCdnService(nodeDao,
                mock(SelfHostedNodeGroupDao.class), mock(SelfHostedGroupNodeDao.class),
                mock(SelfHostedDomainConfigDao.class), mock(SelfHostedCacheJobDao.class),
                mock(SelfHostedCacheJobNodeDao.class), mock(CdnDomainDao.class),
                mock(SelfHostedPortForwardDao.class));
        service.setTelemetryService(telemetryService);
        SelfHostedNode node = SelfHostedNode.builder().id(1L).status("online")
                .lastHeartbeat(new java.util.Date(System.currentTimeMillis() - 30_000L))
                .rxBytes(1_000L).txBytes(2_000L).desiredConfigVersion(8L)
                .appliedConfigVersion(7L).enabled(1).build();
        SelfHostedHeartbeatRequest request = new SelfHostedHeartbeatRequest();
        request.setAppliedConfigVersion(7L);
        request.setRxBytes(31_000L);
        request.setTxBytes(62_000L);

        service.heartbeat(node, request);

        assertTrue(node.getRxRateBps() >= 950L && node.getRxRateBps() <= 1_050L);
        assertTrue(node.getTxRateBps() >= 1_900L && node.getTxRateBps() <= 2_100L);
        verify(telemetryService).recordHeartbeat(node, "online", null);
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
        assertTrue(source.contains("\"agentVersion\": \"1.4.0\""));
        assertTrue(source.contains("def cpu_percent()"));
        assertTrue(source.contains("default_route_interfaces"));
        assertTrue(source.contains("def detected_disks()"));
        assertTrue(source.contains("def cleanup_low_frequency_cache"));
        assertTrue(source.contains("cachePolicy"));
        assertTrue(source.contains("listen 80 default_server"));
        assertTrue(source.contains("return 444"));
        assertTrue(source.contains("remove_distribution_default_server"));
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
