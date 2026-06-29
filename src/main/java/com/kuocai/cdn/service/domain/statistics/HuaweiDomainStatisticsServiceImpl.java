package com.kuocai.cdn.service.domain.statistics;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateTime;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.huawei.cdn.StatisticalAnalysisApi;
import com.kuocai.cdn.api.huawei.cdn.dto.GetTopOneHundredQueryDTO;
import com.kuocai.cdn.constant.KuoCaiConstants;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.service.CdnDomainStatisticsService;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 华为加速域名统计(CdnDomain)服务实现
 */
@Slf4j
@Service
public class HuaweiDomainStatisticsServiceImpl extends BaseService<CdnDomain> implements ICdnStatisticsPlatformService {

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
        } catch (CdnHuaweiException e) {
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
    @Override
    public JSONObject queryVisitsStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        try {
            result.put("visits_summary", queryVisitsSummary(domainName, start, end));
            result.put("visits_detail", queryVisitsDetail(domainName, start, end));
            return result;
        } catch (CdnHuaweiException e) {
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
        } catch (CdnHuaweiException e) {
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
        long startTime = start.getTime();
        long endTime = end.getTime();
        GetTopOneHundredQueryDTO dto = GetTopOneHundredQueryDTO.builder().domain_name(domainName).start_time(startTime).end_time(endTime).stat_type("flux").build();
        JSONArray fluxResult = null;
        try {
            fluxResult = StatisticalAnalysisApi.getTopOneHundred(dto).getJSONArray("top_url_summary");
            dto.setStat_type("req_num");
            JSONArray reqNumResult = StatisticalAnalysisApi.getTopOneHundred(dto).getJSONArray("top_url_summary");
            JSONObject result = new JSONObject();
            result.put("fluxResult", fluxResult);
            result.put("reqNumResult", reqNumResult);
            JedisUtil.setJson(key, result, KuoCaiConstants.smartCacheTime(start, end));
            return result;
        } catch (CdnHuaweiException e) {
            log.error("查询 TOP URI，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
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
        long startTime = start.getTime();
        long endTime = end.getTime();
        GetTopOneHundredQueryDTO dto = GetTopOneHundredQueryDTO.builder().domain_name(domainName).action("summary").stat_type("bw,flux,bs_bw,bs_flux").start_time(startTime).end_time(endTime).build();
        JSONObject jsonObject = null;
        try {
            jsonObject = StatisticalAnalysisApi.getDomainStatistics(dto, false);
        } catch (CdnHuaweiException e) {
            log.error("查询网络资源消耗汇总信息，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        JSONObject result = jsonObject.getJSONObject("result");
        result.put("bw", result.getOrDefault("bw", 0));
        result.put("bs_bw", result.getOrDefault("bs_bw", 0));
        result.put("flux", result.getOrDefault("flux", 0));
        result.put("bs_flux", result.getOrDefault("bs_flux", 0));
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
        long startTime = start.getTime();
        long endTime = end.getTime();
        GetTopOneHundredQueryDTO dto = GetTopOneHundredQueryDTO.builder().domain_name(domainName).action("detail").stat_type("bw,flux,bs_bw,bs_flux").start_time(startTime).end_time(endTime).interval(getInterval(startTime, endTime)).build();
        JSONObject jsonObject = null;
        try {
            jsonObject = StatisticalAnalysisApi.getDomainStatistics(dto, false);
        } catch (CdnHuaweiException e) {
            log.error("查询网络资源消耗汇总信息，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        JSONObject result = jsonObject.getJSONObject("result");
        result.put("bw", result.getOrDefault("bw", getDefaultDataJSONArray(getDefaultDataSize(start, end))));
        result.put("bs_bw", result.getOrDefault("bs_bw", getDefaultDataJSONArray(getDefaultDataSize(start, end))));
        result.put("flux", result.getOrDefault("flux", getDefaultDataJSONArray(getDefaultDataSize(start, end))));
        result.put("bs_flux", result.getOrDefault("bs_flux", getDefaultDataJSONArray(getDefaultDataSize(start, end))));
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
    private JSONObject queryVisitsSummary(String domainName, DateTime start, DateTime end) throws BusinessException {
        long startTime = start.getTime();
        long endTime = end.getTime();
        GetTopOneHundredQueryDTO dto = GetTopOneHundredQueryDTO.builder().domain_name(domainName).action("summary").stat_type("req_num,hit_num,bs_num,bs_fail_num,hit_flux").start_time(startTime).end_time(endTime).build();
        JSONObject jsonObject = null;
        try {
            jsonObject = StatisticalAnalysisApi.getDomainStatistics(dto, false);
        } catch (CdnHuaweiException e) {
            log.error("查询访问情况汇总信息，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        JSONObject result = jsonObject.getJSONObject("result");
        result.put("req_num", result.getOrDefault("req_num", 0));
        result.put("hit_flux", result.getOrDefault("hit_flux", 0));
        result.put("hit_num", result.getOrDefault("hit_num", 0));
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
    private JSONObject queryVisitsDetail(String domainName, DateTime start, DateTime end) throws BusinessException {
        long startTime = start.getTime();
        long endTime = end.getTime();
        GetTopOneHundredQueryDTO dto = GetTopOneHundredQueryDTO.builder().domain_name(domainName).action("detail").stat_type("req_num,hit_num,bs_num,bs_fail_num,hit_flux").start_time(startTime).end_time(endTime).interval(getInterval(startTime, endTime)).build();
        JSONObject jsonObject = null;
        try {
            jsonObject = StatisticalAnalysisApi.getDomainStatistics(dto, false);
        } catch (CdnHuaweiException e) {
            log.error("查询访问情况汇总信息，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        JSONObject result = jsonObject.getJSONObject("result");
        result.put("req_num", result.getOrDefault("req_num", getDefaultDataJSONArray(getDefaultDataSize(start, end))));
        result.put("hit_flux", result.getOrDefault("hit_flux", getDefaultDataJSONArray(getDefaultDataSize(start, end))));
        result.put("hit_num", result.getOrDefault("hit_num", getDefaultDataJSONArray(getDefaultDataSize(start, end))));
        return result;
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
    private List<Long> queryCodeStatusSummary(String domainName, DateTime start, DateTime end) throws BusinessException {
        long startTime = start.getTime();
        long endTime = end.getTime();
        GetTopOneHundredQueryDTO dto = GetTopOneHundredQueryDTO.builder().domain_name(domainName).action("summary").stat_type("http_code_3xx,http_code_2xx,http_code_4xx,http_code_5xx").start_time(startTime).end_time(endTime).build();
        JSONObject jsonObject = null;
        try {
            jsonObject = StatisticalAnalysisApi.getDomainStatistics(dto, false);
        } catch (CdnHuaweiException e) {
            log.error("查询状态码汇总信息，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        JSONObject object = jsonObject.getJSONObject("result");
        return ListUtil.toList(object.getLongValue("http_code_2xx"), object.getLongValue("http_code_3xx"), object.getLongValue("http_code_4xx"), object.getLongValue("http_code_5xx"));
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
    private List<Long> queryBsCodeStatusSummary(String domainName, DateTime start, DateTime end) throws BusinessException {
        long startTime = start.getTime();
        long endTime = end.getTime();
        GetTopOneHundredQueryDTO dto = GetTopOneHundredQueryDTO.builder().domain_name(domainName).action("summary").stat_type("bs_http_code_3xx,bs_http_code_2xx,bs_http_code_4xx,bs_http_code_5xx").start_time(startTime).end_time(endTime).build();
        JSONObject jsonObject = null;
        try {
            jsonObject = StatisticalAnalysisApi.getDomainStatistics(dto, false);
        } catch (CdnHuaweiException e) {
            log.error("查询回源状态码汇总信息，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        JSONObject object = jsonObject.getJSONObject("result");
        return ListUtil.toList(object.getLongValue("bs_http_code_2xx"), object.getLongValue("bs_http_code_3xx"), object.getLongValue("bs_http_code_4xx"), object.getLongValue("bs_http_code_5xx"));
    }

    /**
     * 查询状态码详情信息
     *
     * @param domainName 域名信息
     * @param start      开始时间
     * @param end        结束时间
     * @return 响应
     */
    private List<Object> queryCodeStatusDetail(String domainName, DateTime start, DateTime end) throws BusinessException {
        long startTime = start.getTime();
        long endTime = end.getTime();
        GetTopOneHundredQueryDTO dto = GetTopOneHundredQueryDTO.builder().domain_name(domainName).action("detail").stat_type("http_code_3xx,http_code_2xx,http_code_4xx,http_code_5xx").interval(getInterval(startTime, endTime)).start_time(startTime).end_time(endTime).build();
        JSONObject jsonObject = null;
        try {
            jsonObject = StatisticalAnalysisApi.getDomainStatistics(dto, false);
        } catch (CdnHuaweiException e) {
            log.error("查询状态码详情信息，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        JSONObject data = jsonObject.getJSONObject("result");
        int size = getDefaultDataSize(start, end);
        List<Object> array = new ArrayList<>();
        JSONArray httpCode2xx = data.getJSONArray("http_code_2xx");
        array.add(Assert.notEmpty(httpCode2xx) ? httpCode2xx.toArray() : getLongArray(size));
        JSONArray httpCode3xx = data.getJSONArray("http_code_3xx");
        array.add(Assert.notEmpty(httpCode3xx) ? httpCode3xx.toArray() : getLongArray(size));
        JSONArray httpCode4xx = data.getJSONArray("http_code_4xx");
        array.add(Assert.notEmpty(httpCode4xx) ? httpCode4xx.toArray() : getLongArray(size));
        JSONArray httpCode5xx = data.getJSONArray("http_code_5xx");
        array.add(Assert.notEmpty(httpCode5xx) ? httpCode5xx.toArray() : getLongArray(size));
        return array;
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
        long startTime = start.getTime();
        long endTime = end.getTime();
        GetTopOneHundredQueryDTO dto = GetTopOneHundredQueryDTO.builder().domain_name(domainName).action("detail").stat_type("bs_http_code_3xx,bs_http_code_2xx,bs_http_code_4xx,bs_http_code_5xx").interval(getInterval(startTime, endTime)).start_time(startTime).end_time(endTime).build();
        JSONObject jsonObject = null;
        try {
            jsonObject = StatisticalAnalysisApi.getDomainStatistics(dto, false);
        } catch (CdnHuaweiException e) {
            log.error("查询回源状态码详情信息，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        JSONObject data = jsonObject.getJSONObject("result");
        int size = getDefaultDataSize(start, end);
        List<Object> array = new ArrayList<>();
        JSONArray httpCode2xx = data.getJSONArray("bs_http_code_2xx");
        array.add(Assert.notEmpty(httpCode2xx) ? httpCode2xx.toArray() : getLongArray(size));
        JSONArray httpCode3xx = data.getJSONArray("bs_http_code_3xx");
        array.add(Assert.notEmpty(httpCode3xx) ? httpCode3xx.toArray() : getLongArray(size));
        JSONArray httpCode4xx = data.getJSONArray("bs_http_code_4xx");
        array.add(Assert.notEmpty(httpCode4xx) ? httpCode4xx.toArray() : getLongArray(size));
        JSONArray httpCode5xx = data.getJSONArray("bs_http_code_5xx");
        array.add(Assert.notEmpty(httpCode5xx) ? httpCode5xx.toArray() : getLongArray(size));
        return array;
    }

    /**
     * 默认统计数据大小
     */
    private int getDefaultDataSize(DateTime start, DateTime end) {
        return statisticsService.getLabels(start, end).size();
    }

    /**
     * 默认JSONArray数据
     */
    private JSONArray getDefaultDataJSONArray(int size) {
        Object[] objects = new Object[size];
        Arrays.fill(objects, 0);
        List<Object> list = new ArrayList<>(size);
        Collections.addAll(list, objects);
        return new JSONArray(list);
    }

    /**
     * 获取时间间隔
     *
     * @param startTime 开始时间戳
     * @param endTime   结束时间戳
     */
    private Long getInterval(long startTime, long endTime) {
        return (endTime - startTime) > 86400000L ? 86400L : 3600L;
    }

    /**
     * 默认默认的list
     */
    private Object[] getLongArray(int size) {
        Object[] objects = new Object[size];
        Arrays.fill(objects, 0l);
        return objects;
    }

}
