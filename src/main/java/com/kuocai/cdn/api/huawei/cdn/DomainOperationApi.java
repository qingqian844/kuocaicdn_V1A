/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2018-2022. All rights reserved.
 */

package com.kuocai.cdn.api.huawei.cdn;

import com.alibaba.fastjson.JSONObject;
import com.cloud.apigateway.sdk.utils.Request;
import com.kuocai.cdn.api.huawei.cdn.dto.DomainBodyDTO;
import com.kuocai.cdn.api.huawei.cdn.properties.HuaWeiCdn;
import com.kuocai.cdn.exception.CdnHuaweiException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

/**
 * 华为云域名操作API
 */
@Slf4j
public class DomainOperationApi {

    /**
     * 查询加速域名信息
     *
     * @param query 查询参数
     * @return 域名信息
     * @link https://support.huaweicloud.com/api-cdn/ListDomains.html
     */
    public static JSONObject getDomains(DomainBodyDTO query) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.GET_DOMAINS, "GET");
            HuaweiRequest.addQueryStringParamDTO(request, query);
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "查询加速域名信息失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 创建加速域名
     *
     * @param dto 加速域名信息
     * @return 域名信息
     * @link https://support.huaweicloud.com/api-cdn/CreateDomain.html
     */
    public static JSONObject createDomain(DomainBodyDTO dto) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.POST_DOMAINS, "POST");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("domain", dto);
            request.setBody(jsonObject.toJSONString());
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "创建加速域名失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 查询加速域名详情
     *
     * @param domainId 加速域名ID
     * @return 域名信息
     * @link https://support.huaweicloud.com/api-cdn/ShowDomainDetail.html
     */
    public static JSONObject getDomainDetail(String domainId) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.GET_DOMAIN_DETAIL.replace("{domain_id}", domainId), "GET");
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "查询加速域名详情失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 删除加速域名
     *
     * @param domainId 加速域名ID
     * @return 域名信息
     * @link https://support.huaweicloud.com/api-cdn/DeleteDomain.html
     */
    public static JSONObject deleteDomain(String domainId) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.DELETE_DOMAIN.replace("{domain_id}", domainId), "DELETE");
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "删除加速域名失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 启用加速域名
     *
     * @param domainId 加速域名ID
     * @return 域名信息
     * @link https://support.huaweicloud.com/api-cdn/EnableDomain.html
     */
    public static JSONObject enableDomain(String domainId) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.PUT_DOMAIN_ENABLE.replace("{domain_id}", domainId), "PUT");
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "启用加速域名失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 停用加速域名
     *
     * @param domainId 加速域名ID
     * @return 域名信息
     * @link https://support.huaweicloud.com/api-cdn/DisableDomain.html
     */
    public static JSONObject disableDomain(String domainId) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.PUT_DOMAIN_DISABLE.replace("{domain_id}", domainId), "PUT");
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "停用加速域名失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 查询加速域名详情
     *
     * @param domainName 加速域名ID
     * @return 域名信息
     * @link https://support.huaweicloud.com/intl/zh-cn/api-cdn/ShowDomainDetailByName.html
     */
    public static JSONObject getDomainDetailByDomainName(String domainName) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.GET_DOMAIN_DETAIL_BY_NAME.replace("{domain_name}", domainName), "GET");
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "停用加速域名失败！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }
}