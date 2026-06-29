package com.kuocai.cdn.schedule;

import com.kuocai.cdn.constant.KuoCaiConstants;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.LoginDeviceService;
import com.kuocai.cdn.service.SysUserService;
import com.kuocai.cdn.util.JedisUtil;
import com.kuocai.cdn.util.KuocaiBaseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


/**
 * 获取登录注册数定时任务类
 *
 * @author xiaobo
 */
@Slf4j
@Component
@Profile("prod")
public class LoginRegisterTask {

    @Resource
    private SysUserService sysUserService;

    @Resource
    private LoginDeviceService loginDeviceService;

    /**
     * 获取昨天的登录注册数并更新redis
     */
    // @Scheduled(cron = "0 1 0 * * ?")
    public void getYesterdayLoginAndRegisterCount() throws BusinessException {
        // 这里初始化一个list，防止下面重复创建，下同
        List<String> thisWeekRegisterList = new ArrayList<>();
        List<String> thisWeekLoginList = new ArrayList<>();
        // 获取昨天的时间
        String lastTime = KuocaiBaseUtil.accessTimeString(-1);
        // 获取昨天登录数
        String loginCount = loginDeviceService.getLoginCountByTime(lastTime);
        // 获取昨天注册数
        String registerCountByTime = sysUserService.getRegisterCountByTime(lastTime);
        int dayWeek = KuocaiBaseUtil.getNowWeekNum();
        // 登录数，因为这些数据不是主要数据，所以可以容错(last_login_time可能再刚过凌晨12点更新了)
        try {
            if (JedisUtil.exists(KuoCaiConstants.THIS_WEEK_LOGIN)) {
                List<String> listString = JedisUtil.getListString(KuoCaiConstants.THIS_WEEK_LOGIN);
                if (listString.size() == dayWeek - 1) {
                    thisWeekLoginList = listString;
                }
            } else {
                thisWeekLoginList = loginDeviceService.getLatWeekLoginCount(KuocaiBaseUtil.accessTimeString(0));
                thisWeekLoginList.subList(0, 8 - dayWeek).clear();
                JedisUtil.setList(KuoCaiConstants.THIS_WEEK_LOGIN, thisWeekLoginList);
            }
            thisWeekLoginList.add(loginCount);
            JedisUtil.setList(KuoCaiConstants.THIS_WEEK_LOGIN, thisWeekLoginList);
        } catch (Exception e) {
            // 这里最好是存到异常表中(可优化)
            log.error("处理登录数出错：{}" + e.getMessage());
        }
        // 注册数
        try {
            if (JedisUtil.exists(KuoCaiConstants.THIS_WEEK_REGISTER)) {
                List<String> listString = JedisUtil.getListString(KuoCaiConstants.THIS_WEEK_REGISTER);
                if (listString.size() == dayWeek - 1) {
                    thisWeekRegisterList = listString;
                }
            } else {
                thisWeekRegisterList = sysUserService.getLatWeekRegisterCount(KuocaiBaseUtil.accessTimeString(0));
                thisWeekRegisterList.subList(0, 8 - dayWeek).clear();
                JedisUtil.setList(KuoCaiConstants.THIS_WEEK_LOGIN, thisWeekRegisterList);
            }
            thisWeekRegisterList.add(registerCountByTime);
            JedisUtil.setList(KuoCaiConstants.THIS_WEEK_REGISTER, thisWeekRegisterList);
        } catch (Exception e) {
            // 这里最好是存到异常表中(可优化)
            log.error("处理注册数出错：{}" + e.getMessage());
        }

        // 如果当天是周一
        if (KuocaiBaseUtil.todayIsWeeks(Calendar.MONDAY)) {
            try {
                // 将redis中上上周的数据替换为上周的
                JedisUtil.setList(KuoCaiConstants.LAST_WEEK_LOGIN, thisWeekLoginList);
                JedisUtil.setList(KuoCaiConstants.LAST_WEEK_REGISTER, thisWeekRegisterList);
                // 删除上周的key
                JedisUtil.delKey(KuoCaiConstants.THIS_WEEK_LOGIN);
                JedisUtil.delKey(KuoCaiConstants.THIS_WEEK_REGISTER);
            } catch (Exception e) {
                // 这里最好是存到异常表中(可优化)
                log.error("周一处理数据替换出错：{}" + e.getMessage());
            }
        }
    }

}
