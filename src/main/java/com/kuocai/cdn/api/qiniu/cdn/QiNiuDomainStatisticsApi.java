package com.kuocai.cdn.api.qiniu.cdn;

import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.kuocai.cdn.api.qiniu.cdn.vo.HitMissVo;
import com.kuocai.cdn.api.qiniu.cdn.vo.ReqCountVo;
import com.kuocai.cdn.api.qiniu.cdn.vo.StatusCodeVo;
import com.kuocai.cdn.api.qiniu.cdn.vo.TuneVo;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.util.Assert;
import com.qiniu.cdn.CdnResult;
import com.qiniu.common.Constants;
import com.qiniu.common.QiniuException;
import com.qiniu.util.Json;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.*;

/**
 * 七牛域名统计接口
 */
@Slf4j
public class QiNiuDomainStatisticsApi {

    public static Map<String, List<Long>> queryBw(TuneVo tuneVo, int length) throws BusinessException {
        String url = "/v2/tune/bandwidth";
        try {
            byte[] body = Json.encode(tuneVo).getBytes(Constants.UTF_8);
            JSONObject responseJsonObj = QiNiuRequest.postStatis(url, body, JSONObject.class);
            // 解析 JSON 字符串
            CdnResult.BandwidthResult bwData = parseJsonResponse(responseJsonObj.toJSONString(), CdnResult.BandwidthResult.class);

            Map<String, List<Long>> bwDetailsMap = calculateTotalBandwidthArray(bwData.data, length);
            return bwDetailsMap;
        } catch (QiniuException e) {
            
            throw new BusinessException(e.error());
        }
    }

    /**
     * 计算一段时间内的消耗总数
     * 支持统计到小时为单位
     *
     * @param tuneVo
     */
    public static Map<String, Long> queryBwSum(TuneVo tuneVo, int length) throws BusinessException {
        String url = "/v2/tune/bandwidth";
        try {
            byte[] body = Json.encode(tuneVo).getBytes(Constants.UTF_8);
            JSONObject responseJsonObj = QiNiuRequest.postStatis(url, body, JSONObject.class);
            // 解析 JSON 字符串
            CdnResult.BandwidthResult bandwidthData = parseJsonResponse(responseJsonObj.toJSONString(), CdnResult.BandwidthResult.class);
            // 调用计算总和的方法
            Map<String, Long> stringLongMap = calculateTotalBandwidth(bandwidthData.data, length);
            return stringLongMap;
        } catch (QiniuException e) {
            
            throw new BusinessException(e.error());
        }
    }

    public static Map<String, List<Long>> queryFlux(TuneVo tuneVo, int length) throws BusinessException {
        String url = "/v2/tune/flux";
        try {
            byte[] body = Json.encode(tuneVo).getBytes(Constants.UTF_8);
            JSONObject responseJsonObj = QiNiuRequest.postStatis(url, body, JSONObject.class);
            // 解析 JSON 字符串
            CdnResult.FluxResult fluxData = parseJsonResponse(responseJsonObj.toJSONString(), CdnResult.FluxResult.class);
            // 调用计算总和的方法
            Map<String, List<Long>> fluxDetailsMap = calculateTotalFluxArray(fluxData.data, length);
            return fluxDetailsMap;
        } catch (QiniuException e) {
            
            throw new BusinessException(e.error());
        }
    }

    /**
     * 计算一段时间内的消耗总数
     * 支持统计到小时为单位
     *
     * @param tuneVo
     */
    public static Map<String, Long> queryFluxSum(TuneVo tuneVo, int length) throws BusinessException {
        String url = "/v2/tune/flux";
        try {
            String startDate = tuneVo.getStartDate();
            String endDate = tuneVo.getEndDate();
            byte[] body = Json.encode(tuneVo).getBytes(Constants.UTF_8);
            JSONObject responseJsonObj = QiNiuRequest.postStatis(url, body, JSONObject.class);
            // 解析 JSON 字符串
            CdnResult.FluxResult fluxResult = parseJsonResponse(responseJsonObj.toJSONString(), CdnResult.FluxResult.class);
            // 调用计算总和的方法
            Map<String, Long> stringLongMap = calculateTotalFlux(fluxResult.data, length);
            return stringLongMap;
        } catch (QiniuException e) {
            
            throw new BusinessException(e.error());
        }
    }


    public static List<Double> queryRequest(ReqCountVo reqCountVo, int length) throws BusinessException {
        String url = "/v2/tune/loganalyze/reqcount";
        try {
            byte[] body = Json.encode(reqCountVo).getBytes(Constants.UTF_8);
            JSONObject responseJsonObj = QiNiuRequest.postStatis(url, body, JSONObject.class);
            // 解析 JSON 字符串
            List<Double> reqCount = parseJsonResponse(responseJsonObj.getJSONObject("data").getJSONArray("reqCount").toJSONString(), List.class);
            return reqCount.subList(0, length);
        } catch (QiniuException e) {
            
            throw new BusinessException(e.error());
        }
    }

    /**
     * 计算一段时间内的消耗总数
     * 支持统计到小时为单位
     *
     * @param reqCountVo
     */
    public static Double queryRequestSum(ReqCountVo reqCountVo) throws BusinessException {
        String url = "/v2/tune/loganalyze/reqcount";
        try {
            byte[] body = Json.encode(reqCountVo).getBytes(Constants.UTF_8);
            JSONObject responseJsonObj = QiNiuRequest.postStatis(url, body, JSONObject.class);
            // 解析 JSON 字符串
            List<Double> reqCount = parseJsonResponse(responseJsonObj.getJSONObject("data").getJSONArray("reqCount").toJSONString(), List.class);
            // 调用计算总和的方法
            double sum = reqCount.stream().mapToDouble(Double::doubleValue).sum();
            return sum;
        } catch (QiniuException e) {
            
            throw new BusinessException(e.error());
        }
    }

    /**
     * @param hitMissVo
     * @param type       请求命中次数 hit miss  流量命中次数trafficHit trafficMiss
     * @param length
     * @return
     * @throws BusinessException
     */
    public static List<Double> hitMiss(HitMissVo hitMissVo, String type, int length) throws BusinessException {
        String url = "/v2/tune/loganalyze/hitmiss";
        try {
            byte[] body = Json.encode(hitMissVo).getBytes(Constants.UTF_8);
            JSONObject responseJsonObj = QiNiuRequest.postStatis(url, body, JSONObject.class);
            if(Assert.isEmpty(responseJsonObj.getJSONObject("data"))){
                return new ArrayList<>(Collections.nCopies(length, 0D));
            }
            // 解析 JSON 字符串
            List<Double> results = parseJsonResponse(responseJsonObj.getJSONObject("data").getJSONArray(type).toJSONString(), List.class);
            return results.subList(0, length);
        } catch (QiniuException e) {
            
            throw new BusinessException(e.error());
        }
    }

    /**
     * 计算一段时间内的消耗总数
     * 支持统计到小时为单位
     *
     * @param hitMissVo
     */
    public static Double hitMissSum(HitMissVo hitMissVo, String type, int length) throws BusinessException {
        String url = "/v2/tune/loganalyze/hitmiss";
        try {
            byte[] body = Json.encode(hitMissVo).getBytes(Constants.UTF_8);
            JSONObject responseJsonObj = QiNiuRequest.postStatis(url, body, JSONObject.class);
            // 解析 JSON 字符串
            if(Assert.isEmpty(responseJsonObj.getJSONObject("data"))){
                return 0D;
            }
            List<Double> results = parseJsonResponse(responseJsonObj.getJSONObject("data").getJSONArray(type).toJSONString(), List.class);
            // 调用计算总和的方法
            List<Double> filterResults = results.subList(0, length);
            double sum = filterResults.stream().mapToDouble(Double::doubleValue).sum();
            return sum;
        } catch (QiniuException e) {
            
            throw new BusinessException(e.error());
        }
    }

    /**
     * @param statusCodeVo
     * @param type         请求命中次数 hit miss  流量命中次数trafficHit trafficMiss
     * @param length
     * @return
     * @throws BusinessException
     */
    public static List<Double> queryCode(StatusCodeVo statusCodeVo, String type, int length) throws BusinessException {
        String url = "/v2/tune/loganalyze/statuscode";
        try {
            byte[] body = Json.encode(statusCodeVo).getBytes(Constants.UTF_8);
            JSONObject responseJsonObj = QiNiuRequest.postStatis(url, body, JSONObject.class);
            if(Assert.isEmpty(responseJsonObj.getJSONObject("data"))){
                return new ArrayList<>(Collections.nCopies(length, 0D));
            }
            // 解析 JSON 字符串
            Map<String, List<Double>> responses = parseJsonResponse(responseJsonObj.getJSONObject("data").getJSONObject("codes").toJSONString(), Map.class);
            List<Double> results = new ArrayList<>(Collections.nCopies(length, 0D));
            for (Map.Entry<String, List<Double>> stringListEntry : responses.entrySet()) {
                if (stringListEntry.getKey().startsWith(type)) {
                    for (int i = 0; i < results.size(); i++) {
                        results.set(i, results.get(i) + stringListEntry.getValue().get(i));
                    }
                }
            }
            return results;
        } catch (QiniuException e) {
            
            throw new BusinessException(e.error());
        }
    }

    /**
     * 计算一段时间内的消耗总数
     * 支持统计到小时为单位
     *
     * @param statusCodeVo
     */
    public static Double queryCodeSum(StatusCodeVo statusCodeVo, String type, int length) throws BusinessException {
        String url = "/v2/tune/loganalyze/statuscode";
        try {
            byte[] body = Json.encode(statusCodeVo).getBytes(Constants.UTF_8);
            JSONObject responseJsonObj = QiNiuRequest.postStatis(url, body, JSONObject.class);
            if(Assert.isEmpty(responseJsonObj.getJSONObject("data"))){
                return 0D;
            }
            // 解析 JSON 字符串
            Map<String, List<Double>> responses = parseJsonResponse(responseJsonObj.getJSONObject("data").getJSONObject("codes").toJSONString(), Map.class);
            // 调用计算总和的方法
            double sum = 0;
            for (Map.Entry<String, List<Double>> stringListEntry : responses.entrySet()) {
                if (stringListEntry.getKey().startsWith(type)) {
                    List<Double> filterResults = stringListEntry.getValue().subList(0, length);
                    sum += filterResults.stream().mapToDouble(Double::doubleValue).sum();
                }
            }
            return sum;
        } catch (QiniuException e) {
            
            throw new BusinessException(e.error());
        }
    }

    private static Map<String, Long> calculateTotalBandwidth(Map<String, CdnResult.BandwidthData> dataMap, int length) {
        long totalChinaBandwidth = 0;
        long totalOverSeaBandwidth = 0;

        for (Map.Entry<String, CdnResult.BandwidthData> domainEntry : dataMap.entrySet()) {
            CdnResult.BandwidthData domainData = domainEntry.getValue();
            Long[] chinaValues = domainData.china;
            Long[] overseaValues = domainData.oversea;
            if (Assert.notEmpty(chinaValues)) {
                for (int i = 0; i < length; i++) {
                    totalChinaBandwidth += chinaValues[i];
                }
            }
            if (Assert.notEmpty(overseaValues)) {
                for (int i = 0; i < length; i++) {
                    totalOverSeaBandwidth += overseaValues[i];
                }
            }
        }
        Map<String, Long> info = new HashMap<>();
        info.put("china", totalChinaBandwidth);
        info.put("oversea", totalOverSeaBandwidth);
        info.put("total", totalChinaBandwidth + totalOverSeaBandwidth);
        return info;
    }

    private static Map<String, Long> calculateTotalFlux(Map<String, CdnResult.FluxData> dataMap, int length) {
        long totalChinaFlux = 0;
        long totalOverSeaFlux = 0;

        for (Map.Entry<String, CdnResult.FluxData> domainEntry : dataMap.entrySet()) {
            CdnResult.FluxData domainData = domainEntry.getValue();
            Long[] chinaValues = domainData.china;
            Long[] overseaValues = domainData.oversea;
            if (Assert.notEmpty(chinaValues)) {
                for (int i = 0; i < length; i++) {
                    totalChinaFlux += chinaValues[i];
                }
            }
            if (Assert.notEmpty(overseaValues)) {
                for (int i = 0; i < length; i++) {
                    totalOverSeaFlux += overseaValues[i];
                }
            }

        }
        Map<String, Long> info = new HashMap<>();
        info.put("china", totalChinaFlux);
        info.put("oversea", totalOverSeaFlux);
        info.put("total", totalChinaFlux + totalOverSeaFlux);
        return info;
    }

    private static Map<String, List<Long>> calculateTotalBandwidthArray(Map<String, CdnResult.BandwidthData> dataMap, int length) {
        List<Long> totalChinaBandwidth = new ArrayList<>(Collections.nCopies(length, 0L));
        List<Long> totalOverSeaBandwidth = new ArrayList<>(Collections.nCopies(length, 0L));
        List<Long> totalBandwidth = new ArrayList<>(Collections.nCopies(length, 0L));

        for (Map.Entry<String, CdnResult.BandwidthData> domainEntry : dataMap.entrySet()) {
            CdnResult.BandwidthData domainData = domainEntry.getValue();
            Long[] chinaValues = domainData.china;
            Long[] overseaValues = domainData.oversea;
            for (int i = 0; i < length; i++) {
                long chinaValue = chinaValues != null && i < chinaValues.length ? chinaValues[i] : 0L;
                long overseaValue = overseaValues != null && i < overseaValues.length ? overseaValues[i] : 0L;
                if (Assert.isEmpty(totalChinaBandwidth.get(i))) {
                    totalChinaBandwidth.set(i, 0L);
                }
                totalChinaBandwidth.set(i, totalChinaBandwidth.get(i) + chinaValue);
                if (Assert.isEmpty(totalOverSeaBandwidth.get(i))) {
                    totalOverSeaBandwidth.set(i, 0L);
                }
                totalOverSeaBandwidth.set(i, totalOverSeaBandwidth.get(i) + overseaValue);
                totalBandwidth.set(i, totalChinaBandwidth.get(i) + totalOverSeaBandwidth.get(i));
            }
        }
        Map<String, List<Long>> info = new HashMap<>();
        info.put("china", totalChinaBandwidth);
        info.put("oversea", totalOverSeaBandwidth);
        info.put("total", totalBandwidth);
        return info;
    }

    private static Map<String, List<Long>> calculateTotalFluxArray(Map<String, CdnResult.FluxData> dataMap, int length) {
        List<Long> totalChinaFlux = new ArrayList<>(Collections.nCopies(length, 0L));
        List<Long> totalOverSeaFlux = new ArrayList<>(Collections.nCopies(length, 0L));
        List<Long> totalFlux = new ArrayList<>(Collections.nCopies(length, 0L));

        for (Map.Entry<String, CdnResult.FluxData> domainEntry : dataMap.entrySet()) {
            CdnResult.FluxData domainData = domainEntry.getValue();
            Long[] chinaValues = domainData.china;
            Long[] overseaValues = domainData.oversea;
            for (int i = 0; i < length; i++) {
                long chinaValue = chinaValues != null && i < chinaValues.length ? chinaValues[i] : 0L;
                long overseaValue = overseaValues != null && i < overseaValues.length ? overseaValues[i] : 0L;
                if (Assert.isEmpty(totalChinaFlux.get(i))) {
                    totalChinaFlux.set(i, 0L);
                }
                totalChinaFlux.set(i, totalChinaFlux.get(i) + chinaValue);
                if (Assert.isEmpty(totalOverSeaFlux.get(i))) {
                    totalOverSeaFlux.set(i, 0L);
                }
                totalOverSeaFlux.set(i, totalOverSeaFlux.get(i) + overseaValue);
                totalFlux.set(i, totalChinaFlux.get(i) + totalOverSeaFlux.get(i));
            }
        }
        Map<String, List<Long>> info = new HashMap<>();
        info.put("china", totalChinaFlux);
        info.put("oversea", totalOverSeaFlux);
        info.put("total", totalFlux);
        return info;
    }

    private static <T> T parseJsonResponse(String jsonResponse, Type type) {
        Gson gson = new Gson();
        return gson.fromJson(jsonResponse, type);
    }

}
