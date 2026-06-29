package com.kuocai.cdn.api.cdnetworks.cdn.vo;

import com.kuocai.cdn.api.cdnetworks.cdn.CdnetworksHttp;
import com.kuocai.cdn.exception.CdnetworksException;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class UpdateDomainVO implements IResponseVO {
    private int code;

    private String message;

    public static UpdateDomainVO convert(CdnetworksHttp.Response response) throws CdnetworksException {
        int statusCode = response.getStatusCode();
        String statusMessage = response.getStatusMessage();
        UpdateDomainVO vo = UpdateDomainVO.builder()
                .code(statusCode).message(statusMessage)
                .build();
        if (202 == statusCode) {
            return vo;
        } else {
            DefaultErrorVO errorVO = DefaultErrorVO.convert(response);
            throw new CdnetworksException("更新域名失败", errorVO);
        }
    }
}
