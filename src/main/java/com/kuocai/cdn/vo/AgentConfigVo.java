package com.kuocai.cdn.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * (AgentConfig)实体类
 *
 * @author XUEW
 * @since 2023-06-14 17:24:16
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentConfigVo {

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 代理用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 网站名称
     */
    private String websiteName;

    /**
     * 网站关键词
     */
    private String websiteKeyword;

    /**
     * 网站描述
     */
    private String websiteDescription;

    /**
     * 网站介绍
     */
    private String about;

    /**
     * 解析CNAME
     */
    private String cname;

    /**
     * 网站LOGO
     */
    private String logo;

    /**
     * 网站图标
     */
    private String icon;

    /**
     * 地址
     */
    private String address;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 公司名称
     */
    private String company;

    /**
     * ICP备案号
     */
    private String icp;

    /**
     * 接入域名
     */
    private String domain;

    /**
     * 网站标题
     */
    private String title;

    /**
     * 许可证
     */
    private String licence;

    /**
     * 许可证跳转地址
     */
    private String licenceUrl;

    /**
     * 微信客服地址
     */
    private String wechatServiceUrl;
}
