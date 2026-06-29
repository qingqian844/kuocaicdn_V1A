package com.kuocai.cdn.util;

import cn.hutool.http.HttpUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * 浏览器工具类
 *
 * @author XUEW
 * @date 下午9:03 2023/2/12
 */
public class BrowserUtils {

    private static final String UNKNOWN = "unknown";

    /**
     * 根据请求获取ip地址
     */
    public static String getIp(HttpServletRequest request) throws UnknownHostException {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        String comma = ",";
        String localhost = "127.0.0.1";
        if (ip.contains(comma)) {
            ip = ip.split(",")[0];
        }
        if (localhost.equals(ip)) {
            // 获取本机真正的ip地址
            ip = InetAddress.getLocalHost().getHostAddress();
        }
        return ip;
    }

    /**
     * 获取IP的详细信息
     *
     * @param ip ip地址
     * @return 返回值
     */
    public static Map<String, Object> getIpInfo(String ip) {
        String fullUrl = "https://www.cz88.net/api/cz88/ip/base?ip=" + ip;
        String result = HttpUtil.createGet(fullUrl).timeout(50000).execute().body();
        JSONObject jsonObject = JSONUtil.parseObj(result);
        if (!(Boolean) jsonObject.get("success")) {
            return null;
        }
        JSONObject data = jsonObject.getJSONObject("data");
        Map<String, Object> ipInfo = new HashMap<>();
        for (String key : data.keySet()) {
            ipInfo.put(key, data.get(key));
        }
        return ipInfo;
    }

    /**
     * 获取浏览器相关信息
     */
    public static JSONObject getBrowser(HttpServletRequest request) {
        String browser = request.getHeader("User-Agent");
        UserAgent ua = UserAgentUtil.parse(browser);
        String browserType = ua.getBrowser().toString();
        String version = ua.getVersion();
        String engine = ua.getEngine().toString();
        String engineVersion = ua.getEngineVersion();
        String os = ua.getOs().toString();
        String platform = ua.getPlatform().toString();
        Integer mobile = ua.isMobile() ? 1 : 0;
        JSONObject json = new JSONObject();
        json.set("browserType", browserType).set("platform", platform).set("mobile", mobile).set("os", os).set("platform", platform).set("version", version)
                .set("engine", engine).set("engineVersion", engineVersion);
        return json;
    }

    public static String getDomainByUrl(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String domain = uri.getHost();
//        if (domain.startsWith("www.")) {
//            domain = domain.substring(4);
//        }
        return domain;
    }
}
