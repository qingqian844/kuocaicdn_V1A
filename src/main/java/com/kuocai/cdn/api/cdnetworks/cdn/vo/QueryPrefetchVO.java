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
public class QueryPrefetchVO implements IResponseVO {
    private String name;
    private FileList[] fileList;
    private Metadata metadata;
    private String startTime;

    @Data
    @ToString
    public static class FileList {
        private String url;
    }

    @Data
    @ToString
    public static class Metadata {
        private String id;
        private String submissionTime;
        private int successRate;
        // 取值范围: waiting,inprogress,finished 预取请求的任务执行状态，包括等待中，进行中，已完成等状态。
        private String status;
        private String finishTime;
        private String apiRequestId;
    }

    public static QueryPrefetchVO convert(CdnetworksHttp.Response response) throws CdnetworksException {
        int statusCode = response.getStatusCode();
        // String statusMessage = response.getStatusMessage();
        if (200 == statusCode) {
            return JSONObject.parseObject(response.getBody(), QueryPrefetchVO.class);
        } else {
            DefaultErrorVO errorVO = DefaultErrorVO.convert(response);
            throw new CdnetworksException("查询预热失败，错误信息：" + errorVO.getErrorMessages());
        }
    }
}
