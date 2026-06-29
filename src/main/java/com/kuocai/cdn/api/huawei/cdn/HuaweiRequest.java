package com.kuocai.cdn.api.huawei.cdn;

import com.alibaba.fastjson.JSONObject;
import com.cloud.apigateway.sdk.utils.Client;
import com.cloud.apigateway.sdk.utils.Request;
import com.kuocai.cdn.api.huawei.cdn.properties.HuaWeiCdn;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.BeanUtils;
import com.kuocai.cdn.util.SSLCipherSuiteUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Objects;

/**
 * 通用华为云请求
 */
@Slf4j
public class HuaweiRequest {

    /**
     * 获取华为云 Request 对象
     *
     * @return Request 对象
     */
    public static Request getRequest(String uri) throws CdnHuaweiException, UnsupportedEncodingException {
        String[] split = uri.split("_");
        Request request = new Request();
        try {
            request.setKey(HuaWeiCdn.ACCESS_KEY);
            request.setSecret(HuaWeiCdn.SECRET_ACCESS_KEY);
            request.setUrl(HuaWeiCdn.ENDPOINT + uri);
            request.setMethod(split[0]);
            request.addHeader("Content-Type", "text/plain");
        } catch (Exception e) {
            throw new CdnHuaweiException("创建华为云请求错误！{}", request.getUrl());
        }
        return request;
    }

    /**
     * 获取华为云 Request 对象
     *
     * @return Request 对象
     */
    public static Request getRequest(String uri, String method) throws CdnHuaweiException, UnsupportedEncodingException {
        Request request = new Request();
        try {
            request.setKey(HuaWeiCdn.ACCESS_KEY);
            request.setSecret(HuaWeiCdn.SECRET_ACCESS_KEY);
            request.setUrl(HuaWeiCdn.ENDPOINT + uri);
            request.setMethod(method);
            request.addHeader("Content-Type", "text/plain");
        } catch (Exception e) {
            throw new CdnHuaweiException("创建华为云请求错误！{} -> {}", method, request.getUrl());
        }
        return request;
    }

    /**
     * 获取华为云 Request 对象
     *
     * @return Request 对象
     */
    public static Request getRequest(String endPoint, String uri, String method) throws CdnHuaweiException, UnsupportedEncodingException {
        Request request = new Request();
        try {
            request.setKey(HuaWeiCdn.ACCESS_KEY);
            request.setSecret(HuaWeiCdn.SECRET_ACCESS_KEY);
            request.setUrl(endPoint + uri);
            request.setMethod(method);
            request.addHeader("Content-Type", "text/plain");
        } catch (Exception e) {
            throw new CdnHuaweiException("创建华为云请求错误！{} -> {}", method, request.getUrl());
        }
        return request;
    }

    /**
     * 添加查询参数DTO
     *
     * @param request 请求
     * @param dto     查询DTO
     */
    public static void addQueryStringParamDTO(Request request, Object dto) {
        for (Map.Entry<String, Object> entry : BeanUtils.bean2Map(dto).entrySet()) {
            if (Assert.isEmpty(entry.getValue())) {
                continue;
            }
            request.addQueryStringParam(entry.getKey(), entry.getValue().toString());
        }
    }

    /**
     * 发送请求
     *
     * @param request 请求
     * @return 响应
     */
    public static Response doRequest(Request request) throws CdnHuaweiException, UnsupportedEncodingException {
        try {
            // 将请求进行签名
            okhttp3.Request signedRequest = Client.signOkhttp(request, HuaWeiCdn.SIGNATURE_ALGORITHM_SDK_HMAC_SHA256);
            // 创建客户端
            OkHttpClient client = SSLCipherSuiteUtil.createOkHttpClient(HuaWeiCdn.INTERNATIONAL_PROTOCOL);
            // 发送请求
            return client.newCall(signedRequest).execute();
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new CdnHuaweiException("请求华为云接口错误！{} -> {}", request.getMethod(), request.getUrl());
        }
    }

    /**
     * 处理华为云响应对象
     *
     * @param response 响应
     * @return 响应的JSONObject
     */
    public static JSONObject dealResponse(Response response) throws IOException, CdnHuaweiException {
        ResponseBody body = response.body();
        // body 返回值source可能为空
        JSONObject responseObject = JSONObject.parseObject(body.string());
        if (Assert.notEmpty(responseObject)) {
            JSONObject error = responseObject.getJSONObject("error");
            if (Assert.notEmpty(error)) {
                String errorCode = error.getString("error_code");
                String errorMsg = error.getString("error_msg");
                String msg = HuaweiErrorCodeMap.getMsg(errorCode);
                log.error("【{}】【{}】【{}】", errorCode, msg, errorMsg);
                errorMsg = Assert.isEmpty(msg) ? errorMsg : msg;
                throw new CdnHuaweiException(errorMsg);
            }
        }
        if (!response.isSuccessful()) {
            throw new CdnHuaweiException(Objects.requireNonNull(response.body()).string());
        }
        return responseObject;
    }
}
