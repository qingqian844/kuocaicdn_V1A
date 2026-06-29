package com.kuocai.cdn.api.cdnetworks.cdn.vo;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.cdnetworks.cdn.CdnetworksHttp;
import com.kuocai.cdn.exception.CdnetworksException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class QueryPurgeVO implements IResponseVO {
    private int count;
    private int Code;
    private String Message;
    private int pageNo;
    private int pageSize;
    private ResultDetail[] resultDetail;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class ResultDetail {
        private String beginTime;
        private String createTime;
        private String finishTime;
        private int isDir;
        private String rate;
        private String status;
        private String url;
    }

    public static QueryPurgeVO convert(CdnetworksHttp.Response response) throws CdnetworksException {
        int statusCode = response.getStatusCode();
        // String statusMessage = response.getStatusMessage();
        if (200 == statusCode) {
            return JSONObject.parseObject(response.getBody(), QueryPurgeVO.class);
        } else {
            DefaultErrorVO errorVO = DefaultErrorVO.convert(response);
            throw new CdnetworksException("查询刷新失败，错误信息：" + errorVO.getErrorMessages());
        }
    }
}
