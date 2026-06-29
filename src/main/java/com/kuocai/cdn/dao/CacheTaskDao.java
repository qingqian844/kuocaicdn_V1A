package com.kuocai.cdn.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuocai.cdn.entity.CacheTask;
import org.springframework.stereotype.Repository;

/**
 * (CacheTask)数据库访问层
 *
 * @author makejava
 * @since 2023-05-10 18:53:58
 */
@Repository
public interface CacheTaskDao extends BaseMapper<CacheTask> {

}

