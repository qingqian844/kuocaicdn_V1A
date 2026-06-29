package com.kuocai.cdn.util;

import cn.hutool.core.util.RandomUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 支付的一些通用方法
 */
public class PayUtils {


    /**
     * 生成商户订单号
     * DateTimeFormatter 线程安全的
     */
    public static String getOutTradeNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddhhmmss");
        LocalDateTime dateTime = LocalDateTime.now();
        String currentTime = dateTime.format(formatter);
        String randomNumbers = RandomUtil.randomNumbers(4);
        return currentTime + randomNumbers;
    }
}
