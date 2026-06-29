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
public class EmailTemplateVo {

    /**
     * 用于安全验证的邮件模版内容
     */
    private String notifyTemplateContent;

    /**
     * 用于忘记密码的邮件模版内容
     */
    private String forgetPasswordTemplateContent;

    /**
     * 用于套餐已过期邮件模版内容
     */
    private String packetExpirationTemplateContent;

    /**
     * 套餐即将过期的邮件模版内容
     */
    private String packetWillExpirationTemplateContent;

    /**
     * 用于流量已超限的邮件模版内容
     */
    private String packetGiveOutTemplateContent;

    /**
     * 用于流量即将超限的邮件模版内容
     */
    private String packetWillGiveOutTemplateContent;

    /**
     * 用于流量即将超限的邮件模版内容
     */
    private String workOrderMessageTemplateContent;

}
