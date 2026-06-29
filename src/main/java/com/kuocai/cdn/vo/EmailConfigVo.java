package com.kuocai.cdn.vo;

import com.kuocai.cdn.util.Assert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xiaobo
 * @date 2023/3/24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailConfigVo {

    /**
     * SMTP服务器
     */
    private String smtpServer;

    /**
     * 发件人邮箱
     */
    private String senderMailbox;

    /**
     * 发送人标题
     */
    private String senderTitle;

    /**
     * 授权密码
     */
    private String authorizationPassword;

    /**
     * 端口
     */
    private Integer serverPort;

    public boolean empty() {
        boolean flag = false;
        if (Assert.isEmpty(smtpServer)) {
            flag = true;
        }
        if (Assert.isEmpty(senderMailbox)) {
            flag = true;
        }
        if (Assert.isEmpty(senderTitle)) {
            flag = true;
        }
        if (Assert.isEmpty(authorizationPassword)) {
            flag = true;
        }
        if (Assert.isEmpty(serverPort)) {
            flag = true;
        }
        return flag;
    }
}