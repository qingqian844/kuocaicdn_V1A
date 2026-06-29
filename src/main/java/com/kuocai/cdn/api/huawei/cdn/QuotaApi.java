package com.kuocai.cdn.api.huawei.cdn;

import com.alibaba.fastjson.JSONObject;
import com.cloud.apigateway.sdk.utils.Request;
import com.kuocai.cdn.api.huawei.cdn.properties.HuaWeiCdn;
import com.kuocai.cdn.exception.CdnHuaweiException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

/**
 * 华为云配额中心API
 */
@Slf4j
public class QuotaApi {

    /**
     * 查询当前用户域名、刷新文件、刷新目录和预热的配额。
     *
     * @return {@code JSONObject}
     * @link https://support.huaweicloud.com/intl/zh-cn/api-cdn/ShowQuota.html
     */
    public static JSONObject getQuotaInfo() throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.GET_QUOTA, "GET");
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "查询当前用户域名、刷新文件、刷新目录和预热的配额！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

}
