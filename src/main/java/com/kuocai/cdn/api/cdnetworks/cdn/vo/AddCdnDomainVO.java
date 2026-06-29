package com.kuocai.cdn.api.cdnetworks.cdn.vo;

import com.alibaba.fastjson.JSON;
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
public class AddCdnDomainVO implements IResponseVO {

    private String code;

    private String message;

    private String domainId;

    private String cname;

    public static AddCdnDomainVO convert(CdnetworksHttp.Response response) throws CdnetworksException {
        int statusCode = response.getStatusCode();
        AddCdnDomainVO vo = JSON.parseObject(response.getBody(), AddCdnDomainVO.class);
        if (202 == statusCode) {
            String location = response.getHeader("Location");
            Pattern pattern = Pattern.compile(".*/(\\d+)$");
            Matcher matcher = pattern.matcher(location);
            if (matcher.find()) {
                vo.setDomainId(matcher.group(1));
            }
            vo.setCname(response.getHeader("cname"));
            return vo;
        } else {
            DefaultErrorVO errorVO = DefaultErrorVO.convert(response);
            throw new CdnetworksException(String.format("创建域名失败，错误信息：%s", errorVO.getErrorMessages()));
        }
    }
}
