package com.kuocai.cdn.util;

import cn.hutool.core.date.DateUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * 日期时间工具
 *
 * @author XUEW
 */
public class KuocaiDateUtil {

    /**
     * 判断时间是否超过了指定的天数
     *
     * @param updateTime 目标时间
     * @param days       天数
     */
    public static boolean isOverDays(Date updateTime, int days) {
        // 计算时间差（毫秒）
        long timeDifference = new Date().getTime() - updateTime.getTime();
        // 将时间差转换为天数
        long daysDifference = TimeUnit.MILLISECONDS.toDays(timeDifference);
        // 判断时间差是否超过7天
        return daysDifference > days; // 超过7天
    }

    /**
     * 将日期添加指定天数后返回新的日期
     */
    public static String addDaysToDate(Date date, int days) {
        // 创建一个日历实例
        Calendar calendar = Calendar.getInstance();
        // 将传入的日期设置为日历的时间
        calendar.setTime(date);
        // 添加指定天数
        calendar.add(Calendar.DAY_OF_MONTH, days);
        // 使用 SimpleDateFormat 将日期转换为字符串
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 返回更新后的日期字符串
        return dateFormat.format(calendar.getTime());
    }

    /**
     * 将日期字符串转换为日期
     *
     * @param dateStr 日期字符串
     * @return Date
     */
    public static Date toDate(String dateStr) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return dateFormat.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将日期字符串转换为日期
     *
     * @param dateStr 日期字符串
     * @return Date
     */
    public static Date strToDate(String dateStr) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT' Z", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        try {
            return dateFormat.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将日期转换为 ISO 8601 格式的字符串
     *
     * @param date 日期
     * @return ISO 8601 格式的字符串
     */
    public static String toISOString(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(date);
    }

    public static String getCurrentTime() {
        return DateUtil.format(DateUtil.date(), "yyyy-MM-dd HH:mm:ss");
    }

    public static Date isoStrToDate(String dateStr) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        try {
            return dateFormat.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    public static String toDateStr(Date date) {
        return DateUtil.format(date, "yyyy-MM-dd HH:mm:ss");
    }
}
