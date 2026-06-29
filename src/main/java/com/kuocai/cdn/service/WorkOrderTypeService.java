package com.kuocai.cdn.service;

import com.kuocai.cdn.dao.WorkOrderTypeDao;
import com.kuocai.cdn.entity.WorkOrderType;
import com.kuocai.cdn.service.base.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 工单类型(WorkOrderType)服务
 *
 * @author ChenWEI
 * @since 2023-02-20 21:12:34
 */
@Service
public class WorkOrderTypeService extends BaseService<WorkOrderType> {

    @Autowired
    protected WorkOrderTypeDao dao;
}
