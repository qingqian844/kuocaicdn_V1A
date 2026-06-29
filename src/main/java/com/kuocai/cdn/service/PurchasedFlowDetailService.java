package com.kuocai.cdn.service;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.dao.PurchasedFlowDetailDao;
import com.kuocai.cdn.entity.PurchasedFlowDetail;
import com.kuocai.cdn.service.base.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * (PurchasedFlowDetail)服务
 *
 * @author XUEW
 * @since 2023-04-02 19:40:21
 */
@Service
public class PurchasedFlowDetailService extends BaseService<PurchasedFlowDetail> {

    @Autowired
    protected PurchasedFlowDetailDao dao;

    /**
     * 根据流量包购买记录查询使用明细
     */
    public List<PurchasedFlowDetail> queryBuyPurchasedFlowId(Long purchasedFlowId, DateTime start, DateTime end) {
        QueryWrapper<PurchasedFlowDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("purchased_flow_id", purchasedFlowId);
        queryWrapper.ge("create_time", start);
        queryWrapper.le("create_time", end);
        queryWrapper.orderByAsc("create_time");
        return queryByWrapper(queryWrapper);
    }

    /**
     * 查询在指定时间段内有流量消费记录的用户ID集合
     */
    public Set<Long> getUsersWithRecordsInPeriod(Date startDate, Date endDate) {
        QueryWrapper<PurchasedFlowDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("create_time", startDate);
        queryWrapper.le("create_time", endDate);
        queryWrapper.isNotNull("user_id");
        queryWrapper.select("user_id"); // 移除DISTINCT，在Java层面去重

        List<PurchasedFlowDetail> records = queryByWrapper(queryWrapper);
        Set<Long> userIds = records.stream()
                .map(PurchasedFlowDetail::getUserId)
                .filter(userId -> userId != null) // 过滤null值
                .collect(Collectors.toSet());

        return userIds;
    }
}
