package com.kuocai.cdn.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * (OperationLogVo)
 *
 * @author makejava
 * @since 2023-04-13 15:36:38
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationLogVo {
    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 操作人ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 操作人名称
     */
    private String userName;

    /**
     * 功能模块
     */
    private String module;

    /**
     * 服务名
     */
    private String service;

    /**
     * 操作描述
     */
    private String opDescribe;

    /**
     * 请求参数
     */
    private String request;

    /**
     * 返回参数
     */
    private String response;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 请求URL
     */
    private String url;

    /**
     * 请求IP
     */
    private String ip;

    /**
     * 操作时间
     */
    private Date createTime;

    /**
     * 是否删除，0不删除，1删除，默认为 0
     */
    private String deleted;

    /**
     * 用户头像
     */
    private String img;
}
