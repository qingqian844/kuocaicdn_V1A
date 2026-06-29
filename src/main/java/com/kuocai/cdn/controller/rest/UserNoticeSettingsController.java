package com.kuocai.cdn.controller.rest;

import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.service.UserNoticeSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知设置表(UserNoticeSettings)控制器
 *
 * @author ChenWEI
 * @since 2023-02-20 21:12:34
 */
@RestController
@RequestMapping(value = "UserNoticeSettings")
@Scope(value = "session")
public class UserNoticeSettingsController extends BaseController {

    @Autowired
    protected UserNoticeSettingsService service;
}
