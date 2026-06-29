package com.kuocai.cdn.service.domain.statistics;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.volcengine.cdn.properties.VolcengineCdn;
import com.kuocai.cdn.constant.KuoCaiConstants;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.service.CdnDomainStatisticsService;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import com.volcengine.model.beans.CDN;
import com.volcengine.service.cdn.CDNService;
import com.volcengine.service.cdn.impl.CDNServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 火山云加速域名统计(CdnDomain)服务
 */
@Slf4j
@Service
public class VolCenGineDomainStatisticsServiceImpl extends BaseService<CdnDomain> implements ICdnStatisticsPlatformService {

    @Resource
    private CdnDomainStatisticsService statisticsService;

    /**
     * 查询网络资源消耗统计信息
     *
     * @param domainName 域名
     * @param start      开始时间
     * @param end        结束时间
     * @return 响应
     */
    public JSONObject queryResourceStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        try {
            result.put("resource_summary", queryResourceSummary(domainName, start, end));
            result.put("resource_detail", queryResourceDetail(domainName, start, end));
        } catch (Exception e) {
            log.error("查询网络资源消耗统计信，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        return result;
    }

    /**
     * 查询访问情况统计信息
     *
     * @param domainName 域名
     * @param start      开始时间
     * @param end        结束时间
     * @return 响应
     */
    public JSONObject queryVisitsStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        try {
            result.put("visits_summary", queryVisitsSummary(domainName, start, end));
            result.put("visits_detail", queryVisitsDetail(domainName, start, end));
            return result;
        } catch (Exception e) {
            log.error("查询访问情况统计信息，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 查询HTTP状态码统计信息
     *
     * @param domainName 域名
     * @param start      开始时间
     * @param end        结束时间
     * @return 响应
     */
    public JSONObject queryHttpCodeStatusStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        try {
            result.put("status_summary", queryCodeStatusSummary(domainName, start, end));
            result.put("bs_status_summary", queryBsCodeStatusSummary(domainName, start, end));
            result.put("status_detail", queryCodeStatusDetail(domainName, start, end));
            result.put("bs_status_detail", queryBsCodeStatusDetail(domainName, start, end));
            return result;
        } catch (Exception e) {
            log.error("查询HTTP状态码统计信息，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 查询 TOP URI
     *
     * @param domainName 域名
     * @param start      开始时间
     * @param end        结束时间
     * @return 结果
     * @throws CdnHuaweiException 异常
     */
    public JSONObject queryTopUri(String domainName, DateTime start, DateTime end) throws BusinessException {
        String key = String.format("%s:%s:%s:%s", "top", domainName.hashCode(), start.getTime(), end.getTime());
        JSONObject cacheData = JedisUtil.getJson(key);
        if (Assert.notEmpty(cacheData)) {
            return cacheData;
        }
        try {
            // 手动对多个域名进行排序，火山云支持值单个域名查询
            List<String> domains = Arrays.asList(domainName.split(","));
            JSONArray fluxs = new JSONArray();
            JSONArray pvs = new JSONArray();
            for (String domain : domains) {
                JSONArray flux = queryAccessRank(domain, start, end, "flux");
                if (Assert.notEmpty(flux)) {
                    fluxs.addAll(flux);
                }
                JSONArray pv = queryAccessRank(domain, start, end, "pv");
                if (Assert.notEmpty(pv)) {
                    pvs.addAll(pv);
                }
            }

            JSONArray extractedFluxs = extracted(fluxs);
            JSONArray extractedPvs = extracted(pvs);
            JSONObject result = new JSONObject();
            result.put("fluxResult", extractedFluxs);
            result.put("reqNumResult", extractedPvs);
            JedisUtil.setJson(key, result, KuoCaiConstants.smartCacheTime(start, end));
            return result;
        } catch (CdnHuaweiException e) {
            log.error("查询 TOP URI，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 对jsonArray排序，limit100
     *
     * @param jsonArray
     * @return
     */
    public JSONArray extracted(JSONArray jsonArray) {
        // 对JSONArray进行降序排序
        jsonArray.sort((o1, o2) -> {
            JSONObject j1 = (JSONObject) o1;
            JSONObject j2 = (JSONObject) o2;
            long v1 = j1.getLongValue("value");
            long v2 = j2.getLongValue("value");
            return Long.compare(v2, v1);
        });

        // 限制JSONArray前100个元素
        JSONArray limitedArray = new JSONArray();
        for (int i = 0; i < Math.min(100, jsonArray.size()); i++) {
            limitedArray.add(jsonArray.get(i));
        }
        return limitedArray;
    }

    /**
     * 查询网络资源消耗汇总信息
     *
     * @param domainName 域名
     * @param start      开始时间
     * @param end        结束时间
     * @return 结果
     * @throws CdnHuaweiException 自定义异常
     */
    private JSONObject queryResourceSummary(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        try {
            long fluxValue = queryAccessSummary(domainName, start, end, "flux");
            long bsFlux = queryOriginSummary(domainName, start, end, "flux");
            long bandwidthValue = queryAccessSummary(domainName, start, end, "bandwidth");
            long bsBw = queryOriginSummary(domainName, start, end, "bandwidth");
            result.put("bw", bandwidthValue);
            result.put("bs_bw", bsBw);
            result.put("flux", fluxValue);
            result.put("bs_flux", bsFlux);
        } catch (Exception e) {
            log.error("查询网络资源消耗汇总信息，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        return result;
    }

    /**
     * 查询网络资源消耗汇总信息
     *
     * @param domainName 域名
     * @param start      开始时间
     * @param end        结束时间
     * @return 结果
     * @throws CdnHuaweiException 自定义异常
     */
    private JSONObject queryResourceDetail(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        try {
            JSONArray fluxValue = queryAccessDetail(domainName, start, end, "flux");
            JSONArray bsFlux = queryOriginDetail(domainName, start, end, "flux");
            JSONArray bandwidthValue = queryAccessDetail(domainName, start, end, "bandwidth");
            JSONArray bsBw = queryOriginDetail(domainName, start, end, "bandwidth");
            result.put("bw", bandwidthValue);
            result.put("bs_bw", bsBw);
            result.put("flux", fluxValue);
            result.put("bs_flux", bsFlux);
        } catch (Exception e) {
            log.error("查询网络资源消耗明细信息，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        return result;
    }

    /**
     * 查询访问情况汇总信息
     *
     * @param domainName 域名
     * @param start      开始时间
     * @param end        结束时间
     * @return 结果
     * @throws CdnHuaweiException 自定义异常
     */
    public JSONObject queryVisitsSummary(String domainName, DateTime start, DateTime end) throws BusinessException {
        try {
            // 访问请求总数
            long reqNum = queryAccessSummary(domainName, start, end, "pv");
            // 请求命中率
            double hitRate = queryAccessSummary2Double(domainName, start, end, "pvhitrate");
            // 访问数据的总流量
            long flux = queryAccessSummary(domainName, start, end, "flux");
            // 流量命中率
            double fluxRate = queryAccessSummary2Double(domainName, start, end, "hitrate");
            // 命中流量
            long hitFlux = (long) (flux * fluxRate / 100.0);
            long hitReqNum = (long) (hitRate * reqNum / 100.0);
            // 回源总数
            long bsPv = queryOriginSummary(domainName, start, end, "pv");
            // 回源失败数
            long bsFailNum = 0;
            JSONObject result = new JSONObject();
            result.put("req_num", reqNum);
            result.put("hit_flux", hitFlux);
            result.put("hit_num", hitReqNum);
            result.put("bs_num", bsPv);
//            result.put("bs_fail_num", bsFailNum);
            return result;
        } catch (Exception e) {
            log.error("查询访问情况汇总信息，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 查询访问情况汇总信息
     *
     * @param domainName 域名
     * @param start      开始时间
     * @param end        结束时间
     * @return 结果
     * @throws CdnHuaweiException 自定义异常
     */
    public JSONObject queryVisitsDetail(String domainName, DateTime start, DateTime end) throws BusinessException {
        try {
            // 访问请求总数
            JSONArray reqNum = queryAccessDetail(domainName, start, end, "pv");
            // 请求命中率
            JSONArray hitRate = queryAccessDetail2Double(domainName, start, end, "pvhitrate");
            // 请求命中次数
            JSONArray hitReqNum = new JSONArray();
            for (int i = 0; i < reqNum.size(); i++) {
                long item = (long) (reqNum.getLongValue(i) * hitRate.getDoubleValue(i) / 100);
                hitReqNum.add(item);
            }
            // 访问数据的总流量
            JSONArray flux = queryAccessDetail(domainName, start, end, "flux");
            // 流量命中率
            JSONArray fluxRate = queryAccessDetail2Double(domainName, start, end, "hitrate");
            // 命中流量
            JSONArray hitFlux = new JSONArray();
            for (int i = 0; i < flux.size(); i++) {
                long item = (long) (flux.getLongValue(i) * fluxRate.getDoubleValue(i) / 100.0);
                hitFlux.add(item);
            }
            // 回源总数
            JSONArray bsPv = queryOriginDetail(domainName, start, end, "pv");
            // 回源失败数
            JSONArray bsFailNum = new JSONArray();
            JSONObject result = new JSONObject();
            result.put("req_num", reqNum);
            result.put("hit_flux", hitFlux);
            result.put("hit_num", hitReqNum);
            result.put("bs_num", bsPv);
//            result.put("bs_fail_num", bsFailNum);
            return result;
        } catch (Exception e) {
            log.error("查询访问情况汇总信息，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 查询状态码汇总信息
     *
     * @param domainName 域名信息
     * @param start      开始时间
     * @param end        结束是啊金
     * @return 结果
     * @throws CdnHuaweiException 自定义异常
     */
    public List<Long> queryCodeStatusSummary(String domainName, DateTime start, DateTime end) throws BusinessException {
        try {
            long status2xx = queryAccessSummary(domainName, start, end, "status_2xx");
            long status3xx = queryAccessSummary(domainName, start, end, "status_3xx");
            long status4xx = queryAccessSummary(domainName, start, end, "status_4xx");
            long status5xx = queryAccessSummary(domainName, start, end, "status_5xx");
            List<Long> result = ListUtil.toList(status2xx, status3xx, status4xx, status5xx);
            return result;
        } catch (Exception e) {
            log.error("查询状态码汇总信息，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 查询回源状态码汇总信息
     *
     * @param domainName 域名信息
     * @param start      开始时间
     * @param end        结束是啊金
     * @return 结果
     * @throws CdnHuaweiException
     */
    public List<Long> queryBsCodeStatusSummary(String domainName, DateTime start, DateTime end) throws BusinessException {
        try {
            long status2xx = queryOriginSummary(domainName, start, end, "status_2xx");
            long status3xx = queryOriginSummary(domainName, start, end, "status_3xx");
            long status4xx = queryOriginSummary(domainName, start, end, "status_4xx");
            long status5xx = queryOriginSummary(domainName, start, end, "status_5xx");
            List<Long> result = ListUtil.toList(status2xx, status3xx, status4xx, status5xx);
            return result;
        } catch (Exception e) {
            log.error("查询回源状态码汇总信息，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 查询状态码详情信息
     *
     * @param domainName 域名信息
     * @param start      开始时间
     * @param end        结束时间
     * @return 响应
     */
    public List<Object> queryCodeStatusDetail(String domainName, DateTime start, DateTime end) throws BusinessException {
        try {
            JSONArray status2xxs = queryAccessDetail(domainName, start, end, "status_2xx");
            JSONArray status3xxs = queryAccessDetail(domainName, start, end, "status_3xx");
            JSONArray status4xxs = queryAccessDetail(domainName, start, end, "status_4xx");
            JSONArray status5xxs = queryAccessDetail(domainName, start, end, "status_5xx");
            List<Object> results = ListUtil.toList(status2xxs.toArray(), status3xxs.toArray(), status4xxs.toArray(), status5xxs.toArray());
            return results;
        } catch (Exception e) {
            log.error("查询状态码详情信息，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 查询回源状态码详情信息
     *
     * @param domainName 域名信息
     * @param start      开始时间
     * @param end        结束时间
     * @return 响应
     */
    private List<Object> queryBsCodeStatusDetail(String domainName, DateTime start, DateTime end) throws BusinessException {
        try {
            JSONArray status2xxs = queryOriginDetail(domainName, start, end, "status_2xx");
            JSONArray status3xxs = queryOriginDetail(domainName, start, end, "status_3xx");
            JSONArray status4xxs = queryOriginDetail(domainName, start, end, "status_4xx");
            JSONArray status5xxs = queryOriginDetail(domainName, start, end, "status_5xx");
            List<Object> results = ListUtil.toList(status2xxs.toArray(), status3xxs.toArray(), status4xxs.toArray(), status5xxs.toArray());
            return results;
        } catch (Exception e) {
            log.error("查询回源状态码详情信息，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }


    /**
     * 查询访问统计的汇总数据
     *
     * @param domainName
     * @param startTime
     * @param endTime
     * @param type       flux, bandwidth
     * @return
     * @throws BusinessException
     */
    public long queryAccessSummary(String domainName, DateTime startTime, DateTime endTime, String type) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            long start = startTime.getTime() / 1000;
            long end = endTime.getTime() / 1000;
            CDN.DescribeEdgeNrtDataSummaryRequest req = new CDN.DescribeEdgeNrtDataSummaryRequest()
                    .setDomain(domainName)
                    .setStartTime(start)
                    .setEndTime(end)
                    .setAggregate("aggregate")
                    .setMetric(type);
            CDN.DescribeEdgeNrtDataSummaryResponse resp = null;
            resp = service.describeEdgeNrtDataSummary(req);
            return dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    /**
     * 适用与返回double的
     *
     * @param domainName
     * @param startTime
     * @param endTime
     * @param type
     * @return
     * @throws BusinessException
     */
    public double queryAccessSummary2Double(String domainName, DateTime startTime, DateTime endTime, String type) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            long start = startTime.getTime() / 1000;
            long end = endTime.getTime() / 1000;
            CDN.DescribeEdgeNrtDataSummaryRequest req = new CDN.DescribeEdgeNrtDataSummaryRequest()
                    .setDomain(domainName)
                    .setStartTime(start)
                    .setEndTime(end)
                    .setAggregate("aggregate")
                    .setMetric(type);
            CDN.DescribeEdgeNrtDataSummaryResponse resp = null;
            resp = service.describeEdgeNrtDataSummary(req);
            return dealResponse2Double(JSON.toJSONString(resp));
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    /**
     * 查询回源统计的汇总数据
     *
     * @param domainName
     * @param startTime
     * @param endTime
     * @param type       flux, bandwidth
     * @return
     * @throws BusinessException
     */
    public long queryOriginSummary(String domainName, DateTime startTime, DateTime endTime, String type) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            long start = startTime.getTime() / 1000;
            long end = endTime.getTime() / 1000;
            CDN.DescribeOriginNrtDataSummaryRequest req = new CDN.DescribeOriginNrtDataSummaryRequest()
                    .setDomain(domainName)
                    .setStartTime(start)
                    .setEndTime(end)
                    //设置汇总
                    .setAggregate("aggregate")
                    .setMetric(type);
            CDN.DescribeOriginNrtDataSummaryResponse resp = null;
            resp = service.describeOriginNrtDataSummary(req);
            return dealResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    /**
     * 查询访问统计的汇总数据
     *
     * @param domainName
     * @param startTime
     * @param endTime
     * @param type       flux, bandwidth
     * @return
     * @throws BusinessException
     */
    public JSONArray queryAccessDetail(String domainName, DateTime startTime, DateTime endTime, String type) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            String defaultDataUnit = getDefaultDataUnit(startTime, endTime);
            long start = startTime.getTime() / 1000;
            long end = endTime.getTime() / 1000;
            CDN.DescribeCdnDataRequest req = new CDN.DescribeCdnDataRequest()
                    .setDomain(domainName)
                    .setStartTime(start)
                    .setEndTime(end)
                    .setInterval(defaultDataUnit)
                    .setAggregate("aggregate")
                    .setMetric(type);
            CDN.DescribeCdnDataResponse resp = null;
            resp = service.describeCdnData(req);
            return dealDetailResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    public JSONArray queryAccessDetail2Double(String domainName, DateTime startTime, DateTime endTime, String type) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            String defaultDataUnit = getDefaultDataUnit(startTime, endTime);
            long start = startTime.getTime() / 1000;
            long end = endTime.getTime() / 1000;
            CDN.DescribeCdnDataRequest req = new CDN.DescribeCdnDataRequest()
                    .setDomain(domainName)
                    .setStartTime(start)
                    .setEndTime(end)
                    .setInterval(defaultDataUnit)
                    .setAggregate("aggregate")
                    .setMetric(type);
            CDN.DescribeCdnDataResponse resp = null;
            resp = service.describeCdnData(req);
            return dealDetailResponse2Double(JSON.toJSONString(resp));
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    /**
     * 查询回源统计的汇总数据
     *
     * @param domainName
     * @param startTime
     * @param endTime
     * @param type       flux, bandwidth
     * @return
     * @throws BusinessException
     */
    public JSONArray queryOriginDetail(String domainName, DateTime startTime, DateTime endTime, String type) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            String defaultDataUnit = getDefaultDataUnit(startTime, endTime);
            long start = startTime.getTime() / 1000;
            long end = endTime.getTime() / 1000;
            CDN.DescribeCdnOriginDataRequest req = new CDN.DescribeCdnOriginDataRequest()
                    .setDomain(domainName)
                    .setStartTime(start)
                    .setEndTime(end)
                    .setInterval(defaultDataUnit)
                    .setAggregate("aggregate")
                    .setMetric(type);
            CDN.DescribeCdnOriginDataResponse resp = null;
            resp = service.describeCdnOriginData(req);
            return dealDetailResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    /**
     * 获取访问数据的统计排名
     *
     * @param domainName
     * @param startTime
     * @param endTime
     * @param type       flux, bandwidth
     * @return
     * @throws BusinessException
     */
    public JSONArray queryAccessRank(String domainName, DateTime startTime, DateTime endTime, String type) throws BusinessException {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            long start = startTime.getTime() / 1000;
            long end = endTime.getTime() / 1000;
            CDN.DescribeEdgeTopStatisticalDataRequest req = new CDN.DescribeEdgeTopStatisticalDataRequest()
                    .setDomain(domainName)
                    .setStartTime(start)
                    .setEndTime(end)
                    .setItem("url")
                    .setMetric(type);
            CDN.DescribeEdgeTopStatisticalDataResponse resp = null;
            resp = service.describeEdgeTopStatisticalData(req);
            return dealTopResponse(JSON.toJSONString(resp));
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        } finally {
            service.destroy();
        }
    }

    public long dealResponse(String response) throws BusinessException {
        JSONObject responseObject = JSONObject.parseObject(response);
        JSONObject responseMetadata = responseObject.getJSONObject("ResponseMetadata");
        if (responseMetadata.containsKey("Error")) {
            JSONObject error = responseMetadata.getJSONObject("Error");
            throw new BusinessException(error.getString("Message"));
        } else {
            try {
                return responseObject.getJSONObject("Result").getJSONArray("Resources").getJSONObject(0).getJSONArray("Metrics").getJSONObject(0).getLongValue("Value");
            } catch (Exception e) {
                throw new BusinessException("获取统计信息失败");
            }
        }
    }

    public double dealResponse2Double(String response) throws BusinessException {
        JSONObject responseObject = JSONObject.parseObject(response);
        JSONObject responseMetadata = responseObject.getJSONObject("ResponseMetadata");
        if (responseMetadata.containsKey("Error")) {
            JSONObject error = responseMetadata.getJSONObject("Error");
            throw new BusinessException(error.getString("Message"));
        } else {
            try {
                return responseObject.getJSONObject("Result").getJSONArray("Resources").getJSONObject(0).getJSONArray("Metrics").getJSONObject(0).getDoubleValue("Value");
            } catch (Exception e) {
                throw new BusinessException("获取统计信息失败");
            }
        }
    }

    public JSONArray dealTopResponse(String response) throws BusinessException {
        JSONObject responseObject = JSONObject.parseObject(response);
        JSONObject responseMetadata = responseObject.getJSONObject("ResponseMetadata");
        if (responseMetadata.containsKey("Error")) {
            JSONObject error = responseMetadata.getJSONObject("Error");
            throw new BusinessException(error.getString("Message"));
        } else {
            try {
                JSONArray resultArray = new JSONArray();
                JSONArray jsonArray = (JSONArray) responseObject.getJSONObject("Result").getOrDefault("TopDataDetails", new JSONArray());
                for (Object o : jsonArray) {
                    JSONObject jsonObject = (JSONObject) o;
                    JSONObject valueObj = new JSONObject();
                    valueObj.put("url", jsonObject.getString("ItemKey"));
                    valueObj.put("value", jsonObject.getBigDecimal("Value"));
                    resultArray.add(valueObj);
                }
                return resultArray;
            } catch (Exception e) {
                throw new BusinessException("获取统计信息失败");
            }
        }
    }

    public JSONArray dealDetailResponse(String response) throws BusinessException {
        JSONObject responseObject = JSONObject.parseObject(response);
        JSONObject responseMetadata = responseObject.getJSONObject("ResponseMetadata");
        if (responseMetadata.containsKey("Error")) {
            JSONObject error = responseMetadata.getJSONObject("Error");
            throw new BusinessException(error.getString("Message"));
        } else {
            try {
                JSONArray result = new JSONArray();
                JSONArray jsonArray = responseObject.getJSONObject("Result").getJSONArray("Resources");
                JSONObject resourceObj = jsonArray.getJSONObject(0);
                JSONArray values = resourceObj.getJSONArray("Metrics").getJSONObject(0).getJSONArray("Values");
                for (int j = 0; j < values.size() - 1; j++) {
                    JSONObject valueObj = values.getJSONObject(j);
                    if (ObjectUtil.equal(valueObj.getBigDecimal("Value"), BigDecimal.ZERO)) {
                        result.add(0l);
                    } else {
                        result.add(valueObj.getBigDecimal("Value").longValue());
                    }
                }
                return result;
            } catch (Exception e) {
                throw new BusinessException("获取统计信息失败");
            }
        }
    }

    public JSONArray dealDetailResponse2Double(String response) throws BusinessException {
        JSONObject responseObject = JSONObject.parseObject(response);
        JSONObject responseMetadata = responseObject.getJSONObject("ResponseMetadata");
        if (responseMetadata.containsKey("Error")) {
            JSONObject error = responseMetadata.getJSONObject("Error");
            throw new BusinessException(error.getString("Message"));
        } else {
            try {
                JSONArray result = new JSONArray();
                JSONArray jsonArray = responseObject.getJSONObject("Result").getJSONArray("Resources");
                JSONObject resourceObj = jsonArray.getJSONObject(0);
                JSONArray values = resourceObj.getJSONArray("Metrics").getJSONObject(0).getJSONArray("Values");
                for (int j = 0; j < values.size() - 1; j++) {
                    JSONObject valueObj = values.getJSONObject(j);
                    if (ObjectUtil.equal(valueObj.getBigDecimal("Value"), BigDecimal.ZERO)) {
                        result.add(Double.valueOf("0"));
                    } else {
                        result.add(valueObj.getBigDecimal("Value").doubleValue());
                    }
                }
                return result;
            } catch (Exception e) {
                throw new BusinessException("获取统计信息失败");
            }
        }
    }

    /**
     * 默认统计数据单位
     */
    private String getDefaultDataUnit(DateTime start, DateTime end) {
        long between = DateUtil.between(start, end, DateUnit.DAY);
        if (between > 1) {
            return "day";
        } else {
            return "hour";
        }
    }

}
