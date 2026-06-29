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
 * (Gift)实体类
 *
 * @author makejava
 * @since 2023-05-15 14:43:53
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("gift")
public class Gift implements Serializable {

    private static final long serialVersionUID = -46722872100848286L;

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 流量包名称
     */
    private String flowPackageName;

    /**
     * 流量包大小
     */
    private Long flowPackageSize;

    /**
     * 流量包截止时间
     */
    private Date deadline;

    /**
     * 礼品截止时间
     */
    private Date expireTime;

    /**
     * 礼品总数
     */
    private volatile Integer size;

    /**
     * 兑换码
     */
    private String code;

    /**
     * 赠送记录JSON的形式
     */
    private String purchasedRecord;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    public void reduceSize() {
        this.size = getSize() - 1;
    }
}
