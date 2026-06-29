package com.kuocai.cdn.api.huawei.cdn;

import com.alibaba.fastjson.JSONObject;
import com.cloud.apigateway.sdk.utils.Request;
import com.kuocai.cdn.api.huawei.cdn.dto.LogDTO;
import com.kuocai.cdn.api.huawei.cdn.properties.HuaWeiCdn;
import com.kuocai.cdn.exception.CdnHuaweiException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

/**
 * 华为云日志管理API
 */
@Slf4j
public class LogManageApi {

    /**
     * 日志查询。
     *
     * @param logDTO 查询日志参数
     * @return {@code JSONObject}
     * @link https://support.huaweicloud.com/intl/zh-cn/api-cdn/ShowLogs.html
     */
    public static JSONObject getLogInfos(LogDTO logDTO) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.GET_LOGS, "GET");
            HuaweiRequest.addQueryStringParamDTO(request, logDTO);
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "日志查询！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

}
