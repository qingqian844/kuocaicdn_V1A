package com.kuocai.cdn.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuocai.cdn.dto.SelfHostedNodeMetricPoint;
import com.kuocai.cdn.entity.SelfHostedNodeMetric;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface SelfHostedNodeMetricDao extends BaseMapper<SelfHostedNodeMetric> {
    @Select("SELECT FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(recorded_at) / #{bucketSeconds}) * #{bucketSeconds}) AS recordedAt," +
            "ROUND(AVG(cpu_usage),2) AS cpuUsage,ROUND(AVG(memory_usage),2) AS memoryUsage," +
            "ROUND(AVG(disk_usage),2) AS diskUsage,MAX(rx_bytes) AS rxBytes,MAX(tx_bytes) AS txBytes," +
            "ROUND(AVG(rx_rate_bps)) AS rxRateBps,ROUND(AVG(tx_rate_bps)) AS txRateBps," +
            "ROUND(AVG(cache_bytes)) AS cacheBytes " +
            "FROM self_hosted_node_metric WHERE node_id = #{nodeId} AND recorded_at >= #{startTime} " +
            "GROUP BY recordedAt ORDER BY recordedAt ASC")
    List<SelfHostedNodeMetricPoint> selectAggregated(@Param("nodeId") Long nodeId,
                                                     @Param("startTime") Date startTime,
                                                     @Param("bucketSeconds") int bucketSeconds);
}
