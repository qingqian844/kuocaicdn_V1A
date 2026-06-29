package com.kuocai.cdn.api.volcengine.cdn;


import java.util.HashMap;
import java.util.Map;

/**
 * 错误码Map
 */
public class VolcengineErrorCodeMap {

    /**
     * 错误码Map
     */
    private static Map<String, String> codeMap;

    static {
        codeMap = new HashMap<>(100);
        codeMap.put("NotFound.Domain", "账户下无此域名。");
        codeMap.put("AccessDenied.IAMUnauthorized", "子账号无权限访问指定的域名、项目或使用该 API。");
        codeMap.put("InvalidParameter.<param>", "参数错误。");
        codeMap.put("InvalidParameter.Certificate", "证书内容解析错误。");
        codeMap.put("InvalidParameter.Certificate.KeyNotMatch", "证书与私钥不匹配。");
        codeMap.put("InvalidParameter.Https.CertInfo.ChainMissing", "域名配置失败，证书链不完整。");
        codeMap.put("InternalError.<param>", "内部错误，请稍后重试或联系工程师进行进一步咨询。");
        codeMap.put("InvalidParameter.BillingCode", "参数错误：BillingCode 内容不合法。");
        codeMap.put("InvalidParameter.BillingRegion", "参数错误：BillingRegion。");
        codeMap.put("CDN.ResourceNotFound", "账户下无此域名：xxx,xxx");
        codeMap.put("CDN.PermissionDenied", "您没有权限执行该操作。");
        codeMap.put("CDN.InvalidParameters", "参数错误");
        codeMap.put("CDN.InvalidParam.Certificate", "Certificate参数错误");
        codeMap.put("CDN.InvalidParam.PrivateKey", "PrivateKey参数错误");
        codeMap.put("CDN.InvalidParam.Source", "Source参数错误");
        codeMap.put("CDN.InvalidParam.CertificateChainError", "证书链无法补齐");
        codeMap.put("CDN.InternalError", "未知错误，请联系技术支持");
        codeMap.put("CDN.InvalidParam.CertificateExpired", "证书已过期");
        codeMap.put("CDN.InvalidParam.BillingCode", "BillingCode 参数错误");
        codeMap.put("CDN.InvalidParam.BillingRegion", "BillingRegion 参数错误");
        codeMap.put("CDN.InvalidParam.ExportType", "ExportType 参数错误");
    }

    /**
     * 根据错误码获取错误信息
     *
     * @param code 错误码
     * @return 错误信息
     */
    public static String getMsg(String code) {
        return codeMap.get(code);
    }
}
