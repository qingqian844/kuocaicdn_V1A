package com.kuocai.cdn.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuocai.cdn.entity.Gift;
import org.springframework.stereotype.Repository;

/**
 * (Gift)数据库访问层
 *
 * @author makejava
 * @since 2023-05-15 14:43:53
 */
@Repository
public interface GiftDao extends BaseMapper<Gift> {

}

