package com.kuocai.cdn.api.cdnetworks.cdn;

import com.kuocai.cdn.api.cdnetworks.cdn.vo.DefaultErrorVO;

import java.util.HashMap;
import java.util.Map;

public class CdnetworksErrorCodeHandler {
    private static final Map<String, String> errorCodeMap;

    static {
        errorCodeMap = new HashMap<>(10);
        errorCodeMap.put("DomainNotEnabled", "域名未启用");
        errorCodeMap.put("HeaderDirectionError", "有 Header 为特定值时，Header 方向有错误");
    }

    public static String getErrorDescription(String errorCode) {
        return errorCodeMap.getOrDefault(errorCode, errorCode);
    }

    public static String getErrorDescription(DefaultErrorVO e) {
        return errorCodeMap.getOrDefault(e.getCode(), e.getMessage());
    }
}
