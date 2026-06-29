package com.kuocai.cdn.service.domain.statistics;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.api.tencent.edgeone.TencentEdgeOneClient;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.CdnDomainStatisticsService;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.Assert;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.teo.v20220901.models.DescribeTimingL7AnalysisDataRequest;
import com.tencentcloudapi.teo.v20220901.models.DescribeTimingL7AnalysisDataResponse;
import com.tencentcloudapi.teo.v20220901.models.DescribeTimingL7OriginPullDataRequest;
import com.tencentcloudapi.teo.v20220901.models.DescribeTimingL7OriginPullDataResponse;
import com.tencentcloudapi.teo.v20220901.models.QueryCondition;
import com.tencentcloudapi.teo.v20220901.models.TimingDataItem;
import com.tencentcloudapi.teo.v20220901.models.TimingDataRecord;
import com.tencentcloudapi.teo.v20220901.models.TimingTypeValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class TencentEdgeOneDomainStatisticsServiceImpl extends BaseService<CdnDomain> implements ICdnStatisticsPlatformService {

    private static final String ACCESS_FLUX = "l7Flow_outFlux";
    private static final String ACCESS_BANDWIDTH = "l7Flow_outBandwidth";
    private static final String ACCESS_REQUEST = "l7Flow_request";
    private static final String HIT_FLUX = "l7Flow_hit_outFlux";
    private static final String ORIGIN_FLUX = "l7Flow_inFlux_hy";
    private static final String ORIGIN_BANDWIDTH = "l7Flow_inBandwidth_hy";
    private static final String ORIGIN_REQUEST = "l7Flow_request_hy";

    @Resource
    private CdnDomainStatisticsService statisticsService;

    @Override
    public Object queryResourceStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        int size = statisticsService.getLabels(start, end).size();
        Series accessFlux = new Series(size);
        Series accessBandwidth = new Series(size);
        Series originFlux = new Series(size);
        Series originBandwidth = new Series(size);

        for (String domain : splitDomains(domainName)) {
            EdgeOneQueryTarget target = resolveTarget(domain);
            if (target == null) {
                continue;
            }
            DescribeTimingL7AnalysisDataResponse accessResponse = queryAccess(target, start, end, ACCESS_FLUX, ACCESS_BANDWIDTH);
            accessFlux.add(extractSeries(accessResponse.getData(), ACCESS_FLUX, size));
            accessBandwidth.add(extractSeries(accessResponse.getData(), ACCESS_BANDWIDTH, size));

            try {
                DescribeTimingL7OriginPullDataResponse originResponse = queryOrigin(target, start, end, ORIGIN_FLUX, ORIGIN_BANDWIDTH);
                originFlux.add(extractSeries(originResponse.getTimingDataRecords(), ORIGIN_FLUX, size));
                originBandwidth.add(extractSeries(originResponse.getTimingDataRecords(), ORIGIN_BANDWIDTH, size));
            } catch (BusinessException e) {
                log.warn("Query EdgeOne origin statistics failed, domain: {}, message: {}", domain, e.getMessage());
            }
        }

        JSONObject detail = new JSONObject();
        detail.put("bw", accessBandwidth.toJsonArray());
        detail.put("bs_bw", originBandwidth.toJsonArray());
        detail.put("flux", accessFlux.toJsonArray());
        detail.put("bs_flux", originFlux.toJsonArray());

        JSONObject summary = new JSONObject();
        summary.put("bw", accessBandwidth.max());
        summary.put("bs_bw", originBandwidth.max());
        summary.put("flux", accessFlux.sum());
        summary.put("bs_flux", originFlux.sum());

        JSONObject result = new JSONObject();
        result.put("resource_detail", detail);
        result.put("resource_summary", summary);
        return result;
    }

    @Override
    public Object queryVisitsStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        int size = statisticsService.getLabels(start, end).size();
        Series requests = new Series(size);
        Series hitFlux = new Series(size);
        Series originRequests = new Series(size);

        for (String domain : splitDomains(domainName)) {
            EdgeOneQueryTarget target = resolveTarget(domain);
            if (target == null) {
                continue;
            }
            DescribeTimingL7AnalysisDataResponse accessResponse = queryAccess(target, start, end, ACCESS_REQUEST, HIT_FLUX);
            requests.add(extractSeries(accessResponse.getData(), ACCESS_REQUEST, size));
            hitFlux.add(extractSeries(accessResponse.getData(), HIT_FLUX, size));
            try {
                DescribeTimingL7OriginPullDataResponse originResponse = queryOrigin(target, start, end, ORIGIN_REQUEST);
                originRequests.add(extractSeries(originResponse.getTimingDataRecords(), ORIGIN_REQUEST, size));
            } catch (BusinessException e) {
                log.warn("Query EdgeOne origin request statistics failed, domain: {}, message: {}", domain, e.getMessage());
            }
        }

        JSONObject detail = new JSONObject();
        detail.put("req_num", requests.toJsonArray());
        detail.put("hit_flux", hitFlux.toJsonArray());
        detail.put("bs_num", originRequests.toJsonArray());
        detail.put("hit_num", requests.toJsonArray());

        JSONObject summary = new JSONObject();
        summary.put("req_num", requests.sum());
        summary.put("hit_flux", hitFlux.sum());
        summary.put("bs_num", originRequests.sum());
        summary.put("hit_num", requests.sum());

        JSONObject result = new JSONObject();
        result.put("visits_detail", detail);
        result.put("visits_summary", summary);
        return result;
    }

    @Override
    public Object queryHttpCodeStatusStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        result.put("status_detail", new JSONArray());
        result.put("status_summary", new JSONArray());
        result.put("bs_status_detail", new JSONArray());
        result.put("bs_status_summary", new JSONArray());
        return result;
    }

    @Override
    public Object queryTopUri(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        result.put("fluxResult", new JSONArray());
        result.put("reqNumResult", new JSONArray());
        return result;
    }

    private DescribeTimingL7AnalysisDataResponse queryAccess(EdgeOneQueryTarget target, DateTime start, DateTime end, String... metrics) throws BusinessException {
        DescribeTimingL7AnalysisDataRequest request = new DescribeTimingL7AnalysisDataRequest();
        request.setStartTime(formatTime(start));
        request.setEndTime(formatTime(end));
        request.setMetricNames(metrics);
        request.setZoneIds(new String[]{target.zoneId});
        request.setInterval(getDefaultDataUnit(start, end));
        request.setArea(target.area);
        request.setFilters(new QueryCondition[]{domainFilter(target.domainName)});
        try {
            DescribeTimingL7AnalysisDataResponse response = TencentEdgeOneClient.getClient().DescribeTimingL7AnalysisData(request);
            log.info("Query EdgeOne access statistics success, domain: {}, metrics: {}, requestId: {}",
                    target.domainName, Arrays.toString(metrics), response.getRequestId());
            return response;
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("查询腾讯云 EdgeOne 访问统计失败：" + TencentEdgeOneClient.formatTencentError(e));
        }
    }

    private DescribeTimingL7OriginPullDataResponse queryOrigin(EdgeOneQueryTarget target, DateTime start, DateTime end, String... metrics) throws BusinessException {
        DescribeTimingL7OriginPullDataRequest request = new DescribeTimingL7OriginPullDataRequest();
        request.setZoneIds(new String[]{target.zoneId});
        request.setMetricNames(metrics);
        request.setStartTime(formatTime(start));
        request.setEndTime(formatTime(end));
        request.setInterval(getDefaultDataUnit(start, end));
        request.setFilters(new QueryCondition[]{domainFilter(target.domainName)});
        try {
            DescribeTimingL7OriginPullDataResponse response = TencentEdgeOneClient.getClient().DescribeTimingL7OriginPullData(request);
            log.info("Query EdgeOne origin statistics success, domain: {}, metrics: {}, requestId: {}",
                    target.domainName, Arrays.toString(metrics), response.getRequestId());
            return response;
        } catch (TencentCloudSDKException e) {
            throw new BusinessException("查询腾讯云 EdgeOne 回源统计失败：" + TencentEdgeOneClient.formatTencentError(e));
        }
    }

    private QueryCondition domainFilter(String domainName) {
        QueryCondition condition = new QueryCondition();
        condition.setKey("domain");
        condition.setOperator("equals");
        condition.setValue(new String[]{domainName});
        return condition;
    }

    private Series extractSeries(TimingDataRecord[] records, String metricName, int size) {
        Series result = new Series(size);
        if (records == null) {
            return result;
        }
        for (TimingDataRecord record : records) {
            if (record == null || record.getTypeValue() == null) {
                continue;
            }
            for (TimingTypeValue typeValue : record.getTypeValue()) {
                if (typeValue == null || !metricName.equals(typeValue.getMetricName())) {
                    continue;
                }
                Series series = new Series(size);
                TimingDataItem[] detail = typeValue.getDetail();
                if (detail != null && detail.length > 0) {
                    Arrays.stream(detail)
                            .filter(item -> item != null && item.getValue() != null)
                            .sorted(Comparator.comparing(TimingDataItem::getTimestamp, Comparator.nullsLast(Long::compareTo)))
                            .map(TimingDataItem::getValue)
                            .forEach(series::append);
                } else if (typeValue.getSum() != null && typeValue.getSum() > 0) {
                    series.append(typeValue.getSum());
                }
                result.add(series);
            }
        }
        return result;
    }

    private EdgeOneQueryTarget resolveTarget(String domainName) throws BusinessException {
        String normalized = TencentEdgeOneClient.normalizeDomain(domainName);
        if (Assert.isEmpty(normalized)) {
            return null;
        }
        CdnDomain local = findLocalDomain(normalized);
        String zoneId = local == null ? null : local.getDomainId();
        if (Assert.isEmpty(zoneId)) {
            zoneId = TencentEdgeOneClient.findZoneId(normalized);
        }
        if (Assert.isEmpty(zoneId)) {
            log.warn("Skip EdgeOne statistics because ZoneId is empty, domain: {}", normalized);
            return null;
        }
        return new EdgeOneQueryTarget(normalized, zoneId, convertArea(local == null ? null : local.getServiceArea()));
    }

    private CdnDomain findLocalDomain(String domainName) {
        QueryWrapper<CdnDomain> wrapper = new QueryWrapper<>();
        wrapper.eq("domain_name", domainName)
                .eq("route", CdnRoute.TENCENT_EDGEONE.getCode())
                .last("limit 1");
        List<CdnDomain> domains = queryByWrapper(wrapper);
        return Assert.isEmpty(domains) ? null : domains.get(0);
    }

    private List<String> splitDomains(String domainNames) {
        Set<String> domains = new LinkedHashSet<>();
        if (Assert.notEmpty(domainNames)) {
            for (String item : domainNames.split(",")) {
                String domain = TencentEdgeOneClient.normalizeDomain(item);
                if (Assert.notEmpty(domain)) {
                    domains.add(domain);
                }
            }
        }
        return new ArrayList<>(domains);
    }

    private String convertArea(String serviceArea) {
        if ("mainland_china".equals(serviceArea)) {
            return "mainland";
        }
        if ("outside_mainland_china".equals(serviceArea)) {
            return "overseas";
        }
        if ("global".equals(serviceArea)) {
            return "global";
        }
        return "global";
    }

    private String getDefaultDataUnit(DateTime start, DateTime end) {
        long between = DateUtil.between(start, end, DateUnit.DAY);
        return between > 7 ? "day" : "hour";
    }

    private String formatTime(DateTime time) {
        return DateUtil.format(time, "yyyy-MM-dd'T'HH:mm:ssXXX");
    }

    private static class EdgeOneQueryTarget {
        private final String domainName;
        private final String zoneId;
        private final String area;

        private EdgeOneQueryTarget(String domainName, String zoneId, String area) {
            this.domainName = domainName;
            this.zoneId = zoneId;
            this.area = area;
        }
    }

    private static class Series {
        private final List<Long> values;
        private final int size;

        private Series(int size) {
            this.size = Math.max(size, 0);
            this.values = new ArrayList<>(this.size);
        }

        private void append(Long value) {
            if (value == null) {
                return;
            }
            values.add(value);
        }

        private void add(Series other) {
            if (other == null) {
                return;
            }
            int max = Math.max(values.size(), other.values.size());
            while (values.size() < max) {
                values.add(0L);
            }
            for (int i = 0; i < other.values.size(); i++) {
                values.set(i, values.get(i) + other.values.get(i));
            }
        }

        private JSONArray toJsonArray() {
            JSONArray array = new JSONArray();
            for (int i = 0; i < size; i++) {
                array.add(i < values.size() ? values.get(i) : 0L);
            }
            return array;
        }

        private long sum() {
            return values.stream().mapToLong(Long::longValue).sum();
        }

        private long max() {
            return values.stream().mapToLong(Long::longValue).max().orElse(0L);
        }
    }
}
