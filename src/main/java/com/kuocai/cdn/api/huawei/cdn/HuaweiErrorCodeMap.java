package com.kuocai.cdn.api.huawei.cdn;


import java.util.HashMap;
import java.util.Map;

/**
 * 错误码Map
 */
public class HuaweiErrorCodeMap {

    /**
     * 错误码Map
     */
    private static Map<String, String> codeMap;

    static {
        codeMap = new HashMap<>(100);
        // 200
        codeMap.put("CDN.0005", "超出最大限制。");
        codeMap.put("CDN.0016", "不支持的业务类型。");
        codeMap.put("CDN.0101", "加速域名已经添加。");
        codeMap.put("CDN.0102", "加速域名审核失败。");
        codeMap.put("CDN.0103", "源站审核失败。");
        codeMap.put("CDN.0104", "加速域名已达到上限。");
        codeMap.put("CDN.0105", "加速域名不存在。");
        codeMap.put("CDN.0106", "当前域名状态无法操作。");
        codeMap.put("CDN.0107", "回源host审核失败。");
        codeMap.put("CDN.0108", "域名不属于当前租户。");
        codeMap.put("CDN.0109", "源站域名不能与加速域名相同。");
        codeMap.put("CDN.0110", "URL数超出预热刷新限制。");
        codeMap.put("CDN.0114", "CDN服务未开通。");
        codeMap.put("CDN.0115", "域名已被封禁。");
        codeMap.put("CDN.0116", "域名已被锁定。");
        codeMap.put("CDN.0117", "源站域名为回源HOST，源站不能为IP地址形式。");
        codeMap.put("CDN.0120", "域名已被其他用户添加。");
        codeMap.put("CDN.0121", "输入的IP不能是内网IP。");
        codeMap.put("CDN.0126", "仅停用，配置失败，审核失败，同步失败的域名才可进行删除操作。");
        codeMap.put("CDN.0127", "域名格式错误。");
        codeMap.put("CDN.0128", "业务类型错误。");
        codeMap.put("CDN.0129", "源站类型错误。");
        codeMap.put("CDN.0130", "源站IP个数超出限制。");
        codeMap.put("CDN.0131", "源站域名个数超出限制。");
        codeMap.put("CDN.0132", "回源Host类型错误。");
        codeMap.put("CDN.0133", "回源host错误。");
        codeMap.put("CDN.0134", "缓存规则的权重应为1到100之间的整数。");
        codeMap.put("CDN.0135", "缓存规则类型错误。");
        codeMap.put("CDN.0136", "时间范围必须在0到365天内。");
        codeMap.put("CDN.0137", "缓存过期规则设置格式不合法。");
        codeMap.put("CDN.0138", "防盗链类型错误。");
        codeMap.put("CDN.0139", "防盗链域名数量必须在1~100之间。");
        codeMap.put("CDN.0141", "域名未备案。");
        codeMap.put("CDN.0142", "不支持https协议。");
        codeMap.put("CDN.0143", "泛域名所有权未验证。");
        codeMap.put("CDN.0145", "只有处于已开启状态的域名才可执行此操作。");
        codeMap.put("CDN.0147", "OBS桶做为源站，不能修改回源host。");
        codeMap.put("CDN.0148", "OBS桶做源站，不能添加备源站。");
        codeMap.put("CDN.0156", "源站无效。");
        codeMap.put("CDN.0163", "域名有特殊配置。");
        codeMap.put("CDN.0201", "域名错误。");
        codeMap.put("CDN.0202", "统计时间错误。");
        codeMap.put("CDN.0203", "采样间隔错误。");
        codeMap.put("CDN.0401", "证书删除错误。");
        codeMap.put("CDN.0402", "证书或私钥不能为空。");
        codeMap.put("CDN.0403", "证书格式需要为pem。");
        codeMap.put("CDN.0404", "私钥格式需要为pem。");
        codeMap.put("CDN.0405", "证书与私钥不匹配。");
        codeMap.put("CDN.0406", "证书与域名不匹配。");
        codeMap.put("CDN.0407", "证书错误。");
        codeMap.put("CDN.0408", "证书已过期。");
        codeMap.put("CDN.0409", "证书剩余有效期不足24小时。");
        codeMap.put("CDN.0410", "证书链无法补齐。");
        codeMap.put("CDN.0411", "证书状态不正确。");
        codeMap.put("CDN.0412", "证书或私钥内容长度超出限制。");
        codeMap.put("CDN.0413", "证书还未生效。");
        codeMap.put("CDN.0414", "证书私钥错误。");
        // 400
        codeMap.put("CDN.0001", "参数格式错误（格式错误或者缺失参数）。");
        // 401
        codeMap.put("CDN.0002", "用户未鉴权。");
        // 403
        codeMap.put("CDN.0004", "用户权限不足。");
        codeMap.put("CDN.0020", "对应用户无该企业项目授权。");
        codeMap.put("CDN.0021", "对应用户细粒度鉴权不通过，无操作权限。");
        // 404
        codeMap.put("CDN.0003", "对象不存在。");
        // 500
        codeMap.put("CDN.0000", "系统内部错误。");
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
