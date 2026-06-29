package com.kuocai.cdn.api.yifan.cdn.enums;

import java.io.IOException;

/**
 * 校验节点类型
 */
public enum Type {
    ALIYUN, HUAWEI, QINIU, TENCENT, VOLC;

    public String toValue() {
        switch (this) {
            case ALIYUN: return "ALIYUN";
            case HUAWEI: return "HUAWEI";
            case QINIU: return "QINIU";
            case TENCENT: return "TENCENT";
            case VOLC: return "VOLC";
        }
        return null;
    }

    public static Type forValue(String value) throws IOException {
        if (value.equals("ALIYUN")) return ALIYUN;
        if (value.equals("HUAWEI")) return HUAWEI;
        if (value.equals("QINIU")) return QINIU;
        if (value.equals("TENCENT")) return TENCENT;
        if (value.equals("VOLC")) return VOLC;
        throw new IOException("Cannot deserialize Type");
    }
}
