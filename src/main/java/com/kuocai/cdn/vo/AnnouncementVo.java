package com.kuocai.cdn.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * (Announcement)实体类
 *
 * @author todoitbo
 * @since 2023-05-10 20:39:05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementVo {

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 公告标题
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private String title;

    /**
     * 公告内容
     */
    private String content;

    /**
     * 创建人ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 头像
     */
    private String img;

    /**
     * 状态：published、history
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
