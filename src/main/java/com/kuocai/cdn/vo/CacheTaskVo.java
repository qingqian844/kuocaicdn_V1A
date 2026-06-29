package com.kuocai.cdn.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * (CacheTask)实体类
 *
 * @author makejava
 * @since 2023-05-10 18:53:58
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CacheTaskVo {

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务类型：preheating/refresh
     */
    private String taskType;

    /**
     * 刷新预热类型：file/dictory
     */
    private String refreshType;

    /**
     * CDN提供商
     */
    private String cdnSupplier;

    /**
     * 用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;
}
