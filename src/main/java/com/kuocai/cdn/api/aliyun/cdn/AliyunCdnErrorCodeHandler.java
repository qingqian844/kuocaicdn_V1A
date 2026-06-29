package com.kuocai.cdn.api.aliyun.cdn;

import com.aliyun.tea.TeaException;
import com.aliyun.tea.TeaUnretryableException;
import com.kuocai.cdn.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class AliyunCdnErrorCodeHandler {
    private static final Map<String, String> errorCodeMap;

    static {
        errorCodeMap = new HashMap<>(10);
        errorCodeMap.put("DomainOwnerVerifyFail", "域名所有者验证失败");
        errorCodeMap.put("InvalidFile", "文件不存在");
        errorCodeMap.put("VerifyError", "验证失败");
        errorCodeMap.put("Throttling.User", "用户调用频率超限，请稍后重试");
        errorCodeMap.put("InvalidFunctions.Malformed", "功能配置错误");
        errorCodeMap.put("FunctionMutex", "功能互斥");
        errorCodeMap.put("DomainAlreadyExist", "域名在阿里云中已存在，请删除后再添加");
        errorCodeMap.put("InvalidParameter", "参数错误");
        errorCodeMap.put("SourceInBlacklist", "源站在黑名单中");
        errorCodeMap.put("ServiceBusy", "上游服务繁忙，请稍后再试");
        errorCodeMap.put("InvalidSSLPub", "SSL证书公钥错误，请重新填写");
        errorCodeMap.put("InvalidSSLPri", "SSL证书私钥错误，请填写正确的私钥");
        errorCodeMap.put("Certificate.MissMatch", "SSL证书与私钥不匹配");
        errorCodeMap.put("InvalidArgValue.Malformed", "参数格式错误，请检查后重试");
        errorCodeMap.put("DomainNotRegistration", "域名尚未备案");
        errorCodeMap.put("InvalidObjectPath.Malformed", "目录路径错误");
        errorCodeMap.put("InvalidSource.Content.Malformed", "源站地址格式错误");
        errorCodeMap.put("InvalidEndTime.Mismatch", "请检查时间设置是否正确，结束时间不能小于或等于开始时间");
        errorCodeMap.put("InvalidSources.Malformed", "源站地址格式错误");
    }

    public static String getErrorDescription(String errorCode) {
        return errorCodeMap.getOrDefault(errorCode, errorCode);
    }

    public static String getErrorDescription(TeaException e) {
        return errorCodeMap.getOrDefault(e.getCode(), e.getMessage());
    }

    public static BusinessException catchException(Exception error) {
        log.error("阿里云CDN接口错误 throwable", error);
        if (error instanceof TeaException) {
            TeaException teaException = (TeaException) error;
            log.error("阿里云CDN接口错误：{} : {}", teaException.getCode(), teaException.getMessage());
            return new BusinessException(AliyunCdnErrorCodeHandler.getErrorDescription(teaException));
        }
        if (error instanceof TeaUnretryableException) {
            TeaUnretryableException teaUnretryableException = (TeaUnretryableException) error;
            log.error("阿里云CDN接口错误：{}, {}", teaUnretryableException.getMessage(), teaUnretryableException.getLastRequest());
            return new BusinessException("操作失败，请稍后再试");
        }
        log.error("阿里云CDN接口错误：", error);
        return new BusinessException("操作失败，请稍后再试");
    }
}
