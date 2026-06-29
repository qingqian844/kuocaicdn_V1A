package com.kuocai.cdn.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 通知设置表(UserNoticeSettings)实体类
 *
 * @author ChenWEI
 * @since 2023-02-20 21:12:34
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("user_notice_settings")
public class UserNoticeSettings implements Serializable {

    private static final long serialVersionUID = 559370954741302068L;

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 通知类型
     */
    private String noticeType;

    /**
     * 邮件通知
     */
    private Integer emailAccept;

    /**
     * 短信通知
     */
    private Integer smsAccept;

    /**
     * 微信公众号
     */
    private Integer wechatAccept;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;
}
