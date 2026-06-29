package com.kuocai.cdn.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuocai.cdn.entity.Announcement;
import org.springframework.stereotype.Repository;

/**
 * (Announcement)数据库访问层
 *
 * @author todoitbo
 * @since 2023-05-10 20:39:05
 */
@Repository
public interface AnnouncementDao extends BaseMapper<Announcement> {

}

