package com.kuocai.cdn.api.yifan.cdn.enums;

import java.io.IOException;

/**
 * 加速域名回原地址类型
 */
public enum OriginType {
    DOMAIN, IPADDR;

    public String toValue() {
        switch (this) {
            case DOMAIN: return "domain";
            case IPADDR: return "ipaddr";
        }
        return null;
    }

    public static OriginType forValue(String value) throws IOException {
        if (value.equals("domain")) return DOMAIN;
        if (value.equals("ipaddr")) return IPADDR;
        throw new IOException("Cannot deserialize OriginType");
    }
}
