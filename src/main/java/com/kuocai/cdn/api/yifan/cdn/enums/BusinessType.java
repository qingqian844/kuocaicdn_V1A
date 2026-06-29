package com.kuocai.cdn.api.yifan.cdn.enums;

import java.io.IOException;

/**
 * 加速类型
 */
public enum BusinessType {
    DOWNLOAD, VIDEO, WEB;

    public String toValue() {
        switch (this) {
            case DOWNLOAD: return "download";
            case VIDEO: return "video";
            case WEB: return "web";
        }
        return null;
    }

    public static BusinessType forValue(String value) throws IOException {
        if (value.equals("download")) return DOWNLOAD;
        if (value.equals("video")) return VIDEO;
        if (value.equals("web")) return WEB;
        throw new IOException("Cannot deserialize BusinessType");
    }
}
