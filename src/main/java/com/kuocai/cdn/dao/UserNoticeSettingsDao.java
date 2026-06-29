package com.kuocai.cdn.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuocai.cdn.entity.UserNoticeSettings;
import org.springframework.stereotype.Repository;

/**
 * 通知设置表(UserNoticeSettings)数据库访问层
 *
 * @author ChenWEI
 * @since 2023-02-20 21:12:34
 */
@Repository
public interface UserNoticeSettingsDao extends BaseMapper<UserNoticeSettings> {

}

