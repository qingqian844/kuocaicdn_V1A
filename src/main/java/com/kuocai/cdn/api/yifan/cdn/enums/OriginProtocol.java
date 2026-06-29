package com.kuocai.cdn.api.yifan.cdn.enums;

import java.io.IOException;

/**
 * 回源协议（follow：协议跟随回源，http：HTTP回源(默认)，https：https回源）
 */
public enum OriginProtocol {
    FOLLOW, HTTP, HTTPS;

    public String toValue() {
        switch (this) {
            case FOLLOW: return "follow";
            case HTTP: return "http";
            case HTTPS: return "https";
        }
        return null;
    }

    public static OriginProtocol forValue(String value) throws IOException {
        if (value.equals("follow")) return FOLLOW;
        if (value.equals("http")) return HTTP;
        if (value.equals("https")) return HTTPS;
        throw new IOException("Cannot deserialize OriginProtocol");
    }
}
