package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.dao.SelfHostedNodeEventDao;
import com.kuocai.cdn.dao.SelfHostedNodeMetricDao;
import com.kuocai.cdn.dto.SelfHostedNodeMetricPoint;
import com.kuocai.cdn.entity.SelfHostedNode;
import com.kuocai.cdn.entity.SelfHostedNodeEvent;
import com.kuocai.cdn.entity.SelfHostedNodeMetric;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.util.Assert;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SelfHostedNodeTelemetryService {
    private static final long METRIC_RETENTION_MS = 30L * 24 * 60 * 60 * 1000;
    private static final long EVENT_RETENTION_MS = 90L * 24 * 60 * 60 * 1000;
    private static final Map<String, RangeSpec> RANGE_SPECS = rangeSpecs();

    private final SelfHostedNodeMetricDao metricDao;
    private final SelfHostedNodeEventDao eventDao;

    public SelfHostedNodeTelemetryService(SelfHostedNodeMetricDao metricDao,
                                          SelfHostedNodeEventDao eventDao) {
        this.metricDao = metricDao;
        this.eventDao = eventDao;
    }

    public void recordHeartbeat(SelfHostedNode node, String previousStatus, String previousError) {
        Date recordedAt = node.getLastHeartbeat() == null ? new Date() : node.getLastHeartbeat();
        metricDao.insert(SelfHostedNodeMetric.builder()
                .nodeId(node.getId())
                .recordedAt(recordedAt)
                .status(node.getStatus())
                .cpuUsage(node.getCpuUsage())
                .memoryUsage(node.getMemoryUsage())
                .diskUsage(node.getDiskUsage())
                .rxBytes(valueOrZero(node.getRxBytes()))
                .txBytes(valueOrZero(node.getTxBytes()))
                .rxRateBps(valueOrZero(node.getRxRateBps()))
                .txRateBps(valueOrZero(node.getTxRateBps()))
                .cacheBytes(valueOrZero(node.getCacheBytes()))
                .desiredConfigVersion(valueOrZero(node.getDesiredConfigVersion()))
                .appliedConfigVersion(valueOrZero(node.getAppliedConfigVersion()))
                .build());

        boolean statusChanged = !Objects.equals(previousStatus, node.getStatus());
        boolean errorChanged = "degraded".equals(node.getStatus())
                && !Objects.equals(normalize(previousError), normalize(node.getLastError()));
        if (statusChanged || errorChanged) {
            recordStatusEvent(node.getId(), node.getStatus(), node.getLastError());
        }
    }

    public void recordStatusEvent(Long nodeId, String status, String details) {
        String normalizedStatus = Assert.isEmpty(status) ? "unknown" : status.trim();
        String message;
        String severity;
        switch (normalizedStatus) {
            case "online":
                message = "节点已恢复在线";
                severity = "success";
                break;
            case "degraded":
                message = "节点上报运行异常";
                severity = "warning";
                break;
            case "offline":
                message = "超过 90 秒未收到节点心跳";
                severity = "danger";
                break;
            case "install_failed":
                message = "Agent 安装失败";
                severity = "danger";
                break;
            case "disabled":
                message = "节点已被管理员停用";
                severity = "secondary";
                break;
            case "pending":
                message = "节点正在等待 Agent 上线";
                severity = "info";
                break;
            default:
                message = "节点状态变更为 " + normalizedStatus;
                severity = "info";
                break;
        }
        recordEvent(nodeId, "status", normalizedStatus, severity, message, details);
    }

    public void recordEvent(Long nodeId, String eventType, String status, String severity,
                            String message, String details) {
        eventDao.insert(SelfHostedNodeEvent.builder()
                .nodeId(nodeId)
                .eventType(limit(eventType, 32))
                .status(limit(status, 32))
                .severity(limit(severity, 16))
                .message(limit(message, 255))
                .details(limit(details, 1000))
                .createTime(new Date())
                .build());
    }

    public JSONObject history(SelfHostedNode node, String range) throws BusinessException {
        RangeSpec spec = RANGE_SPECS.get(Assert.isEmpty(range) ? "24h" : range.trim().toLowerCase());
        if (spec == null) {
            throw new BusinessException("历史范围仅支持 1h、6h、24h、7d、30d");
        }
        Date startTime = new Date(System.currentTimeMillis() - spec.durationMs);
        List<SelfHostedNodeMetricPoint> points = metricDao.selectAggregated(
                node.getId(), startTime, spec.bucketSeconds);
        List<SelfHostedNodeEvent> events = eventDao.selectList(new QueryWrapper<SelfHostedNodeEvent>()
                .eq("node_id", node.getId())
                .ge("create_time", startTime)
                .orderByDesc("create_time")
                .last("LIMIT 100"));

        JSONObject result = new JSONObject(true);
        result.put("nodeId", String.valueOf(node.getId()));
        result.put("range", spec.code);
        result.put("startTime", startTime);
        result.put("serverTime", new Date());
        result.put("metricRetentionDays", 30);
        result.put("points", JSON.toJSON(points));
        result.put("events", JSON.toJSON(events));
        return result;
    }

    public void purgeExpired() {
        metricDao.delete(new QueryWrapper<SelfHostedNodeMetric>()
                .lt("recorded_at", new Date(System.currentTimeMillis() - METRIC_RETENTION_MS)));
        eventDao.delete(new QueryWrapper<SelfHostedNodeEvent>()
                .lt("create_time", new Date(System.currentTimeMillis() - EVENT_RETENTION_MS)));
    }

    public void deleteForNode(Long nodeId) {
        metricDao.delete(new QueryWrapper<SelfHostedNodeMetric>().eq("node_id", nodeId));
        eventDao.delete(new QueryWrapper<SelfHostedNodeEvent>().eq("node_id", nodeId));
    }

    private static Map<String, RangeSpec> rangeSpecs() {
        Map<String, RangeSpec> result = new LinkedHashMap<>();
        result.put("1h", new RangeSpec("1h", 60L * 60 * 1000, 30));
        result.put("6h", new RangeSpec("6h", 6L * 60 * 60 * 1000, 60));
        result.put("24h", new RangeSpec("24h", 24L * 60 * 60 * 1000, 300));
        result.put("7d", new RangeSpec("7d", 7L * 24 * 60 * 60 * 1000, 1800));
        result.put("30d", new RangeSpec("30d", 30L * 24 * 60 * 60 * 1000, 7200));
        return result;
    }

    private static Long valueOrZero(Long value) {
        return value == null ? 0L : Math.max(0L, value);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String limit(String value, int maxLength) {
        if (Assert.isEmpty(value)) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private static final class RangeSpec {
        private final String code;
        private final long durationMs;
        private final int bucketSeconds;

        private RangeSpec(String code, long durationMs, int bucketSeconds) {
            this.code = code;
            this.durationMs = durationMs;
            this.bucketSeconds = bucketSeconds;
        }
    }
}
