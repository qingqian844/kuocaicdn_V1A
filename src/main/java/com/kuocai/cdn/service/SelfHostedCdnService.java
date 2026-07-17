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
import com.kuocai.cdn.dto.SelfHostedApplyResultRequest;
import com.kuocai.cdn.dto.SelfHostedCacheResultRequest;
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
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.ConfigureRsaUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SelfHostedCdnService {
    public static final long DNS_RECORD_TTL_SECONDS = 600L;

    private static final Pattern HOST_PATTERN = Pattern.compile("^[a-zA-Z0-9._:-]{1,255}$");
    private static final Pattern USER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_-]{0,63}$");
    private static final Pattern LABEL_PATTERN = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$");
    private static final long OFFLINE_AFTER_MS = 90_000L;

    private final SelfHostedNodeDao nodeDao;
    private final SelfHostedNodeGroupDao groupDao;
    private final SelfHostedGroupNodeDao groupNodeDao;
    private final SelfHostedDomainConfigDao domainConfigDao;
    private final SelfHostedCacheJobDao cacheJobDao;
    private final SelfHostedCacheJobNodeDao cacheJobNodeDao;
    private final CdnDomainDao cdnDomainDao;
    private final SecureRandom secureRandom = new SecureRandom();

    public SelfHostedCdnService(SelfHostedNodeDao nodeDao,
                                SelfHostedNodeGroupDao groupDao,
                                SelfHostedGroupNodeDao groupNodeDao,
                                SelfHostedDomainConfigDao domainConfigDao,
                                SelfHostedCacheJobDao cacheJobDao,
                                SelfHostedCacheJobNodeDao cacheJobNodeDao,
                                CdnDomainDao cdnDomainDao) {
        this.nodeDao = nodeDao;
        this.groupDao = groupDao;
        this.groupNodeDao = groupNodeDao;
        this.domainConfigDao = domainConfigDao;
        this.cacheJobDao = cacheJobDao;
        this.cacheJobNodeDao = cacheJobNodeDao;
        this.cdnDomainDao = cdnDomainDao;
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
            }
        }
        return nodes;
    }

    public List<JSONObject> listNodeViews() {
        List<JSONObject> result = new ArrayList<>();
        for (SelfHostedNode node : listNodes()) {
            JSONObject item = (JSONObject) JSON.toJSON(node);
            item.remove("sshPasswordCipher");
            item.remove("agentTokenHash");
            item.put("passwordConfigured", !Assert.isEmpty(node.getSshPasswordCipher()));
            List<SelfHostedGroupNode> relations = groupNodeDao.selectList(
                    new QueryWrapper<SelfHostedGroupNode>().eq("node_id", node.getId()).orderByAsc("id"));
            if (!relations.isEmpty()) {
                SelfHostedNodeGroup group = groupDao.selectById(relations.get(0).getGroupId());
                if (group != null) {
                    item.put("groupId", group.getId());
                    item.put("groupName", group.getGroupName());
                }
            }
            result.add(item);
        }
        return result;
    }

    public List<SelfHostedNodeGroup> listGroups() {
        return groupDao.selectList(new QueryWrapper<SelfHostedNodeGroup>().orderByDesc("is_default").orderByAsc("id"));
    }

    public SelfHostedNodeGroup defaultGroup() throws BusinessException {
        SelfHostedNodeGroup group = groupDao.selectOne(new QueryWrapper<SelfHostedNodeGroup>()
                .eq("is_default", 1).eq("status", "enabled").last("LIMIT 1"));
        if (group == null) {
            throw new BusinessException("请先创建并启用一个默认自建 CDN 节点组");
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
            node.setCacheBytes(0L);
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
        node.setRemark(trim(request.getRemark()));
        node.setUpdateTime(now);
        if (node.getId() == null) {
            nodeDao.insert(node);
        } else {
            nodeDao.updateById(node);
        }
        Long targetGroupId = request.getGroupId() == null ? defaultGroup().getId() : request.getGroupId();
        groupNodeDao.delete(new QueryWrapper<SelfHostedGroupNode>().eq("node_id", node.getId()));
        assignNode(node.getId(), targetGroupId);
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
        if (group != null && group.getIsDefault() != null && group.getIsDefault() == 1
                && (request.getIsDefault() == null || request.getIsDefault() == 0)
                && groupDao.selectCount(new QueryWrapper<SelfHostedNodeGroup>()
                .eq("is_default", 1).ne("id", group.getId())) == 0) {
            throw new BusinessException("请先将其他节点组设为默认组");
        }
        Date now = new Date();
        if (group == null) {
            group = new SelfHostedNodeGroup();
            group.setCreateTime(now);
        }
        group.setGroupName(request.getGroupName().trim());
        group.setCnameLabel(label);
        group.setIsDefault(request.getIsDefault() == null ? 0 : request.getIsDefault());
        group.setStatus(Assert.isEmpty(request.getStatus()) ? "enabled" : request.getStatus());
        group.setRemark(trim(request.getRemark()));
        group.setUpdateTime(now);
        if (group.getIsDefault() == 1) {
            groupDao.update(null, new UpdateWrapper<SelfHostedNodeGroup>().set("is_default", 0));
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
        node.setStatus(Assert.isEmpty(request.getLastError()) ? "online" : "degraded");
        node.setLastHeartbeat(new Date());
        node.setAgentVersion(trim(request.getAgentVersion()));
        node.setAppliedConfigVersion(request.getAppliedConfigVersion() == null ? node.getAppliedConfigVersion() : request.getAppliedConfigVersion());
        node.setCpuUsage(request.getCpuUsage());
        node.setMemoryUsage(request.getMemoryUsage());
        node.setDiskUsage(request.getDiskUsage());
        node.setRxBytes(valueOrZero(request.getRxBytes()));
        node.setTxBytes(valueOrZero(request.getTxBytes()));
        node.setCacheBytes(valueOrZero(request.getCacheBytes()));
        node.setLastError(trim(request.getLastError()));
        node.setUpdateTime(new Date());
        nodeDao.updateById(node);
        JSONObject response = new JSONObject();
        response.put("desiredConfigVersion", node.getDesiredConfigVersion());
        response.put("configChanged", !safeEquals(node.getDesiredConfigVersion(), node.getAppliedConfigVersion()));
        response.put("heartbeatIntervalSeconds", 30);
        response.put("cacheJobs", pendingCacheJobs(node.getId()));
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
        JSONArray domains = new JSONArray();
        List<SelfHostedGroupNode> relations = groupNodeDao.selectList(
                new QueryWrapper<SelfHostedGroupNode>().eq("node_id", node.getId()));
        for (SelfHostedGroupNode relation : relations) {
            List<SelfHostedDomainConfig> configs = domainConfigDao.selectList(
                    new QueryWrapper<SelfHostedDomainConfig>().eq("node_group_id", relation.getGroupId())
                            .eq("status", "enabled"));
            for (SelfHostedDomainConfig config : configs) {
                CdnDomain domain = cdnDomainDao.selectById(config.getCdnDomainId());
                if (domain == null || !"self_hosted".equals(domain.getRoute()) || "offline".equals(domain.getDomainStatus())) {
                    continue;
                }
                JSONObject item = (JSONObject) JSON.toJSON(config);
                item.remove("certificateCipher");
                item.remove("privateKeyCipher");
                item.put("domainName", domain.getDomainName());
                item.put("cname", domain.getCname());
                item.put("certificate", decryptSecret(config.getCertificateCipher(), "certificate"));
                item.put("privateKey", decryptSecret(config.getPrivateKeyCipher(), "privateKey"));
                domains.add(item);
            }
        }
        result.put("domains", domains);
        return result;
    }

    public void applyResult(SelfHostedNode node, SelfHostedApplyResultRequest request) {
        if (Boolean.TRUE.equals(request.getSuccess())) {
            node.setAppliedConfigVersion(request.getVersion());
            node.setStatus("online");
            node.setLastError(null);
        } else {
            node.setStatus("degraded");
            node.setLastError(trim(request.getError()));
        }
        node.setUpdateTime(new Date());
        nodeDao.updateById(node);
    }

    public SelfHostedDomainConfig createDomainConfig(CdnDomain domain, String originType, String originAddress,
                                                      String originProtocol, Integer httpPort, Integer httpsPort,
                                                      String originHost) throws BusinessException {
        SelfHostedNodeGroup group = defaultGroup();
        requireActiveNode(group.getId());
        Date now = new Date();
        SelfHostedDomainConfig config = SelfHostedDomainConfig.builder()
                .cdnDomainId(domain.getId()).nodeGroupId(group.getId()).originType(originType)
                .originAddress(originAddress).originProtocol(normalizeProtocol(originProtocol))
                .httpPort(httpPort == null ? 80 : httpPort).httpsPort(httpsPort == null ? 443 : httpsPort)
                .originHost(Assert.isEmpty(originHost) ? domain.getDomainName() : originHost.trim())
                .cacheConfigJson("{\"defaultTtl\":3600,\"inactive\":\"7d\"}")
                .httpsEnabled(0).forceRedirect("off").desiredConfigVersion(1L)
                .status("enabled").createTime(now).updateTime(now).build();
        domainConfigDao.insert(config);
        bumpGroupVersion(group.getId());
        return config;
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
                .eq("domain_name", domainName).eq("route", "self_hosted").last("LIMIT 1"));
        if (domain == null) {
            throw new BusinessException("自建 CDN 域名不存在");
        }
        return getDomainConfig(domain.getId());
    }

    public void updateDomainConfig(SelfHostedDomainConfig config) {
        config.setDesiredConfigVersion((config.getDesiredConfigVersion() == null ? 0L : config.getDesiredConfigVersion()) + 1L);
        config.setUpdateTime(new Date());
        domainConfigDao.updateById(config);
        bumpGroupVersion(config.getNodeGroupId());
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
        Map<String, Long> oldRecords = new LinkedHashMap<>();
        List<Long> legacyRecordIds = new ArrayList<>();
        if (!Assert.isEmpty(group.getDnsRecordIds())) {
            try {
                JSONObject stored = JSON.parseObject(group.getDnsRecordIds());
                for (String key : stored.keySet()) {
                    oldRecords.put(key, stored.getLong(key));
                }
            } catch (Exception legacyFormat) {
                try {
                    legacyRecordIds.addAll(JSON.parseArray(group.getDnsRecordIds(), Long.class));
                } catch (Exception ignored) {
                    // Ignore a malformed historical value and rebuild it below.
                }
            }
        }
        Map<String, Long> newRecords = new LinkedHashMap<>();
        try {
            for (SelfHostedNode node : nodes) {
                String ip = resolveIpv4(node.getHost());
                if (oldRecords.containsKey(ip)) {
                    newRecords.put(ip, oldRecords.get(ip));
                    continue;
                }
                CreateRecordDTO dto = new CreateRecordDTO();
                dto.setDomain(TencentDns.LOCAL_DOMAIN_NAME).setSubDomain(group.getCnameLabel())
                        .setRecordType("A").setRecordLine("默认").setValue(ip)
                        .setTTL(DNS_RECORD_TTL_SECONDS);
                CreateRecordResponse response = TencentApi.createRecord(dto);
                if (response == null || response.getRecordId() == null) {
                    throw new BusinessException("DNS 服务未返回节点记录 ID");
                }
                newRecords.put(ip, response.getRecordId());
            }
        } catch (Exception e) {
            for (Map.Entry<String, Long> entry : newRecords.entrySet()) {
                if (oldRecords.containsKey(entry.getKey())) continue;
                try {
                    TencentApi.deleteRecord(new DeleteRecordDTO().setDomain(TencentDns.LOCAL_DOMAIN_NAME)
                            .setRecordId(entry.getValue()));
                } catch (Exception ignored) {
                    // Best-effort rollback of records created during this failed sync.
                }
            }
            throw new BusinessException("节点组 DNS 同步失败：" + e.getMessage());
        }
        List<Long> deleteIds = new ArrayList<>(legacyRecordIds);
        for (Map.Entry<String, Long> entry : oldRecords.entrySet()) {
            if (!newRecords.containsKey(entry.getKey())) {
                deleteIds.add(entry.getValue());
            }
        }
        Map<String, Long> recordsToPersist = new LinkedHashMap<>(newRecords);
        for (Long recordId : deleteIds) {
            try {
                TencentApi.deleteRecord(new DeleteRecordDTO().setDomain(TencentDns.LOCAL_DOMAIN_NAME).setRecordId(recordId));
            } catch (Exception ignored) {
                // Keep failed deletions in the tracked map so the next health reconciliation can retry them.
                for (Map.Entry<String, Long> entry : oldRecords.entrySet()) {
                    if (recordId.equals(entry.getValue())) {
                        recordsToPersist.put(entry.getKey(), entry.getValue());
                        break;
                    }
                }
            }
        }
        group.setDnsRecordIds(JSON.toJSONString(recordsToPersist));
        group.setUpdateTime(new Date());
        groupDao.updateById(group);
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
        bumpNodeGroups(node.getId());
    }

    public void markInstallFailed(SelfHostedNode node, String error) {
        node.setStatus("install_failed");
        node.setLastError(error == null ? null : error.substring(0, Math.min(error.length(), 1000)));
        node.setUpdateTime(new Date());
        nodeDao.updateById(node);
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
        if (request.getId() == null && Assert.isEmpty(request.getSshPassword())) {
            throw new BusinessException("新增节点时 SSH 密码不能为空");
        }
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
        long version = System.currentTimeMillis();
        List<SelfHostedNode> nodes = nodesInGroup(groupId, false);
        for (SelfHostedNode node : nodes) {
            node.setDesiredConfigVersion(version);
            node.setUpdateTime(new Date());
            nodeDao.updateById(node);
        }
    }

    private int normalizePort(Integer port) {
        return port == null ? 22 : port;
    }

    private String resolveIpv4(String host) throws BusinessException {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address instanceof Inet4Address) {
                    return address.getHostAddress();
                }
            }
            throw new BusinessException("节点没有可用的 IPv4 地址：" + host);
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
