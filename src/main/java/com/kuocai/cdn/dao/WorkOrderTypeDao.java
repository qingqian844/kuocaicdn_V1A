package com.kuocai.cdn.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuocai.cdn.entity.WorkOrderType;
import org.springframework.stereotype.Repository;

/**
 * 工单类型(WorkOrderType)数据库访问层
 *
 * @author ChenWEI
 * @since 2023-02-20 21:12:34
 */
@Repository
public interface WorkOrderTypeDao extends BaseMapper<WorkOrderType> {

}

