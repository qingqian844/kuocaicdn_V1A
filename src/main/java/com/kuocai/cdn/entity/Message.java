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
 * (Message)实体类
 *
 * @author makejava
 * @since 2023-05-11 15:48:31
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("message")
public class Message implements Serializable {

    private static final long serialVersionUID = -92105182015600629L;

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
     * 接收用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long receiveUserId;

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
