package com.kuocai.cdn.controller.system;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.constant.KuoCaiConstants;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.entity.Announcement;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.LoginDevice;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.CdnDomainStatisticsService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.CertificateVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 总览页页面跳转控制器
 *
 * @author XUEW
 * @date 下午9:00 2023/2/12
 */
@Slf4j
@Controller
@Scope(value = "session")
public class DashboardPageController extends BaseController {

    @Autowired
    private CdnDomainStatisticsService statisticsService;

    /**
     * 后台管理 主页
     */
    @GetMapping("/dashboard")
    public String dashboard(Map<String, Object> map) throws BusinessException {
        if (isAdmin()) {
            return adminDashboard(map);
        }
        return userDashboard(map);
    }

    /**
     * 普通用户控制台
     */
    public String userDashboard(Map<String, Object> map) throws BusinessException {
        // 数量统计
        Map<String, Long> countStatistics = statisticsService.queryCountStatistics(loginUserId);
        // 最新登录记录
        List<LoginDevice> loginDevices = loginDeviceService.queryUserLastLoginDevice(loginUserId, 1);
        // 获取最新公告
        Announcement announcement = announcementService.getPublished();

        map.put("announcement", announcement);
        map.put("countStatistics", countStatistics);
        map.put("loginDevice", loginDevices.get(0));
        return "user/dashboard";
    }

    /**
     * 管理员控制台
     */
    public String adminDashboard(Map<String, Object> map) throws BusinessException {
        // 以下获取count的时候不进行唯一性的判断
        // 获取域名总数
        Long domainCount = cdnDomainService.countByWrapper(new QueryWrapper<>());
        // 获取证书总数,这块业务重复了，可以统一放到service中
        CertificateVo certificateVo = new CertificateVo();
        JSONObject certificateInfo;
        try {
            certificateInfo = httpsCertificateService.queryCertificateInfosByPage(certificateVo);
        } catch (BusinessException e) {
            log.error("获取华为云证书信息失败，仪表盘将显示默认值", e);
            certificateInfo = new JSONObject();
            certificateInfo.put("https", new JSONArray());
        }
        // TODO 过滤出自己平台的加速域名
        JSONArray resultHttps = new JSONArray();
        List<CdnDomain> cdnDomains = cdnDomainService.queryAll();
        if (Assert.notEmpty(cdnDomains)) {

            List<String> domainIds = cdnDomains.stream().map(CdnDomain::getDomainId).collect(Collectors.toList());
            JSONArray https = (JSONArray) certificateInfo.get("https");
            for (Object http : https) {
                JSONObject httpObject = (JSONObject) http;
                if (domainIds.contains(httpObject.get("domainId").toString())) {
                    resultHttps.add(http);
                }
            }
        }
        // 获取用户总数
        Long userCount = sysUserService.countByWrapper(new QueryWrapper<>());
        // 获取工单总数
        Long orderCount = workOrderService.countByWrapper(new QueryWrapper<>());
        // 获取总充值
        Map<String, BigDecimal> allAccountInfo = sysUserAccountService.getAllAccountInfo();
        // 获取总消费
        // 获取充值排行榜
        List<Map<String, Object>> queryRankingList = sysUserAccountService.queryRankingList(KuoCaiConstants.ACCUMULATIVE_RECHARGE_LIMITS);

        // 获取本周注册，上周注册数
        Map<String, List<String>> weekLoginCount = sysUserService.getWeekLoginCount();
        Map<String, List<String>> weekRegisterCount = sysUserService.getWeekRegisterCount();

        map.put("domainCount", domainCount);
        map.put("certificateCount", resultHttps.size());
        map.put("userCount", userCount);
        map.put("orderCount", orderCount);
        map.put("queryRankingList", queryRankingList);
        map.put("allAccountInfo", allAccountInfo);
        map.put("weekLoginCount", weekLoginCount);
        map.put("weekRegisterCount", weekRegisterCount);
        return "admin/dashboard";
    }

}
