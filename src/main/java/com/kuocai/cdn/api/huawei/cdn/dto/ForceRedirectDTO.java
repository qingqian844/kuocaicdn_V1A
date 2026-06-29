package com.kuocai.cdn.api.huawei.cdn.dto;

import com.alibaba.fastjson.JSONObject;

/**
 * 强制跳转
 */
public class ForceRedirectDTO extends JSONObject {

    /**
     * @param switchStatus 强制跳转开关。1打开。0关闭。
     * @param redirectType 强制跳转类型。http：强制跳转HTTP。https：强制跳转HTTPS。
     */
    public ForceRedirectDTO(Integer switchStatus, String redirectType) {
        this.put("switch", switchStatus);
        this.put("redirect_type", redirectType);
    }
}
