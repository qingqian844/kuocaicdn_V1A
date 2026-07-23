package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.dao.SelfHostedNodeEventDao;
import com.kuocai.cdn.dao.SelfHostedNodeMetricDao;
import com.kuocai.cdn.entity.SelfHostedNode;
import com.kuocai.cdn.entity.SelfHostedNodeEvent;
import com.kuocai.cdn.entity.SelfHostedNodeMetric;
import com.kuocai.cdn.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.annotations.Select;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SelfHostedNodeTelemetryServiceTest {

    @Test
    void aggregationGroupsBySelectedAliasForMysqlStrictMode() throws Exception {
        Select select = SelfHostedNodeMetricDao.class
                .getMethod("selectAggregated", Long.class, Date.class, int.class)
                .getAnnotation(Select.class);
        String sql = String.join(" ", select.value());

        assertTrue(sql.contains("GROUP BY recordedAt"));
        assertFalse(sql.contains("GROUP BY FLOOR(UNIX_TIMESTAMP(recorded_at)"));
    }

    @Test
    void heartbeatStoresMetricAndStatusTransition() {
        SelfHostedNodeMetricDao metricDao = mock(SelfHostedNodeMetricDao.class);
        SelfHostedNodeEventDao eventDao = mock(SelfHostedNodeEventDao.class);
        SelfHostedNodeTelemetryService service = new SelfHostedNodeTelemetryService(metricDao, eventDao);
        SelfHostedNode node = SelfHostedNode.builder().id(7L).status("degraded")
                .lastHeartbeat(new Date()).cpuUsage(new java.math.BigDecimal("12.50"))
                .rxBytes(100L).txBytes(200L).rxRateBps(10L).txRateBps(20L)
                .cacheBytes(300L).desiredConfigVersion(9L).appliedConfigVersion(8L)
                .lastError("nginx config test failed").build();

        service.recordHeartbeat(node, "online", null);

        ArgumentCaptor<SelfHostedNodeMetric> metricCaptor = ArgumentCaptor.forClass(SelfHostedNodeMetric.class);
        verify(metricDao).insert(metricCaptor.capture());
        assertEquals(10L, metricCaptor.getValue().getRxRateBps());
        ArgumentCaptor<SelfHostedNodeEvent> eventCaptor = ArgumentCaptor.forClass(SelfHostedNodeEvent.class);
        verify(eventDao).insert(eventCaptor.capture());
        assertEquals("degraded", eventCaptor.getValue().getStatus());
        assertEquals("warning", eventCaptor.getValue().getSeverity());
    }

    @Test
    void historyUsesBoundedBucketForSelectedRange() throws Exception {
        SelfHostedNodeMetricDao metricDao = mock(SelfHostedNodeMetricDao.class);
        SelfHostedNodeEventDao eventDao = mock(SelfHostedNodeEventDao.class);
        when(metricDao.selectAggregated(eq(7L), any(Date.class), eq(300)))
                .thenReturn(Collections.emptyList());
        when(eventDao.selectList(any())).thenReturn(Collections.emptyList());
        SelfHostedNodeTelemetryService service = new SelfHostedNodeTelemetryService(metricDao, eventDao);

        JSONObject history = service.history(SelfHostedNode.builder().id(7L).build(), "24h");

        assertEquals("24h", history.getString("range"));
        assertEquals(30, history.getIntValue("metricRetentionDays"));
        verify(metricDao).selectAggregated(eq(7L), any(Date.class), eq(300));
    }

    @Test
    void historyRejectsUnsupportedRange() {
        SelfHostedNodeTelemetryService service = new SelfHostedNodeTelemetryService(
                mock(SelfHostedNodeMetricDao.class), mock(SelfHostedNodeEventDao.class));

        assertThrows(BusinessException.class,
                () -> service.history(SelfHostedNode.builder().id(7L).build(), "365d"));
    }
}
