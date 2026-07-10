package com.kuocai.cdn.service.domain.statistics;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.api.tencent.cdn.TencentClient;
import com.kuocai.cdn.api.tencent.cdn.TencentErrorCodeHandler;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.CdnDomainStatisticsService;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.Assert;
import com.tencentcloudapi.cdn.v20180606.CdnClient;
import com.tencentcloudapi.cdn.v20180606.models.CdnData;
import com.tencentcloudapi.cdn.v20180606.models.DescribeBillingDataRequest;
import com.tencentcloudapi.cdn.v20180606.models.DescribeBillingDataResponse;
import com.tencentcloudapi.cdn.v20180606.models.DescribeCdnDataRequest;
import com.tencentcloudapi.cdn.v20180606.models.DescribeCdnDataResponse;
import com.tencentcloudapi.cdn.v20180606.models.DescribeOriginDataRequest;
import com.tencentcloudapi.cdn.v20180606.models.DescribeOriginDataResponse;
import com.tencentcloudapi.cdn.v20180606.models.ListTopDataRequest;
import com.tencentcloudapi.cdn.v20180606.models.ListTopDataResponse;
import com.tencentcloudapi.cdn.v20180606.models.SummarizedData;
import com.tencentcloudapi.cdn.v20180606.models.TimestampData;
import com.tencentcloudapi.cdn.v20180606.models.TopDetailData;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TencentDomainStatisticsServiceImpl extends BaseService<CdnDomain> implements ICdnStatisticsPlatformService {

    @Resource
    private CdnDomainStatisticsService statisticsService;

    @FunctionalInterface
    private interface TencentCdnDataFetcher {
        CdnData fetch(String domainName, DateTime start, DateTime end, String metric, String area) throws BusinessException;
    }

    @Override
    public JSONObject queryResourceStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        try {
            int size = statisticsService.getLabels(start, end).size();
            JSONArray bwDetail = emptyLongArray(size);
            JSONArray bsBwDetail = emptyLongArray(size);
            JSONArray fluxDetail = emptyLongArray(size);
            JSONArray bsFluxDetail = emptyLongArray(size);

            for (Map.Entry<String, String> areaGroup : splitTencentDomainsByArea(domainName).entrySet()) {
                String domains = areaGroup.getValue();
                String area = areaGroup.getKey();

                CdnData cdnData = queryTencentDataWithDomainFallback(domains, start, end, "bandwidth", area, this::queryCdnData);
                bwDetail = sumArray(bwDetail, fixArray(convertData(cdnData.getDetailData(), end), size), size);

                cdnData = queryTencentDataWithDomainFallback(domains, start, end, "bandwidth", area, this::queryOriginData);
                bsBwDetail = sumArray(bsBwDetail, fixArray(convertData(cdnData.getDetailData(), end), size), size);

                cdnData = queryTencentDataWithDomainFallback(domains, start, end, "flux", area, this::queryCdnData);
                fluxDetail = sumArray(fluxDetail, fixArray(convertData(cdnData.getDetailData(), end), size), size);

                cdnData = queryTencentDataWithDomainFallback(domains, start, end, "flux", area, this::queryOriginData);
                bsFluxDetail = sumArray(bsFluxDetail, fixArray(convertData(cdnData.getDetailData(), end), size), size);
            }

            JSONObject rd = new JSONObject();
            rd.put("bw", bwDetail);
            rd.put("bs_bw", bsBwDetail);
            rd.put("flux", fluxDetail);
            rd.put("bs_flux", bsFluxDetail);

            JSONObject rs = new JSONObject();
            rs.put("bw", maxData(bwDetail));
            rs.put("bs_bw", maxData(bsBwDetail));
            rs.put("flux", sumData(fluxDetail));
            rs.put("bs_flux", sumData(bsFluxDetail));

            result.put("resource_detail", rd);
            result.put("resource_summary", rs);
        } catch (Exception e) {
            log.error("Query Tencent CDN resource statistics failed, domain={}, start={}, end={}", domainName, start, end, e);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        return result;
    }

    @Override
    public Object queryVisitsStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        try {
            int size = statisticsService.getLabels(start, end).size();
            JSONArray reqNumDetail = emptyLongArray(size);
            JSONArray hitFluxDetail = emptyLongArray(size);
            JSONArray bsNumDetail = emptyLongArray(size);
            JSONArray hitNumDetail = emptyLongArray(size);

            for (Map.Entry<String, String> areaGroup : splitTencentDomainsByArea(domainName).entrySet()) {
                String domains = areaGroup.getValue();
                String area = areaGroup.getKey();

                CdnData cdnData = queryTencentDataWithDomainFallback(domains, start, end, "request", area, this::queryCdnData);
                reqNumDetail = sumArray(reqNumDetail, fixArray(convertData(cdnData.getDetailData()), size), size);

                cdnData = queryTencentDataWithDomainFallback(domains, start, end, "hitFlux", area, this::queryCdnData);
                hitFluxDetail = sumArray(hitFluxDetail, fixArray(convertData(cdnData.getDetailData()), size), size);

                cdnData = queryTencentDataWithDomainFallback(domains, start, end, "request", area, this::queryOriginData);
                bsNumDetail = sumArray(bsNumDetail, fixArray(convertData(cdnData.getDetailData()), size), size);

                cdnData = queryTencentDataWithDomainFallback(domains, start, end, "hitRequest", area, this::queryCdnData);
                hitNumDetail = sumArray(hitNumDetail, fixArray(convertData(cdnData.getDetailData()), size), size);
            }

            JSONObject vd = new JSONObject();
            vd.put("req_num", reqNumDetail);
            vd.put("hit_flux", hitFluxDetail);
            vd.put("bs_num", bsNumDetail);
            vd.put("hit_num", hitNumDetail);

            JSONObject vs = new JSONObject();
            vs.put("req_num", sumData(reqNumDetail));
            vs.put("hit_flux", sumData(hitFluxDetail));
            vs.put("bs_num", sumData(bsNumDetail));
            vs.put("hit_num", sumData(hitNumDetail));

            result.put("visits_detail", vd);
            result.put("visits_summary", vs);
        } catch (Exception e) {
            log.error("Query Tencent CDN visit statistics failed, domain={}, start={}, end={}", domainName, start, end, e);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        return result;
    }

    @Override
    public Object queryHttpCodeStatusStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        try {
            int size = statisticsService.getLabels(start, end).size();
            JSONArray sd = new JSONArray();
            JSONArray ss = new JSONArray();
            JSONArray bsd = new JSONArray();
            JSONArray bss = new JSONArray();

            for (String code : new String[]{"2xx", "3xx", "4xx", "5xx"}) {
                JSONArray detail = emptyLongArray(size);
                JSONArray originDetail = emptyLongArray(size);
                for (Map.Entry<String, String> areaGroup : splitTencentDomainsByArea(domainName).entrySet()) {
                    String domains = areaGroup.getValue();
                    String area = areaGroup.getKey();

                    CdnData cdnData = queryTencentDataWithDomainFallback(domains, start, end, code, area, this::queryCdnData);
                    detail = sumArray(detail, fixArray(convertData(cdnData.getDetailData()), size), size);

                    cdnData = queryTencentDataWithDomainFallback(domains, start, end, code, area, this::queryOriginData);
                    originDetail = sumArray(originDetail, fixArray(convertData(cdnData.getDetailData()), size), size);
                }
                sd.add(detail);
                ss.add(sumData(detail));
                bsd.add(originDetail);
                bss.add(sumData(originDetail));
            }

            result.put("status_detail", sd);
            result.put("status_summary", ss);
            result.put("bs_status_detail", bsd);
            result.put("bs_status_summary", bss);
        } catch (Exception e) {
            log.error("Query Tencent CDN status statistics failed, domain={}, start={}, end={}", domainName, start, end, e);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        return result;
    }

    @Override
    public JSONObject queryTopUri(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        try {
            JSONArray fluxResult = new JSONArray();
            JSONArray reqNumResult = new JSONArray();
            for (Map.Entry<String, String> areaGroup : splitTencentDomainsByArea(domainName).entrySet()) {
                ListTopDataResponse listTopDataResponse = getListTopData(areaGroup.getValue(), start, end, "url", "flux", areaGroup.getKey());
                fluxResult.addAll(convertTopData(listTopDataResponse.getData()[0].getDetailData()));

                listTopDataResponse = getListTopData(areaGroup.getValue(), start, end, "url", "request", areaGroup.getKey());
                reqNumResult.addAll(convertTopData(listTopDataResponse.getData()[0].getDetailData()));
            }
            result.put("fluxResult", fluxResult);
            result.put("reqNumResult", reqNumResult);
            log.info("Query Tencent CDN top url success, domain={}, start={}, end={}", domainName, start, end);
        } catch (Exception e) {
            log.error("Query Tencent CDN top url failed, domain={}, start={}, end={}", domainName, start, end, e);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        return result;
    }

    public ListTopDataResponse getListTopData(String domainName, DateTime startTime, DateTime endTime, String type, String filter) throws BusinessException {
        return getListTopData(domainName, startTime, endTime, type, filter, "mainland");
    }

    public ListTopDataResponse getListTopData(String domainName, DateTime startTime, DateTime endTime, String type, String filter, String area) throws BusinessException {
        ListTopDataRequest req = new ListTopDataRequest();
        req.setStartTime(startTime.toString("yyyy-MM-dd HH:mm:ss"));
        req.setEndTime(endTime.toString("yyyy-MM-dd HH:mm:ss"));
        req.setMetric(type);
        req.setFilter(filter);
        req.setDomains(domainName.split(","));
        req.setArea(area);
        CdnClient client = TencentClient.getCdnClient();
        try {
            ListTopDataResponse resp = client.ListTopData(req);
            log.info("Query Tencent CDN top data success, domain={}, type={}, area={}, start={}, end={}", domainName, type, area, startTime, endTime);
            return resp;
        } catch (TencentCloudSDKException e) {
            log.error("Query Tencent CDN top data failed, domain={}, type={}, area={}, start={}, end={}", domainName, type, area, startTime, endTime);
            throw new BusinessException(TencentErrorCodeHandler.getErrorDescription(e)).setCause(e).log();
        }
    }

    private DescribeBillingDataResponse getDescribeBillingData(String domainName, DateTime startTime, DateTime endTime, String type) throws BusinessException {
        DescribeBillingDataRequest req = new DescribeBillingDataRequest();
        req.setStartTime(startTime.toString("yyyy-MM-dd HH:mm:ss"));
        req.setEndTime(endTime.toString("yyyy-MM-dd HH:mm:ss"));
        req.setMetric(type);
        req.setInterval(getDefaultDataUnit(startTime, endTime));
        req.setDomain(domainName);
        req.setArea("mainland");
        CdnClient client = TencentClient.getCdnClient();
        try {
            DescribeBillingDataResponse resp = client.DescribeBillingData(req);
            log.info("Query Tencent CDN billing data success, domain={}, type={}, start={}, end={}", domainName, type, startTime, endTime);
            return resp;
        } catch (TencentCloudSDKException e) {
            log.error("Query Tencent CDN billing data failed, domain={}, type={}, start={}, end={}", domainName, type, startTime, endTime);
            throw new BusinessException(TencentErrorCodeHandler.getErrorDescription(e)).setCause(e).log();
        }
    }

    private DescribeCdnDataResponse getDescribeCdnData(String domainName, DateTime startTime, DateTime endTime, String type, String area) throws BusinessException {
        DescribeCdnDataRequest req = new DescribeCdnDataRequest();
        req.setStartTime(startTime.toString("yyyy-MM-dd HH:mm:ss"));
        req.setEndTime(endTime.toString("yyyy-MM-dd HH:mm:ss"));
        req.setMetric(type);
        req.setInterval(getDefaultDataUnit(startTime, endTime));
        req.setDomains(domainName.split(","));
        req.setArea(area);
        CdnClient client = TencentClient.getCdnClient();
        try {
            DescribeCdnDataResponse resp = client.DescribeCdnData(req);
            log.info("Query Tencent CDN data success, domain={}, type={}, area={}, start={}, end={}", domainName, type, area, startTime, endTime);
            return resp;
        } catch (TencentCloudSDKException e) {
            log.error("Query Tencent CDN data failed, domain={}, type={}, area={}, start={}, end={}", domainName, type, area, startTime, endTime);
            throw new BusinessException(TencentErrorCodeHandler.getErrorDescription(e)).setCause(e).log();
        }
    }

    private CdnData queryCdnData(String domainName, DateTime startTime, DateTime endTime, String type, String area) throws BusinessException {
        return firstCdnData(getDescribeCdnData(domainName, startTime, endTime, type, area));
    }

    private DescribeOriginDataResponse getDescribeOriginData(String domainName, DateTime startTime, DateTime endTime, String type, String area) throws BusinessException {
        DescribeOriginDataRequest req = new DescribeOriginDataRequest();
        req.setDomains(domainName.split(","));
        req.setStartTime(startTime.toString("yyyy-MM-dd HH:mm:ss"));
        req.setEndTime(endTime.toString("yyyy-MM-dd HH:mm:ss"));
        req.setMetric(type);
        req.setInterval(getDefaultDataUnit(startTime, endTime));
        req.setArea(area);
        CdnClient client = TencentClient.getCdnClient();
        try {
            DescribeOriginDataResponse resp = client.DescribeOriginData(req);
            log.info("Query Tencent CDN origin data success, domain={}, type={}, area={}, start={}, end={}", domainName, type, area, startTime, endTime);
            return resp;
        } catch (TencentCloudSDKException e) {
            log.error("Query Tencent CDN origin data failed, domain={}, type={}, area={}, start={}, end={}", domainName, type, area, startTime, endTime);
            throw new BusinessException(TencentErrorCodeHandler.getErrorDescription(e)).setCause(e).log();
        }
    }

    private CdnData queryOriginData(String domainName, DateTime startTime, DateTime endTime, String type, String area) throws BusinessException {
        return firstOriginData(getDescribeOriginData(domainName, startTime, endTime, type, area));
    }

    private CdnData queryTencentDataWithDomainFallback(String domainNames, DateTime start, DateTime end, String metric,
                                                       String area, TencentCdnDataFetcher fetcher) throws BusinessException {
        try {
            return fetcher.fetch(domainNames, start, end, metric, area);
        } catch (BusinessException batchException) {
            List<String> domains = Arrays.stream(domainNames.split(","))
                    .map(String::trim)
                    .filter(Assert::notEmpty)
                    .collect(Collectors.toList());
            if (domains.size() <= 1) {
                throw batchException;
            }

            log.warn("Tencent CDN batch statistics failed, fallback to single-domain query. domains={}, metric={}, area={}, message={}",
                    domainNames, metric, area, batchException.getMessage());
            CdnData merged = new CdnData();
            boolean hasData = false;
            for (String domain : domains) {
                try {
                    CdnData item = fetcher.fetch(domain, start, end, metric, area);
                    mergeCdnData(merged, item);
                    hasData = true;
                } catch (BusinessException singleException) {
                    log.warn("Skip Tencent CDN statistics for invalid or unavailable domain. domain={}, metric={}, area={}, message={}",
                            domain, metric, area, singleException.getMessage());
                }
            }
            if (!hasData) {
                throw batchException;
            }
            return merged;
        }
    }

    private void mergeCdnData(CdnData target, CdnData source) {
        if (source == null) {
            return;
        }
        if (Assert.isEmpty(target.getMetric()) && Assert.notEmpty(source.getMetric())) {
            target.setMetric(source.getMetric());
        }
        target.setDetailData(mergeTimestampData(target.getDetailData(), source.getDetailData()));

        float current = target.getSummarizedData() == null || target.getSummarizedData().getValue() == null
                ? 0F : target.getSummarizedData().getValue();
        float addition = source.getSummarizedData() == null || source.getSummarizedData().getValue() == null
                ? sumTimestampData(source.getDetailData()) : source.getSummarizedData().getValue();
        SummarizedData summarizedData = target.getSummarizedData() == null ? new SummarizedData() : target.getSummarizedData();
        summarizedData.setName(source.getSummarizedData() == null ? target.getMetric() : source.getSummarizedData().getName());
        summarizedData.setValue(current + addition);
        target.setSummarizedData(summarizedData);
    }

    private TimestampData[] mergeTimestampData(TimestampData[] base, TimestampData[] addition) {
        Map<String, Float> values = new LinkedHashMap<>();
        addTimestampData(values, base);
        addTimestampData(values, addition);
        return values.entrySet().stream()
                .map(entry -> {
                    TimestampData item = new TimestampData();
                    item.setTime(entry.getKey());
                    item.setValue(entry.getValue());
                    return item;
                })
                .toArray(TimestampData[]::new);
    }

    private void addTimestampData(Map<String, Float> values, TimestampData[] items) {
        if (items == null) {
            return;
        }
        for (TimestampData item : items) {
            if (item == null || item.getTime() == null || item.getValue() == null) {
                continue;
            }
            values.put(item.getTime(), values.getOrDefault(item.getTime(), 0F) + item.getValue());
        }
    }

    private float sumTimestampData(TimestampData[] items) {
        if (items == null) {
            return 0F;
        }
        float result = 0F;
        for (TimestampData item : items) {
            if (item != null && item.getValue() != null) {
                result += item.getValue();
            }
        }
        return result;
    }

    private String getDefaultDataUnit(DateTime start, DateTime end) {
        long between = DateUtil.between(start, end, DateUnit.DAY);
        return between > 1 ? "day" : "hour";
    }

    private JSONArray fixArray(JSONArray data, int size) {
        JSONArray result = data == null ? new JSONArray() : data;
        while (result.size() < size) {
            result.add(0L);
        }
        return result;
    }

    private JSONArray emptyLongArray(int size) {
        JSONArray result = new JSONArray();
        for (int i = 0; i < size; i++) {
            result.add(0L);
        }
        return result;
    }

    private JSONArray sumArray(JSONArray base, JSONArray add, int size) {
        JSONArray result = new JSONArray();
        for (int i = 0; i < size; i++) {
            long a = i < base.size() ? Long.parseLong(base.get(i).toString()) : 0L;
            long b = i < add.size() ? Long.parseLong(add.get(i).toString()) : 0L;
            result.add(a + b);
        }
        return result;
    }

    private JSONArray convertTopData(TopDetailData[] topDetailData) {
        if (topDetailData == null) {
            return new JSONArray();
        }
        return Arrays.stream(topDetailData)
                .map(td -> td.getValue().longValue())
                .collect(Collectors.toCollection(JSONArray::new));
    }

    private JSONArray convertData(TimestampData[] timestampData) {
        if (timestampData == null) {
            return new JSONArray();
        }
        return Arrays.stream(timestampData)
                .map(td -> td.getValue().longValue())
                .collect(Collectors.toCollection(JSONArray::new));
    }

    private JSONArray convertData(TimestampData[] timestampData, DateTime end) {
        if (timestampData == null) {
            return new JSONArray();
        }
        String endTime = end.toString("yyyy-MM-dd HH:mm:ss");
        return Arrays.stream(timestampData)
                .filter(td -> td.getTime() != null && td.getTime().compareTo(endTime) < 0)
                .map(td -> td.getValue().longValue())
                .collect(Collectors.toCollection(JSONArray::new));
    }

    private long sumData(JSONArray data) {
        return data.stream().mapToLong(item -> Long.parseLong(item.toString())).sum();
    }

    private long maxData(JSONArray data) {
        return data.stream().mapToLong(item -> Long.parseLong(item.toString())).max().orElse(0L);
    }

    private CdnData firstCdnData(DescribeCdnDataResponse response) throws BusinessException {
        if (response == null || response.getData() == null || response.getData().length == 0
                || response.getData()[0].getCdnData() == null || response.getData()[0].getCdnData().length == 0) {
            throw new BusinessException("腾讯云 CDN 统计未返回数据");
        }
        return response.getData()[0].getCdnData()[0];
    }

    private CdnData firstOriginData(DescribeOriginDataResponse response) throws BusinessException {
        if (response == null || response.getData() == null || response.getData().length == 0
                || response.getData()[0].getOriginData() == null || response.getData()[0].getOriginData().length == 0) {
            throw new BusinessException("腾讯云 CDN 回源统计未返回数据");
        }
        return response.getData()[0].getOriginData()[0];
    }

    private Map<String, String> splitTencentDomainsByArea(String domainNames) {
        Map<String, Set<String>> areaDomains = new LinkedHashMap<>();
        areaDomains.put("mainland", new LinkedHashSet<>());
        areaDomains.put("overseas", new LinkedHashSet<>());

        Map<String, CdnDomain> localDomains = queryLocalTencentDomains(domainNames);
        for (String domain : domainNames.split(",")) {
            String normalized = domain == null ? "" : domain.trim();
            if (Assert.isEmpty(normalized)) {
                continue;
            }
            CdnDomain local = localDomains.get(normalized.toLowerCase());
            for (String area : toTencentStatisticsAreas(local == null ? null : local.getServiceArea())) {
                areaDomains.get(area).add(normalized);
            }
        }

        Map<String, String> result = new LinkedHashMap<>();
        areaDomains.forEach((area, domains) -> {
            if (!domains.isEmpty()) {
                result.put(area, String.join(",", domains));
            }
        });
        return result;
    }

    private Map<String, CdnDomain> queryLocalTencentDomains(String domainNames) {
        List<String> names = Arrays.stream(domainNames.split(","))
                .map(String::trim)
                .filter(Assert::notEmpty)
                .distinct()
                .collect(Collectors.toList());
        Map<String, CdnDomain> result = new LinkedHashMap<>();
        if (names.isEmpty()) {
            return result;
        }
        QueryWrapper<CdnDomain> wrapper = new QueryWrapper<>();
        wrapper.in("domain_name", names).eq("route", CdnRoute.TENCENT.getCode());
        for (CdnDomain domain : queryByWrapper(wrapper)) {
            if (Assert.notEmpty(domain.getDomainName())) {
                result.put(domain.getDomainName().toLowerCase(), domain);
            }
        }
        return result;
    }

    private List<String> toTencentStatisticsAreas(String serviceArea) {
        if ("outside_mainland_china".equals(serviceArea)) {
            return Arrays.asList("overseas");
        }
        if ("global".equals(serviceArea)) {
            return Arrays.asList("mainland", "overseas");
        }
        return Arrays.asList("mainland");
    }
}
