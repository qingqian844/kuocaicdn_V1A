package com.kuocai.cdn.vo;

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
public class SmsTemplateVo {

    /**
     * 用于安全验证的短信模版ID
     */
    private Integer notifyTemplateTitle;

    /**
     * 忘记密码
     */
    private Integer forgetPasswordTemplateTitle;

    /**
     * 用于套餐已过期短信模版ID
     */
    private Integer packetExpirationTemplateTitle;

    /**
     * 套餐即将过期的短信模版ID
     */
    private Integer packetWillExpirationTemplateTitle;

    /**
     * 用于流量已超限的短信模版ID
     */
    private Integer packetGiveOutTemplateTitle;

    /**
     * 用于流量即将超限的短信模版ID
     */
    private Integer packetWillGiveOutTemplateTitle;

}
