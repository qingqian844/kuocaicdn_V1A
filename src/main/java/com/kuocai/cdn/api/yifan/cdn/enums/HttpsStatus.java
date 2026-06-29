package com.kuocai.cdn.api.yifan.cdn.enums;

import java.io.IOException;

/**
 * 是否开启https
 */
public enum HttpsStatus {
    OFF, ON;

    public String toValue() {
        switch (this) {
            case OFF: return "off";
            case ON: return "on";
        }
        return null;
    }

    public static HttpsStatus forValue(String value) throws IOException {
        if (value.equals("off")) return OFF;
        if (value.equals("on")) return ON;
        throw new IOException("Cannot deserialize HTTPSStatus");
    }
}
