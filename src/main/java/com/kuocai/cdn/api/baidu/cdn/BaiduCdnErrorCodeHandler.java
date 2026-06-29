package com.kuocai.cdn.api.baidu.cdn;

import com.baidubce.BceServiceException;
import com.kuocai.cdn.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class BaiduCdnErrorCodeHandler {
    private static final Map<String, String> errorCodeMap;

    static {
        errorCodeMap = new HashMap<>(10);
        errorCodeMap.put("DuplicateDomain", "域名已存在，请联系客服");
        // ？？
        errorCodeMap.put("InvalidDomainAttribute", "域名属性验证失败");
    }

    public static String getErrorDescription(String errorCode) {
        return errorCodeMap.getOrDefault(errorCode, errorCode);
    }

    public static String getErrorDescription(BceServiceException e) {
        return errorCodeMap.getOrDefault(e.getErrorCode(), e.getErrorMessage());
    }

    public static BusinessException catchException(Exception error) {
        log.error("百度CDN接口错误 throwable", error);
        if (error instanceof BceServiceException) {
            BceServiceException bceServiceException = (BceServiceException) error;
            log.error("百度CDN接口错误：{} : {} - {}", bceServiceException.getErrorType(), bceServiceException.getErrorCode(), bceServiceException.getErrorMessage());
            return new BusinessException(getErrorDescription(bceServiceException));
        }
        log.error("百度CDN接口错误：", error);
        return new BusinessException("操作失败，请稍后再试");
    }
}
