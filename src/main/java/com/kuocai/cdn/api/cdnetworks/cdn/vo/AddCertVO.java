package com.kuocai.cdn.api.cdnetworks.cdn.vo;

import com.alibaba.fastjson.JSONObject;
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
public class AddCertVO implements IResponseVO {
    private String code;

    private String message;

    private String csrId;

    public static AddCertVO convert(CdnetworksHttp.Response response) throws CdnetworksException {
        int statusCode = response.getStatusCode();
        // String statusMessage = response.getStatusMessage();
        AddCertVO addCertVO = JSONObject.parseObject(response.getBody(), AddCertVO.class);
        if (200 == statusCode) {
            String location = response.getHeader("Location");
            // e.g https://api.cdnetworks.com/api/certificate/1447648
            Pattern pattern = Pattern.compile(".*/(\\d+)$");
            Matcher matcher = pattern.matcher(location);
            if (matcher.find()) {
                addCertVO.setCsrId(matcher.group(1));
            }
            return addCertVO;
        } else {
            // 如果存在了的情况
            if ("36540148".equals(addCertVO.getCode())) {
                // e.g The SSL content you provided is already in our system.certificateId:1447648,certificateName:abc.20mo.cn-v27869zx
                Pattern pattern = Pattern.compile("certificateId:(\\d+),");
                Matcher matcher = pattern.matcher(addCertVO.getMessage());
                if (matcher.find()) {
                    addCertVO.setCsrId(matcher.group(1));
                    return addCertVO;
                }
            }
            DefaultErrorVO errorVO = DefaultErrorVO.convert(response);
            throw new CdnetworksException("新增 Cert 证书失败，错误信息：" + errorVO.getErrorMessages());
        }
    }
}
