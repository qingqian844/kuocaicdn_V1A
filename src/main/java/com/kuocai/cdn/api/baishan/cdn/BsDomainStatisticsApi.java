package com.kuocai.cdn.api.baishan.cdn;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.baishan.cdn.properties.BsCdn;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.util.Assert;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 白山云域名统计API
 */
@Slf4j
public class BsDomainStatisticsApi {

    public static final String format = "yyyy-MM-dd HH:mm";

    public static final Integer hourNumber = 3600;
    public static final Integer dayNumber = 86400;

    public static void main(String[] args) throws BusinessException {
    }


    /**
     * 获取域名带宽流量,按照5min粒度求和
     *
     * @param domainName
     * @param start
     * @param end
     * @param dataType
     * @return
     * @throws BusinessException
     */
    public static double bandwidthSummary(String domainName, DateTime start, DateTime end, String dataType) throws BusinessException {
        JSONObject minute5 = bandwidth(domainName, start, end, dataType, "minute5");
        double v = calculateDomainDataSum(minute5);
        return v;

    }


    /**
     * 获取域名回源带宽流量,按照5min粒度求和
     *
     * @param domainName
     * @param start
     * @param end
     * @param dataType
     * @return
     * @throws BusinessException
     */
    public static double originBandwidthSummary(String domainName, DateTime start, DateTime end, String dataType) throws BusinessException {
        JSONObject minute5 = originBandwidth(domainName, start, end, dataType, "minute5");
        double v = calculateDomainDataSum(minute5);
        return v;

    }

    public static double calculateDomainDataSum(JSONObject dataObj) {
        Map<String, Double> domainSumMap = new HashMap<>();
        // 遍历域名数据
        double sum = 0;
        for (Map.Entry<String, Object> entry : dataObj.entrySet()) {
            String domain = entry.getKey();
            JSONObject value = (JSONObject) entry.getValue();
            JSONArray dataArray = value.getJSONArray("data");
            // 计算数据数组中第二个元素的汇总

            for (int i = 0; i < dataArray.size(); i++) {
                JSONArray element = dataArray.getJSONArray(i);
                if (element.size() >= 2) {
                    sum += element.getDoubleValue(1);
                }
            }
        }

        return sum;
    }


    /**
     * 获取域名带宽流量,按照5min粒度求和
     *
     * @param domainName
     * @param start
     * @param end
     * @param dataType
     * @param grad
     * @return
     * @throws BusinessException
     */
    public static List<Double> bandwidthDetails(String domainName, DateTime start, DateTime end, String dataType, String grad) throws BusinessException {
        JSONObject dataObj = bandwidth(domainName, start, end, dataType, grad);
        TreeMap<Long, Double> treeMap = new TreeMap<>();
        int interval = hourNumber;
        if (ObjectUtil.equal(grad, "day")) {
            interval = dayNumber;
        }
        for (long i = start.getTime() / 1000; i <= end.getTime() / 1000; i += interval) {
            treeMap.put(i, 0D);
        }
        for (Map.Entry<String, Object> entry : dataObj.entrySet()) {
            JSONObject value = (JSONObject) entry.getValue();
            JSONArray dataArray = value.getJSONArray("data");
            // 计算数据数组中第二个元素的汇总
            for (int i = 0; i < dataArray.size(); i++) {
                JSONArray element = dataArray.getJSONArray(i);
                if (element.size() >= 2) {
                    long key = element.getLongValue(0);
                    treeMap.put(key, treeMap.get(key) + element.getDoubleValue(1));
                }
            }
        }
        List<Double> result = treeMap.values().stream().collect(Collectors.toList());
        return result;
    }


    /**
     * 获取域名回源带宽流量,按照5min粒度求和
     *
     * @param domainName
     * @param start
     * @param end
     * @param dataType
     * @param grad
     * @return
     * @throws BusinessException
     */
    public static List<Double> originBandwidthDetails(String domainName, DateTime start, DateTime end, String dataType, String grad) throws BusinessException {
        JSONObject dataObj = originBandwidth(domainName, start, end, dataType, grad);
        TreeMap<Long, Double> treeMap = new TreeMap<>();
        int interval = hourNumber;
        if (ObjectUtil.equal(grad, "day")) {
            interval = dayNumber;
        }
        for (long i = start.getTime() / 1000; i <= end.getTime() / 1000; i += interval) {
            treeMap.put(i, 0D);
        }
        for (Map.Entry<String, Object> entry : dataObj.entrySet()) {
            JSONObject value = (JSONObject) entry.getValue();
            JSONArray dataArray = value.getJSONArray("data");
            // 计算数据数组中第二个元素的汇总
            for (int i = 0; i < dataArray.size(); i++) {
                JSONArray element = dataArray.getJSONArray(i);
                if (element.size() >= 2) {
                    long key = element.getLongValue(0);
                    treeMap.put(key, treeMap.get(key) + element.getDoubleValue(1));
                }
            }
        }
        List<Double> result = treeMap.values().stream().collect(Collectors.toList());
        return result;

    }


    /**
     * 获取域名带宽流量,按照5min粒度求和
     *
     * @param domainName
     * @param start
     * @param end
     * @return
     * @throws BusinessException
     */
    public static double requestSummary(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject minute5 = request(domainName, start, end, "minute5");
        double v = calculateDomainDataSum(minute5);
        return v;

    }


    /**
     * 获取域名回源带宽流量,按照5min粒度求和
     *
     * @param domainName
     * @param start
     * @param end
     * @return
     * @throws BusinessException
     */
    public static double originRequestSummary(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject minute5 = originRequest(domainName, start, end, "minute5");
        double v = calculateDomainDataSum(minute5);
        return v;

    }


    /**
     * 获取域名带宽流量,按照5min粒度求和
     *
     * @param domainName
     * @param start
     * @param end
     * @param grad
     * @return
     * @throws BusinessException
     */
    public static List<Double> requestDetails(String domainName, DateTime start, DateTime end, String grad) throws BusinessException {
        JSONObject dataObj = request(domainName, start, end, grad);
        TreeMap<Long, Double> treeMap = new TreeMap<>();
        int interval = hourNumber;
        if (ObjectUtil.equal(grad, "day")) {
            interval = dayNumber;
        }
        for (long i = start.getTime() / 1000; i <= end.getTime() / 1000; i += interval) {
            treeMap.put(i, 0D);
        }
        for (Map.Entry<String, Object> entry : dataObj.entrySet()) {
            JSONObject value = (JSONObject) entry.getValue();
            JSONArray dataArray = value.getJSONArray("data");
            // 计算数据数组中第二个元素的汇总
            for (int i = 0; i < dataArray.size(); i++) {
                JSONArray element = dataArray.getJSONArray(i);
                if (element.size() >= 2) {
                    long key = element.getLongValue(0);
                    treeMap.put(key, treeMap.get(key) + element.getDoubleValue(1));
                }
            }
        }
        List<Double> result = treeMap.values().stream().collect(Collectors.toList());
        return result;
    }


    /**
     * 获取域名回源带宽流量,按照5min粒度求和
     *
     * @param domainName
     * @param start
     * @param end
     * @param grad
     * @return
     * @throws BusinessException
     */
    public static List<Double> originRequestDetails(String domainName, DateTime start, DateTime end, String grad) throws BusinessException {
        JSONObject dataObj = originRequest(domainName, start, end, grad);
        TreeMap<Long, Double> treeMap = new TreeMap<>();
        int interval = hourNumber;
        if (ObjectUtil.equal(grad, "day")) {
            interval = dayNumber;
        }
        for (long i = start.getTime() / 1000; i <= end.getTime() / 1000; i += interval) {
            treeMap.put(i, 0D);
        }
        for (Map.Entry<String, Object> entry : dataObj.entrySet()) {
            JSONObject value = (JSONObject) entry.getValue();
            JSONArray dataArray = value.getJSONArray("data");
            // 计算数据数组中第二个元素的汇总
            for (int i = 0; i < dataArray.size(); i++) {
                JSONArray element = dataArray.getJSONArray(i);
                if (element.size() >= 2) {
                    long key = element.getLongValue(0);
                    treeMap.put(key, treeMap.get(key) + element.getDoubleValue(1));
                }
            }
        }
        List<Double> result = treeMap.values().stream().collect(Collectors.toList());
        return result;

    }

    /**
     * 获取域名状态码,按照5min粒度求和
     *
     * @param domainName
     * @param start
     * @param end
     * @return
     * @throws BusinessException
     */
    public static Map<String, Double> httpCodeSummary(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject minute5 = httpCode(domainName, start, end, "minute5");
        Map<String, Double> map = calculateDomainHttpCodeDataSum(minute5);
        return map;

    }

    /**
     * 获取域名状态码,按照5min粒度求和
     *
     * @param domainName
     * @param start
     * @param end
     * @return
     * @throws BusinessException
     */
    public static Map<String, List<Double>> httpCodeDetails(String domainName, DateTime start, DateTime end, String grad) throws BusinessException {
        Map<String, List<Double>> map = new HashMap<>();
        JSONObject dataObj = httpCode(domainName, start, end, grad);
        TreeMap<Long, Double> treeMap2 = new TreeMap<>();
        TreeMap<Long, Double> treeMap3 = new TreeMap<>();
        TreeMap<Long, Double> treeMap4 = new TreeMap<>();
        TreeMap<Long, Double> treeMap5 = new TreeMap<>();
        int interval = hourNumber;
        if (ObjectUtil.equal(grad, "day")) {
            interval = dayNumber;
        }
        for (long i = start.getTime() / 1000; i <= end.getTime() / 1000; i += interval) {
            treeMap2.put(i, 0D);
            treeMap3.put(i, 0D);
            treeMap4.put(i, 0D);
            treeMap5.put(i, 0D);
        }
        for (Map.Entry<String, Object> entry : dataObj.entrySet()) {
            JSONObject value = (JSONObject) entry.getValue();
            JSONArray dataArray = value.getJSONArray("data");
            // 计算数据数组中第二个元素的汇总
            for (int i = 0; i < dataArray.size(); i++) {
                JSONArray element = dataArray.getJSONArray(i);
                if (element.size() >= 2) {
                    JSONObject httpCodeJsonObj = element.getJSONObject(1);
                    for (String s : httpCodeJsonObj.keySet()) {
                        long key = element.getLongValue(0);
                        if(s.startsWith("2")){
                            treeMap2.put(key, treeMap2.get(key) + httpCodeJsonObj.getDoubleValue(s));
                        }else if(s.startsWith("3")){
                            treeMap3.put(key, treeMap3.get(key) + httpCodeJsonObj.getDoubleValue(s));
                        }else if(s.startsWith("4")){
                            treeMap4.put(key, treeMap4.get(key) + httpCodeJsonObj.getDoubleValue(s));
                        }else if(s.startsWith("5")){
                            treeMap5.put(key, treeMap5.get(key) + httpCodeJsonObj.getDoubleValue(s));
                        }
                    }
                }
            }
        }
        map.put("2",treeMap2.values().stream().collect(Collectors.toList()));
        map.put("3",treeMap3.values().stream().collect(Collectors.toList()));
        map.put("4",treeMap4.values().stream().collect(Collectors.toList()));
        map.put("5",treeMap5.values().stream().collect(Collectors.toList()));
        return map;
    }

    public static Map<String, Double> calculateDomainHttpCodeDataSum(JSONObject dataObj) {
        Map<String, Double> domainSumMap = new HashMap<>();
        domainSumMap.put("2", 0D);
        domainSumMap.put("3", 0D);
        domainSumMap.put("4", 0D);
        domainSumMap.put("5", 0D);
        // 遍历域名数据
        for (Map.Entry<String, Object> entry : dataObj.entrySet()) {
            String domain = entry.getKey();
            JSONObject value = (JSONObject) entry.getValue();
            JSONArray dataArray = value.getJSONArray("data");
            // 计算数据数组中第二个元素的汇总
            for (int i = 0; i < dataArray.size(); i++) {
                JSONArray element = dataArray.getJSONArray(i);
                if (element.size() >= 2) {
                    JSONObject httpCodeJsonObj = element.getJSONObject(1);
                    for (String s : httpCodeJsonObj.keySet()) {
                        if(s.startsWith("2")){
                            domainSumMap.put("2", domainSumMap.get("2") + httpCodeJsonObj.getDoubleValue(s));
                        }else if(s.startsWith("3")){
                            domainSumMap.put("3", domainSumMap.get("3") + httpCodeJsonObj.getDoubleValue(s));
                        }else if(s.startsWith("4")){
                            domainSumMap.put("4", domainSumMap.get("4") + httpCodeJsonObj.getDoubleValue(s));
                        }else if(s.startsWith("5")){
                            domainSumMap.put("5", domainSumMap.get("5") + httpCodeJsonObj.getDoubleValue(s));
                        }
                    }
                }
            }
        }

        return domainSumMap;
    }

    /**
     * https://cdnx.console.baishan.com/#/cdn/help/API%E6%96%87%E6%A1%A3/%E7%BB%9F%E8%AE%A1%E5%88%86%E6%9E%90/%E5%88%86%E5%9F%9F%E5%90%8D%E5%B8%A6%E5%AE%BD_%E6%B5%81%E9%87%8F%E6%9F%A5%E8%AF%A2
     *
     * @param domains   指定查询的加速域名，最多可一次性查询10个加速域名。当查询多个加速域名时，返回每个加速域名的带宽、流量数据。
     * @param startTime 指定查询起始时间，如：2020-06-01 10:00、2020-06-01，其中2020-06-01代表以 2020-06-01 00:00:00作为起始时间。
     *                  返回结果大于等于指定时间，根据指定时间粒度不同，会进行向前规整，如：2020-06-01 10:03在按5分钟的时间粒度查询时，返回的第一个数据对应时间点为2020-06-01 10:00。
     *                  起始时间与结束时间间隔小于等于31天。
     * @param endTime   指定查询结束时间，如：2020-06-02 10:00、2020-06-02，其中2020-06-02代表以 2020-06-02 23:59:59作为结束时间。
     *                  返回结果小于等于指定时间，根据指定时间粒度不同，会进行向前规整，如：2020-06-02 10:17在按5分钟的时间粒度查询时，返回的第一个数据对应时间点为2020-06-02 10:15:00。
     *                  起始时间与结束时间间隔小于等于31天。
     * @param dataType  指定数据指标查询：
     *                  traffic：流量，单位byte
     *                  bandwidth：带宽，单位bps
     *                  不填充时，默认值为bandwidth。
     * @param grad      指定数据统计的时间粒度查询：
     *                  minute5：5分钟粒度
     *                  hour：小时粒度
     *                  day：天粒度
     *                  不填充时，默认为minute5。
     * @return {"code":0,"data":{"bs.kedaya.site":{"domain":"bs.kedaya.site","data":[[1690128000,6.54],[1690131600,1086.74],[1690146000,7.08],[1690149600,319.76],[1690171200,6.62],[1690182000,7.09],[1690200000,7.09],[1690214400,651.69]]}}}
     * @throws BusinessException
     */
    public static JSONObject bandwidth(String domains, DateTime startTime, DateTime endTime, String dataType, String grad) throws BusinessException {
        String url = BsCdn.API + "/v2/stat/bandwidth/eachDomain";
        Map<String, String> params = new HashMap<>();
        try {
            params.put("token", BsCdn.TOKEN);
            params.put("domains", domains);
            params.put("start_time", DateUtil.format(startTime, format));
            params.put("end_time", DateUtil.format(endTime, format));
            params.put("data_type", dataType);
            params.put("grad", grad);
            String s = BsRequest.sendGetRequest(url, params);
            return JSONObject.parseObject(s).getJSONObject("data");
        } catch (Exception e) {
            log.error("白山云获取域名带宽_流量统计错误:[params:[{}];error:{}]", params, e.getMessage());
            throw new BusinessException("bs获取域名带宽_流量统计错误");
        }
    }


    /**
     * https://cdnx.console.baishan.com/#/cdn/help/API%E6%96%87%E6%A1%A3/%E7%BB%9F%E8%AE%A1%E5%88%86%E6%9E%90/%E5%88%86%E5%9F%9F%E5%90%8D%E5%9B%9E%E6%BA%90%E5%B8%A6%E5%AE%BD_%E6%B5%81%E9%87%8F
     *
     * @param domains   指定查询的加速域名，最多可一次性查询10个加速域名。当查询多个加速域名时，返回每个加速域名的带宽、流量数据。
     * @param startTime 指定查询起始时间，如：2020-06-01 10:00、2020-06-01，其中2020-06-01代表以 2020-06-01 00:00:00作为起始时间。
     *                  返回结果大于等于指定时间，根据指定时间粒度不同，会进行向前规整，如：2020-06-01 10:03在按5分钟的时间粒度查询时，返回的第一个数据对应时间点为2020-06-01 10:00。
     *                  起始时间与结束时间间隔小于等于31天。
     * @param endTime   指定查询结束时间，如：2020-06-02 10:00、2020-06-02，其中2020-06-02代表以 2020-06-02 23:59:59作为结束时间。
     *                  返回结果小于等于指定时间，根据指定时间粒度不同，会进行向前规整，如：2020-06-02 10:17在按5分钟的时间粒度查询时，返回的第一个数据对应时间点为2020-06-02 10:15:00。
     *                  起始时间与结束时间间隔小于等于31天。
     * @param dataType  指定数据指标查询：
     *                  traffic：流量，单位byte
     *                  bandwidth：带宽，单位bps
     *                  不填充时，默认值为bandwidth。
     * @param grad      指定数据统计的时间粒度查询：
     *                  minute5：5分钟粒度
     *                  hour：小时粒度
     *                  day：天粒度
     *                  不填充时，默认为minute5。
     * @return {"code":0,"data":{"bs.kedaya.site":{"domain":"bs.kedaya.site","data":[[1690149600,27.53],[1690171200,0.4],[1690182000,0.4],[1690214400,25.52]]}}}
     * @throws BusinessException
     */
    public static JSONObject originBandwidth(String domains, DateTime startTime, DateTime endTime, String dataType, String grad) throws BusinessException {
        String url = BsCdn.API + "/v2/stat/originBandwidth/eachDomain";
        Map<String, String> params = new HashMap<>();
        try {
            params.put("token", BsCdn.TOKEN);
            params.put("domains", domains);
            params.put("start_time", DateUtil.format(startTime, format));
            params.put("end_time", DateUtil.format(endTime, format));
            params.put("data_type", dataType);
            params.put("grad", grad);
            String s = BsRequest.sendGetRequest(url, params);
            return JSONObject.parseObject(s).getJSONObject("data");
        } catch (Exception e) {
            log.error("白山云获取域名带宽_流量统计错误:[params:[{}];error:{}]", params, e.getMessage());
            throw new BusinessException("bs获取域名带宽_流量统计错误");
        }
    }

    /**
     * https://cdnx.console.baishan.com/#/cdn/help/API%E6%96%87%E6%A1%A3/%E7%BB%9F%E8%AE%A1%E5%88%86%E6%9E%90/%E5%88%86%E5%9F%9F%E5%90%8D%E8%AF%B7%E6%B1%82%E6%95%B0%E6%9F%A5%E8%AF%A2
     *
     * @param domains   指定查询的加速域名，最多可一次性查询10个加速域名。当查询多个加速域名时，返回每个加速域名的带宽、流量数据。
     * @param startTime 指定查询起始时间，如：2020-06-01 10:00、2020-06-01，其中2020-06-01代表以 2020-06-01 00:00:00作为起始时间。
     *                  返回结果大于等于指定时间，根据指定时间粒度不同，会进行向前规整，如：2020-06-01 10:03在按5分钟的时间粒度查询时，返回的第一个数据对应时间点为2020-06-01 10:00。
     *                  起始时间与结束时间间隔小于等于31天。
     * @param endTime   指定查询结束时间，如：2020-06-02 10:00、2020-06-02，其中2020-06-02代表以 2020-06-02 23:59:59作为结束时间。
     *                  返回结果小于等于指定时间，根据指定时间粒度不同，会进行向前规整，如：2020-06-02 10:17在按5分钟的时间粒度查询时，返回的第一个数据对应时间点为2020-06-02 10:15:00。
     *                  起始时间与结束时间间隔小于等于31天。
     *                  不填充时，默认值为bandwidth。
     * @param grad      指定数据统计的时间粒度查询：
     *                  minute5：5分钟粒度
     *                  hour：小时粒度
     *                  day：天粒度
     *                  不填充时，默认为minute5。
     * @return {"code":0,"data":{"bs.kedaya.site":{"domain":"bs.kedaya.site","data":[[1690149600,27.53],[1690171200,0.4],[1690182000,0.4],[1690214400,25.52]]}}}
     * @throws BusinessException
     */
    public static JSONObject request(String domains, DateTime startTime, DateTime endTime, String grad) throws BusinessException {
        String url = BsCdn.API + "/v2/stat/request/eachDomain";
        Map<String, String> params = new HashMap<>();
        try {
            params.put("token", BsCdn.TOKEN);
            params.put("domains", domains);
            params.put("start_time", DateUtil.format(startTime, format));
            params.put("end_time", DateUtil.format(endTime, format));
            params.put("grad", grad);
            String s = BsRequest.sendGetRequest(url, params);
            return JSONObject.parseObject(s).getJSONObject("data");
        } catch (Exception e) {
            log.error("白山云获取分域名请求数统计错误:[params:[{}];error:{}]", params, e.getMessage());
            throw new BusinessException("bs获取分域名请求数统计错误");
        }
    }

    /**
     * https://cdnx.console.baishan.com/#/cdn/help/API%E6%96%87%E6%A1%A3/%E7%BB%9F%E8%AE%A1%E5%88%86%E6%9E%90/%E5%88%86%E5%9F%9F%E5%90%8D%E5%9B%9E%E6%BA%90%E8%AF%B7%E6%B1%82%E6%95%B0
     *
     * @param domains   指定查询的加速域名，最多可一次性查询10个加速域名。当查询多个加速域名时，返回每个加速域名的带宽、流量数据。
     * @param startTime 指定查询起始时间，如：2020-06-01 10:00、2020-06-01，其中2020-06-01代表以 2020-06-01 00:00:00作为起始时间。
     *                  返回结果大于等于指定时间，根据指定时间粒度不同，会进行向前规整，如：2020-06-01 10:03在按5分钟的时间粒度查询时，返回的第一个数据对应时间点为2020-06-01 10:00。
     *                  起始时间与结束时间间隔小于等于31天。
     * @param endTime   指定查询结束时间，如：2020-06-02 10:00、2020-06-02，其中2020-06-02代表以 2020-06-02 23:59:59作为结束时间。
     *                  返回结果小于等于指定时间，根据指定时间粒度不同，会进行向前规整，如：2020-06-02 10:17在按5分钟的时间粒度查询时，返回的第一个数据对应时间点为2020-06-02 10:15:00。
     *                  起始时间与结束时间间隔小于等于31天。
     *                  不填充时，默认值为bandwidth。
     * @param grad      指定数据统计的时间粒度查询：
     *                  minute5：5分钟粒度
     *                  hour：小时粒度
     *                  day：天粒度
     *                  不填充时，默认为minute5。
     * @return {"code":0,"data":{"bs.kedaya.site":{"domain":"bs.kedaya.site","data":[[1690149600,27.53],[1690171200,0.4],[1690182000,0.4],[1690214400,25.52]]}}}
     * @throws BusinessException
     */
    public static JSONObject originRequest(String domains, DateTime startTime, DateTime endTime, String grad) throws BusinessException {
        String url = BsCdn.API + "/v2/stat/originRequest/eachDomain";
        Map<String, String> params = new HashMap<>();
        try {
            params.put("token", BsCdn.TOKEN);
            params.put("domains", domains);
            params.put("start_time", DateUtil.format(startTime, format));
            params.put("end_time", DateUtil.format(endTime, format));
            params.put("grad", grad);
            String s = BsRequest.sendGetRequest(url, params);
            return JSONObject.parseObject(s).getJSONObject("data");
        } catch (Exception e) {
            log.error("白山云获取分域名回源请求数统计错误:[params:[{}];error:{}]", params, e.getMessage());
            throw new BusinessException("bs获取分域名回源请求数统计错误");
        }
    }



    /**
     * https://cdnx.console.baishan.com/#/cdn/help/API%E6%96%87%E6%A1%A3/%E7%BB%9F%E8%AE%A1%E5%88%86%E6%9E%90/%E5%88%86%E5%9F%9F%E5%90%8D%E7%8A%B6%E6%80%81%E7%A0%81%E6%9F%A5%E8%AF%A2
     *
     * @param domains   指定查询的加速域名，最多可一次性查询10个加速域名。当查询多个加速域名时，返回每个加速域名的带宽、流量数据。
     * @param startTime 指定查询起始时间，如：2020-06-01 10:00、2020-06-01，其中2020-06-01代表以 2020-06-01 00:00:00作为起始时间。
     *                  返回结果大于等于指定时间，根据指定时间粒度不同，会进行向前规整，如：2020-06-01 10:03在按5分钟的时间粒度查询时，返回的第一个数据对应时间点为2020-06-01 10:00。
     *                  起始时间与结束时间间隔小于等于31天。
     * @param endTime   指定查询结束时间，如：2020-06-02 10:00、2020-06-02，其中2020-06-02代表以 2020-06-02 23:59:59作为结束时间。
     *                  返回结果小于等于指定时间，根据指定时间粒度不同，会进行向前规整，如：2020-06-02 10:17在按5分钟的时间粒度查询时，返回的第一个数据对应时间点为2020-06-02 10:15:00。
     *                  起始时间与结束时间间隔小于等于31天。
     *                  不填充时，默认值为bandwidth。
     * @param grad      指定数据统计的时间粒度查询：
     *                  minute5：5分钟粒度
     *                  hour：小时粒度
     *                  day：天粒度
     *                  不填充时，默认为minute5。
     * @return {"code":0,"data":{"bs.kedaya.site":{"domain":"bs.kedaya.site","data":[[1690149600,27.53],[1690171200,0.4],[1690182000,0.4],[1690214400,25.52]]}}}
     * @throws BusinessException
     */
    public static JSONObject httpCode(String domains, DateTime startTime, DateTime endTime, String grad) throws BusinessException {
        String url = BsCdn.API + "/v2/stat/httpcode/eachDomain";
        Map<String, String> params = new HashMap<>();
        try {
            params.put("token", BsCdn.TOKEN);
            params.put("domains", domains);
            params.put("start_time", DateUtil.format(startTime, format));
            params.put("end_time", DateUtil.format(endTime, format));
            params.put("grad", grad);
            String s = BsRequest.sendGetRequest(url, params);
            if(JSONObject.parseObject(s).get("data") instanceof JSONArray){
                return new JSONObject();
            }
            return JSONObject.parseObject(s).getJSONObject("data");
        } catch (Exception e) {
            log.error("白山云获取分域名状态码统计错误:[params:[{}];error:{}]", params, e.getMessage());
            throw new BusinessException("bs获取分域名状态码统计错误");
        }
    }

}
