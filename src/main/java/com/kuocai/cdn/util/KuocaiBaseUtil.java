package com.kuocai.cdn.util;

import cn.hutool.core.util.ObjectUtil;
import com.kuocai.cdn.constant.TransactionOrderPayType;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xiaobo
 * @date 2023/2/20
 */
public class KuocaiBaseUtil {

    private KuocaiBaseUtil() {
    }

    /**
     * description: 获取时间字符串
     *
     * @param days 负数为过去，正数为未来
     * @return java.lang.String
     * @author bo
     * @date 2023/2/21 10:26 AM
     */
    public static String accessTimeString(Integer days) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, days);
        return sdf.format(calendar.getTime());
    }

    /**
     * description: 获取上周日的时间
     *
     * @return java.lang.String
     * @author bo
     * @date 2023/3/28 17:49
     */
    public static String getLastSunDayTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Calendar calendarEnd = Calendar.getInstance();
        calendarEnd.setTime(new Date());
        // 判断当前日期是否为周末，因为周末是本周第一天，如果不向后推迟一天的到的将是下周一的零点，而不是本周周一零点
        if (1 == calendarEnd.get(Calendar.DAY_OF_WEEK)) {
            calendarEnd.add(Calendar.DATE, -1);
        }
        calendarEnd.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

        return sdf.format(calendarEnd.getTime());
    }

    /**
     * description: 判断今天是不是周几
     *
     * @param weeks 最好用Calendar.
     * @return boolean
     * @throws
     * @author bo
     * @date 2023/3/28 20:51
     */
    public static boolean todayIsWeeks(int weeks) {
        Calendar calendar = Calendar.getInstance();
        int data = calendar.get(Calendar.DAY_OF_WEEK);
        return weeks == data;
    }

    public static String mysqlQueryTime(String date, String field) {
        return "to_days(" + date + ") = to_days(" + field + ")";
    }

    /**
     * 将时间格式格式为"yyyy-MM-DDTHH:mm:ss+TIMEZONE"转换为指定格式的时间字符串
     *
     * @param inputTime    待转换的时间字符串，
     * @param outputFormat 输出时间的格式，如"yyyy-MM-dd HH:mm:ss"
     * @return 转换后的时间字符串
     */
    public static String convertTimeFormat(String inputTime, String outputFormat) throws ParseException {
        // 创建输入格式化器
        SimpleDateFormat inputFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+08:00");
        // 创建输出格式化器
        SimpleDateFormat outputFormatter = new SimpleDateFormat(outputFormat);
        // 将输入的时间字符串解析为Date对象
        Date date = inputFormatter.parse(inputTime);
        // 获取系统默认时区
        TimeZone timeZone = TimeZone.getDefault();
        // 设置输出格式化器的时区
        outputFormatter.setTimeZone(timeZone);
        // 将Date对象转换为指定格式的时间字符串
        return outputFormatter.format(date);
    }

    /**
     * 获取几分钟后的时间
     *
     * @param min
     * @return
     */
    public static String getWechatOrAlipayOrderExpireTime(Integer min, String payType) {
        SimpleDateFormat sdf;
        // 过期时间
        if (TransactionOrderPayType.WECHAT_PAY.equals(payType)) {
            sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        } else {
            sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
        // 支付订单过期时间
        String timeExpire = sdf.format(new Date(System.currentTimeMillis() + 60000 * min));
        return timeExpire;
    }

    /**
     * 获取几分钟前的时间
     *
     * @param min
     * @return
     */
    public static String getNowToExpireTime(Integer min) {
        SimpleDateFormat sdf;
        // 过期时间
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 支付订单过期时间
        String timeExpire = sdf.format(new Date(System.currentTimeMillis() - 60000 * min));
        return timeExpire;
    }

    /**
     * 获取今天是周几
     *
     * @return int
     */
    public static int getNowWeekNum() {
        Calendar calendar = Calendar.getInstance();
        return (calendar.get(Calendar.DAY_OF_WEEK) - 1) == 0 ? 7 : (calendar.get(Calendar.DAY_OF_WEEK) - 1);
    }

    /**
     * 根据流量包类型 获取有效时间
     *
     * @param chargeType
     * @return
     */
    public static int getChargeValue(String chargeType) {
        switch (chargeType) {
            case "month":
                return 1;
            case "quarter":
                return 3;
            case "year":
                return 12;
            default:
                // 如果传入的 chargeType 不是 month、quarter 或 year，则返回默认值 -1 或者抛出异常
                return -1;
        }
    }

    /**
     * 计算多久月之后的时间
     *
     * @param date   时间
     * @param months 多少月
     * @return
     */
    public static LocalDateTime addMonths(LocalDateTime date, int months) {
        // 将传入的月数加到日期上，并返回加上对应月数后的新日期
        return date.plusMonths(months);
    }

    /**
     * 计算当前时间多久月之后的时间
     *
     * @param months 多少月
     * @return
     */
    public static Date getAfterMonthDate(int months) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime localDateTime = addMonths(now, months);
        return toDate(localDateTime);
    }

    /**
     * 将LocalDateTime转成Date类型
     *
     * @param localDateTime 时间
     * @return
     */
    public static Date toDate(LocalDateTime localDateTime) {
        return java.util.Date.from(localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
    }

    /**
     * @param ttl     时间
     * @param ttlType 时间单位
     * @return
     */
    public static Long toSeconds(int ttl, String ttlType) {
        if (ObjectUtil.equal(ttlType, "s")) {
            return Long.valueOf(ttl);
        } else if (ObjectUtil.equal(ttlType, "m")) {
            return Long.valueOf(ttl * 60);
        } else if (ObjectUtil.equal(ttlType, "h")) {
            return Long.valueOf(ttl * 60 * 60);
        } else if (ObjectUtil.equal(ttlType, "d")) {
            return Long.valueOf(ttl * 24 * 60 * 60);
        } else {
            return Long.valueOf(ttl);
        }
    }

    /**
     * 输入流量大小换算出Byte大小，每个量级相差1024
     *
     * @param size 大小
     */
    public static long flowUnitBack2Byte(String size) {
        if (size.endsWith("PB")) {
            String temp = size.replace("PB", "");
            return BigDecimal.valueOf(Double.valueOf(temp)).multiply(BigDecimal.valueOf(Math.pow(1024, 5))).longValue();
        }
        if (size.endsWith("TB")) {
            String temp = size.replace("TB", "");
            return BigDecimal.valueOf(Double.valueOf(temp)).multiply(BigDecimal.valueOf(Math.pow(1024, 4))).longValue();
        }
        if (size.endsWith("GB")) {
            String temp = size.replace("GB", "");
            return BigDecimal.valueOf(Double.valueOf(temp)).multiply(BigDecimal.valueOf(Math.pow(1024, 3))).longValue();
        }
        if (size.endsWith("MB")) {
            String temp = size.replace("MB", "");
            return BigDecimal.valueOf(Double.valueOf(temp)).multiply(BigDecimal.valueOf(Math.pow(1024, 2))).longValue();
        }
        if (size.endsWith("KB")) {
            String temp = size.replace("KB", "");
            return BigDecimal.valueOf(Double.valueOf(temp)).multiply(BigDecimal.valueOf(Math.pow(1024, 1))).longValue();
        }
        if (size.endsWith("B")) {
            String temp = size.replace("B", "");
            return Long.valueOf(temp);
        } else {
            return 0;
        }
    }

    /**
     * 自动换算流量单位 基础单位MB
     *
     * @param size 流量
     * @return {@code String}
     */
    public static String autoReducedFlowUnit(double size) {
        BigDecimal newSize;
        if (size >= Math.pow(1024, 5)) {
            newSize = BigDecimal.valueOf(size).divide(BigDecimal.valueOf(Math.pow(1024, 5)), 2, RoundingMode.HALF_UP);
            return newSize + "PB";
        }
        if (size >= Math.pow(1024, 4)) {
            newSize = BigDecimal.valueOf(size).divide(BigDecimal.valueOf(Math.pow(1024, 4)), 2, RoundingMode.HALF_UP);
            return newSize + "TB";
        }
        if (size >= Math.pow(1024, 3)) {
            newSize = BigDecimal.valueOf(size).divide(BigDecimal.valueOf(Math.pow(1024, 3)), 2, RoundingMode.HALF_UP);
            return newSize + "GB";
        }
        if (size >= Math.pow(1024, 2)) {
            newSize = BigDecimal.valueOf(size).divide(BigDecimal.valueOf(Math.pow(1024, 2)), 2, RoundingMode.HALF_UP);
            return newSize + "MB";
        }
        if (size >= Math.pow(1024, 1)) {
            newSize = BigDecimal.valueOf(size).divide(BigDecimal.valueOf(Math.pow(1024, 1)), 2, RoundingMode.HALF_UP);
            return newSize + "KB";
        }
        return size + "B";
    }

    /**
     * 统计展示用流量单位换算。云厂商控制台的用量页面通常按 1000 进位展示 GB/TB。
     */
    public static String autoReducedFlowUnitDecimal(double size) {
        BigDecimal newSize;
        if (size >= Math.pow(1000, 5)) {
            newSize = BigDecimal.valueOf(size).divide(BigDecimal.valueOf(Math.pow(1000, 5)), 2, RoundingMode.HALF_UP);
            return newSize + "PB";
        }
        if (size >= Math.pow(1000, 4)) {
            newSize = BigDecimal.valueOf(size).divide(BigDecimal.valueOf(Math.pow(1000, 4)), 2, RoundingMode.HALF_UP);
            return newSize + "TB";
        }
        if (size >= Math.pow(1000, 3)) {
            newSize = BigDecimal.valueOf(size).divide(BigDecimal.valueOf(Math.pow(1000, 3)), 2, RoundingMode.HALF_UP);
            return newSize + "GB";
        }
        if (size >= Math.pow(1000, 2)) {
            newSize = BigDecimal.valueOf(size).divide(BigDecimal.valueOf(Math.pow(1000, 2)), 2, RoundingMode.HALF_UP);
            return newSize + "MB";
        }
        if (size >= Math.pow(1000, 1)) {
            newSize = BigDecimal.valueOf(size).divide(BigDecimal.valueOf(Math.pow(1000, 1)), 2, RoundingMode.HALF_UP);
            return newSize + "KB";
        }
        return size + "B";
    }

    /**
     * 自动换算带宽单位 基础单位bit/s
     *
     * @param size 流量
     * @return {@code String}
     */
    public static String autoReducedBwUnit(double size) {
        BigDecimal newSize;
        if (size >= Math.pow(1000, 4)) {
            newSize = BigDecimal.valueOf(size).divide(BigDecimal.valueOf(Math.pow(1024, 4)), 2, RoundingMode.HALF_UP);
            return newSize + "Tbit/s";
        }
        if (size >= Math.pow(1000, 3)) {
            newSize = BigDecimal.valueOf(size).divide(BigDecimal.valueOf(Math.pow(1024, 3)), 2, RoundingMode.HALF_UP);
            return newSize + "Gbit/s";
        }
        if (size >= Math.pow(1000, 2)) {
            newSize = BigDecimal.valueOf(size).divide(BigDecimal.valueOf(Math.pow(1024, 2)), 2, RoundingMode.HALF_UP);
            return newSize + "Mbit/s";
        }
        if (size >= Math.pow(1000, 1)) {
            newSize = BigDecimal.valueOf(size).divide(BigDecimal.valueOf(Math.pow(1024, 1)), 2, RoundingMode.HALF_UP);
            return newSize + "Kbit/s";
        }
        return size + "bit/s";
    }

    /**
     * 获取最合适的单位
     *
     * @param size 流量
     */
    public static String getSuitableFlowUnit(Long size) {
        if (size >= Math.pow(1024, 5)) {
            return "PB";
        }
        if (size >= Math.pow(1024, 4)) {
            return "TB";
        }
        if (size >= Math.pow(1024, 3)) {
            return "GB";
        }
        if (size >= Math.pow(1024, 2)) {
            return "MB";
        }
        if (size >= Math.pow(1024, 1)) {
            return "KB";
        }
        return "B";
    }

    public static String getSuitableFlowUnitDecimal(Long size) {
        if (size >= Math.pow(1000, 5)) {
            return "PB";
        }
        if (size >= Math.pow(1000, 4)) {
            return "TB";
        }
        if (size >= Math.pow(1000, 3)) {
            return "GB";
        }
        if (size >= Math.pow(1000, 2)) {
            return "MB";
        }
        if (size >= Math.pow(1000, 1)) {
            return "KB";
        }
        return "B";
    }

    /**
     * 获取最合适的单位
     *
     * @param size 带宽
     */
    public static String getSuitableBwUnit(Long size) {
        if (size >= Math.pow(1000, 4)) {
            return "Tbit/s";
        }
        if (size >= Math.pow(1000, 3)) {
            return "Gbit/s";
        }
        if (size >= Math.pow(1000, 2)) {
            return "Mbit/s";
        }
        if (size >= Math.pow(1000, 1)) {
            return "Kbit/s";
        }
        return "bit/s";
    }

    /**
     * 进行流量单位转换
     */
    public static Map<String, Object> convertFlowUnit(List<BigDecimal> data) {
        HashMap<String, Object> result = new HashMap<>();
        if (Assert.isEmpty(data)) {
            return result;
        }
        BigDecimal big = data.stream().max(BigDecimal::compareTo).get();
        String suitableUnit = getSuitableFlowUnit(big.longValue());
        List<BigDecimal> newData = convertFlowUnit(data, suitableUnit);
        result.put("unit", suitableUnit);
        result.put("data", newData);
        return result;
    }

    public static Map<String, Object> convertFlowUnitDecimal(List<BigDecimal> data) {
        HashMap<String, Object> result = new HashMap<>();
        if (Assert.isEmpty(data)) {
            return result;
        }
        BigDecimal big = data.stream().max(BigDecimal::compareTo).get();
        String suitableUnit = getSuitableFlowUnitDecimal(big.longValue());
        List<BigDecimal> newData = convertFlowUnitDecimal(data, suitableUnit);
        result.put("unit", suitableUnit);
        result.put("data", newData);
        return result;
    }

    /**
     * 进行带宽单位转换
     */
    public static Map<String, Object> convertBwUnit(List<BigDecimal> data) {
        HashMap<String, Object> result = new HashMap<>();
        if (Assert.isEmpty(data)) {
            return result;
        }
        BigDecimal big = data.stream().max(BigDecimal::compareTo).get();
        String suitableUnit = getSuitableBwUnit(big.longValue());
        List<BigDecimal> newData = convertBwUnit(data, suitableUnit);
        result.put("unit", suitableUnit);
        result.put("data", newData);
        return result;
    }

    /**
     * 进行带宽单位转换
     */
    public static List<BigDecimal> convertFlowUnit(List<BigDecimal> data, String unit) {
        double rate = 1;
        switch (unit) {
            case "KB":
                rate = Math.pow(1024, 1);
                break;
            case "MB":
                rate = Math.pow(1024, 2);
                break;
            case "GB":
                rate = Math.pow(1024, 3);
                break;
            case "TB":
                rate = Math.pow(1024, 4);
                break;
            case "PB":
                rate = Math.pow(1024, 5);
                break;
        }
        double finalRate = rate;
        return data.stream()
                .map(d -> d.divide(BigDecimal.valueOf(finalRate)).setScale(2, RoundingMode.UP))
                .collect(Collectors.toList());
    }

    public static List<BigDecimal> convertFlowUnitDecimal(List<BigDecimal> data, String unit) {
        double rate = 1;
        switch (unit) {
            case "KB":
                rate = Math.pow(1000, 1);
                break;
            case "MB":
                rate = Math.pow(1000, 2);
                break;
            case "GB":
                rate = Math.pow(1000, 3);
                break;
            case "TB":
                rate = Math.pow(1000, 4);
                break;
            case "PB":
                rate = Math.pow(1000, 5);
                break;
        }
        double finalRate = rate;
        return data.stream()
                .map(d -> d.divide(BigDecimal.valueOf(finalRate)).setScale(2, RoundingMode.UP))
                .collect(Collectors.toList());
    }

    /**
     * 进行带宽单位转换
     */
    public static List<BigDecimal> convertBwUnit(List<BigDecimal> data, String unit) {
        double rate = 1;
        switch (unit) {
            case "Kbit/s":
                rate = Math.pow(1000, 1);
                break;
            case "Mbit/s":
                rate = Math.pow(1000, 2);
                break;
            case "Gbit/s":
                rate = Math.pow(1000, 3);
                break;
            case "Tbit/s":
                rate = Math.pow(1000, 4);
                break;
        }
        double finalRate = rate;
        return data.stream()
                .map(d -> d.divide(BigDecimal.valueOf(finalRate)).setScale(2, RoundingMode.DOWN))
                .collect(Collectors.toList());
    }

    /**
     * 获取使用百分比
     */
    public static String percentage(double dividend, double divisor) {
        return BigDecimal.valueOf(dividend * 100).divide(BigDecimal.valueOf(divisor), 1, RoundingMode.HALF_UP).doubleValue() + "%";
    }

    /**
     * 流量单位转换
     */
    public static BigDecimal flowUnitConversion(long flow, String unit) {
        double divisor = 0;
        switch (unit) {
            case "KB":
                divisor = Math.pow(1024, 1);
                return new BigDecimal(Double.toString(flow)).divide(new BigDecimal(Double.toString(divisor)), 2, RoundingMode.CEILING);
            case "MB":
                divisor = Math.pow(1024, 2);
                return new BigDecimal(Double.toString(flow)).divide(new BigDecimal(Double.toString(divisor)), 2, RoundingMode.CEILING);
            case "GB":
                divisor = Math.pow(1024, 3);
                return new BigDecimal(Double.toString(flow)).divide(new BigDecimal(Double.toString(divisor)), 2, RoundingMode.CEILING);
            case "TB":
                divisor = Math.pow(1024, 4);
                return new BigDecimal(Double.toString(flow)).divide(new BigDecimal(Double.toString(divisor)), 2, RoundingMode.CEILING);
            case "PB":
                divisor = Math.pow(1024, 5);
                return new BigDecimal(Double.toString(flow)).divide(new BigDecimal(Double.toString(divisor)), 2, RoundingMode.CEILING);
            default:
                return BigDecimal.ZERO;
        }
    }


    /**
     * 解析微信公众号token验证
     */
    public static String sha1(String data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        //把字符串转为字节数组
        byte[] b = data.getBytes();
        //使用指定的字节来更新我们的摘要
        md.update(b);
        //获取密文  （完成摘要计算）
        byte[] b2 = md.digest();
        //获取计算的长度
        int len = b2.length;
        //16进制字符串
        String str = "0123456789abcdef";
        //把字符串转为字符串数组
        char[] ch = str.toCharArray();
        //创建一个40位长度的字节数组
        char[] chs = new char[len * 2];
        //循环20次
        for (int i = 0, k = 0; i < len; i++) {
            //获取摘要计算后的字节数组中的每个字节
            byte b3 = b2[i];
            // >>>:无符号右移
            // &:按位与
            //0xf:0-15的数字
            chs[k++] = ch[b3 >>> 4 & 0xf];
            chs[k++] = ch[b3 & 0xf];
        }
        //字符数组转为字符串
        return new String(chs);
    }

    /**
     * 获取最合适的单位
     *
     * @param size 流量
     */
    public static String getCacheTimeUnit(Integer size) {
        if (size >= 60 * 60 * 24) {
            return "d";
        }
        if (size >= Math.pow(60, 2)) {
            return "h";
        }
        if (size >= Math.pow(60, 1)) {
            return "m";
        }
        return "s";
    }

    /**
     * 获取最合适的单位
     *
     * @param size 流量
     */
    public static int getUnitCacheTime(Integer size) {
        if (size >= 60 * 60 * 24) {
            return (int) (size / (60 * 60 * 24));
        }
        if (size >= Math.pow(60, 2)) {
            return (int) (size / Math.pow(60, 2));
        }
        if (size >= Math.pow(60, 1)) {
            return (int) (size / Math.pow(60, 1));
        }
        return size;
    }

    /**
     * 数据分片
     *
     * @param list       目标列表
     * @param numBatches 分批批次数
     */
    public static <T> List<List<T>> partition(List<T> list, int numBatches) {
        if (numBatches <= 0) {
            throw new IllegalArgumentException("Number of batches must be a positive integer");
        }
        int totalSize = list.size();
        int batchSize = (int) Math.ceil((double) totalSize / numBatches);
        List<List<T>> batches = new ArrayList<>(numBatches);
        int fromIndex = 0;
        for (int i = 0; i < numBatches; i++) {
            int remainingSize = totalSize - fromIndex;
            int batchSizeForThisBatch = Math.min(remainingSize, batchSize);
            int toIndex = fromIndex + batchSizeForThisBatch;
            List<T> batch = new ArrayList<>(list.subList(fromIndex, toIndex));
            batches.add(batch);
            fromIndex = toIndex;
        }
        return batches;
    }

    public static boolean fileExists(String path) {
        File file = new File(path);
        return file.exists();
    }
}
