package com.kuocai.cdn.api.huawei.cdn.properties;

/**
 * 华为云CDN常量类
 */
public class HuaWeiCdn {

    public static final String HTTPS = "HTTPS";
    public static final String GM_PROTOCOL = "GMTLS";
    public static final String INTERNATIONAL_PROTOCOL = "TLSv1.2";
    public static final String SIGNATURE_ALGORITHM_SDK_HMAC_SHA256 = "SDK-HMAC-SHA256";
    public static final String SIGNATURE_ALGORITHM_SDK_HMAC_SM3 = "SDK-HMAC-SM3";

    /**
     * 访问密钥 AK
     * 测试环境：BGEVVXXLY0WFYBVI4SVL
     * 生成环境：BGEVVXXLY0WFYBVI4SVL
     */
    public static String ACCESS_KEY = "";

    /**
     * 访问密钥 SK
     * 测试环境：KvGRyUGfYjP226FHOpWGXDyIOaO1IZFVwj2QogkH
     * 生成环境：KvGRyUGfYjP226FHOpWGXDyIOaO1IZFVwj2QogkH
     */
    public static String SECRET_ACCESS_KEY = "";

    /**
     * 地区与终端节点
     */
    public static final String ENDPOINT = "https://cdn.myhuaweicloud.com";

    /**
     * IAM（CCM）证书管理的地区与终端节点
     */
    public static final String CCM_ENDPOINT = "https://scm.ap-southeast-1.myhuaweicloud.com";

    /**
     * 查询加速域名 URI
     */
    public static final String GET_DOMAINS = "/v1.0/cdn/domains";

    /**
     * 创建加速域名 URI
     */
    public static final String POST_DOMAINS = "/v1.0/cdn/domains";

    /**
     * 加速域名详情 URI
     */
    public static final String GET_DOMAIN_DETAIL = "/v1.0/cdn/domains/{domain_id}/detail";

    /**
     * 删除加速域名 URI
     */
    public static final String DELETE_DOMAIN = "/v1.0/cdn/domains/{domain_id}";

    /**
     * 启用加速域名 URI
     */
    public static final String PUT_DOMAIN_ENABLE = "/v1.0/cdn/domains/{domain_id}/enable";

    /**
     * 停用加速域名 URI
     */
    public static final String PUT_DOMAIN_DISABLE = "/v1.0/cdn/domains/{domain_id}/disable";

    /**
     * 修改源站信息 URI
     */
    public static final String PUT_ORIGIN = "/v1.0/cdn/domains/{domain_id}/origin";

    /**
     * 修改回源HOST URI
     */
    public static final String PUT_ORIGIN_HOST = "/v1.0/cdn/domains/{domain_id}/originhost";

    /**
     * 修改回源HOST URI
     */
    public static final String GET_ORIGIN_HOST = "/v1.0/cdn/domains/{domain_id}/originhost";

    /**
     * 开启/关闭Range回源 URI
     */
    public static final String PUT_RANGE_SWITCH = "/v1.0/cdn/domains/{domain_id}/range-switch";

    /**
     * 开启/关闭回源跟随 URI
     */
    public static final String PUT_FOLLOW_SWITCH = "/v1.0/cdn/domains/{domain_id}/follow302-switch";

    /**
     * 设置Referer过滤规则 URI
     */
    public static final String PUT_REFERER = "/v1.0/cdn/domains/{domain_id}/referer";

    /**
     * 查询Referer过滤规则 URI
     */
    public static final String GET_REFERER = "/v1.0/cdn/domains/{domain_id}/referer";

    /**
     * 查询IP黑白名单 URI
     */
    public static final String GET_IP_ACL = "/v1.0/cdn/domains/{domain_id}/ip-acl";

    /**
     * 设置IP黑白名单 URI
     */
    public static final String PUT_IP_ACL = "/v1.0/cdn/domains/{domain_id}/ip-acl";

    /**
     * 设置缓存规则 URI
     */
    public static final String PUT_CACHE = "/v1.0/cdn/domains/{domain_id}/cache";

    /**
     * 查询缓存规则 URI
     */
    public static final String GET_CACHE = "/v1.0/cdn/domains/{domain_id}/cache";

    /**
     * 配置HTTPS URI
     */
    public static final String PUT_HTTPS_INFO = "/v1.0/cdn/domains/{domain_id}/https-info";

    /**
     * 查询HTTPS配置 URI
     */
    public static final String GET_HTTPS_INFO = "/v1.0/cdn/domains/{domain_id}/https-info";

    /**
     * 查询IP归属信息 URI
     */
    public static final String GET_IP_INFO = "/v1.0/cdn/ip-info";

    /**
     * 新增/修改响应头配置 URI
     */
    public static final String PUT_RESPONSE_HEADER = "/v1.0/cdn/domains/{domain_id}/response-header";

    /**
     * 查询响应头配置 URI
     */
    public static final String GET_RESPONSE_HEADER = "/v1.0/cdn/domains/{domain_id}/response-header";

    /**
     * 修改私有桶开启关闭状态 URI
     */
    public static final String PUT_PRIVATE_BUCKET_ACCESS = "/v1.0/cdn/domains/{domain_id}/private-bucket-access";

    /**
     * 一个证书批量设置多个域名 URI
     */
    public static final String PUT_CONFIG_HTTPS_INFO = "/v1.0/cdn/domains/config-https-info";

    /**
     * 查询所有绑定HTTPS证书的域名信息 URI
     */
    public static final String GET_HTTPS_CERTIFICATE_INFO = "/v1.0/cdn/domains/https-certificate-info";

    /**
     * 修改域名全量配置接口 URI
     */
    public static final String PUT_DOMAIN_CONFIGS = "/v1.1/cdn/configuration/domains/{domain_name}/configs";

    /**
     * 修改域名全量配置接口 URI
     */
    public static final String GET_DOMAIN_CONFIGS = "/v1.1/cdn/configuration/domains/{domain_name}/configs";

    /**
     * 查询资源标签列表配置接口 URI
     */
    public static final String GET_RESOURCE_TAGS = "/v1.0/cdn/configuration/tags";

    /**
     * 创建资源标签配置接口 URI
     */
    public static final String POST_RESOURCE_TAGS = "/v1.0/cdn/configuration/tags";

    /**
     * 删除资源标签配置接口 URI
     */
    public static final String POST_RESOURCE_TAGS_BATCH_DELETE = "/v1.0/cdn/configuration/tags/batch-delete";

    /**
     * 批量域名复制 URI
     */
    public static final String POST_BATCH_COPY_DOMAINS = "/v1.0/cdn/configuration/domains/batch-copy";

    /**
     * 查询Top100明细
     */
    public static final String GET_TOP_ONE_HUNDRED = "/v1.0/cdn/statistics/top-url";

    /**
     * 方法字符串post
     */
    public static final String METHOD_POST_STRING = "POST";

    /**
     * 方法字符串get
     */
    public static final String METHOD_GET_STRING = "GET";

    /**
     * 查询域名统计数据-区域运营商
     */
    public static final String GET_DOMAIN_STATISTICS_AREA = "/v1.0/cdn/statistics/domain-location-stats";

    /**
     * 查询域名统计数据-非区域运营商
     */
    public static final String GET_DOMAIN_STATISTICS = "/v1.0/cdn/statistics/domain-stats";

    /**
     * 创建刷新缓存任务
     */
    public static final String POST_REFRESH_TASKS = "/v1.0/cdn/content/refresh-tasks";

    /**
     * 创建预热任务
     */
    public static final String POST_PREHEATING_TASKS = "/v1.0/cdn/content/preheating-tasks";

    /**
     * 查询刷新预热任务。
     */
    public static final String GET_HISTORY_TASK = "/v1.0/cdn/historytasks";

    /**
     * 查询刷新预热任务详情。
     */
    public static final String GET_HISTORY_TASK_DETAIL = "/v1.0/cdn/historytasks/{history_tasks_id}/detail";


    /**
     * 查询刷新预热URL记录。如需此接口，请提交工单开通。
     */
    public static final String GET_URL_TASKS = "/v1.0/cdn/contentgateway/url-tasks";

    /**
     * 日志查询
     */
    public static final String GET_LOGS = "/v1.0/cdn/logs";

    /**
     * 查询用户配额
     */
    public static final String GET_QUOTA = "/v1.0/cdn/quota";

    /**
     * 用户计费管理
     */
    public static final String CHARGE_MODES = "/v1.0/cdn/charge/charge-modes";

    /**
     * 查询网络总流量
     */
    public static final String QUERY_NETWORK_TRAFFIC = "/v1.0/cdn/statistics/flux";

    /**
     * 查询网络流量明细
     */
    public static final String QUERY_NETWORK_TRAFFIC_DETAILS = "/v1.0/cdn/statistics/flux-detail";

    /**
     * 查询网络带宽峰值
     */
    public static final String QUERY_NETWORK_BANDWIDTH = "/v1.0/cdn/statistics/bandwidth";

    /**
     * 查询网络带宽明细
     */
    public static final String DETAILED_QUERY_NETWORK_BANDWIDTH = "/v1.0/cdn/statistics/bandwidth-detail";

    /**
     * 查询消耗统计
     */
    public static final String QUERY_CONSUMPTION_STATISTICS = "/v1.0/cdn/statistics/domain-summary";

    /**
     * 查询消耗明细
     */
    public static final String QUERY_COST_DETAIL = "/v1.0/cdn/statistics/domain-summary-detail";

    /**
     * 查询域名消耗统计
     */
    public static final String QUERY_DOMAIN_CONSUMPTION_STATISTICS = "/v1.0/cdn/statistics/domain";

    /**
     * 查询区域消耗统计
     */
    public static final String QUERY_AREA_CONSUMPTION_STATISTICS = "/v1.0/cdn/statistics/region-detail-summary";

    /**
     * 查询运营商消耗统计
     */
    public static final String QUERY_OPERATORS_CONSUMPTION_STATISTICS = "/v1.0/cdn/statistics/carrier-detail-summary";

    /**
     * 查询区域和运营商下的域名消耗统计
     */
    public static final String REGIONAL_CARRIER_DOMAIN_STATISTICS = "/v1.0/cdn/statistics/region-carrier-domain";

    /**
     * 查询区域和运营商消耗统计
     */
    public static final String REGION_CARRIER_DETAIL_STATISTICS = "/v1.0/cdn/statistics/region-carrier-detail";

    /**
     * 批量查询域名的区域、运营商统计明细-按域名单独返回
     */
    public static final String DOMAIN_ITEM_LOCATION_DETAILS_STATISTICS = "/v1.0/cdn/statistics/domain-item-location-details";

    /**
     * 批量查询域名的统计明细-按域名单独返回
     */
    public static final String DOMAIN_ITEM_DETAILS_STATISTICS = "/v1.0/cdn/statistics/domain-item-details";

    /**
     * 下载统计指标数据表格文件
     */
    public static final String STATISTIC_DATA_DOWNLOAD_FORM_FILE = "/v1.0/cdn/statistics/statistics-excel";

    /**
     * 查询TOP域名
     */
    public static final String QUERY_TOP_DOMAIN = "/v1/cdn/statistics/top-domain-names";

    /**
     * 查询域名带宽峰值类数据
     */
    public static final String PEAK_CLASS_DATA_QUERY_DOMAIN_BANDWIDTH = "/v1.0/cdn/statistics/bandwidth-calc";

    /**
     * 下载区域运营商指标数据表格文件
     */
    public static final String REGION_CARRIER_EXCEL = "/v1.0/cdn/statistics/region-carrier-excel";

    /**
     * 查询HTTPS证书关联域名接口
     */
    public static final String POST_HTTPS_CERTIFICATION = "/v1.0/cdn/certificate-matched-domain-names?enterprise_project_id=ALL";


    /**
     * 查询加速域名详情
     */
    public static final String GET_DOMAIN_DETAIL_BY_NAME = "/v1.0/cdn/configuration/domains/{domain_name}";
}
