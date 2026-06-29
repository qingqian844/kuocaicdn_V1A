package com.kuocai.cdn.api.cdnetworks.cdn.vo;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.cdnetworks.cdn.CdnetworksHttp;
import com.kuocai.cdn.exception.CdnetworksException;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class UpdateInnerRedirectVO implements IResponseVO {
    private String domainId;

    private String domainName;

    public static UpdateInnerRedirectVO convert(CdnetworksHttp.Response response) throws CdnetworksException {
        int statusCode = response.getStatusCode();
        // String statusMessage = response.getStatusMessage();
        if (202 == statusCode) {
            return JSONObject.parseObject(response.getBody(), UpdateInnerRedirectVO.class);
        } else {
            DefaultErrorVO errorVO = DefaultErrorVO.convert(response);
            throw new CdnetworksException("更新域名 InnerRedirect 配置失败，错误信息：" + errorVO.getErrorMessages());
        }
    }
}
