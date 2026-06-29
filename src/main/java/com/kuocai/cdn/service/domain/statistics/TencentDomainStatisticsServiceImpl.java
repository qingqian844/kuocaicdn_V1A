package com.kuocai.cdn.service.domain.statistics;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.tencent.cdn.TencentClient;
import com.kuocai.cdn.api.tencent.cdn.TencentErrorCodeHandler;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.CdnDomainStatisticsService;
import com.kuocai.cdn.service.base.BaseService;
import com.tencentcloudapi.cdn.v20180606.CdnClient;
import com.tencentcloudapi.cdn.v20180606.models.*;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TencentDomainStatisticsServiceImpl extends BaseService<CdnDomain> implements ICdnStatisticsPlatformService {

    @Resource
    private CdnDomainStatisticsService statisticsService;

    @Override
    public JSONObject queryResourceStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        try {
            int sized = statisticsService.getLabels(start, end).size();
            JSONObject rd = new JSONObject();
            JSONObject rs = new JSONObject();
            // 访问带宽
            DescribeCdnDataResponse bandwidthResponse = getDescribeCdnData(domainName, start, end, "bandwidth");
            CdnData cdnData = bandwidthResponse.getData()[0].getCdnData()[0];
            JSONArray bwDetail = convertData(cdnData.getDetailData(), end);
            rd.put("bw", fixArray(bwDetail, sized));
            rs.put("bw", maxData(bwDetail));
            // 回源带宽
            DescribeOriginDataResponse bandwidthOriginResponse = getDescribeOriginData(domainName, start, end, "bandwidth");
            cdnData = bandwidthOriginResponse.getData()[0].getOriginData()[0];
            JSONArray bsBwDetail = convertData(cdnData.getDetailData(), end);
            rd.put("bs_bw", fixArray(bsBwDetail, sized));
            rs.put("bs_bw", maxData(bsBwDetail));
            // 访问流量
            DescribeCdnDataResponse fluxResponse = getDescribeCdnData(domainName, start, end, "flux");
            cdnData = fluxResponse.getData()[0].getCdnData()[0];
            JSONArray fluxDetail = convertData(cdnData.getDetailData(), end);
            rd.put("flux", fixArray(fluxDetail, sized));
            rs.put("flux", sumData(fluxDetail));
            // 回源流量
            DescribeOriginDataResponse fluxOriginResponse = getDescribeOriginData(domainName, start, end, "flux");
            cdnData = fluxOriginResponse.getData()[0].getOriginData()[0];
            JSONArray bsFluxDetail = convertData(cdnData.getDetailData(), end);
            rd.put("bs_flux", fixArray(bsFluxDetail, sized));
            rs.put("bs_flux", sumData(bsFluxDetail));
            result.put("resource_detail", rd);
            result.put("resource_summary", rs);
        } catch (Exception e) {
            log.error("查询网络资源消耗统计信，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        return result;
    }

    @Override
    public Object queryVisitsStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        try {
            int sized = statisticsService.getLabels(start, end).size();
            JSONObject vs = new JSONObject();
            JSONObject vd = new JSONObject();
            // 访问次数
            DescribeCdnDataResponse requestResponse = getDescribeCdnData(domainName, start, end, "request");
            CdnData cdnData = requestResponse.getData()[0].getCdnData()[0];
            vd.put("req_num", fixArray(convertData(cdnData.getDetailData()), sized));
            vs.put("req_num", cdnData.getSummarizedData().getValue().longValue());
            // 命中次数
            DescribeCdnDataResponse hitFluxResponse = getDescribeCdnData(domainName, start, end, "hitFlux");
            cdnData = hitFluxResponse.getData()[0].getCdnData()[0];
            vd.put("hit_flux", fixArray(convertData(cdnData.getDetailData()), sized));
            vs.put("hit_flux", cdnData.getSummarizedData().getValue().longValue());
            // 访问流量
            DescribeOriginDataResponse requestOriginResponse = getDescribeOriginData(domainName, start, end, "request");
            cdnData = requestOriginResponse.getData()[0].getOriginData()[0];
            vd.put("bs_num", fixArray(convertData(cdnData.getDetailData()), sized));
            vs.put("bs_num", cdnData.getSummarizedData().getValue().longValue());
            // 命中流量
            DescribeCdnDataResponse hitRequestResponse = getDescribeCdnData(domainName, start, end, "hitRequest");
            cdnData = hitRequestResponse.getData()[0].getCdnData()[0];
            vd.put("hit_num", fixArray(convertData(cdnData.getDetailData()), sized));
            vs.put("hit_num", cdnData.getSummarizedData().getValue().longValue());
            result.put("visits_detail", vd);
            result.put("visits_summary", vs);
        } catch (Exception e) {
            log.error("查询访问统计信息失败，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        return result;
    }

    @Override
    public Object queryHttpCodeStatusStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        try {
            int sized = statisticsService.getLabels(start, end).size();
            JSONArray sd = new JSONArray();
            JSONArray ss = new JSONArray();
            JSONArray bsd = new JSONArray();
            JSONArray bss = new JSONArray();
            // 访问状态码 2xx
            DescribeCdnDataResponse code2xxResponse = getDescribeCdnData(domainName, start, end, "2xx");
            CdnData cdnData = code2xxResponse.getData()[0].getCdnData()[0];
            sd.add(fixArray(convertData(cdnData.getDetailData()), sized));
            ss.add(cdnData.getSummarizedData().getValue().longValue());
            // 访问状态码 3xx
            DescribeCdnDataResponse code3xxResponse = getDescribeCdnData(domainName, start, end, "3xx");
            cdnData = code3xxResponse.getData()[0].getCdnData()[0];
            sd.add(fixArray(convertData(cdnData.getDetailData()), sized));
            ss.add(cdnData.getSummarizedData().getValue().longValue());
            // 访问状态码 4xx
            DescribeCdnDataResponse code4xxResponse = getDescribeCdnData(domainName, start, end, "4xx");
            cdnData = code4xxResponse.getData()[0].getCdnData()[0];
            sd.add(fixArray(convertData(cdnData.getDetailData()), sized));
            ss.add(cdnData.getSummarizedData().getValue().longValue());
            // 访问状态码 5xx
            DescribeCdnDataResponse code5xxResponse = getDescribeCdnData(domainName, start, end, "5xx");
            cdnData = code5xxResponse.getData()[0].getCdnData()[0];
            sd.add(fixArray(convertData(cdnData.getDetailData()), sized));
            ss.add(cdnData.getSummarizedData().getValue().longValue());
            // 回源状态码 2xx
            DescribeOriginDataResponse code2xxOriginResponse = getDescribeOriginData(domainName, start, end, "2xx");
            cdnData = code2xxOriginResponse.getData()[0].getOriginData()[0];
            bsd.add(fixArray(convertData(cdnData.getDetailData()), sized));
            bss.add(cdnData.getSummarizedData().getValue().longValue());
            // 回源状态码 3xx
            DescribeOriginDataResponse code3xxOriginResponse = getDescribeOriginData(domainName, start, end, "3xx");
            cdnData = code3xxOriginResponse.getData()[0].getOriginData()[0];
            bsd.add(fixArray(convertData(cdnData.getDetailData()), sized));
            bss.add(cdnData.getSummarizedData().getValue().longValue());
            // 回源状态码 4xx
            DescribeOriginDataResponse code4xxOriginResponse = getDescribeOriginData(domainName, start, end, "4xx");
            cdnData = code4xxOriginResponse.getData()[0].getOriginData()[0];
            bsd.add(fixArray(convertData(cdnData.getDetailData()), sized));
            bss.add(cdnData.getSummarizedData().getValue().longValue());
            // 回源状态码 5xx
            DescribeOriginDataResponse code5xxOriginResponse = getDescribeOriginData(domainName, start, end, "5xx");
            cdnData = code5xxOriginResponse.getData()[0].getOriginData()[0];
            bsd.add(fixArray(convertData(cdnData.getDetailData()), sized));
            bss.add(cdnData.getSummarizedData().getValue().longValue());
            result.put("status_detail", sd);
            result.put("status_summary", ss);
            result.put("bs_status_detail", bsd);
            result.put("bs_status_summary", bss);
        } catch (Exception e) {
            log.error("查询HTTP状态码统计信息失败，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        return result;
    }

    @Override
    public JSONObject queryTopUri(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        try {
            // 访问流量排行
            ListTopDataResponse listTopDataResponse = getListTopData(domainName, start, end, "url", "flux");
            result.put("fluxResult", convertTopData(listTopDataResponse.getData()[0].getDetailData()));
            // 访问次数排行
            listTopDataResponse = getListTopData(domainName, start, end, "url", "request");
            result.put("reqNumResult", convertTopData(listTopDataResponse.getData()[0].getDetailData()));
            log.info("查询TOP URL成功，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            log.info("查询TOP URL成功，返回结果：{}", result);
        } catch (Exception e) {
            log.error("查询TOP URL失败，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        return result;
    }

    public ListTopDataResponse getListTopData(String domainName, DateTime startTime, DateTime endTime, String type, String filter) throws BusinessException {
        ListTopDataRequest req = new ListTopDataRequest();
        req.setStartTime(startTime.toString("yyyy-MM-dd HH:mm:ss"));
        req.setEndTime(endTime.toString("yyyy-MM-dd HH:mm:ss"));
        req.setMetric(type);
        req.setFilter(filter);
        req.setDomains(domainName.split(","));
        req.setArea("mainland");
        CdnClient client = TencentClient.getCdnClient();
        try {
            ListTopDataResponse resp = client.ListTopData(req);
            log.info("查询域名访问详情成功，域名：{}，维度：{}，开始时间：{}，结束时间：{}", domainName, type, startTime, endTime);
            log.info("查询域名访问详情成功，返回结果：{}", ListTopDataResponse.toJsonString(resp));
            return resp;
        } catch (TencentCloudSDKException e) {
            log.error("查询域名访问详情失败，域名：{}，开始时间：{}，结束时间：{}", domainName, startTime, endTime);
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
            log.info("查询域名计费数据成功，域名：{}，维度：{}，开始时间：{}，结束时间：{}", domainName, type, startTime, endTime);
            log.info("查询域名计费数据成功，返回结果：{}", DescribeBillingDataResponse.toJsonString(resp));
            return resp;
        } catch (TencentCloudSDKException e) {
            log.error("查询域名计费数据失败，域名：{}，维度：{}，开始时间：{}，结束时间：{}", domainName, type, startTime, endTime);
            throw new BusinessException(TencentErrorCodeHandler.getErrorDescription(e)).setCause(e).log();
        }
    }

    private DescribeCdnDataResponse getDescribeCdnData(String domainName, DateTime startTime, DateTime endTime, String type) throws BusinessException {
        DescribeCdnDataRequest req = new DescribeCdnDataRequest();
        req.setStartTime(startTime.toString("yyyy-MM-dd HH:mm:ss"));
        req.setEndTime(endTime.toString("yyyy-MM-dd HH:mm:ss"));
        req.setMetric(type);
        req.setInterval(getDefaultDataUnit(startTime, endTime));
        req.setDomains(domainName.split(","));
        req.setArea("mainland");
        CdnClient client = TencentClient.getCdnClient();
        try {
            DescribeCdnDataResponse resp = client.DescribeCdnData(req);
            log.info("查询域名访问详情成功，域名：{}，维度：{}，开始时间：{}，结束时间：{}", domainName, type, startTime, endTime);
            log.info("查询域名访问详情成功，返回结果：{}", DescribeCdnDataResponse.toJsonString(resp));
            return resp;
        } catch (TencentCloudSDKException e) {
            log.error("查询域名访问详情失败，域名：{}，维度：{}，开始时间：{}，结束时间：{}", domainName, type, startTime, endTime);
            throw new BusinessException(TencentErrorCodeHandler.getErrorDescription(e)).setCause(e).log();
        }
    }


    private DescribeOriginDataResponse getDescribeOriginData(String domainName, DateTime startTime, DateTime endTime, String type) throws BusinessException {
        DescribeOriginDataRequest req = new DescribeOriginDataRequest();
        req.setDomains(domainName.split(","));
        req.setStartTime(startTime.toString("yyyy-MM-dd HH:mm:ss"));
        req.setEndTime(endTime.toString("yyyy-MM-dd HH:mm:ss"));
        req.setMetric(type);
        req.setInterval(getDefaultDataUnit(startTime, endTime));
        req.setArea("mainland");
        CdnClient client = TencentClient.getCdnClient();
        try {
            DescribeOriginDataResponse resp = client.DescribeOriginData(req);
            log.info("查询域名回源详情成功，域名：{}，维度：{}，开始时间：{}，结束时间：{}", domainName, type, startTime, endTime);
            log.info("查询域名回源详情成功，返回结果：{}", DescribeOriginDataResponse.toJsonString(resp));
            return resp;
        } catch (TencentCloudSDKException e) {
            log.error("查询域名回源详情失败，域名：{}，开始时间：{}，结束时间：{}", domainName, startTime, endTime);
            throw new BusinessException(TencentErrorCodeHandler.getErrorDescription(e)).setCause(e).log();
        }
    }

    private String getDefaultDataUnit(DateTime start, DateTime end) {
        long between = DateUtil.between(start, end, DateUnit.DAY);
        if (between > 1) {
            return "day";
        } else {
            return "hour";
        }
    }

    private JSONArray fixArray(JSONArray data, int size) {
        int sized = data.size();
        if (sized < size) {
            for (int i = 0; i < size - sized; i++) {
                data.add(0);
            }
        }
        return data;
    }

    private JSONArray convertTopData(TopDetailData[] topDetailData) {
        return Arrays.stream(topDetailData)
                .map(td -> td.getValue().longValue())
                .collect(Collectors.toCollection(JSONArray::new));
    }

    private JSONArray convertData(TimestampData[] timestampData) {
        return Arrays.stream(timestampData)
                .map(td -> td.getValue().longValue())
                .collect(Collectors.toCollection(JSONArray::new));
    }

    private JSONArray convertData(TimestampData[] timestampData, DateTime end) {
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

    private TimestampData[] mixData(ArrayList<TimestampData[]> timestampData) {
        Map<String, Float> map = timestampData.stream()
                .flatMap(Arrays::stream)
                .collect(Collectors.toMap(
                        TimestampData::getTime,  // 使用 time 作为 key
                        TimestampData::getValue,  // 使用 value 作为 value
                        Float::sum,
                        LinkedHashMap::new));
        return map.entrySet().stream()
                .map(entry -> {
                    TimestampData td = new TimestampData();
                    td.setTime(entry.getKey());
                    td.setValue(entry.getValue());
                    return td;
                })
                .toArray(TimestampData[]::new);
    }
}
