package com.kuocai.cdn.api.yifan.cdn.enums;

import java.io.IOException;

/**
 * 加速服务类型，目前只有国内加速
 */
public enum ServiceArea {
    MAINLAND_CHINA;

    public String toValue() {
        switch (this) {
            case MAINLAND_CHINA: return "mainland_china";
        }
        return null;
    }

    public static ServiceArea forValue(String value) throws IOException {
        if (value.equals("mainland_china")) return MAINLAND_CHINA;
        throw new IOException("Cannot deserialize ServiceArea");
    }
}