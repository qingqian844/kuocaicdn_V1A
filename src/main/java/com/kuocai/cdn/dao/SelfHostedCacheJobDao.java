package com.kuocai.cdn.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuocai.cdn.entity.SelfHostedCacheJob;
import org.springframework.stereotype.Repository;

@Repository
public interface SelfHostedCacheJobDao extends BaseMapper<SelfHostedCacheJob> {
}
