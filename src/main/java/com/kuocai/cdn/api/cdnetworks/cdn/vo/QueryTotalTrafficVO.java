package com.kuocai.cdn.api.cdnetworks.cdn.vo;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.cdnetworks.cdn.CdnetworksHttp;
import com.kuocai.cdn.exception.CdnetworksException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class QueryTotalTrafficVO implements IResponseVO {
    // 总流量,保留2位小数,单位为MB
    private String flowSummary;

    private FlowData[] flowData;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class FlowData {
        private String timestamp;
        private String flow;
    }

    public static QueryTotalTrafficVO convert(CdnetworksHttp.Response response) throws CdnetworksException {
        int statusCode = response.getStatusCode();
        // String statusMessage = response.getStatusMessage();
        if (200 == statusCode) {
            return JSONObject.parseObject(response.getBody(), QueryTotalTrafficVO.class);
        } else {
            DefaultErrorVO errorVO = DefaultErrorVO.convert(response);
            throw new CdnetworksException("查询域名 TotalTraffic 配置失败，错误信息：" + errorVO.getErrorMessages());
        }
    }
}
