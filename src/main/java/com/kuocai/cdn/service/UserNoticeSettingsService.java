package com.kuocai.cdn.service;

import com.kuocai.cdn.dao.UserNoticeSettingsDao;
import com.kuocai.cdn.entity.UserNoticeSettings;
import com.kuocai.cdn.service.base.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 通知设置表(UserNoticeSettings)服务
 *
 * @author ChenWEI
 * @since 2023-02-20 21:12:34
 */
@Service
public class UserNoticeSettingsService extends BaseService<UserNoticeSettings> {

    @Autowired
    protected UserNoticeSettingsDao dao;
}
