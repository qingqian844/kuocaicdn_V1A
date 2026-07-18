package com.kuocai.cdn.service;

import com.kuocai.cdn.dao.SelfHostedGroupNodeDao;
import com.kuocai.cdn.dao.SelfHostedNodeDao;
import com.kuocai.cdn.dao.SelfHostedNodeGroupDao;
import com.kuocai.cdn.dao.SelfHostedPortForwardDao;
import com.kuocai.cdn.dto.SelfHostedPortForwardSaveRequest;
import com.kuocai.cdn.entity.SelfHostedGroupNode;
import com.kuocai.cdn.entity.SelfHostedNode;
import com.kuocai.cdn.entity.SelfHostedNodeGroup;
import com.kuocai.cdn.entity.SelfHostedPortForward;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SelfHostedPortForwardServiceTest {
    @Test
    void rejectsReservedPortsBeforeWritingRule() {
        SelfHostedPortForwardService service = newService();
        SelfHostedPortForwardSaveRequest request = request(80, 8080);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.save(request, 7L, "self_hosted_overseas", true));

        assertTrue(exception.getMessage().contains("22、80、443"));
    }

    @Test
    void rejectsRulesThatOverlapOnTheSameNodeAndProtocol() {
        SelfHostedPortForwardDao portDao = mock(SelfHostedPortForwardDao.class);
        SelfHostedNodeGroupDao groupDao = mock(SelfHostedNodeGroupDao.class);
        SelfHostedGroupNodeDao groupNodeDao = mock(SelfHostedGroupNodeDao.class);
        SelfHostedNodeDao nodeDao = mock(SelfHostedNodeDao.class);
        SelfHostedCdnService cdnService = mock(SelfHostedCdnService.class);
        SysUserService sysUserService = mock(SysUserService.class);
        when(sysUserService.queryById(7L)).thenReturn(new SysUser());
        SelfHostedPortForwardService service = new SelfHostedPortForwardService(
                portDao, groupDao, groupNodeDao, nodeDao, cdnService, sysUserService);
        when(groupDao.selectById(8L)).thenReturn(SelfHostedNodeGroup.builder().id(8L).status("enabled").build());
        when(groupNodeDao.selectList(any())).thenReturn(Collections.singletonList(
                SelfHostedGroupNode.builder().groupId(8L).nodeId(1L).build()));
        when(nodeDao.selectById(1L)).thenReturn(SelfHostedNode.builder().id(1L).enabled(1).build());
        when(portDao.selectList(any())).thenReturn(Collections.singletonList(
                SelfHostedPortForward.builder().id(9L).nodeGroupId(8L).protocol("tcp")
                        .listenPort(10000).status("enabled").build()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.save(request(10000, 8080), 7L, "self_hosted_overseas", true));

        assertTrue(exception.getMessage().contains("10000"));
    }

    private SelfHostedPortForwardService newService() {
        return new SelfHostedPortForwardService(mock(SelfHostedPortForwardDao.class),
                mock(SelfHostedNodeGroupDao.class), mock(SelfHostedGroupNodeDao.class),
                mock(SelfHostedNodeDao.class), mock(SelfHostedCdnService.class), mock(SysUserService.class));
    }

    private SelfHostedPortForwardSaveRequest request(int listenPort, int originPort) {
        SelfHostedPortForwardSaveRequest request = new SelfHostedPortForwardSaveRequest();
        request.setRuleName("测试规则");
        request.setProtocol("tcp");
        request.setListenPort(listenPort);
        request.setOriginHost("192.0.2.10");
        request.setOriginPort(originPort);
        request.setNodeGroupId(8L);
        request.setUserId(7L);
        request.setStatus("enabled");
        return request;
    }
}
