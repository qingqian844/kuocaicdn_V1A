package com.kuocai.cdn.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuocai.cdn.entity.OperationLog;
import org.springframework.stereotype.Repository;

/**
 * 操作记录数据库访问层
 *
 * @author XUEW
 * @date 下午9:01 2023/2/12
 */
@Repository
public interface OperationLogDao extends BaseMapper<OperationLog> {

}
