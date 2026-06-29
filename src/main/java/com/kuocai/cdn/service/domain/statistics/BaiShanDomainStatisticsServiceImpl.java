package com.kuocai.cdn.service.domain.statistics;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.baishan.cdn.BsDomainStatisticsApi;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.service.CdnDomainStatisticsService;
import com.kuocai.cdn.service.base.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 白山云加速域名统计(CdnDomain)服务
 */
@Slf4j
@Service
public class BaiShanDomainStatisticsServiceImpl extends BaseService<CdnDomain> implements ICdnStatisticsPlatformService {

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
    @Override
    public JSONObject queryResourceStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        result.put("resource_summary", queryResourceSummary(domainName, start, end));
        result.put("resource_detail", queryResourceDetail(domainName, start, end));
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
    private JSONObject queryResourceSummary(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        try {
            // 流量
            double flux = BsDomainStatisticsApi.bandwidthSummary(domainName, start, end, "traffic");
            //带宽
            double bw = BsDomainStatisticsApi.bandwidthSummary(domainName, start, end, "bandwidth");
            // 回源流量
            double bsFlux = BsDomainStatisticsApi.originBandwidthSummary(domainName, start, end, "traffic");
            // 回源带宽
            double bsBw = BsDomainStatisticsApi.originBandwidthSummary(domainName, start, end, "bandwidth");
            result.put("bw",bw);
            result.put("bs_bw", bsBw);
            result.put("flux", flux);
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
            String defaultDataUnit = getDefaultDataUnit(start, end);
            List<Double> flux = BsDomainStatisticsApi.bandwidthDetails(domainName, start, end, "traffic", defaultDataUnit);
            List<Double> bw = BsDomainStatisticsApi.bandwidthDetails(domainName, start, end, "bandwidth", defaultDataUnit);
            List<Double> bsFlux = BsDomainStatisticsApi.originBandwidthDetails(domainName, start, end, "traffic", defaultDataUnit);
            List<Double> bsBw = BsDomainStatisticsApi.originBandwidthDetails(domainName, start, end, "bandwidth", defaultDataUnit);
            result.put("bw", bw);
            result.put("bs_bw", bsBw);
            result.put("flux", flux);
            result.put("bs_flux", bsFlux);
        } catch (Exception e) {
            log.error("查询网络资源消耗明细信息，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        return result;
    }

    @Override
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

            JSONObject result = new JSONObject();
            result.put("req_num", 0l);
            result.put("hit_flux", 0l);
            result.put("hit_num", 0l);
//            result.put("bs_num", 0l);
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
            String defaultDataUnit = getDefaultDataUnit(start, end);
            int defaultDataSize = getDefaultDataSize(start, end);
            JSONObject result = new JSONObject();
            result.put("req_num", getDefaultDataJSONArray(defaultDataSize));
            result.put("hit_flux", getDefaultDataJSONArray(defaultDataSize));
            result.put("hit_num", getDefaultDataJSONArray(defaultDataSize));
//            result.put("bs_num", bsPv);
//            result.put("bs_fail_num", bsFailNum);
            return result;
        } catch (Exception e) {
            log.error("查询访问情况汇总信息，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }


    @Override
    public JSONObject queryHttpCodeStatusStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        try {
            result.put("status_summary", queryCodeStatusSummary(domainName, start, end));
            // 七牛目前没有
            result.put("bs_status_summary", queryBsCodeStatusSummary(domainName, start, end));
            result.put("status_detail", queryCodeStatusDetail(domainName, start, end));
            // 七牛目前没有
            result.put("bs_status_detail", queryBsCodeStatusDetail(domainName, start, end));
            return result;
        } catch (Exception e) {
            log.error("查询HTTP状态码统计信息，域名：{}，开始时间：{}，结束时间：{}", domainName, start, end);
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
            Map<String, Double> stringDoubleMap = BsDomainStatisticsApi.httpCodeSummary(domainName, start, end);
            long status2xx = stringDoubleMap.get("2").longValue();
            long status3xx = stringDoubleMap.get("3").longValue();
            long status4xx = stringDoubleMap.get("4").longValue();
            long status5xx = stringDoubleMap.get("5").longValue();
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
        long status2xx = 0l;
        long status3xx = 0l;
        long status4xx = 0l;
        long status5xx = 0l;
        List<Long> result = ListUtil.toList(status2xx, status3xx, status4xx, status5xx);
        return result;
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
            String defaultDataUnit = getDefaultDataUnit(start, end);
            Map<String, List<Double>> stringListMap = BsDomainStatisticsApi.httpCodeDetails(domainName, start, end, defaultDataUnit);
            List<Long> status2xxs = Convert.toList(Long.class, stringListMap.get("2"));
            List<Long> status3xxs = Convert.toList(Long.class, stringListMap.get("3"));
            List<Long> status4xxs = Convert.toList(Long.class, stringListMap.get("4"));
            List<Long> status5xxs = Convert.toList(Long.class, stringListMap.get("5"));
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
        String defaultDataUnit = getDefaultDataUnit(start, end);
        int defaultDataSize = getDefaultDataSize(start, end);
        JSONArray status2xxs = getDefaultDataJSONArray(defaultDataSize);
        JSONArray status3xxs = getDefaultDataJSONArray(defaultDataSize);
        JSONArray status4xxs = getDefaultDataJSONArray(defaultDataSize);
        JSONArray status5xxs = getDefaultDataJSONArray(defaultDataSize);
        List<Object> results = ListUtil.toList(status2xxs.toArray(), status3xxs.toArray(), status4xxs.toArray(), status5xxs.toArray());
        return results;
    }

    /**
     * 查询 TOP URI
     * 暂未使用
     *
     * @param domainName 域名
     * @param start      开始时间
     * @param end        结束时间
     * @return 结果
     * @throws CdnHuaweiException 异常
     */
    @Override
    public Object queryTopUri(String domainName, DateTime start, DateTime end) throws BusinessException {
        return null;
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

    public static void main(String[] args) throws BusinessException {

    }
}
