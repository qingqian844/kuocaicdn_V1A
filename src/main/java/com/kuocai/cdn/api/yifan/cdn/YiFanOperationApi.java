package com.kuocai.cdn.api.yifan.cdn;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.huawei.cdn.dto.UrlAuthDTO;
import com.kuocai.cdn.api.yifan.cdn.dto.*;
import com.kuocai.cdn.api.yifan.cdn.enums.OriginProtocol;
import com.kuocai.cdn.api.yifan.cdn.enums.ServiceArea;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

@Slf4j
public class YiFanOperationApi {

    /**
     * 查询可以使用的路线
     *
     * @return
     * @throws Exception
     */
    public static JSONArray getMyLines() throws Exception {
//        HashMap<String, Object> requestBody = new HashMap<>();

//        TreeMap<String, String> queryMap = new TreeMap<>();
//        queryMap.put("page", "1");
//        queryMap.put("size", "10");

        JSONObject response = YiFanRequest.request("/api/v1.0/my_line", "GET", new TreeMap<>(), new byte[0]/*JSON.toJSONBytes(requestBody)*/);
        return response.getJSONArray("data");
    }

    /**
     * 查询可以创建的域名数量
     *
     * @return
     * @throws Exception
     */
    public static Integer quotas() throws Exception {
        JSONObject response = YiFanRequest.request("/api/v1.0/domain/quotas", "GET", new TreeMap<>(), new byte[0]/*JSON.toJSONBytes(requestBody)*/);
        return response.getInteger("data");
    }


    public static JSONObject createDomain(String domainName, String businessType, String serviceArea, String originType, String ipOrDomain) throws Exception {
        DomainDTO domainDTO = new DomainDTO();
        domainDTO.setServiceArea(ServiceArea.MAINLAND_CHINA.toValue());
        JSONArray myLines = getMyLines();
        List<Empty> preCreateChecks = myLines.getJSONObject(0).getJSONArray("preCreateChecks").toJavaList(Empty.class);
        String lineId = myLines.getJSONObject(0).getString("lineId");
        domainDTO.setDomainSources(preCreateChecks);
        domainDTO.setDomainName(domainName);
        domainDTO.setBusinessType(businessType);
        SourceElement sourceElement = new SourceElement().setOriginType(originType).setIpOrDomain(ipOrDomain);
        domainDTO.setSources(Arrays.asList(sourceElement));
        domainDTO.setLineId(lineId);
        domainDTO.setServiceArea(serviceArea);
        JSONObject response = YiFanRequest.request("/api/v1.0/domain/create", "POST", new TreeMap<>(), JSON.toJSONBytes(domainDTO));
        return response.getJSONObject("data");
    }

    /**
     * 6492566fdd5cbc2f49a61514
     * 64a3d79d53422345319cbb8d
     * 启用加速域名
     *
     * @param domainIds
     * @throws Exception
     */
    public static void enable(List<String> domainIds) throws Exception {
        YiFanRequest.request("/api/v1.0/domain/enable", "POST", new TreeMap<>(), JSON.toJSONBytes(domainIds));
    }


    /**
     * 禁用加速域名
     *
     * @param domainIds
     * @throws Exception
     */
    public static void disable(List<String> domainIds) throws Exception {
        YiFanRequest.request("/api/v1.0/domain/disable", "POST", new TreeMap<>(), JSON.toJSONBytes(domainIds));
    }

    /**
     * 删除加速域名
     *
     * @param domainIds
     * @throws Exception
     */
    public static void delete(List<String> domainIds) throws Exception {
        YiFanRequest.request("/api/v1.0/domain/delete", "POST", new TreeMap<>(), JSON.toJSONBytes(domainIds));
    }


    /**
     * 修改源站信息
     *
     * @param sourceElement
     * @param domainId
     * @throws Exception
     */
    public static void saveSourceStationInfos(String domainId, SourceElement sourceElement) throws Exception {
        String postUrl = "/api/v1.0/domain/{domainId}/origin";
        String url = postUrl.replace("{domainId}", domainId);
        YiFanRequest.request(url, "POST", new TreeMap<>(), JSON.toJSONBytes(sourceElement));
    }

    public static void saveOriginProtocol(String domainId, OriginProtocol originProtocol) throws Exception {
        String postUrl = "/api/v1.0/domain/{domainId}/origin-protocol";
        String url = postUrl.replace("{domainId}", domainId);
        YiFanRequest.request(url, "POST", new TreeMap<>(), JSON.toJSONBytes(originProtocol.toValue()));
    }

    public static void rangeSwitch(String domainId, String rangeSwitch) throws Exception {
        String postUrl = "/api/v1.0/domain/{domainId}/range-switch";
        String url = postUrl.replace("{domainId}", domainId);
        HashMap<String, Object> requestBody = new HashMap<>();
        requestBody.put("rangeSwitch", rangeSwitch);
        YiFanRequest.request(url, "POST", new TreeMap<>(), JSON.toJSONBytes(requestBody));

    }

    /**
     * 新增/修改回源请求头配置
     *
     * @param domainId
     * @param requestHeaderDTOS
     * @throws Exception
     */
    public static void requestHeader(String domainId, List<RequestHeaderDTO> requestHeaderDTOS) throws Exception {
        String postUrl = "/api/v1.0/domain/{domainId}/request-header";
        String url = postUrl.replace("{domainId}", domainId);
        YiFanRequest.request(url, "POST", new TreeMap<>(), JSON.toJSONBytes(requestHeaderDTOS));
    }

    /**
     * https配置
     *
     * @param domainId
     * @param httpsConfigDTO
     * @throws Exception
     */
    public static void httpsConfig(String domainId, HttpsConfigDTO httpsConfigDTO) throws Exception {
        String postUrl = "/api/v1.0/domain/{domainId}/https-info";
        String url = postUrl.replace("{domainId}", domainId);
        YiFanRequest.request(url, "POST", new TreeMap<>(), JSON.toJSONBytes(httpsConfigDTO));
    }

    /**
     * 强制重定向配置
     *
     * @param domainId
     * @param forceRedirect
     * @throws Exception
     */
    public static void forceRedirect(String domainId, ForceRedirectDTO forceRedirect) throws Exception {
        String postUrl = "/api/v1.0/domain/{domainId}/force-redirect";
        String url = postUrl.replace("{domainId}", domainId);
        YiFanRequest.request(url, "POST", new TreeMap<>(), JSON.toJSONBytes(forceRedirect));
    }

    /**
     * 忽略 URL 参数
     *
     * @param domainId
     * @param ignoreUrlParameter
     * @throws Exception
     */
    public static void ignoreUrlParameter(String domainId, boolean ignoreUrlParameter) throws Exception {
        String postUrl = "/api/v1.0/domain/{domainId}/cache-rule";
        String url = postUrl.replace("{domainId}", domainId);
        CacheRuleDTO cacheRuleDTO = new CacheRuleDTO().setIgnoreUrlParameter(ignoreUrlParameter);
        YiFanRequest.request(url, "POST", new TreeMap<>(), JSON.toJSONBytes(cacheRuleDTO));
    }

    /**
     * 智能压缩
     *
     * @param domainId
     * @param compress 1/0
     * @throws Exception
     */
    public static void compress(String domainId, Integer compress) throws Exception {
        String postUrl = "/api/v1.0/domain/{domainId}/cache-rule";
        String url = postUrl.replace("{domainId}", domainId);
        CacheRuleDTO cacheRuleDTO = new CacheRuleDTO().setCompress(compress);
        YiFanRequest.request(url, "POST", new TreeMap<>(), JSON.toJSONBytes(cacheRuleDTO));
    }

    /**
     * 缓存配置
     *
     * @param domainId
     * @param cacheRuleDTO
     * @throws Exception
     */
    public static void cacheRuleConfig(String domainId, CacheRuleDTO cacheRuleDTO) throws Exception {
        String postUrl = "/api/v1.0/domain/{domainId}/cache-rule";
        String url = postUrl.replace("{domainId}", domainId);
        YiFanRequest.request(url, "POST", new TreeMap<>(), JSON.toJSONBytes(cacheRuleDTO));
    }

    /**
     * 配置防盗链
     *
     * @param domainId
     * @param hotlinkPreventionDTO
     * @throws Exception
     */
    public static void saveHotlinkPrevention(String domainId, HotlinkPreventionDTO hotlinkPreventionDTO) throws Exception {
        String postUrl = "/api/domain/config/{domainId}/refere";
        String url = postUrl.replace("{domainId}", domainId);
        YiFanRequest.request(url, "POST", new TreeMap<>(), JSON.toJSONBytes(hotlinkPreventionDTO));
    }

    /**
     * 配置IP黑白名单
     *
     * @param domainId
     * @param ipAclDTO
     * @throws Exception
     */
    public static void saveIpAcl(String domainId, IpAclDTO ipAclDTO) throws Exception {
        String postUrl = "/api/domain/config/{domainId}/ip-acl";
        String url = postUrl.replace("{domainId}", domainId);
        YiFanRequest.request(url, "POST", new TreeMap<>(), JSON.toJSONBytes(ipAclDTO));
    }

    /**
     * Url鉴权配置
     * @param domainId
     * @param urlAuthDTO
     * @throws Exception
     */
    public static void saveUrlAuth(String domainId, UrlAuthDTO urlAuthDTO) throws Exception {
        String postUrl = "/api/domain/config/{domainId}/url-auth";
        String url = postUrl.replace("{domainId}", domainId);
        YiFanRequest.request(url, "POST", new TreeMap<>(), JSON.toJSONBytes(urlAuthDTO));
    }

    /**
     * 新增/修改域名响应头配置
     * @param domainId
     * @param responseHeaderDTOs
     * @throws Exception
     */
    public static void responseHeader(String domainId, List<ResponseHeaderDTO> responseHeaderDTOs) throws Exception {
        String postUrl = "/api/domain/config/{domainId}/response-header";
        String url = postUrl.replace("{domainId}", domainId);
        YiFanRequest.request(url, "POST", new TreeMap<>(), JSON.toJSONBytes(responseHeaderDTOs));
    }

    public static void preCheck() throws Exception {
        HashMap<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", "6406b6197a19386f9babbd83");
        requestBody.put("host", "xuewei.world");
        YiFanRequest.request("/api/v1.0/aliyun/pre_check", "POST", new TreeMap<>(), JSON.toJSONBytes(requestBody));
    }

    public static void main(String[] args) throws Exception {
        String getUrl = "/api/v1.0/domain/64a4155ed3eec25c6c590a93/detail";
        JSONObject response = YiFanRequest.request(getUrl, "GET", new TreeMap<>(), new byte[0]);
    }
}
