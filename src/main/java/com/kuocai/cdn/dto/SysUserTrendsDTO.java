package com.kuocai.cdn.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xiaobo
 * @date 2023/2/21
 */
@Data
@Accessors(chain = true)
public class SysUserTrendsDTO {

    /**
     * 总用户数
     */
    private Long totalNumberUsers;

    /**
     * 昨日活跃数
     */
    private Long numberActiveYesterday;

    /**
     * 昨日充值
     */
    private Long payYesterday;

    /**
     * 昨日注销
     */
    private Long yesterdayCancellation;

    /**
     * 今日新增用户数
     */
    private Long todayNewUsers;

    /**
     * 前天活跃
     */
    private Long dayBeforeYesterdayIsActive;

    /**
     * 前天充值
     */
    private Long dayBeforeYesterdayPay;
}
