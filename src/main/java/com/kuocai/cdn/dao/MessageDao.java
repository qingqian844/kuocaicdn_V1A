package com.kuocai.cdn.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuocai.cdn.entity.Message;
import org.springframework.stereotype.Repository;

/**
 * (Message)数据库访问层
 *
 * @author makejava
 * @since 2023-05-11 15:48:32
 */
@Repository
public interface MessageDao extends BaseMapper<Message> {

}

