package com.kuocai.cdn.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuocai.cdn.entity.TransactionOrder;
import com.kuocai.cdn.vo.OrderCollectVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * (TransactionOrder)数据库访问层
 *
 * @author makejava
 * @since 2023-03-10 10:10:55
 */
@Repository
public interface TransactionOrderDao extends BaseMapper<TransactionOrder> {

    /**
     * description: 得到没有支付且过期的订单(流量账单扣款不能设置过期)
     *
     * @param userId     用户Id
     * @param expireTime 现在-过期时间
     * @return java.util.List<com.kuocai.cdn.entity.TransactionOrder>
     * @author bo
     * @date 2023/4/8 16:54
     */
    @Select("<script> " +
            "SELECT * " +
            "FROM transaction_order " +
            "WHERE status = 'WAIT_BUYER_PAY' " +
            "AND order_type != 'flow_deduction' " +
            "AND create_time &lt; #{expireTime} " +
            "<if test='userId != null'> " +
            "AND user_id = #{userId} " +
            "</if>" +
            "</script>")
    List<TransactionOrder> getNoPayAndExpireOrder(@Param("userId") Long userId, @Param("expireTime") String expireTime);

    @Select("select count(1) from transaction_order where month(create_time) = month(now()) and order_type = 'balance_withdrawal' and user_id = #{userId}")
    Integer toTestWhetherThisMonth(Long userId);

    @Select("<script> " +
            "SELECT * " +
            "FROM transaction_order " +
            "WHERE status = 'REFUNDING' " +
            "<if test='userId != null'> " +
            "AND user_id = #{userId} " +
            "</if>" +
            "</script>")
    List<TransactionOrder> getRefundingOrder(@Param("userId") Long userId);


    @Select("SELECT o.* FROM transaction_order o LEFT JOIN sys_user u ON o.user_id = u.id  WHERE o.user_name != u.user_name;")
    List<TransactionOrder> getRename();

    /**
     * description: getAmount
     *
     * @param userId  用户id
     * @param nowTime 当前时间
     * @return java.util.Map<java.lang.String, java.math.BigDecimal>
     * @date 2023/7/22 14:54
     */
    @Select("select (select sum(amount)\n" +
            "        from transaction_order\n" +
            "        where (order_type = 'flow_package' or order_type = 'flow_deduction')\n" +
            "          and status = 'TRADE_SUCCESS'\n" +
            "          and user_id = #{userId}\n" +
            "          and DATE(pay_time) =  DATE(#{nowTime})) as nowAmount,\n" +
            "       (select sum(amount)\n" +
            "        from transaction_order\n" +
            "        where (order_type = 'flow_package' or order_type = 'flow_deduction')\n" +
            "          and status = 'TRADE_SUCCESS'\n" +
            "          and user_id = #{userId}\n" +
            "          and DATE(pay_time) = DATE(subdate(#{nowTime},1))) as yesterdayAmount,\n" +
            "       (select sum(amount)\n" +
            "        from transaction_order\n" +
            "        where (order_type = 'flow_package' or order_type = 'flow_deduction')\n" +
            "          and status = 'TRADE_SUCCESS'\n" +
            "          and user_id = #{userId}\n" +
            "          and DATE(pay_time) >= DATE(subdate(#{nowTime},7))) as sevenAmount,\n" +
            "       (select sum(amount)\n" +
            "        from transaction_order\n" +
            "        where (order_type = 'flow_package' or order_type = 'flow_deduction')\n" +
            "          and status = 'TRADE_SUCCESS'\n" +
            "          and user_id = #{userId}\n" +
            "          and DATE(pay_time) >= DATE(subdate(#{nowTime},30))) as thirtyAmount,\n" +
            "       (select sum(amount)\n" +
            "        from transaction_order\n" +
            "        where (order_type = 'flow_package' or order_type = 'flow_deduction')\n" +
            "          and status = 'TRADE_SUCCESS'\n" +
            "          and user_id = #{userId}) as allAmount")
    OrderCollectVo getAmount(@Param("userId") Long userId, @Param("nowTime") String nowTime);


}

