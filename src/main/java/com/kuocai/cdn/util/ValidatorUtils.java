package com.kuocai.cdn.util;

import java.util.regex.Pattern;

/**
 * 常用正则验证工具类
 *
 * @author XUEW
 * @date 2021/3/13 下午8:33
 */
public class ValidatorUtils {

    /**
     * 正则表达式：验证用户名
     */
//    public static final String REGEX_USERNAME = "^[a-zA-Z]\\w{5,20}$";

    /**
     * 正则表达式：验证密码
     */
//    public static final String REGEX_PASSWORD = "^[A-Za-z0-9~!@#$%^&*()_+`\\-={}:\";'<>?,.\\/]{8,16}$";

    /**
     * 正则表达式：验证手机号
     */
//    public static final String REGEX_MOBILE = "^1[3456789]\\d{9}$";

    /**
     * 正则表达式：验证邮箱
     */
//    public static final String REGEX_EMAIL = "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
//    public static final String REGEX_EMAIL = "^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$";

    /**
     * 正则表达式：验证汉字
     */
//    public static final String REGEX_CHINESE = "^[\u4e00-\u9fa5],{0,}$";

    /**
     * 正则表达式：验证身份证
     */
//    public static final String REGEX_ID_CARD = "^[1-9]\\d{5}(?:18|19|20)\\d{2}(?:0[1-9]|10|11|12)(?:0[1-9]|[1-2]\\d|30|31)\\d{3}[0-9Xx]$";

    /**
     * 正则表达式：验证URL
     */
//    public static final String REGEX_URL = "http(s)?://([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=]*)?";

    /**
     * 正则表达式：验证IP地址
     */
    public static final String REGEX_IP_ADDR = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";


    /**
     * 用户名正则
     */
    public static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z]\\w{5,20}$");

    /**
     * 密码正则
     */
    public static final Pattern PASSWORD_PATTERN = Pattern.compile("^[A-Za-z0-9~!@#$%^&*()_+`\\-={}:\";'<>?,./]{8,16}$");

    /**
     * 手机号正则
     */
    public static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3456789]\\d{9}$");

    /**
     * 邮箱正则
     */
    public static final Pattern EMAIL_PATTERN = Pattern.compile("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$");

    /**
     * 汉字正则
     */
    public static final Pattern CHINESE_PATTERN = Pattern.compile("^[\u4e00-\u9fa5],{0,}$");

    /**
     * 身份证正则
     */
    public static final Pattern ID_CARD_PATTERN = Pattern.compile("^[1-9]\\d{5}(?:18|19|20)\\d{2}(?:0[1-9]|10|11|12)(?:0[1-9]|[1-2]\\d|30|31)\\d{3}[0-9Xx]$");

    /**
     * URL 正则
     */
    public static final Pattern URL_PATTERN = Pattern.compile("http(s)?://([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=]*)?");

    /**
     * IP 地址正则
     */
    public static final Pattern IP_ADDR_PATTERN = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    /**
     * IPv4 正则
     */
    public static final Pattern IPV4_PATTERN = Pattern.compile("^((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}$");

    /**
     * scheme uri 正则
     */
    public static final Pattern SCHEME_URI_PATTERN = Pattern.compile("^(http|https)://\\S+$");

    /**
     * 校验用户名
     *
     * @param username 用户名
     * @return 校验通过返回true，否则返回false
     */
    public static boolean isUsername(String username) {
        return USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * 校验密码
     *
     * @param password 密码
     * @return 校验通过返回true，否则返回false
     */
    public static boolean isPassword(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * 校验手机号
     *
     * @param phone 手机号
     * @return 校验通过返回true，否则返回false
     */
    public static boolean isPhone(String phone) {
        return MOBILE_PATTERN.matcher(phone).matches();
    }

    /**
     * 校验邮箱
     *
     * @param email 邮箱
     * @return 校验通过返回true，否则返回false
     */
    public static boolean isEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 校验汉字
     *
     * @param chinese 汉字
     * @return 校验通过返回true，否则返回false
     */
    public static boolean isChinese(String chinese) {
        return CHINESE_PATTERN.matcher(chinese).matches();
    }

    /**
     * 校验身份证
     *
     * @param idCard 身份证
     * @return 校验通过返回true，否则返回false
     */
    public static boolean isIDCard(String idCard) {
        return ID_CARD_PATTERN.matcher(idCard).matches();
    }

    /**
     * 校验URL
     *
     * @param url URL
     * @return 校验通过返回true，否则返回false
     */
    public static boolean isUrl(String url) {
        return URL_PATTERN.matcher(url).matches();
    }

    /**
     * 校验IP地址
     *
     * @param ip ip
     * @return
     */
    public static boolean isIPAddress(String ip) {
        return IP_ADDR_PATTERN.matcher(ip).matches();
    }

    public static boolean isIncludeHttpOrHttps(String url) {
//        return Pattern.matches("^(http|https):\\/\\/[^\\s]+$", url);
        return SCHEME_URI_PATTERN.matcher(url).matches();
    }

    public static boolean isIpv4(String str) {
        return IPV4_PATTERN.matcher(str).matches();
    }
}
