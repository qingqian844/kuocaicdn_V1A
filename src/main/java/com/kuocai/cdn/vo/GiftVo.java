package com.kuocai.cdn.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.kuocai.cdn.entity.SysUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

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
public class GiftVo {

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
    @JsonSerialize(using = ToStringSerializer.class)
    private Double flowPackageSize;

    /**
     * 流量包大小，已自动换算单位
     */
    private String flowPackageSizeString;

    /**
     * 流量包截止时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deadline;

    /**
     * 礼品截止时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    /**
     * 礼品份数
     */
    private Integer batchCount;

    /**
     * 礼品总数
     */
    private Integer size;

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

    /**
     * 用户信息
     */
    private List<SysUser> users;
}
