package com.kuocai.cdn.api.cdnetworks.cdn.vo;

import com.kuocai.cdn.api.cdnetworks.cdn.CdnetworksHttp;
import com.kuocai.cdn.exception.CdnetworksException;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class EnableDomainVO implements IResponseVO {
    private int code;

    private String message;

    public static EnableDomainVO convert(CdnetworksHttp.Response response) throws CdnetworksException {
        int statusCode = response.getStatusCode();
        String statusMessage = response.getStatusMessage();
        EnableDomainVO vo = EnableDomainVO.builder()
                .code(statusCode).message(statusMessage)
                .build();
        if (202 == statusCode) {
            return vo;
        } else {
            DefaultErrorVO errorVO = DefaultErrorVO.convert(response);
            throw new CdnetworksException(String.format("启用域名失败，错误信息：%s", errorVO.getErrorMessages()));
        }
    }
}
