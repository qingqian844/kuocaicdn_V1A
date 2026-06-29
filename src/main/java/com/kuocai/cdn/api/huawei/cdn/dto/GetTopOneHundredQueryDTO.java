package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * <h1>查询TOP100明细的query</h1>
 *
 * @author xiaobo
 * @date 2023/2/26
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class GetTopOneHundredQueryDTO {

    /**
     * 当用户开启企业项目功能时，该参数生效，表示查询资源所属项目，"all"表示所有项目。注意：当使用子帐号调用接口时，该参数必传。
     * 您可以通过调用企业项目管理服务（EPS）的查询企业项目列表接口（ListEnterpriseProject）查询企业项目id。
     */
    private String enterprise_project_id;

    /**
     * 查询起始时间戳（单位：毫秒）。该时间戳的取值在转化为日期格式后须满足以下格式：XXXX-XX-XX 00:00:00
     */
    private Long start_time;

    /**
     * 查询结束时间戳（单位：毫秒）。该时间戳的取值在转化为日期格式后须满足以下格式：XXXX-XX-XX 00:00:00
     */
    private Long end_time;

    /**
     * 域名列表，多个域名以逗号（半角）分隔，如：www.test1.com,www.test2.com ，ALL表示查询名下全部域名
     */
    private String domain_name;

    /**
     * mainland_china(中国大陆)，outside_mainland_china(中国境外)，默认为mainland_china。
     */
    private String service_area;

    /**
     * 参数类型支持：flux(流量),req_num(请求总数)。
     */
    private String stat_type;

    /**
     * 动作名称，可选location_summary、location_detail。location_summary：查询汇总数据。location_detail：查询数据详情。
     */
    private String action;

    /**
     * 查询时间间隔 300(5分钟)：最大查询跨度2天
     * 3600(1小时)：最大查询跨度7天
     * 86400(1天)：最大查询跨度31天
     * 如果不传，默认取对应时间跨度的最小间隔。
     */
    private Long interval;

    /**
     * 数据分组方式，多个以英文逗号分隔，可选domain、country、province、isp，默认不分组。
     */
    private String groupBy;

    /**
     * 国家编码，多个以英文逗号分隔，all表示全部，取值见附录。
     */
    private String country;

    /**
     * 省份编码，当country为cn（中国）时有效，多个以英文逗号分隔，all表示全部，取值见附录。
     */
    private String province;

    /**
     * 运营商编码，多个以英文逗号分隔，all表示全部，取值见附录。
     */
    private String isp;

    /**
     * 区域列表，包括：中国34个省级行政区域（包含中国大陆省份、直辖市及港澳台）、中国以外及其他，多个区域以逗号分隔，
     */
    private String region;

    /**
     * 运营商列表，英文首字母缩写，目前支持CTCC（电信）, CMCC（移动）, CUCC（联通）, ENET（教育）, CRC（铁通）。多个运营商以逗号分隔，
     */
    private String carrier;

    /**
     * 统计数据表格类型,目前支持
     * 用量统计数据(excel_type_usage)
     * 访问情况统计数据(excel_type_access)
     * 回源情况统计数据（excel_type_origin）
     * http_code统计数据(excel_type_http_code)
     */
    private String excel_type;

    /**
     * top域名查询数量,默认为20,最大为500，最小为0
     */
    private String limit;
}
