package com.kuocai.cdn.api.yifan.cdn.enums;

import java.io.IOException;

/**
 * 开关状态
 */
public enum Status {
    OFF, ON;

    public String toValue() {
        switch (this) {
            case OFF: return "off";
            case ON: return "on";
        }
        return null;
    }

    public static Status forValue(String value) throws IOException {
        if (value.equals("off")) return OFF;
        if (value.equals("on")) return ON;
        throw new IOException("Cannot deserialize Status");
    }
}
