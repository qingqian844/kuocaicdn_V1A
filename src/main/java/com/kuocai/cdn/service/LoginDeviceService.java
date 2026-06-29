package com.kuocai.cdn.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.dao.LoginDeviceDao;
import com.kuocai.cdn.entity.LoginDevice;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.BrowserUtils;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.mica.ip2region.core.Ip2regionSearcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.net.UnknownHostException;
import java.util.*;


/**
 * 登录设备(SysLoginDevice)服务
 *
 * @author XUEW
 * @since 2023-02-15 20:09:23
 */
@Slf4j
@Service
public class LoginDeviceService extends BaseService<LoginDevice> {

    @Autowired
    protected LoginDeviceDao dao;

    /**
     * IP查询组件：ip2regionSearcher.memorySearch("127.0.0.1");
     */
    @Autowired
    private Ip2regionSearcher ip2regionSearcher;

    /**
     * 保存用户登录设备
     *
     * @param userId  用户ID
     * @param request 请求
     */
    public void saveLoginDevice(Long userId, HttpServletRequest request) {
        String ip = "";
        try {
            ip = BrowserUtils.getIp(request);
        } catch (UnknownHostException e) {
            ip = "未知地址";
        }
        JSONObject browser = BrowserUtils.getBrowser(request);
        Map<String, Object> ipInfo = BrowserUtils.getIpInfo(ip);
        String location = "特殊地址-未知-未知";
        if (Assert.notEmpty(ipInfo)) {
            location = ipInfo.get("country") + "-" + ipInfo.get("city") + "-" + ipInfo.get("districts");
        }
        // 生成登录记录
        LoginDevice curDevice = LoginDevice.builder()
                .userId(userId)
                .browser(browser.get("browserType") + "")
                .device(browser.get("platform") + "")
                .loginIp(ip)
                .location(location)
                .loginTime(new Date())
                .build();
        save(curDevice);
        log.info("保存用户登录设备");
    }

    /**
     * 获取用户最近的登录设备
     *
     * @param userId 用户ID
     * @param limit  记录条数
     * @return 登录设备列表
     */
    public List<LoginDevice> queryUserLastLoginDevice(Long userId, int limit) {
        QueryWrapper<LoginDevice> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("login_time");
        wrapper.last("limit " + limit);
        return queryByWrapper(wrapper);
    }

    /**
     * description: getLatWeekLoginCount 获取上周登录数
     *
     * @param lastSunTime 上周日时间
     * @return java.util.List<java.lang.String>
     * @author bo
     * @date 2023/4/16 18:58
     */
    public List<String> getLatWeekLoginCount(String lastSunTime) {
        return new ArrayList<>(Arrays.asList(dao.getLatWeekLoginCount(lastSunTime).split(",")));
    }

    /**
     * description: 根据传入时间获取登录数(可以与获取注册数归为一个)
     *
     * @param inTime String
     * @return java.lang.String
     */
    public String getLoginCountByTime(String inTime) {
        return dao.getLoginCountByTime(inTime);
    }
}
