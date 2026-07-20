package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.dao.SelfHostedGroupNodeDao;
import com.kuocai.cdn.dao.SelfHostedNodeDao;
import com.kuocai.cdn.dao.SelfHostedNodeGroupDao;
import com.kuocai.cdn.dao.SelfHostedPortForwardDao;
import com.kuocai.cdn.dto.SelfHostedPortForwardSaveRequest;
import com.kuocai.cdn.entity.SelfHostedGroupNode;
import com.kuocai.cdn.entity.SelfHostedNode;
import com.kuocai.cdn.entity.SelfHostedNodeGroup;
import com.kuocai.cdn.entity.SelfHostedPortForward;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.util.Assert;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SelfHostedPortForwardService {
    private static final Pattern HOST_PATTERN = Pattern.compile("^[a-zA-Z0-9._:-]{1,255}$");
    private static final Set<Integer> RESERVED_PORTS = new HashSet<>();

    static {
        RESERVED_PORTS.add(22);
        RESERVED_PORTS.add(80);
        RESERVED_PORTS.add(443);
    }

    private final SelfHostedPortForwardDao portForwardDao;
    private final SelfHostedNodeGroupDao groupDao;
    private final SelfHostedGroupNodeDao groupNodeDao;
    private final SelfHostedNodeDao nodeDao;
    private final SelfHostedCdnService selfHostedCdnService;
    private final SysUserService sysUserService;
    private final CdnAreaRouteService cdnAreaRouteService;

    public SelfHostedPortForwardService(SelfHostedPortForwardDao portForwardDao,
                                        SelfHostedNodeGroupDao groupDao,
                                        SelfHostedGroupNodeDao groupNodeDao,
                                         SelfHostedNodeDao nodeDao,
                                         SelfHostedCdnService selfHostedCdnService,
                                         SysUserService sysUserService,
                                         CdnAreaRouteService cdnAreaRouteService) {
        this.portForwardDao = portForwardDao;
        this.groupDao = groupDao;
        this.groupNodeDao = groupNodeDao;
        this.nodeDao = nodeDao;
        this.selfHostedCdnService = selfHostedCdnService;
        this.sysUserService = sysUserService;
        this.cdnAreaRouteService = cdnAreaRouteService;
    }

    public List<JSONObject> list(Long userId, boolean admin) {
        QueryWrapper<SelfHostedPortForward> query = new QueryWrapper<>();
        if (!admin) {
            query.eq("user_id", userId);
        }
        query.orderByDesc("id");
        List<JSONObject> result = new ArrayList<>();
        for (SelfHostedPortForward rule : portForwardDao.selectList(query)) {
            JSONObject item = (JSONObject) JSON.toJSON(rule);
            SelfHostedNodeGroup group = groupDao.selectById(rule.getNodeGroupId());
            item.put("nodeGroupName", group == null ? "已删除节点组" : group.getGroupName());
            item.put("coverage", group == null ? null : group.getCoverage());
            result.add(item);
        }
        return result;
    }

    public List<SelfHostedNodeGroup> availableGroups(String route, boolean admin) throws BusinessException {
        if (admin) {
            return groupDao.selectList(new QueryWrapper<SelfHostedNodeGroup>()
                    .eq("status", "enabled").orderByDesc("is_default").orderByAsc("id"));
        }
        List<String> selfHostedRoutes = availableSelfHostedRoutes(route);
        if (selfHostedRoutes.isEmpty()) {
            throw new BusinessException("当前账号未开通自建 CDN");
        }
        List<SelfHostedNodeGroup> result = new ArrayList<>();
        Set<Long> groupIds = new LinkedHashSet<>();
        BusinessException lastError = null;
        for (String selfHostedRoute : selfHostedRoutes) {
            try {
                SelfHostedNodeGroup group = selfHostedCdnService.defaultGroup(selfHostedRoute);
                if (group != null && groupIds.add(group.getId())) {
                    result.add(group);
                }
            } catch (BusinessException e) {
                lastError = e;
            }
        }
        if (result.isEmpty()) {
            if (lastError != null) {
                throw lastError;
            }
            throw new BusinessException("当前账号没有可用的自建 CDN 节点组");
        }
        return result;
    }

    public boolean isAvailable(String route, boolean admin) {
        return admin || !availableSelfHostedRoutes(route).isEmpty();
    }

    @Transactional(rollbackFor = Exception.class)
    public SelfHostedPortForward save(SelfHostedPortForwardSaveRequest request,
                                      Long loginUserId, String route, boolean admin) throws BusinessException {
        if (!isAvailable(route, admin)) {
            throw new BusinessException("当前账号未开通自建 CDN");
        }
        validateRequest(request);
        SelfHostedPortForward existing = request.getId() == null ? null : portForwardDao.selectById(request.getId());
        if (request.getId() != null && existing == null) {
            throw new BusinessException("端口转发规则不存在");
        }
        if (existing != null && !admin && !loginUserId.equals(existing.getUserId())) {
            throw new BusinessException("无权修改该端口转发规则");
        }
        Long ownerId = admin && request.getUserId() != null ? request.getUserId() : loginUserId;
        if (ownerId == null) {
            throw new BusinessException("用户信息已失效，请重新登录");
        }
        if (sysUserService.queryById(ownerId) == null) {
            throw new BusinessException("指定用户不存在");
        }
        Long groupId = resolveGroupId(request.getNodeGroupId(), route, admin);
        SelfHostedNodeGroup group = groupDao.selectById(groupId);
        if (group == null || !"enabled".equals(group.getStatus())) {
            throw new BusinessException("节点组不存在或已停用");
        }
        String status = "enabled".equalsIgnoreCase(request.getStatus()) ? "enabled" : "disabled";
        if ("enabled".equals(status)) {
            ensureHasEnabledNode(groupId);
            ensureNoConflict(request.getId(), groupId, request.getProtocol().trim().toLowerCase(), request.getListenPort());
        }
        Date now = new Date();
        if (existing == null) {
            existing = new SelfHostedPortForward();
            existing.setCreateTime(now);
        }
        Long oldGroupId = existing.getNodeGroupId();
        existing.setUserId(ownerId);
        existing.setRuleName(request.getRuleName().trim());
        existing.setProtocol(request.getProtocol().trim().toLowerCase());
        existing.setListenPort(request.getListenPort());
        existing.setOriginHost(request.getOriginHost().trim());
        existing.setOriginPort(request.getOriginPort());
        existing.setNodeGroupId(groupId);
        existing.setStatus(status);
        existing.setRemark(trim(request.getRemark()));
        existing.setUpdateTime(now);
        if (existing.getId() == null) {
            portForwardDao.insert(existing);
        } else {
            portForwardDao.updateById(existing);
        }
        selfHostedCdnService.markGroupConfigurationChanged(groupId);
        if (oldGroupId != null && !oldGroupId.equals(groupId)) {
            selfHostedCdnService.markGroupConfigurationChanged(oldGroupId);
        }
        return portForwardDao.selectById(existing.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void setStatus(Long id, boolean enabled, Long loginUserId, boolean admin) throws BusinessException {
        SelfHostedPortForward rule = getOwnedRule(id, loginUserId, admin);
        if (enabled) {
            SelfHostedNodeGroup group = groupDao.selectById(rule.getNodeGroupId());
            if (group == null || !"enabled".equals(group.getStatus())) {
                throw new BusinessException("节点组不存在或已停用");
            }
            ensureHasEnabledNode(rule.getNodeGroupId());
            ensureNoConflict(rule.getId(), rule.getNodeGroupId(), rule.getProtocol(), rule.getListenPort());
        }
        rule.setStatus(enabled ? "enabled" : "disabled");
        rule.setUpdateTime(new Date());
        portForwardDao.updateById(rule);
        selfHostedCdnService.markGroupConfigurationChanged(rule.getNodeGroupId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long loginUserId, boolean admin) throws BusinessException {
        SelfHostedPortForward rule = getOwnedRule(id, loginUserId, admin);
        portForwardDao.deleteById(id);
        selfHostedCdnService.markGroupConfigurationChanged(rule.getNodeGroupId());
    }

    private Long resolveGroupId(Long requestedGroupId, String route, boolean admin) throws BusinessException {
        if (admin) {
            if (requestedGroupId == null) {
                throw new BusinessException("请选择节点组");
            }
            return requestedGroupId;
        }
        List<SelfHostedNodeGroup> groups = availableGroups(route, false);
        if (requestedGroupId == null) {
            return groups.get(0).getId();
        }
        for (SelfHostedNodeGroup group : groups) {
            if (requestedGroupId.equals(group.getId())) {
                return requestedGroupId;
            }
        }
        throw new BusinessException("所选节点组不属于当前账号已开放的自建 CDN 线路");
    }

    private List<String> availableSelfHostedRoutes(String route) {
        if (CdnRoute.isSelfHosted(route)) {
            List<String> routes = new ArrayList<>();
            routes.add(route);
            return routes;
        }
        return cdnAreaRouteService.configuredSelfHostedRoutes();
    }

    private SelfHostedPortForward getOwnedRule(Long id, Long loginUserId, boolean admin) throws BusinessException {
        if (id == null) {
            throw new BusinessException("端口转发规则不能为空");
        }
        SelfHostedPortForward rule = portForwardDao.selectById(id);
        if (rule == null) {
            throw new BusinessException("端口转发规则不存在");
        }
        if (!admin && !loginUserId.equals(rule.getUserId())) {
            throw new BusinessException("无权操作该端口转发规则");
        }
        return rule;
    }

    private void validateRequest(SelfHostedPortForwardSaveRequest request) throws BusinessException {
        if (request == null || Assert.isEmpty(request.getRuleName()) || Assert.isEmpty(request.getProtocol())
                || Assert.isEmpty(request.getOriginHost())) {
            throw new BusinessException("规则名称、协议和源站地址不能为空");
        }
        String protocol = request.getProtocol().trim().toLowerCase();
        if (!"tcp".equals(protocol) && !"udp".equals(protocol)) {
            throw new BusinessException("协议只支持 TCP 或 UDP");
        }
        if (request.getListenPort() == null || request.getListenPort() < 1 || request.getListenPort() > 65535
                || RESERVED_PORTS.contains(request.getListenPort())) {
            throw new BusinessException("监听端口必须在 1-65535 范围内，且不能使用 22、80、443");
        }
        if (request.getOriginPort() == null || request.getOriginPort() < 1 || request.getOriginPort() > 65535) {
            throw new BusinessException("源站端口必须在 1-65535 范围内");
        }
        String host = request.getOriginHost().trim();
        if (!HOST_PATTERN.matcher(host).matches() || host.contains("//")) {
            throw new BusinessException("源站地址格式不正确，请填写 IP 或主机名，不要填写协议和路径");
        }
        if (request.getRuleName().trim().length() > 128 || host.length() > 255) {
            throw new BusinessException("规则名称或源站地址过长");
        }
    }

    private void ensureHasEnabledNode(Long groupId) throws BusinessException {
        for (SelfHostedGroupNode relation : groupNodeDao.selectList(
                new QueryWrapper<SelfHostedGroupNode>().eq("group_id", groupId))) {
            SelfHostedNode node = nodeDao.selectById(relation.getNodeId());
            if (node != null && (node.getEnabled() == null || node.getEnabled() == 1)) {
                return;
            }
        }
        throw new BusinessException("节点组暂未配置启用节点");
    }

    private void ensureNoConflict(Long ruleId, Long groupId, String protocol, Integer port) throws BusinessException {
        Set<Long> targetNodes = enabledNodeIds(groupId);
        List<SelfHostedPortForward> candidates = portForwardDao.selectList(new QueryWrapper<SelfHostedPortForward>()
                .eq("protocol", protocol).eq("listen_port", port).eq("status", "enabled"));
        for (SelfHostedPortForward candidate : candidates) {
            if (candidate.getId().equals(ruleId)) {
                continue;
            }
            Set<Long> candidateNodes = enabledNodeIds(candidate.getNodeGroupId());
            for (Long nodeId : targetNodes) {
                if (candidateNodes.contains(nodeId)) {
                    throw new BusinessException("监听端口 " + port + " 已被同一节点上的其他规则占用");
                }
            }
        }
    }

    private Set<Long> enabledNodeIds(Long groupId) {
        Set<Long> ids = new LinkedHashSet<>();
        for (SelfHostedGroupNode relation : groupNodeDao.selectList(
                new QueryWrapper<SelfHostedGroupNode>().eq("group_id", groupId))) {
            SelfHostedNode node = nodeDao.selectById(relation.getNodeId());
            if (node != null && (node.getEnabled() == null || node.getEnabled() == 1)) {
                ids.add(node.getId());
            }
        }
        return ids;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
