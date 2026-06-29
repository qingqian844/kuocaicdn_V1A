package com.kuocai.cdn.api.cdnetworks.cdn.vo;

import com.alibaba.fastjson.JSON;
import com.kuocai.cdn.api.cdnetworks.cdn.CdnetworksErrorCodeHandler;
import com.kuocai.cdn.api.cdnetworks.cdn.CdnetworksHttp;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class DefaultErrorVO implements IResponseVO {
    private String code;
    private String message;
    private String errorMessages;

    public static DefaultErrorVO convert(CdnetworksHttp.Response response) {
        DefaultErrorVO defaultErrorVO = JSON.parseObject(response.getBody(), DefaultErrorVO.class);
        String errorMessage = CdnetworksErrorCodeHandler.getErrorDescription(defaultErrorVO);
        defaultErrorVO.setErrorMessages(String.format("%s，%s", defaultErrorVO.getCode(), errorMessage));
        return defaultErrorVO;
    }
}
