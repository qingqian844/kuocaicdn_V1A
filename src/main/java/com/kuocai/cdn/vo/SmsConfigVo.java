package com.kuocai.cdn.vo;

import com.kuocai.cdn.util.Assert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xiaobo
 * @date 2023/3/27
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsConfigVo {

    /**
     * 短信应用的唯一标识
     */
    private Integer sdkAppId;

    /**
     * 校验合法性Id
     */
    private String secretId;

    /**
     * 校验合法性秘钥
     */
    private String secretKey;

    /**
     * 短信签名
     */
    private String smsSign;

    public boolean empty() {
        boolean flag = false;
        if (Assert.isEmpty(sdkAppId)) {
            flag = true;
        }
        if (Assert.isEmpty(secretKey)) {
            flag = true;
        }
        if (Assert.isEmpty(smsSign)) {
            flag = true;
        }
        return flag;
    }
}
