package com.kuocai.cdn.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author xiaobo
 * @date 2023/7/22
 */
@Data
public class OrderCollectVo implements Serializable {

    /**
     * 今日消费
     */
    private BigDecimal nowAmount;

    /**
     * 昨日消费
     */
    private BigDecimal yesterdayAmount;

    /**
     * 近7天消费
     */
    private BigDecimal sevenAmount;

    /**
     * 近30天消费
     */
    private BigDecimal thirtyAmount;

    /**
     * 总消费
     */
    private BigDecimal allAmount;
}
