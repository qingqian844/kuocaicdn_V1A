package com.kuocai.cdn.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuocai.cdn.entity.WorkOrder;
import org.springframework.stereotype.Repository;

/**
 * (WorkOrder)数据库访问层
 *
 * @author XUEW
 * @since 2023-02-20 21:06:04
 */
@Repository
public interface WorkOrderDao extends BaseMapper<WorkOrder> {

}

