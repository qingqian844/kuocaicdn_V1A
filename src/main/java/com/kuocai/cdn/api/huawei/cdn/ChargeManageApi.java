package com.kuocai.cdn.api.huawei.cdn;

import com.alibaba.fastjson.JSONObject;
import com.cloud.apigateway.sdk.utils.Request;
import com.kuocai.cdn.api.huawei.cdn.dto.ChargeModeDTO;
import com.kuocai.cdn.api.huawei.cdn.properties.HuaWeiCdn;
import com.kuocai.cdn.exception.CdnHuaweiException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

/**
 * 华为云计费管理API
 */
@Slf4j
public class ChargeManageApi {

    /**
     * 查询用户计费模式。
     * 服务区域仅支持mainland_china（国内，默认）和outside_mainland_china（海外）
     * 计费模式状态支持active（已生效），upcoming（待生效）两种状态，默认为active(已生效)
     * 加速类型仅支持base（基础加速）
     * 单租户调用频率：5次/s。
     *
     * @param chargeModeDTO 用户计费参数
     * @return {@code JSONObject}
     * @link https://support.huaweicloud.com/intl/zh-cn/api-cdn/ShowChargeModes.html
     */
    public static JSONObject getChargeModes(ChargeModeDTO chargeModeDTO) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.CHARGE_MODES, "GET");
            HuaweiRequest.addQueryStringParamDTO(request, chargeModeDTO);
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "查询用户计费模式！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 设置用户计费模式。
     * 服务区域仅支持mainland_china（国内）
     * 计费模式仅支持设置flux（流量），bw（带宽）
     * 加速类型仅支持base（基础加速）
     * 单租户调用频率：10次/min。
     *
     * @param chargeModeDTO 用户计费参数
     * @return {@code JSONObject}
     * @link https://support.huaweicloud.com/intl/zh-cn/api-cdn/SetChargeModes.html
     */
    public static JSONObject setChargeMode(ChargeModeDTO chargeModeDTO) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.CHARGE_MODES, "PUT");
            request.setBody(JSONObject.toJSONString(chargeModeDTO));
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "设置用户计费模式！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }
}
