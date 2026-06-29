package com.kuocai.cdn.api.cdnetworks.cdn.vo;

import com.kuocai.cdn.api.cdnetworks.cdn.CdnetworksHttp;
import com.kuocai.cdn.exception.CdnetworksException;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class DisableDomainVO implements IResponseVO {
    private int code;

    private String message;

    public static DisableDomainVO convert(CdnetworksHttp.Response response) throws CdnetworksException {
        int statusCode = response.getStatusCode();
        String statusMessage = response.getStatusMessage();
        DisableDomainVO vo = DisableDomainVO.builder()
                .code(statusCode).message(statusMessage)
                .build();
        if (202 == statusCode) {
            return vo;
        } else {
            DefaultErrorVO errorVO = DefaultErrorVO.convert(response);
            throw new CdnetworksException(String.format("停用域名失败，错误信息：%s", errorVO.getErrorMessages()));
        }
    }
}
