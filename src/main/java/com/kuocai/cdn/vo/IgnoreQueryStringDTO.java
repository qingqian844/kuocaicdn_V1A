package com.kuocai.cdn.vo;

import lombok.Data;

@Data
public class IgnoreQueryStringDTO {
    /**
     * on | off
     */
    private String enable;
    /**
     * block：删除部分参数；allow：保留部分参数；
     */
    private String type;
    /**
     * 保留/删除的参数，多个用逗号（英文、半角）分隔
     */
    private String hashKeyArgs;
} 