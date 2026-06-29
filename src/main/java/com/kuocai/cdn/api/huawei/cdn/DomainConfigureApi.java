/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2018-2022. All rights reserved.
 */

package com.kuocai.cdn.api.huawei.cdn;

import com.alibaba.fastjson.JSONObject;
import com.cloud.apigateway.sdk.utils.Request;
import com.kuocai.cdn.api.huawei.cdn.dto.*;
import com.kuocai.cdn.api.huawei.cdn.properties.HuaWeiCdn;
import com.kuocai.cdn.exception.CdnHuaweiException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

import java.util.List;
import java.util.Map;

/**
 * 华为云域名配置API
 */
@Slf4j
public class DomainConfigureApi {

    /**
     * 修改源站信息
     * 源站IP地址或域名都可以指引CDN节点回源到对应的源站服务器，源站域名不能与加速域名相同。
     *
     * @param domainId 加速域名ID
     * @param sources  源站信息数组
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/UpdateDomainOrigin.html
     */
    public static JSONObject updateOrigin(String domainId, List<SourceWithPortDTO> sources) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.PUT_ORIGIN.replace("{domain_id}", domainId), "PUT");
            JSONObject originObj = new JSONObject();
            originObj.put("sources", sources);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("origin", originObj);
            request.setBody(jsonObject.toJSONString());
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "修改源站信息失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 修改回源HOST
     * 回源HOST是CDN节点在回源过程中，在源站访问的站点域名，即http请求头中的host信息。
     *
     * @param domainId       加速域名ID
     * @param originHostBody 修改回源HOST
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/UpdateOriginHost.html
     */
    public static JSONObject updateOriginHost(String domainId, OriginHostBodyDTO originHostBody) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.PUT_ORIGIN_HOST.replace("{domain_id}", domainId), "PUT");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("origin_host", originHostBody);
            request.setBody(jsonObject.toJSONString());
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "修改回源HOST失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 查询回源HOST
     * 回源HOST是CDN节点在回源过程中，在源站访问的站点域名，即http请求头中的host信息。
     *
     * @param domainId 加速域名ID
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/ShowOriginHost.html
     */
    public static JSONObject getOriginHost(String domainId) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.GET_ORIGIN_HOST.replace("{domain_id}", domainId), "GET");
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "查询回源HOST失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 开启/关闭Range回源
     * Range回源是指源站在收到CDN节点回源请求时，根据http请求头中的Range信息返回指定范围的数据给CDN节点。
     * 开启Range回源前需要确认源站是否支持Range请求，若源站不支持Range请求，开启Range回源将导致资源无法缓存。
     *
     * @param domainId 加速域名ID
     * @param status   开启（on）或关闭（off）状态
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/UpdateRangeSwitch.html
     */
    public static JSONObject updateRangeSwitch(String domainId, String status) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.PUT_RANGE_SWITCH.replace("{domain_id}", domainId), "PUT");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("range_status", status);
            request.setBody(jsonObject.toJSONString());
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "开启/关闭Range回源失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 开启/关闭回源跟随
     * 开启此项配置后，当CDN节点回源请求源站返回301/302状态码时，CDN节点会先跳转到301/302对应地址获取资源并缓存后再返回给用户。
     *
     * @param domainId 加速域名ID
     * @param status   开启（on）或关闭（off）状态
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/UpdateFollow302Switch.html
     */
    public static JSONObject updateFollowSwitch(String domainId, String status) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.PUT_FOLLOW_SWITCH.replace("{domain_id}", domainId), "PUT");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("follow302_status", status);
            request.setBody(jsonObject.toJSONString());
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "开启/关闭Range回源失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 设置Referer过滤规则
     * 通过设置过滤策略，对访问者身份进行识别和过滤，实现限制访问来源的目的。
     *
     * @param domainId 加速域名ID
     * @param referer  过滤规则
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/UpdateRefer.html
     */
    public static JSONObject updateReferer(String domainId, RefererDTO referer) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.PUT_REFERER.replace("{domain_id}", domainId), "PUT");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("referer", referer);
            request.setBody(jsonObject.toJSONString());
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "设置Referer过滤规则失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 查询Referer过滤规则
     *
     * @param domainId 加速域名ID
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/ShowRefer.html
     */
    public static JSONObject getReferer(String domainId) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.GET_REFERER.replace("{domain_id}", domainId), "GET");
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "查询Referer过滤规则失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 查询IP黑白名单
     * 查询域名已经设置的IP黑白名单。
     *
     * @param domainId 加速域名ID
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/ShowBlackWhiteList.html
     */
    public static JSONObject getIpAcl(String domainId) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.GET_IP_ACL.replace("{domain_id}", domainId), "GET");
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "查询IP黑白名单失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 设置IP黑白名单
     * 设置域名的IP黑白名单。
     *
     * @param domainId 加速域名ID
     * @param type     IP黑白名单类型 常量类参考 IpAclType
     * @param ips      IP黑白名单列表（支持掩码且有掩码的情况下IP必须是该IP段的第一个IP）
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/UpdateBlackWhiteList.html
     */
    public static JSONObject updateIpAcl(String domainId, Integer type, List<String> ips) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.PUT_IP_ACL.replace("{domain_id}", domainId), "PUT");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", type);
            jsonObject.put("ip_list", ips);
            request.setBody(jsonObject.toJSONString());
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "设置IP黑白名单失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 设置缓存规则
     * 设置CDN节点上缓存资源的缓存策略。
     *
     * @param domainId    加速域名ID
     * @param cacheConfig 缓存规则
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/UpdateCacheRules.html
     */
    public static JSONObject updateCache(String domainId, CacheConfigRequestDTO cacheConfig) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.PUT_CACHE.replace("{domain_id}", domainId), "PUT");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("cache_config", cacheConfig);
            request.setBody(jsonObject.toJSONString());
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "设置缓存规则失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 查询缓存规则
     *
     * @param domainId 加速域名ID
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/ShowCacheRules.html
     */
    public static JSONObject getCache(String domainId) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.GET_CACHE.replace("{domain_id}", domainId), "GET");
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "查询缓存规则失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 配置HTTPS
     * 通过配置加速域名的HTTPS证书，并将其部署在全网CDN节点，实现HTTPS安全加速。
     *
     * @param domainId 加速域名ID
     * @param httpInfo 配置信息
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/UpdateHttpsInfo.html
     */
    public static JSONObject updateHttpsInfo(String domainId, HttpInfoRequestBodyDTO httpInfo) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.PUT_HTTPS_INFO.replace("{domain_id}", domainId), "PUT");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("https", httpInfo);
            request.setBody(jsonObject.toJSONString());
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "配置HTTPS失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 查询HTTPS配置
     * 获取加速域名证书。
     *
     * @param domainId 加速域名ID
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/ShowHttpInfo.html
     */
    public static JSONObject getHttpsInfo(String domainId) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.GET_HTTPS_INFO.replace("{domain_id}", domainId), "GET");
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "查询HTTPS配置失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 查询IP归属信息
     *
     * @param ips IP地址列表，以“，”分割，最多20个。
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/ShowIpInfo.html
     */
    public static JSONObject getIpInfo(String ips) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.GET_IP_INFO, "GET");
            request.addQueryStringParam("ips", ips);
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "查询IP归属信息失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 新增/修改响应头配置
     *
     * @param domainId 加速域名ID
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/UpdateResponseHeader.html
     */
    public static JSONObject updateResponseHeader(String domainId, Map<String, String> headerMap) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.PUT_RESPONSE_HEADER.replace("{domain_id}", domainId), "PUT");
            JSONObject jsonObject = new JSONObject();
            JSONObject headers = new JSONObject();
            headers.putAll(headerMap);
            jsonObject.put("headers", headers);
            request.setBody(jsonObject.toJSONString());
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "新增/修改响应头配置失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 查询响应头配置
     * 列举header所有配置。
     *
     * @param domainId 加速域名ID
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/ShowResponseHeader.html
     */
    public static JSONObject getResponseHeader(String domainId) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.GET_RESPONSE_HEADER.replace("{domain_id}", domainId), "GET");
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "查询响应头配置失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 修改私有桶开启关闭状态
     *
     * @param domainId 加速域名ID
     * @param status   桶开启关闭状态（true：开启；false：关闭）
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/UpdatePrivateBucketAccess.html
     */
    public static JSONObject updatePrivateBucketAccess(String domainId, Boolean status) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.PUT_PRIVATE_BUCKET_ACCESS.replace("{domain_id}", domainId), "PUT");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("status", status);
            request.setBody(jsonObject.toJSONString());
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "修改私有桶开启关闭状态失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 一个证书批量设置多个域名
     * 设置域名强制https回源参数。
     *
     * @param dto https对象
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/UpdateDomainMultiCertificates.html
     */
    public static JSONObject updateConfigHttpsInfo(UpdateDomainMultiCertificatesRequestBodyContentDTO dto) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.PUT_CONFIG_HTTPS_INFO, "PUT");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("https", dto);
            request.setBody(jsonObject.toJSONString());
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "一个证书批量设置多个域名失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 查询所有绑定HTTPS证书的域名信息
     *
     * @param query 查询参数
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/ShowCertificatesHttpsInfo.html
     */
    public static JSONObject getHttpsCertificateInfo(HttpsCertificateInfoQueryDTO query) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.GET_HTTPS_CERTIFICATE_INFO, "GET");
            HuaweiRequest.addQueryStringParamDTO(request, query);
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "查询所有绑定HTTPS证书的域名信息失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 修改域名全量配置接口
     * 修改域名全量配置接口，支持配置回源请求头、HTTP header配置、URL鉴权、证书、源站、回源协议、强制重定向、智能压缩、缓存URL参数、IPv6、状态码缓存时间、Range回源、User-Agent黑白名单、改写回源URL、自定义错误页面
     *
     * @param domainName 加速域名
     * @param configs    全量配置
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/UpdateDomainFullConfig.html
     */
    public static JSONObject updateDomainConfigs(String domainName, DomainConfigsDTO configs) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.PUT_DOMAIN_CONFIGS.replace("{domain_name}", domainName), "PUT");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("configs", configs);
            request.setBody(jsonObject.toJSONString());
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "修改域名全量配置接口失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 查询域名配置接口
     * 查询域名配置接口，支持查询回源请求头、HTTP header配置、URL鉴权、证书、源站、回源协议、强制重定向、智能压缩、缓存URL参数、IPv6开关、状态码缓存时间、Range回源、User-Agent黑白名单、改写回源URL、自定义错误页面
     *
     * @param domainName 加速域名
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/ShowDomainFullConfig.html
     */
    public static JSONObject getDomainConfigs(String domainName) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.GET_DOMAIN_CONFIGS.replace("{domain_name}", domainName), "GET");
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "查询域名配置接口失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 查询资源标签列表配置接口
     *
     * @param resourceId 资源id
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/ShowTags.html
     */
    public static JSONObject getResourceTags(String resourceId) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.GET_RESOURCE_TAGS, "GET");
            request.addQueryStringParam("resource_id", resourceId);
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "查询资源标签列表配置接口失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 创建资源标签配置接口
     *
     * @param resourceId 资源id
     * @param tags       资源标签列表
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/CreateTags.html
     */
    public static JSONObject createResourceTags(String resourceId, List<ResourceTagDTO> tags) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.POST_RESOURCE_TAGS, "POST");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("resource_id", resourceId);
            jsonObject.put("tags", tags);
            request.setBody(jsonObject.toJSONString());
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "创建资源标签配置接口接口失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 删除资源标签配置接口
     *
     * @param resourceId 资源id
     * @param tags       键列表
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/BatchDeleteTags.html
     */
    public static JSONObject deleteResourceTags(String resourceId, List<String> tags) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.POST_RESOURCE_TAGS_BATCH_DELETE, "POST");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("resource_id", resourceId);
            jsonObject.put("tags", tags);
            request.setBody(jsonObject.toJSONString());
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "删除资源标签配置接口失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 批量域名复制
     *
     * @param dto 原域名所有配置
     * @return 响应结果
     * @link https://support.huaweicloud.com/api-cdn/BatchCopyDomain.html
     */
    public static JSONObject batchCopyDomains(BatchCopyConfigsDTO dto) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.POST_BATCH_COPY_DOMAINS, "POST");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("configs", dto);
            request.setBody(jsonObject.toJSONString());
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "批量域名复制失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 查询HTTPS证书关联域名接口
     */
    public static JSONObject certificateAssociatedDomainName(CertificateDomainNameDTO dto) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.POST_HTTPS_CERTIFICATION, "POST");
            request.setBody(JSONObject.toJSONString(dto));
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "查询HTTPS证书关联域名接口！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }
}