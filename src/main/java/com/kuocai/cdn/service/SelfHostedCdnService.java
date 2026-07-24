package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.kuocai.cdn.api.tencent.dns.CreateRecordResponse;
import com.kuocai.cdn.api.tencent.dns.TencentApi;
import com.kuocai.cdn.api.tencent.dns.dto.CreateRecordDTO;
import com.kuocai.cdn.api.tencent.dns.dto.DeleteRecordDTO;
import com.kuocai.cdn.api.tencent.dns.properties.TencentDns;
import com.kuocai.cdn.dao.CdnDomainDao;
import com.kuocai.cdn.dao.SelfHostedDomainConfigDao;
import com.kuocai.cdn.dao.SelfHostedCacheJobDao;
import com.kuocai.cdn.dao.SelfHostedCacheJobNodeDao;
import com.kuocai.cdn.dao.SelfHostedGroupNodeDao;
import com.kuocai.cdn.dao.SelfHostedNodeDao;
import com.kuocai.cdn.dao.SelfHostedNodeGroupDao;
import com.kuocai.cdn.dao.SelfHostedPortForwardDao;
import com.kuocai.cdn.dto.SelfHostedApplyResultRequest;
import com.kuocai.cdn.dto.SelfHostedCacheResultRequest;
import com.kuocai.cdn.dto.SelfHostedDiskInfo;
import com.kuocai.cdn.dto.SelfHostedGroupSaveRequest;
import com.kuocai.cdn.dto.SelfHostedHeartbeatRequest;
import com.kuocai.cdn.dto.SelfHostedNodeSaveRequest;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.SelfHostedCacheJob;
import com.kuocai.cdn.entity.SelfHostedCacheJobNode;
import com.kuocai.cdn.entity.SelfHostedDomainConfig;
import com.kuocai.cdn.entity.SelfHostedGroupNode;
import com.kuocai.cdn.entity.SelfHostedNode;
import com.kuocai.cdn.entity.SelfHostedNodeGroup;
import com.kuocai.cdn.entity.SelfHostedPortForward;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.ConfigureRsaUtils;
import com.kuocai.cdn.util.SelfHostedDomainValidator;
import com.kuocai.cdn.util.SelfHostedOriginValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SelfHostedCdnService {
    public static final long DNS_RECORD_TTL_SECONDS = 600L;

    private static final Pattern HOST_PATTERN = Pattern.compile("^[a-zA-Z0-9._:-]{1,255}$");
    private static final Pattern USER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_-]{0,63}$");
    private static final Pattern LABEL_PATTERN = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$");
    private static final long OFFLINE_AFTER_MS = 90_000L;
    private static final long CACHE_JOB_TIMEOUT_MS = 10 * 60_000L;
    private static final long CONFIG_APPLY_POLL_MS = 1_000L;
    private static final String LEGACY_COVERAGE = "legacy";
    private static final String DEFAULT_CACHE_DISK_MOUNT = "/";
    private static final int DEFAULT_CACHE_MAX_SIZE_GB = 50;
    private static final int DEFAULT_CACHE_CLEANUP_AGE_DAYS = 7;
    private static final int DEFAULT_CACHE_CLEANUP_MIN_HITS = 1;
    private static final List<String> GROUP_COVERAGES = Arrays.asList(
            LEGACY_COVERAGE, "mainland", "overseas", "global");

    private final SelfHostedNodeDao nodeDao;
    private final SelfHostedNodeGroupDao groupDao;
    private final SelfHostedGroupNodeDao groupNodeDao;
    private final SelfHostedDomainConfigDao domainConfigDao;
    private final SelfHostedCacheJobDao cacheJobDao;
    private final SelfHostedCacheJobNodeDao cacheJobNodeDao;
    private final CdnDomainDao cdnDomainDao;
    private final SelfHostedPortForwardDao portForwardDao;
    private final SecureRandom secureRandom = new SecureRandom();
    private SelfHostedNodeTelemetryService telemetryService;

    public SelfHostedCdnService(SelfHostedNodeDao nodeDao,
                                SelfHostedNodeGroupDao groupDao,
                                SelfHostedGroupNodeDao groupNodeDao,
                                SelfHostedDomainConfigDao domainConfigDao,
                                SelfHostedCacheJobDao cacheJobDao,
                                SelfHostedCacheJobNodeDao cacheJobNodeDao,
                                CdnDomainDao cdnDomainDao,
                                SelfHostedPortForwardDao portForwardDao) {
        this.nodeDao = nodeDao;
        this.groupDao = groupDao;
        this.groupNodeDao = groupNodeDao;
        this.domainConfigDao = domainConfigDao;
        this.cacheJobDao = cacheJobDao;
        this.cacheJobNodeDao = cacheJobNodeDao;
        this.cdnDomainDao = cdnDomainDao;
        this.portForwardDao = portForwardDao;
    }

    @Autowired
    public void setTelemetryService(SelfHostedNodeTelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    public List<SelfHostedNode> listNodes() {
        List<SelfHostedNode> nodes = nodeDao.selectList(new QueryWrapper<SelfHostedNode>().orderByDesc("id"));
        Date now = new Date();
        for (SelfHostedNode node : nodes) {
            if (node.getEnabled() != null && node.getEnabled() == 0) {
                node.setStatus("disabled");
            } else if (node.getLastHeartbeat() != null
                    && now.getTime() - node.getLastHeartbeat().getTime() > OFFLINE_AFTER_MS) {
                node.setStatus("offline");
                node.setRxRateBps(0L);
                node.setTxRateBps(0L);
            }
        }
        return nodes;
    }

    public List<JSONObject> listNodeViews() {
        List<JSONObject> result = new ArrayList<>();
        for (SelfHostedNode node : listNodes()) {
            result.add(toNodeView(node));
        }
        return result;
    }

    public JSONObject nodeView(Long id) throws BusinessException {
        SelfHostedNode node = getNode(id);
        if (node.getEnabled() != null && node.getEnabled() == 0) {
            node.setStatus("disabled");
        } else if (node.getLastHeartbeat() != null
                && System.currentTimeMillis() - node.getLastHeartbeat().getTime() > OFFLINE_AFTER_MS) {
            node.setStatus("offline");
            node.setRxRateBps(0L);
            node.setTxRateBps(0L);
        }
        return toNodeView(node);
    }

    private JSONObject toNodeView(SelfHostedNode node) {
        JSONObject item = (JSONObject) JSON.toJSON(node);
        item.remove("sshPasswordCipher");
        item.remove("agentTokenHash");
        item.remove("detectedDisksJson");
        String cacheDiskMount = normalizeStoredCacheDiskMount(node.getCacheDiskMount());
        item.put("cacheDiskMount", cacheDiskMount);
        item.put("cacheDirectory", cacheDirectory(cacheDiskMount));
        item.put("cacheMaxSizeGb", valueOrDefault(node.getCacheMaxSizeGb(), DEFAULT_CACHE_MAX_SIZE_GB));
        item.put("cacheCleanupEnabled", valueOrDefault(node.getCacheCleanupEnabled(), 1));
        item.put("cacheCleanupAgeDays", valueOrDefault(
                node.getCacheCleanupAgeDays(), DEFAULT_CACHE_CLEANUP_AGE_DAYS));
        item.put("cacheCleanupMinHits", valueOrDefault(
                node.getCacheCleanupMinHits(), DEFAULT_CACHE_CLEANUP_MIN_HITS));
        item.put("detectedDisks", parseDetectedDisks(node.getDetectedDisksJson()));
        item.put("passwordConfigured", !Assert.isEmpty(node.getSshPasswordCipher()));
        List<SelfHostedGroupNode> relations = groupNodeDao.selectList(
                new QueryWrapper<SelfHostedGroupNode>().eq("node_id", node.getId()).orderByAsc("id"));
        List<Long> groupIds = new ArrayList<>();
        List<String> groupNames = new ArrayList<>();
        if (!relations.isEmpty()) {
            for (SelfHostedGroupNode relation : relations) {
                SelfHostedNodeGroup group = groupDao.selectById(relation.getGroupId());
                if (group != null) {
                    groupIds.add(group.getId());
                    groupNames.add(group.getGroupName());
                }
            }
            if (!groupIds.isEmpty()) {
                item.put("groupId", groupIds.get(0));
                item.put("groupIds", groupIds);
                item.put("groupName", String.join("、", groupNames));
            }
        }
        return item;
    }

    public List<SelfHostedNodeGroup> listGroups() {
        return groupDao.selectList(new QueryWrapper<SelfHostedNodeGroup>().orderByDesc("is_default").orderByAsc("id"));
    }

    public SelfHostedNodeGroup defaultGroup() throws BusinessException {
        SelfHostedNodeGroup group = groupDao.selectOne(new QueryWrapper<SelfHostedNodeGroup>()
                .eq("coverage", LEGACY_COVERAGE).eq("is_default", 1)
                .eq("status", "enabled").last("LIMIT 1"));
        if (group == null) {
            group = groupDao.selectOne(new QueryWrapper<SelfHostedNodeGroup>()
                    .eq("is_default", 1).eq("status", "enabled").last("LIMIT 1"));
        }
        if (group == null) {
            throw new BusinessException("请先创建并启用一个默认自建 CDN 节点组");
        }
        return group;
    }

    public SelfHostedNodeGroup defaultGroup(String route) throws BusinessException {
        String coverage = CdnRoute.selfHostedCoverage(route);
        if (coverage == null) {
            return defaultGroup();
        }
        SelfHostedNodeGroup group = groupDao.selectOne(new QueryWrapper<SelfHostedNodeGroup>()
                .eq("coverage", coverage).eq("is_default", 1)
                .eq("status", "enabled").last("LIMIT 1"));
        if (group == null) {
            throw new BusinessException("请先创建并启用一个“" + coverageName(coverage) + "”默认节点组");
        }
        return group;
    }

    @Transactional(rollbackFor = Exception.class)
    public SelfHostedNode saveNode(SelfHostedNodeSaveRequest request) throws BusinessException {
        validateNode(request);
        SelfHostedNode node = request.getId() == null ? null : nodeDao.selectById(request.getId());
        if (request.getId() != null && node == null) {
            throw new BusinessException("节点不存在");
        }
        QueryWrapper<SelfHostedNode> duplicate = new QueryWrapper<SelfHostedNode>()
                .eq("host", request.getHost().trim()).eq("ssh_port", normalizePort(request.getSshPort()));
        if (request.getId() != null) {
            duplicate.ne("id", request.getId());
        }
        if (nodeDao.selectCount(duplicate) > 0) {
            throw new BusinessException("该 IP 和 SSH 端口已存在");
        }
        Date now = new Date();
        if (node == null) {
            node = new SelfHostedNode();
            node.setCreateTime(now);
            node.setStatus("pending");
            node.setDesiredConfigVersion(0L);
            node.setAppliedConfigVersion(0L);
            node.setRxBytes(0L);
            node.setTxBytes(0L);
            node.setRxRateBps(0L);
            node.setTxRateBps(0L);
            node.setCacheBytes(0L);
            node.setCacheDiskMount(DEFAULT_CACHE_DISK_MOUNT);
            node.setCacheMaxSizeGb(DEFAULT_CACHE_MAX_SIZE_GB);
            node.setCacheCleanupEnabled(1);
            node.setCacheCleanupAgeDays(DEFAULT_CACHE_CLEANUP_AGE_DAYS);
            node.setCacheCleanupMinHits(DEFAULT_CACHE_CLEANUP_MIN_HITS);
        }
        node.setNodeName(request.getNodeName().trim());
        node.setHost(request.getHost().trim());
        node.setSshPort(normalizePort(request.getSshPort()));
        node.setSshUsername(request.getSshUsername().trim());
        if (request.getSshPassword() != null && !request.getSshPassword().isEmpty()) {
            Map<String, String> secret = new HashMap<>();
            secret.put("password", request.getSshPassword());
            node.setSshPasswordCipher(ConfigureRsaUtils.encryptConfigStr(secret));
        }
        node.setRegion(trim(request.getRegion()));
        node.setWeight(request.getWeight() == null ? 100 : request.getWeight());
        node.setEnabled(request.getEnabled() == null ? 1 : request.getEnabled());
        String requestedCacheMount = request.getCacheDiskMount() == null
                ? normalizeStoredCacheDiskMount(node.getCacheDiskMount()) : request.getCacheDiskMount();
        node.setCacheDiskMount(normalizeCacheDiskMount(
                requestedCacheMount, node.getCacheDiskMount(), node.getDetectedDisksJson()));
        node.setCacheMaxSizeGb(request.getCacheMaxSizeGb() == null
                ? valueOrDefault(node.getCacheMaxSizeGb(), DEFAULT_CACHE_MAX_SIZE_GB)
                : request.getCacheMaxSizeGb());
        node.setCacheCleanupEnabled(request.getCacheCleanupEnabled() == null
                ? valueOrDefault(node.getCacheCleanupEnabled(), 1)
                : (request.getCacheCleanupEnabled() == 0 ? 0 : 1));
        node.setCacheCleanupAgeDays(request.getCacheCleanupAgeDays() == null
                ? valueOrDefault(node.getCacheCleanupAgeDays(), DEFAULT_CACHE_CLEANUP_AGE_DAYS)
                : request.getCacheCleanupAgeDays());
        node.setCacheCleanupMinHits(request.getCacheCleanupMinHits() == null
                ? valueOrDefault(node.getCacheCleanupMinHits(), DEFAULT_CACHE_CLEANUP_MIN_HITS)
                : request.getCacheCleanupMinHits());
        node.setRemark(trim(request.getRemark()));
        node.setUpdateTime(now);
        if (node.getId() == null) {
            nodeDao.insert(node);
        } else {
            nodeDao.updateById(node);
        }
        List<SelfHostedGroupNode> oldRelations = groupNodeDao.selectList(
                new QueryWrapper<SelfHostedGroupNode>().eq("node_id", node.getId()));
        Set<Long> targetGroupIds = new LinkedHashSet<>();
        if (request.getGroupIds() != null) {
            for (Long groupId : request.getGroupIds()) {
                if (groupId != null) {
                    targetGroupIds.add(groupId);
                }
            }
        }
        if (targetGroupIds.isEmpty() && request.getGroupId() != null) {
            targetGroupIds.add(request.getGroupId());
        }
        if (targetGroupIds.isEmpty()) {
            targetGroupIds.add(defaultGroup().getId());
        }
        groupNodeDao.delete(new QueryWrapper<SelfHostedGroupNode>().eq("node_id", node.getId()));
        for (Long targetGroupId : targetGroupIds) {
            assignNode(node.getId(), targetGroupId);
        }
        for (SelfHostedGroupNode oldRelation : oldRelations) {
            if (!targetGroupIds.contains(oldRelation.getGroupId())) {
                bumpGroupVersion(oldRelation.getGroupId());
            }
        }
        return nodeDao.selectById(node.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public SelfHostedNodeGroup saveGroup(SelfHostedGroupSaveRequest request) throws BusinessException {
        if (request == null || Assert.isEmpty(request.getGroupName()) || Assert.isEmpty(request.getCnameLabel())) {
            throw new BusinessException("节点组名称和 CNAME 标签不能为空");
        }
        String label = request.getCnameLabel().trim().toLowerCase();
        if (!LABEL_PATTERN.matcher(label).matches()) {
            throw new BusinessException("CNAME 标签格式不正确");
        }
        QueryWrapper<SelfHostedNodeGroup> duplicate = new QueryWrapper<SelfHostedNodeGroup>()
                .and(wrapper -> wrapper.eq("group_name", request.getGroupName().trim())
                        .or().eq("cname_label", label));
        if (request.getId() != null) {
            duplicate.ne("id", request.getId());
        }
        if (groupDao.selectCount(duplicate) > 0) {
            throw new BusinessException("节点组名称或调度标签已存在");
        }
        SelfHostedNodeGroup group = request.getId() == null ? null : groupDao.selectById(request.getId());
        if (request.getId() != null && group == null) {
            throw new BusinessException("节点组不存在");
        }
        String coverage = normalizeCoverage(request.getCoverage(), group);
        Date now = new Date();
        if (group == null) {
            group = new SelfHostedNodeGroup();
            group.setCreateTime(now);
        }
        group.setGroupName(request.getGroupName().trim());
        group.setCnameLabel(label);
        group.setCoverage(coverage);
        group.setIsDefault(request.getIsDefault() == null ? 0 : request.getIsDefault());
        group.setStatus(Assert.isEmpty(request.getStatus()) ? "enabled" : request.getStatus());
        group.setRemark(trim(request.getRemark()));
        group.setUpdateTime(now);
        if (group.getIsDefault() == 1) {
            groupDao.update(null, new UpdateWrapper<SelfHostedNodeGroup>()
                    .eq("coverage", coverage).set("is_default", 0));
        }
        if (group.getId() == null) {
            groupDao.insert(group);
        } else {
            groupDao.updateById(group);
        }
        if (request.getNodeIds() != null) {
            groupNodeDao.delete(new QueryWrapper<SelfHostedGroupNode>().eq("group_id", group.getId()));
            for (Long nodeId : request.getNodeIds()) {
                assignNode(nodeId, group.getId());
            }
        }
        bumpGroupVersion(group.getId());
        return groupDao.selectById(group.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignNode(Long nodeId, Long groupId) throws BusinessException {
        if (nodeDao.selectById(nodeId) == null || groupDao.selectById(groupId) == null) {
            throw new BusinessException("节点或节点组不存在");
        }
        if (groupNodeDao.selectCount(new QueryWrapper<SelfHostedGroupNode>()
                .eq("group_id", groupId).eq("node_id", nodeId)) == 0) {
            Date now = new Date();
            groupNodeDao.insert(SelfHostedGroupNode.builder().groupId(groupId).nodeId(nodeId)
                    .weight(100).priority(100).createTime(now).updateTime(now).build());
        }
        bumpGroupVersion(groupId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setNodeEnabled(Long id, boolean enabled) throws BusinessException {
        SelfHostedNode node = nodeDao.selectById(id);
        if (node == null) {
            throw new BusinessException("节点不存在");
        }
        node.setEnabled(enabled ? 1 : 0);
        node.setStatus(enabled ? "pending" : "disabled");
        node.setUpdateTime(new Date());
        nodeDao.updateById(node);
        recordStatusEventSafely(node.getId(), node.getStatus(), null);
        bumpNodeGroups(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteNode(Long id) throws BusinessException {
        if (nodeDao.selectById(id) == null) {
            throw new BusinessException("节点不存在");
        }
        List<SelfHostedGroupNode> relations = groupNodeDao.selectList(
                new QueryWrapper<SelfHostedGroupNode>().eq("node_id", id));
        groupNodeDao.delete(new QueryWrapper<SelfHostedGroupNode>().eq("node_id", id));
        deleteTelemetrySafely(id);
        nodeDao.deleteById(id);
        for (SelfHostedGroupNode relation : relations) {
            bumpGroupVersion(relation.getGroupId());
        }
    }

    public String issueAgentToken(Long nodeId) throws BusinessException {
        SelfHostedNode node = nodeDao.selectById(nodeId);
        if (node == null) {
            throw new BusinessException("节点不存在");
        }
        byte[] bytes = new byte[36];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        int updated = nodeDao.update(null, new UpdateWrapper<SelfHostedNode>()
                .eq("id", nodeId)
                .ne("status", "installing")
                .set("agent_token_hash", sha256(token))
                .set("status", "installing")
                .set("last_error", null)
                .set("update_time", new Date()));
        if (updated != 1) {
            throw new BusinessException("该节点正在安装中，请等待当前安装任务完成");
        }
        return token;
    }

    public SelfHostedNode authenticate(Long nodeId, String authorization) throws BusinessException {
        SelfHostedNode node = nodeDao.selectById(nodeId);
        if (node == null || Assert.isEmpty(node.getAgentTokenHash()) || Assert.isEmpty(authorization)
                || !authorization.startsWith("Bearer ")) {
            throw new BusinessException("节点认证失败");
        }
        String actual = sha256(authorization.substring(7).trim());
        if (!MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8),
                node.getAgentTokenHash().getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException("节点认证失败");
        }
        if (node.getEnabled() != null && node.getEnabled() == 0) {
            throw new BusinessException("节点已停用");
        }
        return node;
    }

    public JSONObject heartbeat(SelfHostedNode node, SelfHostedHeartbeatRequest request) {
        String previousStatus = node.getStatus();
        String previousError = node.getLastError();
        Date previousHeartbeat = node.getLastHeartbeat();
        long previousRx = valueOrZero(node.getRxBytes());
        long previousTx = valueOrZero(node.getTxBytes());
        Date heartbeatAt = new Date();
        long currentRx = valueOrZero(request.getRxBytes());
        long currentTx = valueOrZero(request.getTxBytes());
        node.setStatus(Assert.isEmpty(request.getLastError()) ? "online" : "degraded");
        node.setLastHeartbeat(heartbeatAt);
        node.setAgentVersion(trim(request.getAgentVersion()));
        node.setAppliedConfigVersion(request.getAppliedConfigVersion() == null ? node.getAppliedConfigVersion() : request.getAppliedConfigVersion());
        node.setCpuUsage(request.getCpuUsage());
        node.setMemoryUsage(request.getMemoryUsage());
        node.setDiskUsage(request.getDiskUsage());
        node.setRxBytes(currentRx);
        node.setTxBytes(currentTx);
        node.setRxRateBps(calculateRate(previousRx, currentRx, previousHeartbeat, heartbeatAt));
        node.setTxRateBps(calculateRate(previousTx, currentTx, previousHeartbeat, heartbeatAt));
        node.setCacheBytes(valueOrZero(request.getCacheBytes()));
        if (request.getDisks() != null) {
            node.setDetectedDisksJson(normalizeDetectedDisks(request.getDisks()).toJSONString());
        }
        node.setLastError(trim(request.getLastError()));
        node.setUpdateTime(heartbeatAt);
        nodeDao.updateById(node);
        recordHeartbeatSafely(node, previousStatus, previousError);
        boolean configCurrent = isNodeConfigCurrent(node);
        if (configCurrent) {
            reconcileAppliedDomains(node);
        }
        JSONObject response = new JSONObject();
        response.put("desiredConfigVersion", node.getDesiredConfigVersion());
        response.put("configChanged", !configCurrent);
        response.put("heartbeatIntervalSeconds", 30);
        response.put("cacheJobs", configCurrent ? pendingCacheJobs(node.getId()) : new JSONArray());
        return response;
    }

    public JSONArray pendingCacheJobs(Long nodeId) {
        JSONArray result = new JSONArray();
        List<SelfHostedCacheJobNode> records = cacheJobNodeDao.selectList(
                new QueryWrapper<SelfHostedCacheJobNode>().eq("node_id", nodeId).eq("status", "pending")
                        .orderByAsc("id").last("LIMIT 20"));
        for (SelfHostedCacheJobNode record : records) {
            SelfHostedCacheJob job = cacheJobDao.selectById(record.getJobId());
            if (job == null) continue;
            JSONObject item = new JSONObject();
            item.put("taskId", job.getTaskId());
            item.put("operation", job.getOperation());
            item.put("targetType", job.getTargetType());
            String targetsJson = Assert.isEmpty(record.getTargetsJson()) ? job.getTargetsJson() : record.getTargetsJson();
            item.put("targets", JSON.parseArray(targetsJson, String.class));
            result.add(item);
        }
        return result;
    }

    public void cacheResult(SelfHostedNode node, SelfHostedCacheResultRequest request) throws BusinessException {
        SelfHostedCacheJob job = cacheJobDao.selectOne(new QueryWrapper<SelfHostedCacheJob>()
                .eq("task_id", request.getTaskId()).last("LIMIT 1"));
        if (job == null) throw new BusinessException("缓存任务不存在");
        SelfHostedCacheJobNode record = cacheJobNodeDao.selectOne(new QueryWrapper<SelfHostedCacheJobNode>()
                .eq("job_id", job.getId()).eq("node_id", node.getId()).last("LIMIT 1"));
        if (record == null) throw new BusinessException("节点无权处理该缓存任务");
        record.setStatus(Boolean.TRUE.equals(request.getSuccess()) ? "completed" : "failed");
        record.setLastError(trim(request.getError()));
        record.setUpdateTime(new Date());
        cacheJobNodeDao.updateById(record);
        int success = cacheJobNodeDao.selectCount(new QueryWrapper<SelfHostedCacheJobNode>()
                .eq("job_id", job.getId()).eq("status", "completed")).intValue();
        int failed = cacheJobNodeDao.selectCount(new QueryWrapper<SelfHostedCacheJobNode>()
                .eq("job_id", job.getId()).eq("status", "failed")).intValue();
        job.setSuccessNodes(success);
        job.setFailedNodes(failed);
        if (success + failed >= job.getTotalNodes()) {
            job.setStatus(failed == 0 ? "completed" : "failed");
        } else {
            job.setStatus("processing");
        }
        job.setUpdateTime(new Date());
        cacheJobDao.updateById(job);
    }

    public JSONObject desiredConfig(SelfHostedNode node) {
        JSONObject result = new JSONObject();
        result.put("version", node.getDesiredConfigVersion());
        result.put("generatedAt", System.currentTimeMillis());
        JSONObject cachePolicy = new JSONObject();
        String cacheDiskMount = normalizeStoredCacheDiskMount(node.getCacheDiskMount());
        cachePolicy.put("diskMount", cacheDiskMount);
        cachePolicy.put("directory", cacheDirectory(cacheDiskMount));
        cachePolicy.put("maxSizeGb", valueOrDefault(node.getCacheMaxSizeGb(), DEFAULT_CACHE_MAX_SIZE_GB));
        cachePolicy.put("cleanupEnabled", valueOrDefault(node.getCacheCleanupEnabled(), 1) == 1);
        cachePolicy.put("cleanupAgeDays", valueOrDefault(
                node.getCacheCleanupAgeDays(), DEFAULT_CACHE_CLEANUP_AGE_DAYS));
        cachePolicy.put("cleanupMinHits", valueOrDefault(
                node.getCacheCleanupMinHits(), DEFAULT_CACHE_CLEANUP_MIN_HITS));
        result.put("cachePolicy", cachePolicy);
        JSONArray domains = new JSONArray();
        List<SelfHostedGroupNode> relations = groupNodeDao.selectList(
                new QueryWrapper<SelfHostedGroupNode>().eq("node_id", node.getId()));
        for (SelfHostedGroupNode relation : relations) {
            List<SelfHostedDomainConfig> configs = domainConfigDao.selectList(
                    new QueryWrapper<SelfHostedDomainConfig>().eq("node_group_id", relation.getGroupId())
                            .eq("status", "enabled"));
            for (SelfHostedDomainConfig config : configs) {
                CdnDomain domain = cdnDomainDao.selectById(config.getCdnDomainId());
                if (domain == null || !CdnRoute.isSelfHosted(domain.getRoute()) || "offline".equals(domain.getDomainStatus())) {
                    continue;
                }
                JSONObject normalizedOriginConfig;
                String normalizedOriginAddress;
                String normalizedDomainName;
                try {
                    normalizedDomainName = SelfHostedDomainValidator.validateAndNormalize(domain.getDomainName());
                    normalizedOriginAddress = SelfHostedOriginValidator.validateAndNormalize(
                            config.getOriginType(), config.getOriginAddress(), "主源站");
                    normalizedOriginConfig = validateAndNormalizeNestedOrigins(config.getOriginConfigJson());
                } catch (BusinessException e) {
                    quarantineInvalidDomainConfig(config, domain, e.getMessage());
                    continue;
                }
                JSONObject item = (JSONObject) JSON.toJSON(config);
                item.remove("certificateCipher");
                item.remove("privateKeyCipher");
                item.remove("accessConfigCipher");
                item.put("domainName", SelfHostedDomainValidator.toAgentServerName(normalizedDomainName));
                item.put("cname", domain.getCname());
                item.put("originAddress", normalizedOriginAddress);
                if (Assert.isEmpty(config.getOriginHost())
                        || normalizedDomainName.equalsIgnoreCase(config.getOriginHost().trim())) {
                    item.put("originHost", SelfHostedDomainValidator.defaultOriginHost(normalizedDomainName));
                }
                item.put("originConfigJson", normalizedOriginConfig.toJSONString());
                item.put("accessConfigJson", readAccessConfig(config).toJSONString());
                item.put("certificate", decryptSecret(config.getCertificateCipher(), "certificate"));
                item.put("privateKey", decryptSecret(config.getPrivateKeyCipher(), "privateKey"));
                domains.add(item);
            }
        }
        result.put("domains", domains);
        JSONArray portForwards = new JSONArray();
        Set<Long> processedPortForwardIds = new LinkedHashSet<>();
        for (SelfHostedGroupNode relation : relations) {
            List<SelfHostedPortForward> rules = portForwardDao.selectList(
                    new QueryWrapper<SelfHostedPortForward>().eq("node_group_id", relation.getGroupId())
                            .eq("status", "enabled"));
            if (rules == null) {
                continue;
            }
            for (SelfHostedPortForward rule : rules) {
                if (!processedPortForwardIds.add(rule.getId())) {
                    continue;
                }
                JSONObject item = (JSONObject) JSON.toJSON(rule);
                item.put("ruleId", rule.getId());
                portForwards.add(item);
            }
        }
        result.put("portForwards", portForwards);
        return result;
    }

    public void applyResult(SelfHostedNode node, SelfHostedApplyResultRequest request) {
        String previousStatus = node.getStatus();
        String previousError = node.getLastError();
        if (Boolean.TRUE.equals(request.getSuccess())) {
            node.setAppliedConfigVersion(request.getVersion());
            node.setStatus("online");
            node.setLastError(null);
            node.setUpdateTime(new Date());
            nodeDao.update(null, new UpdateWrapper<SelfHostedNode>()
                    .eq("id", node.getId())
                    .set("applied_config_version", request.getVersion())
                    .set("status", "online")
                    .set("last_error", null)
                    .set("update_time", node.getUpdateTime()));
        } else {
            node.setStatus("degraded");
            node.setLastError(trim(request.getError()));
            node.setUpdateTime(new Date());
            nodeDao.updateById(node);
        }
        if (!java.util.Objects.equals(previousStatus, node.getStatus())
                || !java.util.Objects.equals(previousError, node.getLastError())) {
            recordStatusEventSafely(node.getId(), node.getStatus(), node.getLastError());
        }
        if (Boolean.TRUE.equals(request.getSuccess()) && isNodeConfigCurrent(node)) {
            reconcileAppliedDomains(node);
        }
    }

    public boolean isDomainConfigurationApplied(Long cdnDomainId) throws BusinessException {
        SelfHostedDomainConfig config = getDomainConfig(cdnDomainId);
        long now = System.currentTimeMillis();
        for (SelfHostedNode node : nodesInGroup(config.getNodeGroupId(), true)) {
            if (node.getLastHeartbeat() != null
                    && now - node.getLastHeartbeat().getTime() <= OFFLINE_AFTER_MS
                    && isNodeConfigCurrent(node)) {
                return true;
            }
        }
        return false;
    }

    public boolean waitForDomainConfigurationApplied(Long cdnDomainId, long timeoutMs) throws BusinessException {
        long deadline = System.currentTimeMillis() + Math.max(0L, timeoutMs);
        while (System.currentTimeMillis() <= deadline) {
            if (isDomainConfigurationApplied(cdnDomainId)) {
                return true;
            }
            try {
                Thread.sleep(CONFIG_APPLY_POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException("等待节点配置下发时任务被中断");
            }
        }
        return false;
    }

    public void expireStaleCacheJobs() {
        Date cutoff = new Date(System.currentTimeMillis() - CACHE_JOB_TIMEOUT_MS);
        List<SelfHostedCacheJob> jobs = cacheJobDao.selectList(new QueryWrapper<SelfHostedCacheJob>()
                .in("status", "pending", "processing").lt("update_time", cutoff));
        for (SelfHostedCacheJob job : jobs) {
            cacheJobNodeDao.update(null, new UpdateWrapper<SelfHostedCacheJobNode>()
                    .eq("job_id", job.getId()).in("status", "pending", "processing")
                    .set("status", "failed")
                    .set("last_error", "节点在规定时间内未返回缓存任务结果")
                    .set("update_time", new Date()));
            int success = cacheJobNodeDao.selectCount(new QueryWrapper<SelfHostedCacheJobNode>()
                    .eq("job_id", job.getId()).eq("status", "completed")).intValue();
            int failed = cacheJobNodeDao.selectCount(new QueryWrapper<SelfHostedCacheJobNode>()
                    .eq("job_id", job.getId()).eq("status", "failed")).intValue();
            job.setSuccessNodes(success);
            job.setFailedNodes(failed);
            job.setStatus("failed");
            job.setUpdateTime(new Date());
            cacheJobDao.updateById(job);
        }
    }

    public void markStaleNodesOffline() {
        Date cutoff = new Date(System.currentTimeMillis() - OFFLINE_AFTER_MS);
        List<SelfHostedNode> staleNodes = nodeDao.selectList(new QueryWrapper<SelfHostedNode>()
                .eq("enabled", 1)
                .lt("last_heartbeat", cutoff)
                .ne("status", "offline"));
        for (SelfHostedNode node : staleNodes) {
            node.setStatus("offline");
            node.setRxRateBps(0L);
            node.setTxRateBps(0L);
            node.setUpdateTime(new Date());
            nodeDao.updateById(node);
            recordStatusEventSafely(node.getId(), "offline", node.getLastError());
        }
    }

    public void purgeNodeTelemetry() {
        if (telemetryService != null) {
            telemetryService.purgeExpired();
        }
    }

    public SelfHostedDomainConfig createDomainConfig(CdnDomain domain, String originType, String originAddress,
                                                      String originProtocol, Integer httpPort, Integer httpsPort,
                                                      String originHost) throws BusinessException {
        originAddress = SelfHostedOriginValidator.validateAndNormalize(originType, originAddress, "主源站");
        SelfHostedNodeGroup group = defaultGroup(domain.getRoute());
        requireActiveNode(group.getId());
        Date now = new Date();
        SelfHostedDomainConfig config = SelfHostedDomainConfig.builder()
                .cdnDomainId(domain.getId()).nodeGroupId(group.getId()).originType(originType)
                .originAddress(originAddress).originProtocol(normalizeProtocol(originProtocol))
                .httpPort(httpPort == null ? 80 : httpPort).httpsPort(httpsPort == null ? 443 : httpsPort)
                .originHost(Assert.isEmpty(originHost) ? domain.getDomainName() : originHost.trim())
                .originConfigJson(defaultOriginConfig().toJSONString())
                .cacheConfigJson("{\"defaultTtl\":3600,\"inactive\":\"7d\"}")
                .accessConfigCipher(encryptConfigJson(new JSONObject()))
                .advancedConfigJson(defaultAdvancedConfig().toJSONString())
                .httpsConfigJson(defaultHttpsConfig().toJSONString())
                .ipv6Enabled(0)
                .httpsEnabled(0).forceRedirect("off").desiredConfigVersion(1L)
                .status("enabled").createTime(now).updateTime(now).build();
        domainConfigDao.insert(config);
        bumpGroupVersion(group.getId());
        return config;
    }

    private JSONObject validateAndNormalizeNestedOrigins(String originConfigJson) throws BusinessException {
        JSONObject originConfig;
        try {
            originConfig = Assert.isEmpty(originConfigJson)
                    ? new JSONObject() : JSON.parseObject(originConfigJson);
        } catch (Exception e) {
            throw new BusinessException("回源配置格式不正确");
        }
        if (originConfig == null) {
            originConfig = new JSONObject();
        }
        JSONObject standby = originConfig.getJSONObject("standby");
        if (standby != null && !Assert.isEmpty(standby.getString("ipOrDomain"))) {
            standby.put("ipOrDomain", SelfHostedOriginValidator.validateAndNormalize(
                    standby.getString("originType"), standby.getString("ipOrDomain"), "备用源站"));
        }
        JSONArray flexibleOrigins = originConfig.getJSONArray("flexibleOrigins");
        if (flexibleOrigins != null) {
            for (int i = 0; i < flexibleOrigins.size(); i++) {
                JSONObject rule = flexibleOrigins.getJSONObject(i);
                JSONArray backSources = rule == null ? null : rule.getJSONArray("back_sources");
                if (backSources == null) {
                    continue;
                }
                for (int j = 0; j < backSources.size(); j++) {
                    JSONObject source = backSources.getJSONObject(j);
                    if (source == null || Assert.isEmpty(source.getString("ip_or_domain"))) {
                        continue;
                    }
                    source.put("ip_or_domain", SelfHostedOriginValidator.validateAndNormalize(
                            source.getString("sources_type"), source.getString("ip_or_domain"), "高级回源"));
                }
            }
        }
        return originConfig;
    }

    private void quarantineInvalidDomainConfig(SelfHostedDomainConfig config, CdnDomain domain, String error) {
        String message = "自建 CDN 配置无效，已自动隔离：" + error;
        if (message.length() > 1000) {
            message = message.substring(0, 1000);
        }
        try {
            domainConfigDao.update(null, new UpdateWrapper<SelfHostedDomainConfig>()
                    .eq("id", config.getId())
                    .set("status", "invalid")
                    .set("last_error", message)
                    .set("update_time", new Date()));
            if (domain != null) {
                domain.setDomainStatus("configure_failed");
                domain.setUpdateTime(new Date());
                cdnDomainDao.updateById(domain);
            }
        } catch (Exception persistenceError) {
            log.error("Unable to persist quarantined self-hosted domain configuration, configId={}",
                    config.getId(), persistenceError);
        }
        log.error("Self-hosted domain configuration quarantined, domain={}, configId={}, reason={}",
                domain == null ? config.getCdnDomainId() : domain.getDomainName(), config.getId(), error);
    }

    public SelfHostedDomainConfig getDomainConfig(Long cdnDomainId) throws BusinessException {
        SelfHostedDomainConfig config = domainConfigDao.selectOne(new QueryWrapper<SelfHostedDomainConfig>()
                .eq("cdn_domain_id", cdnDomainId).last("LIMIT 1"));
        if (config == null) {
            throw new BusinessException("自建 CDN 域名配置不存在");
        }
        return config;
    }

    public SelfHostedDomainConfig getDomainConfigByName(String domainName) throws BusinessException {
        CdnDomain domain = cdnDomainDao.selectOne(new QueryWrapper<CdnDomain>()
                .eq("domain_name", domainName).in("route", CdnRoute.selfHostedCodes()).last("LIMIT 1"));
        if (domain == null) {
            throw new BusinessException("自建 CDN 域名不存在");
        }
        return getDomainConfig(domain.getId());
    }

    public void updateDomainConfig(SelfHostedDomainConfig config) {
        config.setDesiredConfigVersion((config.getDesiredConfigVersion() == null ? 0L : config.getDesiredConfigVersion()) + 1L);
        config.setUpdateTime(new Date());
        domainConfigDao.updateById(config);
        if ("enabled".equals(config.getStatus())) {
            config.setLastError(null);
            domainConfigDao.update(null, new UpdateWrapper<SelfHostedDomainConfig>()
                    .eq("id", config.getId()).set("last_error", null));
        }
        bumpGroupVersion(config.getNodeGroupId());
    }

    public JSONObject readAccessConfig(SelfHostedDomainConfig config) {
        if (config == null || Assert.isEmpty(config.getAccessConfigCipher())) {
            return new JSONObject();
        }
        try {
            Map<?, ?> values = ConfigureRsaUtils.decryptConfigStr(config.getAccessConfigCipher(), Map.class);
            return JSON.parseObject(JSON.toJSONString(values));
        } catch (Exception e) {
            log.warn("Unable to decrypt self-hosted access configuration, configId={}", config.getId());
            return new JSONObject();
        }
    }

    public void writeAccessConfig(SelfHostedDomainConfig config, JSONObject accessConfig) {
        config.setAccessConfigCipher(encryptConfigJson(accessConfig == null ? new JSONObject() : accessConfig));
        updateDomainConfig(config);
    }

    public void deleteDomainConfig(Long cdnDomainId) {
        SelfHostedDomainConfig config = domainConfigDao.selectOne(new QueryWrapper<SelfHostedDomainConfig>()
                .eq("cdn_domain_id", cdnDomainId).last("LIMIT 1"));
        if (config != null) {
            domainConfigDao.deleteById(config.getId());
            bumpGroupVersion(config.getNodeGroupId());
        }
    }

    public void saveCertificate(SelfHostedDomainConfig config, boolean enabled, String certificate, String privateKey,
                                String forceRedirect) throws BusinessException {
        boolean newCertificateProvided = !Assert.isEmpty(certificate) || !Assert.isEmpty(privateKey);
        if (enabled && newCertificateProvided && (Assert.isEmpty(certificate) || Assert.isEmpty(privateKey))) {
            throw new BusinessException("开启 HTTPS 时证书和私钥不能为空");
        }
        if (enabled && !newCertificateProvided
                && (Assert.isEmpty(config.getCertificateCipher()) || Assert.isEmpty(config.getPrivateKeyCipher()))) {
            throw new BusinessException("首次开启 HTTPS 时证书和私钥不能为空");
        }
        config.setHttpsEnabled(enabled ? 1 : 0);
        if (enabled) {
            if (newCertificateProvided) {
                config.setCertificateCipher(encryptSecret("certificate", certificate));
                config.setPrivateKeyCipher(encryptSecret("privateKey", privateKey));
            }
        } else {
            domainConfigDao.update(null, new UpdateWrapper<SelfHostedDomainConfig>()
                    .eq("id", config.getId()).set("certificate_cipher", null)
                    .set("private_key_cipher", null));
            config.setCertificateCipher(null);
            config.setPrivateKeyCipher(null);
        }
        config.setForceRedirect(Assert.isEmpty(forceRedirect) ? "off" : forceRedirect);
        updateDomainConfig(config);
    }

    public String groupCname(SelfHostedNodeGroup group) {
        return group.getCnameLabel() + "." + TencentDns.LOCAL_DOMAIN_NAME;
    }

    public synchronized void syncGroupDns(Long groupId) throws BusinessException {
        SelfHostedNodeGroup group = groupDao.selectById(groupId);
        if (group == null) {
            throw new BusinessException("节点组不存在");
        }
        List<SelfHostedNode> nodes = healthyNodesInGroup(groupId);
        boolean ipv6Required = domainConfigDao.selectCount(new QueryWrapper<SelfHostedDomainConfig>()
                .eq("node_group_id", groupId).eq("status", "enabled").eq("ipv6_enabled", 1)) > 0;
        Set<String> desiredAddresses = new LinkedHashSet<>();
        for (SelfHostedNode node : nodes) {
            Map<String, String> addresses = resolveNodeAddresses(node.getHost(), ipv6Required);
            for (Map.Entry<String, String> address : addresses.entrySet()) {
                desiredAddresses.add(SelfHostedDnsPlan.recordKey(address.getKey(), address.getValue()));
            }
        }

        SelfHostedDnsPlan.State state = SelfHostedDnsPlan.State.parse(group.getDnsRecordIds());
        SelfHostedDnsPlan.Plan plan = SelfHostedDnsPlan.build(
                group, desiredAddresses, state, TencentDns.LOCAL_DOMAIN_NAME);
        try {
            deleteStaleParentRecords(group, state, plan.parentRecords);
            deleteStaleShardRecords(group, state, plan.shardAddresses);
            createMissingShardRecords(group, state, plan.shardAddresses);
            deleteLegacyDirectRecords(group, state);
            createMissingParentRecords(group, state, plan.parentRecords);

            state.shardRecords.keySet().retainAll(plan.shardAddresses.keySet());
            for (String shard : new ArrayList<>(state.shardRecords.keySet())) {
                if (state.shardRecords.get(shard).isEmpty()) {
                    state.shardRecords.remove(shard);
                }
            }
            persistDnsState(group, state);
        } catch (Exception e) {
            if (e instanceof BusinessException && e.getMessage() != null
                    && e.getMessage().startsWith("自建 CDN 节点组最多支持")) {
                throw (BusinessException) e;
            }
            throw new BusinessException("节点组 DNS 同步失败：" + e.getMessage()).setCause(e);
        }
    }

    private void deleteStaleParentRecords(SelfHostedNodeGroup group, SelfHostedDnsPlan.State state,
                                          Set<String> desiredParentRecords) throws Exception {
        for (String key : new ArrayList<>(state.parentRecords.keySet())) {
            if (!desiredParentRecords.contains(key)) {
                deleteTrackedDnsRecord(group, state, state.parentRecords, key);
            }
        }
    }

    private void deleteStaleShardRecords(SelfHostedNodeGroup group, SelfHostedDnsPlan.State state,
                                         Map<String, LinkedHashSet<String>> desiredShards) throws Exception {
        for (String shard : new ArrayList<>(state.shardRecords.keySet())) {
            Set<String> desired = desiredShards.get(shard);
            Map<String, Long> records = state.shardRecords.get(shard);
            for (String key : new ArrayList<>(records.keySet())) {
                if (desired == null || !desired.contains(key)) {
                    deleteTrackedDnsRecord(group, state, records, key);
                }
            }
            if (desired == null && records.isEmpty()) {
                state.shardRecords.remove(shard);
                persistDnsState(group, state);
            }
        }
    }

    private void createMissingShardRecords(SelfHostedNodeGroup group, SelfHostedDnsPlan.State state,
                                           Map<String, LinkedHashSet<String>> desiredShards) throws Exception {
        for (Map.Entry<String, LinkedHashSet<String>> shard : desiredShards.entrySet()) {
            LinkedHashMap<String, Long> records = state.shardRecords.computeIfAbsent(
                    shard.getKey(), key -> new LinkedHashMap<>());
            for (String recordKey : shard.getValue()) {
                if (!records.containsKey(recordKey)) {
                    createTrackedDnsRecord(group, state, records, recordKey, shard.getKey());
                }
            }
        }
    }

    private void deleteLegacyDirectRecords(SelfHostedNodeGroup group,
                                           SelfHostedDnsPlan.State state) throws Exception {
        for (String key : new ArrayList<>(state.legacyDirectRecords.keySet())) {
            deleteTrackedDnsRecord(group, state, state.legacyDirectRecords, key);
        }
        for (Long recordId : new ArrayList<>(state.legacyRecordIds)) {
            deleteDnsRecordAllowMissing(recordId);
            state.legacyRecordIds.remove(recordId);
            persistDnsState(group, state);
        }
    }

    private void createMissingParentRecords(SelfHostedNodeGroup group, SelfHostedDnsPlan.State state,
                                            Set<String> desiredParentRecords) throws Exception {
        for (String recordKey : desiredParentRecords) {
            if (!state.parentRecords.containsKey(recordKey)) {
                createTrackedDnsRecord(group, state, state.parentRecords,
                        recordKey, group.getCnameLabel());
            }
        }
    }

    private void createTrackedDnsRecord(SelfHostedNodeGroup group, SelfHostedDnsPlan.State state,
                                        Map<String, Long> records, String recordKey,
                                        String subDomain) throws Exception {
        CreateRecordDTO dto = new CreateRecordDTO().setDomain(TencentDns.LOCAL_DOMAIN_NAME)
                .setSubDomain(subDomain)
                .setRecordType(SelfHostedDnsPlan.recordType(recordKey))
                .setRecordLine("默认")
                .setValue(SelfHostedDnsPlan.recordValue(recordKey))
                .setTTL(DNS_RECORD_TTL_SECONDS);
        CreateRecordResponse response = createDnsRecord(dto);
        if (response == null || response.getRecordId() == null) {
            throw new BusinessException("DNS 服务未返回节点记录 ID");
        }
        records.put(recordKey, response.getRecordId());
        try {
            persistDnsState(group, state);
        } catch (Exception persistError) {
            records.remove(recordKey);
            try {
                deleteDnsRecord(response.getRecordId());
            } catch (Exception cleanupError) {
                log.warn("Failed to roll back an untracked self-hosted DNS record, recordId={}",
                        response.getRecordId(), cleanupError);
            }
            throw persistError;
        }
    }

    private void deleteTrackedDnsRecord(SelfHostedNodeGroup group, SelfHostedDnsPlan.State state,
                                        Map<String, Long> records, String recordKey) throws Exception {
        Long recordId = records.get(recordKey);
        if (recordId != null) {
            deleteDnsRecordAllowMissing(recordId);
        }
        records.remove(recordKey);
        persistDnsState(group, state);
    }

    private void deleteDnsRecordAllowMissing(Long recordId) throws Exception {
        try {
            deleteDnsRecord(recordId);
        } catch (Exception e) {
            String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
            if (!message.contains("记录不存在") && !message.contains("record not exist")
                    && !message.contains("invalidparameter.recordid")
                    && !message.contains("invalid record id")) {
                throw e;
            }
        }
    }

    private void persistDnsState(SelfHostedNodeGroup group,
                                 SelfHostedDnsPlan.State state) throws BusinessException {
        group.setDnsRecordIds(state.toJson());
        group.setUpdateTime(new Date());
        if (groupDao.updateById(group) != 1) {
            throw new BusinessException("保存节点组 DNS 状态失败");
        }
    }

    protected CreateRecordResponse createDnsRecord(CreateRecordDTO request) throws Exception {
        return TencentApi.createRecord(request);
    }

    protected void deleteDnsRecord(Long recordId) throws Exception {
        TencentApi.deleteRecord(new DeleteRecordDTO().setDomain(TencentDns.LOCAL_DOMAIN_NAME)
                .setRecordId(recordId));
    }

    public List<SelfHostedNode> nodesInGroup(Long groupId, boolean enabledOnly) {
        List<SelfHostedGroupNode> relations = groupNodeDao.selectList(
                new QueryWrapper<SelfHostedGroupNode>().eq("group_id", groupId));
        List<SelfHostedNode> result = new ArrayList<>();
        for (SelfHostedGroupNode relation : relations) {
            SelfHostedNode node = nodeDao.selectById(relation.getNodeId());
            if (node != null && (!enabledOnly || (node.getEnabled() != null && node.getEnabled() == 1))) {
                result.add(node);
            }
        }
        return result;
    }

    public List<SelfHostedNode> healthyNodesInGroup(Long groupId) {
        List<SelfHostedNode> result = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (SelfHostedNode node : nodesInGroup(groupId, true)) {
            if (node.getLastHeartbeat() != null
                    && now - node.getLastHeartbeat().getTime() <= OFFLINE_AFTER_MS
                    && ("online".equals(node.getStatus()) || "degraded".equals(node.getStatus()))) {
                result.add(node);
            }
        }
        return result;
    }

    public void requireActiveNode(Long groupId) throws BusinessException {
        if (activeNodeCount(groupId) == 0) {
            throw new BusinessException("自建 CDN 节点组中没有在线节点，请先安装节点并等待节点上线");
        }
    }

    public void reconcileGroupDns() {
        for (SelfHostedNodeGroup group : listGroups()) {
            if (!"enabled".equals(group.getStatus())) continue;
            try {
                syncGroupDns(group.getId());
            } catch (BusinessException e) {
                log.warn("Self-hosted CDN group DNS reconciliation failed, groupId={}: {}",
                        group.getId(), e.getMessage());
            }
        }
    }

    public String decryptSshPassword(SelfHostedNode node) throws BusinessException {
        if (node == null || Assert.isEmpty(node.getSshPasswordCipher())) {
            throw new BusinessException("节点未保存 SSH 密码，请重新编辑节点");
        }
        try {
            Map<?, ?> secret = ConfigureRsaUtils.decryptConfigStr(node.getSshPasswordCipher(), Map.class);
            Object password = secret.get("password");
            if (password == null) {
                throw new IllegalStateException("missing password");
            }
            return String.valueOf(password);
        } catch (Exception e) {
            throw new BusinessException("SSH 密码解密失败");
        }
    }

    public SelfHostedNode getNode(Long id) throws BusinessException {
        SelfHostedNode node = nodeDao.selectById(id);
        if (node == null) {
            throw new BusinessException("节点不存在");
        }
        return node;
    }

    public void recordHostKey(SelfHostedNode node, String hostKey) {
        node.setSshHostKey(hostKey);
        node.setUpdateTime(new Date());
        nodeDao.updateById(node);
    }

    public void markInstalled(SelfHostedNode node) {
        nodeDao.update(null, new UpdateWrapper<SelfHostedNode>().eq("id", node.getId())
                .set("status", "pending").set("ssh_password_cipher", null)
                .set("last_error", null).set("update_time", new Date()));
        recordStatusEventSafely(node.getId(), "pending", null);
        bumpNodeGroups(node.getId());
    }

    public void markGroupConfigurationChanged(Long groupId) {
        if (groupId != null) {
            bumpGroupVersion(groupId);
        }
    }

    public void markInstallFailed(SelfHostedNode node, String error) {
        node.setStatus("install_failed");
        node.setLastError(error == null ? null : error.substring(0, Math.min(error.length(), 1000)));
        node.setUpdateTime(new Date());
        nodeDao.updateById(node);
        recordStatusEventSafely(node.getId(), "install_failed", node.getLastError());
    }

    private long calculateRate(long previous, long current, Date previousTime, Date currentTime) {
        if (previousTime == null || current < previous) {
            return 0L;
        }
        long elapsedMs = currentTime.getTime() - previousTime.getTime();
        if (elapsedMs < 1000L) {
            return 0L;
        }
        double rate = (current - previous) * 1000.0d / elapsedMs;
        return rate >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0L, Math.round(rate));
    }

    private void recordHeartbeatSafely(SelfHostedNode node, String previousStatus, String previousError) {
        if (telemetryService == null) {
            return;
        }
        try {
            telemetryService.recordHeartbeat(node, previousStatus, previousError);
        } catch (Exception e) {
            log.warn("Unable to record self-hosted node heartbeat telemetry, nodeId={}: {}",
                    node.getId(), e.getMessage());
        }
    }

    private void recordStatusEventSafely(Long nodeId, String status, String details) {
        if (telemetryService == null) {
            return;
        }
        try {
            telemetryService.recordStatusEvent(nodeId, status, details);
        } catch (Exception e) {
            log.warn("Unable to record self-hosted node status event, nodeId={}: {}", nodeId, e.getMessage());
        }
    }

    private void deleteTelemetrySafely(Long nodeId) {
        if (telemetryService == null) {
            return;
        }
        try {
            telemetryService.deleteForNode(nodeId);
        } catch (Exception e) {
            log.warn("Unable to delete self-hosted node telemetry, nodeId={}: {}", nodeId, e.getMessage());
        }
    }

    private void validateNode(SelfHostedNodeSaveRequest request) throws BusinessException {
        if (request == null || Assert.isEmpty(request.getNodeName()) || Assert.isEmpty(request.getHost())
                || Assert.isEmpty(request.getSshUsername())) {
            throw new BusinessException("节点名称、IP/主机名和 SSH 用户名不能为空");
        }
        if (!HOST_PATTERN.matcher(request.getHost().trim()).matches()) {
            throw new BusinessException("节点 IP 或主机名格式不正确");
        }
        if (!USER_PATTERN.matcher(request.getSshUsername().trim()).matches()) {
            throw new BusinessException("SSH 用户名格式不正确");
        }
        int port = normalizePort(request.getSshPort());
        if (port < 1 || port > 65535) {
            throw new BusinessException("SSH 端口必须在 1-65535 范围内");
        }
        if (request.getWeight() != null && (request.getWeight() < 1 || request.getWeight() > 1000)) {
            throw new BusinessException("节点权重必须在 1-1000 范围内");
        }
        if (request.getCacheMaxSizeGb() != null
                && (request.getCacheMaxSizeGb() < 1 || request.getCacheMaxSizeGb() > 102400)) {
            throw new BusinessException("最大缓存容量必须在 1-102400 GB 范围内");
        }
        if (request.getCacheCleanupAgeDays() != null
                && (request.getCacheCleanupAgeDays() < 1 || request.getCacheCleanupAgeDays() > 3650)) {
            throw new BusinessException("缓存清理时间必须在 1-3650 天范围内");
        }
        if (request.getCacheCleanupMinHits() != null
                && (request.getCacheCleanupMinHits() < 1 || request.getCacheCleanupMinHits() > 1000000)) {
            throw new BusinessException("缓存访问次数阈值必须在 1-1000000 次范围内");
        }
        if (request.getId() == null && Assert.isEmpty(request.getSshPassword())) {
            throw new BusinessException("新增节点时 SSH 密码不能为空");
        }
    }

    private JSONArray normalizeDetectedDisks(List<SelfHostedDiskInfo> disks) {
        JSONArray result = new JSONArray();
        if (disks == null) {
            return result;
        }
        Set<String> seenMounts = new LinkedHashSet<>();
        for (SelfHostedDiskInfo disk : disks) {
            if (disk == null || result.size() >= 64) {
                continue;
            }
            String mountPath = safeReportedMount(disk.getMountPath());
            if (mountPath == null || !seenMounts.add(mountPath)) {
                continue;
            }
            JSONObject item = new JSONObject();
            item.put("device", trimLength(disk.getDevice(), 255));
            item.put("mountPath", mountPath);
            item.put("fsType", trimLength(disk.getFsType(), 64));
            item.put("totalBytes", Math.max(0L, valueOrZero(disk.getTotalBytes())));
            item.put("availableBytes", Math.max(0L, valueOrZero(disk.getAvailableBytes())));
            item.put("usedPercent", disk.getUsedPercent());
            item.put("writable", Boolean.TRUE.equals(disk.getWritable()));
            result.add(item);
        }
        return result;
    }

    private JSONArray parseDetectedDisks(String json) {
        if (Assert.isEmpty(json)) {
            return new JSONArray();
        }
        try {
            JSONArray disks = JSON.parseArray(json);
            return disks == null ? new JSONArray() : disks;
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private String normalizeCacheDiskMount(String requested, String current,
                                           String detectedDisksJson) throws BusinessException {
        String mount = safeReportedMount(requested);
        if (mount == null) {
            throw new BusinessException("缓存磁盘挂载点格式不正确");
        }
        if (DEFAULT_CACHE_DISK_MOUNT.equals(mount)
                || mount.equals(normalizeStoredCacheDiskMount(current))) {
            return mount;
        }
        JSONArray disks = parseDetectedDisks(detectedDisksJson);
        for (int index = 0; index < disks.size(); index++) {
            JSONObject disk = disks.getJSONObject(index);
            if (disk != null && mount.equals(safeReportedMount(disk.getString("mountPath")))) {
                if (!disk.getBooleanValue("writable")) {
                    throw new BusinessException("所选缓存磁盘当前不可写");
                }
                return mount;
            }
        }
        throw new BusinessException("所选磁盘未被节点检测到，请等待节点心跳刷新后重试");
    }

    private String normalizeStoredCacheDiskMount(String value) {
        String normalized = safeReportedMount(value);
        return normalized == null ? DEFAULT_CACHE_DISK_MOUNT : normalized;
    }

    private String safeReportedMount(String value) {
        if (value == null) {
            return null;
        }
        String mount = value.trim().replace('\\', '/');
        while (mount.length() > 1 && mount.endsWith("/")) {
            mount = mount.substring(0, mount.length() - 1);
        }
        if (!mount.startsWith("/") || mount.length() > 512
                || mount.indexOf('\n') >= 0 || mount.indexOf('\r') >= 0 || mount.indexOf('\0') >= 0) {
            return null;
        }
        for (String segment : mount.split("/")) {
            if ("..".equals(segment)) {
                return null;
            }
        }
        return mount;
    }

    static String cacheDirectory(String mountPath) {
        String mount = mountPath == null || mountPath.trim().isEmpty()
                ? DEFAULT_CACHE_DISK_MOUNT : mountPath.trim();
        return DEFAULT_CACHE_DISK_MOUNT.equals(mount)
                ? "/var/cache/kuocai-cdn" : mount + "/kuocai-cdn-cache";
    }

    private int valueOrDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String trimLength(String value, int maxLength) {
        String normalized = trim(value);
        if (normalized == null || normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private int activeNodeCount(Long groupId) {
        return healthyNodesInGroup(groupId).size();
    }

    private void bumpNodeGroups(Long nodeId) {
        List<SelfHostedGroupNode> relations = groupNodeDao.selectList(
                new QueryWrapper<SelfHostedGroupNode>().eq("node_id", nodeId));
        for (SelfHostedGroupNode relation : relations) {
            bumpGroupVersion(relation.getGroupId());
        }
    }

    private void bumpGroupVersion(Long groupId) {
        long now = System.currentTimeMillis();
        List<SelfHostedNode> nodes = nodesInGroup(groupId, false);
        for (SelfHostedNode node : nodes) {
            long current = node.getDesiredConfigVersion() == null ? 0L : node.getDesiredConfigVersion();
            long version = Math.max(now, current + 1L);
            node.setDesiredConfigVersion(version);
            node.setUpdateTime(new Date());
            nodeDao.updateById(node);
        }
    }

    private boolean isNodeConfigCurrent(SelfHostedNode node) {
        return node != null
                && node.getDesiredConfigVersion() != null
                && node.getDesiredConfigVersion() > 0
                && safeEquals(node.getDesiredConfigVersion(), node.getAppliedConfigVersion());
    }

    private void reconcileAppliedDomains(SelfHostedNode node) {
        List<SelfHostedGroupNode> relations = groupNodeDao.selectList(
                new QueryWrapper<SelfHostedGroupNode>().eq("node_id", node.getId()));
        Set<Long> processedDomainIds = new LinkedHashSet<>();
        for (SelfHostedGroupNode relation : relations) {
            List<SelfHostedDomainConfig> configs = domainConfigDao.selectList(
                    new QueryWrapper<SelfHostedDomainConfig>().eq("node_group_id", relation.getGroupId())
                            .eq("status", "enabled"));
            for (SelfHostedDomainConfig config : configs) {
                if (!processedDomainIds.add(config.getCdnDomainId())) {
                    continue;
                }
                CdnDomain domain = cdnDomainDao.selectById(config.getCdnDomainId());
                if (domain == null || !CdnRoute.isSelfHosted(domain.getRoute())
                        || !"configuring".equals(domain.getDomainStatus()) || Assert.isEmpty(domain.getCname())) {
                    continue;
                }
                domain.setDomainStatus("online");
                domain.setUpdateTime(new Date());
                cdnDomainDao.updateById(domain);
            }
        }
    }

    private int normalizePort(Integer port) {
        return port == null ? 22 : port;
    }

    private String normalizeCoverage(String coverage, SelfHostedNodeGroup existing) throws BusinessException {
        String normalized = Assert.isEmpty(coverage)
                ? (existing == null || Assert.isEmpty(existing.getCoverage()) ? "global" : existing.getCoverage())
                : coverage.trim().toLowerCase();
        if (!GROUP_COVERAGES.contains(normalized)) {
            throw new BusinessException("节点组服务区域不正确");
        }
        return normalized;
    }

    private String coverageName(String coverage) {
        if ("mainland".equals(coverage)) {
            return "国内自建 CDN";
        }
        if ("overseas".equals(coverage)) {
            return "海外自建 CDN";
        }
        if ("global".equals(coverage)) {
            return "全球自建 CDN";
        }
        return "旧版自建 CDN";
    }

    private Map<String, String> resolveNodeAddresses(String host, boolean includeIpv6) throws BusinessException {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address instanceof Inet4Address && !result.containsKey("A")) {
                    result.put("A", address.getHostAddress());
                } else if (includeIpv6 && address instanceof Inet6Address && !result.containsKey("AAAA")) {
                    String value = address.getHostAddress();
                    int zoneIndex = value.indexOf('%');
                    result.put("AAAA", zoneIndex < 0 ? value : value.substring(0, zoneIndex));
                }
            }
            if (result.isEmpty() || (!includeIpv6 && !result.containsKey("A"))) {
                throw new BusinessException("节点没有可用的 IP 地址：" + host);
            }
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("节点地址解析失败：" + host);
        }
    }

    private String normalizeProtocol(String protocol) {
        if ("https".equalsIgnoreCase(protocol) || "follow".equalsIgnoreCase(protocol)) {
            return protocol.toLowerCase();
        }
        return "http";
    }

    private String encryptSecret(String key, String value) {
        Map<String, String> secret = new LinkedHashMap<>();
        secret.put(key, value);
        return ConfigureRsaUtils.encryptConfigStr(secret);
    }

    private String encryptConfigJson(JSONObject config) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (config != null) {
            values.putAll(config);
        }
        return ConfigureRsaUtils.encryptConfigStr(values);
    }

    private JSONObject defaultOriginConfig() {
        JSONObject config = new JSONObject();
        config.put("rangeStatus", "on");
        config.put("etagStatus", "off");
        config.put("originReceiveTimeout", 30);
        config.put("followRedirectStatus", "off");
        config.put("followRedirectMaxTimes", 1);
        config.put("originRequestUrlRewrite", new JSONArray());
        config.put("flexibleOrigins", new JSONArray());
        config.put("originRequestHeader", new JSONArray());
        return config;
    }

    private JSONObject defaultAdvancedConfig() {
        JSONObject config = new JSONObject();
        config.put("httpResponseHeaders", new JSONArray());
        config.put("errorCodeRedirectRules", new JSONArray());
        config.put("errorPages", new JSONArray());
        JSONObject compress = new JSONObject();
        compress.put("status", "on");
        compress.put("type", "gzip");
        config.put("compress", compress);
        return config;
    }

    private JSONObject defaultHttpsConfig() {
        JSONObject config = new JSONObject();
        config.put("certificateName", "self-hosted");
        config.put("http2Status", "on");
        config.put("tlsVersion", "TLSv1.2,TLSv1.3");
        config.put("ocspStaplingStatus", "off");
        return config;
    }

    private String decryptSecret(String cipher, String key) {
        if (Assert.isEmpty(cipher)) {
            return "";
        }
        try {
            Map<?, ?> secret = ConfigureRsaUtils.decryptConfigStr(cipher, Map.class);
            Object value = secret.get(key);
            return value == null ? "" : String.valueOf(value);
        } catch (Exception e) {
            return "";
        }
    }

    private String sha256(String value) throws BusinessException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new BusinessException("节点令牌生成失败");
        }
    }

    private boolean safeEquals(Long left, Long right) {
        return left == null ? right == null : left.equals(right);
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
