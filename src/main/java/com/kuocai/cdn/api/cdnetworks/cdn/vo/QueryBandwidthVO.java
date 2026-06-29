package com.kuocai.cdn.api.cdnetworks.cdn.vo;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.cdnetworks.cdn.CdnetworksHttp;
import com.kuocai.cdn.exception.CdnetworksException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class QueryBandwidthVO implements IResponseVO {
    // 总流量,保留2位小数,单位为MB
    private String flowSummary;

    private QueryBandwidthVO.BandwidthData[] bandwidthReport;

    public double getFlowSummary() {
        double sum = 0;
        for (BandwidthData bandwidthData : bandwidthReport) {
            sum += Double.parseDouble(bandwidthData.getBandwidth());
        }
        return sum;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class BandwidthData {
        private String timestamp;
        private String bandwidth;
    }

    public static QueryBandwidthVO convert(CdnetworksHttp.Response response) throws CdnetworksException {
        int statusCode = response.getStatusCode();
        // String statusMessage = response.getStatusMessage();
        if (200 == statusCode) {
            String body = response.getBody();
            if ("{".equals(String.valueOf(body.charAt(0)))) {
                // {"bandwidth-data":{"timestamp":"2024-03-20 01","bandwidth":"0.8792"}} => [{"timestamp":"2024-03-20 01","bandwidth":"0.8792"}]
                body = String.format("[%s]", body.substring(18, body.length() - 1));
            }
            List<BandwidthData> bandwidthData = JSONObject.parseArray(body, BandwidthData.class);
            QueryBandwidthVO queryBandwidthVO = new QueryBandwidthVO();
            queryBandwidthVO.setBandwidthReport(bandwidthData.toArray(new BandwidthData[0]));
            return queryBandwidthVO;
        } else {
            DefaultErrorVO errorVO = DefaultErrorVO.convert(response);
            throw new CdnetworksException("查询域名 Bandwidth 配置失败，错误信息：" + errorVO.getErrorMessages());
        }
    }
}
