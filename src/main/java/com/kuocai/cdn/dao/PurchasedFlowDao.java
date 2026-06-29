package com.kuocai.cdn.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuocai.cdn.entity.PurchasedFlow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * (PurchasedFlow)数据库访问层
 *
 * @author makejava
 * @since 2023-04-01 17:01:41
 */
@Repository
public interface PurchasedFlowDao extends BaseMapper<PurchasedFlow> {

    /**
     * description: 获取第二天是否到期(这里只查询两个字段，方便后期加索引不需要重写方法)
     *
     * @param inTime 时间
     * @return List<Map < String, Object>>
     * @author bo
     */
    @Select("SELECT id, deadline \n" +
            "FROM purchased_flow \n" +
            "WHERE status ='on_used' \n" +
            "  AND DATE(deadline) = DATE(#{inTime}) \n" +
            "  AND flow_package_name NOT LIKE '%赠送%' \n" +
            "  AND flow_package_name NOT LIKE '%邀请%' \n" +
            "  AND flow_package_name NOT LIKE '%受邀%' \n" +
            "  AND flow_package_name NOT LIKE '%福利%' ")
    List<Map<String, Object>> getNextDayDeadline(String inTime);


    /**
     * 获取已过期的流量包
     */
    @Select("SELECT * FROM purchased_flow WHERE status != 'on_expired' AND deadline < NOW()")
    List<PurchasedFlow> getOveredPackage();

    @Select("select transaction_order_id\n" +
            "from purchased_flow\n" +
            "where create_time > #{nowTime} and used_flow = 0 and id = #{id}")
    Long checkRefund(@Param("nowTime") String nowTime, @Param("id") Long id);
}


