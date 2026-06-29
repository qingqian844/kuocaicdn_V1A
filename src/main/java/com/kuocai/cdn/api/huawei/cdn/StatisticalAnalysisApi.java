package com.kuocai.cdn.api.huawei.cdn;


import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.cloud.apigateway.sdk.utils.Request;
import com.kuocai.cdn.api.huawei.cdn.dto.GetTopOneHundredQueryDTO;
import com.kuocai.cdn.api.huawei.cdn.properties.HuaWeiCdn;
import com.kuocai.cdn.exception.CdnHuaweiException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

/**
 * <h1>统计分析</h1>
 *
 * @author xiaobo
 */
@Slf4j
public class StatisticalAnalysisApi {

    private StatisticalAnalysisApi() {
    }


    public static JSONObject methodGetBase(String url, GetTopOneHundredQueryDTO getTopOneHundredQuery, String error) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(url, HuaWeiCdn.METHOD_GET_STRING);
            HuaweiRequest.addQueryStringParamDTO(request, getTopOneHundredQuery);
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }

    }


    /**
     * description: <h2>查询TOP100 URL明细</h2>
     * 查询TOP100 URL明细。<br/>
     * 支持查询90天内的数据。<br/>
     * 查询跨度不能超过31天。<br/>
     * 起始时间和结束时间，左闭右开，需要同时指定。如查询2021-10-24 00:00:00 到 2021-10-25 00:00:00 的数据，表示取 [2021-10-24 00:00:00, 2021-10-25 00:00:00)的统计数据。<br/>
     * 开始时间、结束时间必须传毫秒级时间戳，且必须为凌晨0点整时刻点，如果传的不是凌晨0点整时刻点，返回数据可能与预期不一致。<br/>
     * 流量类指标单位统一为Byte（字节）、请求数类指标单位统一为次数。用于查询指定域名、指定统计指标的明细数据。<br/>
     * 单租户调用频率：5次/s。<br/>
     *
     * @param getTopOneHundredQuery 参数必传 start_time,end_time,domain_name,stat_type 非必传 enterprise_project_id,service_area
     * @return com.alibaba.fastjson.JSONObject
     * @author bo
     * @date 2023/2/26 2:10 PM
     * @link <a>https://support.huaweicloud.com/api-cdn/ShowTopUrl.html</a>
     */
    public static JSONObject getTopOneHundred(GetTopOneHundredQueryDTO getTopOneHundredQuery) throws CdnHuaweiException {
        String error = "查询前100数据失败！错误原因：{}";
        return methodGetBase(HuaWeiCdn.GET_TOP_ONE_HUNDRED, getTopOneHundredQuery, error);

    }

    /**
     * description: <h2>查询域名统计数据-(区域运营商)</h2>
     * 支持查询90天内的数据。<br/>
     * 支持多指标同时查询，不超过5个。<br/>
     * 最多同时指定20个域名。<br/>
     * 起始时间和结束时间需要同时指定，左闭右开，毫秒级时间戳，且时间点必须为与查询时间间隔参数匹配的整时刻点。比如查询时间间隔为5分钟时，起始时间和结束时间必须为5分钟整时刻点，如：0分、5分、10分、15分等，如果时间点与时间间隔不匹配，返回数据可能与预期不一致。统一用开始时间表示一个时间段，如：2019-01-24 20:15:00 表示取 [20:15:00, 20:20:00)的统计数据，且左闭右开。<br/>
     * action取值：如果是区域则取location_detail,location_summary，否则取detail,summary<br/>
     * 流量类指标单位统一为Byte（字节）、带宽类指标单位统一为bit/s（比特/秒）、请求数类和状态码类指标单位统一为次数。用于查询指定域名、指定统计指标的区域运营商明细数据。<br/>
     * 单租户调用频率：15次/s。
     *
     * @param getTopOneHundredQuery 必传参数action,start_time,end_time,domain_name,stat_type 非必传参数 enterprise_project_id,isp,province,country,group_by,interval
     * @return com.alibaba.fastjson.JSONObject
     * @throws CdnHuaweiException 自定义异常
     * @author bo
     * @date 2023/2/26 3:46 PM
     * @link 区域：<a>https://support.huaweicloud.com/api-cdn/ShowDomainLocationStats.html</a>
     * @link 非区域：<a>https://support.huaweicloud.com/api-cdn/ShowDomainLocationStats.html</a>
     */
    public static JSONObject getDomainStatistics(GetTopOneHundredQueryDTO getTopOneHundredQuery, boolean isArea) throws CdnHuaweiException {
        String error = "查询" + (isArea ? "区域运营商域名统计数据" : "域名统计数据") + "失败！错误原因：{}";
        return methodGetBase(isArea ? HuaWeiCdn.GET_DOMAIN_STATISTICS_AREA : HuaWeiCdn.GET_DOMAIN_STATISTICS, getTopOneHundredQuery, error);
    }

    /**
     * description: <h2>查询网络总流量</h2>
     * 支持查询90天内的数据。<br/>
     * 时间跨度最小支持1小时，最大不超过31天。<br/>
     * 最多同时指定100个域名。<br/>
     * 起始时间和结束时间需同时指定，为毫秒级时间戳，且必须为1小时或1天整时刻点。时间跨度<7天，需为1小时整时刻点，如2020-07-01 08:00:00、2020-07-01 09:00:00；7天<时间跨度<31天，需为1天整时刻点，如2020-07-01 00:00:00、2020-07-02 00:00:00。<br/>
     * 如果起始时间、结束时间不满足对应时间跨度的整时刻点，返回数据可能与预期不一致。
     *
     * @param getTopOneHundredQuery 必传参数domain_name 非必传参数 start_time,end_time,enterprise_project_id
     * @return com.alibaba.fastjson.JSONObject
     * @throws CdnHuaweiException 自定义异常
     * @author bo
     * @date 2023/2/26 10:26 PM
     * @link <a>https://support.huaweicloud.com/api-cdn/cdn_02_0057.html</a>
     */
    public static JSONObject queryNetworkTraffic(GetTopOneHundredQueryDTO getTopOneHundredQuery) throws CdnHuaweiException {
        String error = "查询网络总流量失败！错误原因：{}";
        return methodGetBase(HuaWeiCdn.QUERY_NETWORK_TRAFFIC, getTopOneHundredQuery, error);

    }

    /**
     * description: <h2>查询网络流量明细</h2>
     * 此功能描述和上面几乎一样,下同
     *
     * @param getTopOneHundredQuery 必传参数domain_name 非必传参数 start_time,end_time,enterprise_project_id,interval
     * @return com.alibaba.fastjson.JSONObject
     * @throws CdnHuaweiException 自定义异常
     * @author bo
     * @date 2023/2/26 10:40 PM
     * @link <a>https://support.huaweicloud.com/api-cdn/cdn_02_0058.html</a>
     */
    public static JSONObject queryNetworkTrafficDetails(GetTopOneHundredQueryDTO getTopOneHundredQuery) throws CdnHuaweiException {
        String error = "查询网络流量明细失败！错误原因：{}";
        return methodGetBase(HuaWeiCdn.QUERY_NETWORK_TRAFFIC_DETAILS, getTopOneHundredQuery, error);
    }

    /**
     * description: <h2>查询网络带宽峰值</h2>
     *
     * @param getTopOneHundredQuery 必传值 domain_name 非必传参数 start_time,end_time,enterprise_project_id
     * @return com.alibaba.fastjson.JSONObject
     * @throws CdnHuaweiException 自定义异常
     * @author bo
     * @date 2023/2/26 10:50 PM
     * @link <a>https://support.huaweicloud.com/api-cdn/cdn_02_0059.html</a>
     */
    public static JSONObject queryNetworkBandwidth(GetTopOneHundredQueryDTO getTopOneHundredQuery) throws CdnHuaweiException {
        String error = "查询网络带宽峰值失败！错误原因：{}";
        return methodGetBase(HuaWeiCdn.QUERY_NETWORK_BANDWIDTH, getTopOneHundredQuery, error);
    }

    /**
     * description: <h2>查询网络带宽明细</h2>
     *
     * @param getTopOneHundredQuery 必传值domain_name 非必传参数 start_time,end_time,enterprise_project_id,interval
     * @return com.alibaba.fastjson.JSONObject
     * @throws CdnHuaweiException 自定义异常
     * @author bo
     * @date 2023/2/26 10:54 PM
     * @link <a>https://support.huaweicloud.com/api-cdn/cdn_02_0060.html</a>
     */
    public static JSONObject detailedQueryNetworkBandwidth(GetTopOneHundredQueryDTO getTopOneHundredQuery) throws CdnHuaweiException {
        String error = "查询网络带宽明细失败！错误原因：{}";
        return methodGetBase(HuaWeiCdn.DETAILED_QUERY_NETWORK_BANDWIDTH, getTopOneHundredQuery, error);
    }

    /**
     * description: <h2>查询消耗统计</h2>
     * 用于查询指定时间区间、数据类型、域名对应的统计量汇总信息。
     *
     * @param getTopOneHundredQuery 必传参数domain_name，stat_type 非必传参数 start_time,end_time,enterprise_project_id,service_area
     * @return com.alibaba.fastjson.JSONObject
     * @throws CdnHuaweiException 自定义异常
     * @author bo
     * @date 2023/2/26 11:03 PM
     * @link <a>https://support.huaweicloud.com/api-cdn/cdn_02_0061.html</a>
     */
    public static JSONObject queryConsumptionStatistics(GetTopOneHundredQueryDTO getTopOneHundredQuery) throws CdnHuaweiException {
        String error = "查询消耗统计失败！错误原因：{}";
        return methodGetBase(HuaWeiCdn.QUERY_CONSUMPTION_STATISTICS, getTopOneHundredQuery, error);
    }

    /**
     * description: <h2>查询消耗明细</h2>
     * 用于查询指定时间区、数据类型、域名对应的统计明细。
     *
     * @param getTopOneHundredQuery 必传参数domain_name,stat_type 非必传参数 start_time,end_time,enterprise_project_id,service_area,interval
     * @return com.alibaba.fastjson.JSONObject
     * @throws CdnHuaweiException 自定义异常
     * @author bo
     * @date 2023/2/26 11:32 PM
     * @link <a>https://support.huaweicloud.com/api-cdn/cdn_02_0062.html</a>
     */
    public static JSONObject queryCostDetail(GetTopOneHundredQueryDTO getTopOneHundredQuery) throws CdnHuaweiException {
        String error = "查询消耗明细失败！错误原因：{}";
        return methodGetBase(HuaWeiCdn.QUERY_COST_DETAIL, getTopOneHundredQuery, error);
    }

    /**
     * description: <h2>查询域名消耗统计</h2>
     * 用于查询指定时间区间、数据类型、指定域名的统计汇总容量。
     *
     * @param getTopOneHundredQuery 必传参数domain_name，stat_type 非必传参数 start_time,end_time,enterprise_project_id,service_area
     * @return com.alibaba.fastjson.JSONObject
     * @throws CdnHuaweiException 自定义异常
     * @author bo
     * @date 2023/2/26 11:40 PM
     * @link <a>https://support.huaweicloud.com/api-cdn/cdn_02_0063.html</a>
     */
    public static JSONObject queryDomainConsumptionStatistics(GetTopOneHundredQueryDTO getTopOneHundredQuery) throws CdnHuaweiException {
        String error = "查询域名消耗统计失败！错误原因：{}";
        return methodGetBase(HuaWeiCdn.QUERY_DOMAIN_CONSUMPTION_STATISTICS, getTopOneHundredQuery, error);
    }

    /**
     * description: <h2>查询区域消耗统计</h2>
     * 用于查询指定域名在指定区域、时间范围内的相关指标统计详情。
     *
     * @param getTopOneHundredQuery 必传参数domain_name,stat_type 非必传参数 start_time,end_time,enterprise_project_id,region
     * @return com.alibaba.fastjson.JSONObject
     * @throws CdnHuaweiException 自定义异常
     * @author bo
     * @date 2023/2/27 10:01 AM
     * @link <a>https://support.huaweicloud.com/api-cdn/cdn_02_0064.html</a>
     */
    public static JSONObject queryAreaConsumptionStatistics(GetTopOneHundredQueryDTO getTopOneHundredQuery) throws CdnHuaweiException {
        String error = "查询区域消耗统计！错误原因：{}";
        return methodGetBase(HuaWeiCdn.QUERY_AREA_CONSUMPTION_STATISTICS, getTopOneHundredQuery, error);
    }

    /**
     * description: <h2>查询运营商消耗统计</h2>
     * 用于查询指定域名在指定运营商、时间范围内的相关指标统计详情。
     *
     * @param getTopOneHundredQuery 必传参数 carrier,stat_type,domain_name 非必传参数 start_time,end_time,enterprise_project_id
     * @return com.alibaba.fastjson.JSONObject
     * @throws CdnHuaweiException 自定义异常
     * @author bo
     * @date 2023/2/27 10:05 AM
     * @link <a>https://support.huaweicloud.com/api-cdn/cdn_02_0065.html</a>
     */
    public static JSONObject queryOperatorsConsumptionStatistics(GetTopOneHundredQueryDTO getTopOneHundredQuery) throws CdnHuaweiException {
        String error = "查询运营商消耗统计失败！错误原因：{}";
        return methodGetBase(HuaWeiCdn.QUERY_OPERATORS_CONSUMPTION_STATISTICS, getTopOneHundredQuery, error);
    }

    /**
     * description: <h2>查询区域和运营商下的域名消耗统计</h2>
     *
     * @param getTopOneHundredQuery 必传参数 carrier,stat_type,domain_name,region 非必传参数 start_time,end_time,enterprise_project_id
     * @return com.alibaba.fastjson.JSONObject
     * @throws CdnHuaweiException 自定义异常
     * @author bo
     * @date 2023/2/27 10:12 AM
     * @link <a>https://support.huaweicloud.com/api-cdn/cdn_02_0066.html</a>
     */
    public static JSONObject regionalCarrierDomainStatistics(GetTopOneHundredQueryDTO getTopOneHundredQuery) throws CdnHuaweiException {
        String error = "查询区域和运营商下的域名消耗统计失败！错误原因：{}";
        return methodGetBase(HuaWeiCdn.REGIONAL_CARRIER_DOMAIN_STATISTICS, getTopOneHundredQuery, error);
    }


    /**
     * description: <h2>查询区域和运营商消耗统计</h2>
     *
     * @param getTopOneHundredQuery 必传参数 carrier,stat_type,domain_name,region 非必传参数 start_time,end_time,enterprise_project_id,interval
     * @return com.alibaba.fastjson.JSONObject
     * @throws CdnHuaweiException 自定义异常
     * @author bo
     * @date 2023/2/27 10:19 AM
     * @link <a>https://support.huaweicloud.com/api-cdn/cdn_02_0067.html</a>
     */
    public static JSONObject regionCarrierDetailStatistics(GetTopOneHundredQueryDTO getTopOneHundredQuery) throws CdnHuaweiException {
        String error = "查询区域和运营商消耗统计失败！错误原因：{}";
        return methodGetBase(HuaWeiCdn.REGION_CARRIER_DETAIL_STATISTICS, getTopOneHundredQuery, error);
    }

    /**
     * description: <h2>批量查询域名的区域、运营商统计明细-按域名单独返回</h2>
     *
     * @param getTopOneHundredQuery 必传参数 start_time,end_time,domain_name,stat_type,region,isp 非必传参数 enterprise_project_id
     * @return com.alibaba.fastjson.JSONObject
     * @throws CdnHuaweiException 自定义异常
     * @author bo
     * @date 2023/2/27 10:23 AM
     * @link <a>https://support.huaweicloud.com/api-cdn/cdn_02_0088.html</a>
     */
    public static JSONObject domainItemLocationDetailsStatistics(GetTopOneHundredQueryDTO getTopOneHundredQuery) throws CdnHuaweiException {
        String error = "批量查询域名的区域、运营商统计明细-按域名单独返回失败！错误原因：{}";
        return methodGetBase(HuaWeiCdn.DOMAIN_ITEM_LOCATION_DETAILS_STATISTICS, getTopOneHundredQuery, error);
    }

    /**
     * description: <h2>批量查询域名的统计明细-按域名单独返回</h2>
     *
     * @param getTopOneHundredQuery 必传参数 start_time,end_time,domain_name,stat_type 非必传参数 service_area,enterprise_project_id
     * @return com.alibaba.fastjson.JSONObject
     * @throws CdnHuaweiException 自定义异常
     * @author bo
     * @date 2023/2/27 10:23 AM
     * @link <a>https://support.huaweicloud.com/api-cdn/cdn_02_0087.html</a>
     */
    public static JSONObject domainItemDetailsStatistics(GetTopOneHundredQueryDTO getTopOneHundredQuery) throws CdnHuaweiException {
        String error = "批量查询域名的统计明细-按域名单独返回失败！错误原因：{}";
        return methodGetBase(HuaWeiCdn.DOMAIN_ITEM_DETAILS_STATISTICS, getTopOneHundredQuery, error);
    }

    /**
     * description: <h2>下载统计指标数据表格文件</h2>
     * 单租户调用频率：10次/min。
     *
     * @param getTopOneHundredQuery 必传参数 start_time,end_time,domain_name,excel_type 非必传参数 enterprise_project_id,interval,service_area
     * @return com.alibaba.fastjson.JSONObject
     * @throws CdnHuaweiException 自定义异常
     * @author bo
     * @date 2023/2/27 10:31 AM
     * @link <a>https://support.huaweicloud.com/api-cdn/DownloadStatisticsExcel.html</a>
     */
    public static JSONObject statisticDataDownloadFormFile(GetTopOneHundredQueryDTO getTopOneHundredQuery) throws CdnHuaweiException {
        String error = "下载统计指标数据表格文件失败！错误原因：{}";
        return methodGetBase(HuaWeiCdn.STATISTIC_DATA_DOWNLOAD_FORM_FILE, getTopOneHundredQuery, error);
    }


    /**
     * description: <h2>查询TOP域名</h2>
     *
     * @param getTopOneHundredQuery 必传参数 start_time,end_time,stat_type，非必传参数service_area,limit,enterprise_project_id
     * @return com.alibaba.fastjson.JSONObject
     * @throws CdnHuaweiException 自定义异常
     * @author bo
     * @date 2023/2/27 10:36 AM
     * @link <a>https://support.huaweicloud.com/api-cdn/ShowTopDomainNames.html</a>
     */
    public static JSONObject queryTopDomain(GetTopOneHundredQueryDTO getTopOneHundredQuery) throws CdnHuaweiException {
        String error = "查询TOP域名失败！错误原因：{}";
        return methodGetBase(HuaWeiCdn.QUERY_TOP_DOMAIN, getTopOneHundredQuery, error);
    }

    /**
     * description: peakClassDataQueryDomainBandwidth
     *
     * @param getTopOneHundredQuery 必传参数start_time,end_time,domain_name,calc_type 非必传参数service_area,enterprise_project_id
     * @return com.alibaba.fastjson.JSONObject
     * @throws CdnHuaweiException 自定义异常
     * @author bo
     * @date 2023/2/27 10:42 AM
     * @link <a>https://support.huaweicloud.com/api-cdn/ShowBandwidthCalc.html</a>
     */
    public static JSONObject peakClassDataQueryDomainBandwidth(GetTopOneHundredQueryDTO getTopOneHundredQuery) throws CdnHuaweiException {
        String error = "查询域名带宽峰值类数据失败！错误原因：{}";
        return methodGetBase(HuaWeiCdn.PEAK_CLASS_DATA_QUERY_DOMAIN_BANDWIDTH, getTopOneHundredQuery, error);
    }

    /**
     * description: <h2>下载区域运营商指标数据表格文件</h2>
     *
     * @param getTopOneHundredQuery 必传参数start_time,end_time,domain_name,excel_type 非必传参数carrier,region,enterprise_project_id,excel_language,country,interval
     * @return com.alibaba.fastjson.JSONObject
     * @throws CdnHuaweiException 自定义异常
     * @author bo
     * @date 2023/2/27 10:46 AM
     * @link <a>https://support.huaweicloud.com/api-cdn/DownloadRegionCarrierExcel.html</a>
     */
    public static JSONObject regionCarrierExcel(GetTopOneHundredQueryDTO getTopOneHundredQuery) throws CdnHuaweiException {
        String error = "下载区域运营商指标数据表格文件失败！错误原因：{}";
        return methodGetBase(HuaWeiCdn.REGION_CARRIER_EXCEL, getTopOneHundredQuery, error);
    }

    public static void main(String[] args) throws CdnHuaweiException {
        long startTime = DateUtil.parse("2023-04-16 00:00:00").getTime();
        long endTime = DateUtil.parse("2023-04-17 00:00:00").getTime();
        GetTopOneHundredQueryDTO dto = GetTopOneHundredQueryDTO.builder()
                .domain_name("test.kuocaicdn.com")
                .start_time(startTime)
                .end_time(endTime)
                .stat_type("flux")
                .action("summary")
                .build();
    }
}
