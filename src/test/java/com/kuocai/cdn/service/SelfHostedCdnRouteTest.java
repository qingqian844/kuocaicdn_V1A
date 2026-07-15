package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuocai.cdn.dto.SelfHostedNodeSaveRequest;
import com.kuocai.cdn.dao.CdnDomainDao;
import com.kuocai.cdn.dao.SelfHostedCacheJobDao;
import com.kuocai.cdn.dao.SelfHostedCacheJobNodeDao;
import com.kuocai.cdn.dao.SelfHostedDomainConfigDao;
import com.kuocai.cdn.dao.SelfHostedGroupNodeDao;
import com.kuocai.cdn.dao.SelfHostedNodeDao;
import com.kuocai.cdn.dao.SelfHostedNodeGroupDao;
import com.kuocai.cdn.entity.SelfHostedNode;
import com.kuocai.cdn.entity.SelfHostedGroupNode;
import com.kuocai.cdn.entity.SelfHostedDomainConfig;
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
        SelfHostedGroupNodeDao groupNodeDao = mock(SelfHostedGroupNodeDao.class);
        when(nodeDao.selectList(any())).thenReturn(Collections.singletonList(SelfHostedNode.builder()
                .id(1L).nodeName("edge-1").host("192.0.2.10").sshPort(22)
                .sshPasswordCipher("encrypted-password").agentTokenHash("token-hash")
                .enabled(1).status("pending").build()));
        when(groupNodeDao.selectList(any())).thenReturn(Collections.emptyList());

        SelfHostedCdnService service = new SelfHostedCdnService(nodeDao,
                mock(SelfHostedNodeGroupDao.class), groupNodeDao,
                mock(SelfHostedDomainConfigDao.class), mock(SelfHostedCacheJobDao.class),
                mock(SelfHostedCacheJobNodeDao.class), mock(CdnDomainDao.class));

        String json = service.listNodeViews().get(0).toJSONString();
        assertFalse(json.contains("encrypted-password"));
        assertFalse(json.contains("token-hash"));
        assertFalse(json.contains("sshPasswordCipher"));
        assertFalse(json.contains("agentTokenHash"));
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
                        .privateKeyCipher("private-key-ciphertext").status("enabled").build()));
        when(domainDao.selectById(4L)).thenReturn(CdnDomain.builder().id(4L).domainName("cdn.example.com")
                .route("self_hosted").domainStatus("online").build());
        SelfHostedCdnService service = new SelfHostedCdnService(mock(SelfHostedNodeDao.class),
                mock(SelfHostedNodeGroupDao.class), groupNodeDao, domainConfigDao,
                mock(SelfHostedCacheJobDao.class), mock(SelfHostedCacheJobNodeDao.class), domainDao);

        String json = service.desiredConfig(SelfHostedNode.builder().id(1L).desiredConfigVersion(5L).build()).toJSONString();
        assertFalse(json.contains("certificate-ciphertext"));
        assertFalse(json.contains("private-key-ciphertext"));
        assertFalse(json.contains("certificateCipher"));
        assertFalse(json.contains("privateKeyCipher"));
    }

    @Test
    void enablingHttpsWithoutNewPemKeepsTheStoredCertificate() throws Exception {
        SelfHostedDomainConfigDao domainConfigDao = mock(SelfHostedDomainConfigDao.class);
        SelfHostedGroupNodeDao groupNodeDao = mock(SelfHostedGroupNodeDao.class);
        when(groupNodeDao.selectList(any())).thenReturn(Collections.emptyList());
        SelfHostedCdnService service = new SelfHostedCdnService(mock(SelfHostedNodeDao.class),
                mock(SelfHostedNodeGroupDao.class), groupNodeDao, domainConfigDao,
                mock(SelfHostedCacheJobDao.class), mock(SelfHostedCacheJobNodeDao.class), mock(CdnDomainDao.class));
        SelfHostedDomainConfig config = SelfHostedDomainConfig.builder().id(3L).nodeGroupId(2L)
                .certificateCipher("stored-certificate").privateKeyCipher("stored-private-key")
                .httpsEnabled(1).desiredConfigVersion(1L).build();

        service.saveCertificate(config, true, "", "", "off");

        assertEquals("stored-certificate", config.getCertificateCipher());
        assertEquals("stored-private-key", config.getPrivateKeyCipher());
    }
}
