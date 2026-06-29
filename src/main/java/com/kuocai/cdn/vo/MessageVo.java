package com.kuocai.cdn.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * (Message)实体类
 *
 * @author makejava
 * @since 2023-05-11 15:48:32
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageVo {

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 发送用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sendUserId;

    /**
     * 发送用户姓名
     */
    private String sendUserName;

    /**
     * 发送用户头像
     */
    private String sendUserImg;

    /**
     * 接收用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long receiveUserId;

    /**
     * 接收用户姓名
     */
    private String receiveUserName;

    /**
     * 接收用户头像
     */
    private String receiveUserImg;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 状态：read、unread
     */
    private String status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
