package com.kuocai.cdn.api.cdnetworks.cdn.vo;

import com.kuocai.cdn.api.cdnetworks.cdn.CdnetworksHttp;
import com.kuocai.cdn.exception.CdnetworksException;
import lombok.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class CreatePrefetchVO implements IResponseVO {
    private String code;
    private String message;
    private String itemId;

    public static CreatePrefetchVO convert(CdnetworksHttp.Response response) throws CdnetworksException {
        int statusCode = response.getStatusCode();
        String statusMessage = response.getStatusMessage();
        CreatePrefetchVO vo = CreatePrefetchVO.builder().code(String.valueOf(statusCode)).message(statusMessage).itemId("").build();
        if (200 == statusCode) {
            String location = response.getHeader("Location");
            Pattern pattern = Pattern.compile(".*/(\\d+)$");
            Matcher matcher = pattern.matcher(location);
            if (matcher.find()) {
                vo.setItemId(matcher.group(1));
            }
            return vo;
        } else {
            DefaultErrorVO errorVO = DefaultErrorVO.convert(response);
            throw new CdnetworksException("预热失败，错误信息：" + errorVO.getErrorMessages());
        }
    }
}
