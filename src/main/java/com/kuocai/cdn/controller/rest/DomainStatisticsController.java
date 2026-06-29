package com.kuocai.cdn.controller.rest;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.constant.StatisticsType;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.CdnDomainService;
import com.kuocai.cdn.service.CdnDomainStatisticsService;
import com.kuocai.cdn.service.domain.statistics.ICdnStatisticsPlatformService;
import com.kuocai.cdn.service.domain.statistics.KingsoftDomainStatisticsServiceImpl;
import com.kuocai.cdn.service.factory.CdnStatisticsPlatformFactory;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import static com.kuocai.cdn.constant.StatisticsType.*;

/**
 * 域名统计
 *
 * @author XUEW
 * @date 下午9:00 2023/2/12
 */
@Slf4j
@RestController
@RequestMapping(value = "CdnDomainStatistics")
@Scope(value = "session")
public class DomainStatisticsController extends BaseController {

    @Autowired
    private CdnDomainService service;

    @Autowired
    private CdnDomainStatisticsService statisticsService;

    @Resource
    private CdnDomainStatisticsService cdnDomainStatisticsService;

    @Autowired
    private CdnDomainService cdnDomainService;


    /**
     * 【临时诊断接口】检查并修复金山云域名ID
     * @return 诊断和修复结果
     */
    @GetMapping("/statistics/check-domain-id")
    public String checkKingsoftDomainId() {
        if (!isAdmin()) {
            return "FORBIDDEN";
        }
        StringBuilder results = new StringBuilder();
        results.append("--- 诊断开始 ---\n\n");

        // 定义一个可重用的诊断函数
        BiConsumer<String, StringBuilder> runCheck = (domainName, sb) -> {
            sb.append("正在检查域名: ").append(domainName).append("\n");
            try {
                CdnDomain domain = cdnDomainService.queryByDomainName(domainName);
                if (domain == null) {
                    sb.append("结果: 在我们的数据库中找不到域名 ").append(domainName).append("\n");
                    return;
                }
                String route = domain.getRoute();
                if (!"kingsoft".equalsIgnoreCase(route)) {
                    sb.append("结果: 域名 ").append(domainName).append(" 的路线是 '").append(route).append("', 不是金山云, 跳过诊断。\n");
                    return;
                }

                ICdnStatisticsPlatformService platformService = CdnStatisticsPlatformFactory.getCdnPlatform(route);

                if (platformService instanceof KingsoftDomainStatisticsServiceImpl) {
                    String result = ((KingsoftDomainStatisticsServiceImpl) platformService).checkAndFixDomainId(domainName);
                    sb.append(result).append("\n");
                } else {
                    sb.append("错误: 无法获取金山云服务实例。\n");
                }
            } catch (Exception e) {
                log.error("执行金山云域名ID诊断时发生未知错误 (" + domainName + ")", e);
                sb.append("诊断 '").append(domainName).append("' 时发生严重错误: ").append(e.getMessage()).append("\n");
            }
        };

        // 1. 检查 'www.achong0427.cn'
        runCheck.accept("www.achong0427.cn", results);

        results.append("\n--------------------\n\n");

        // 2. 检查 'achong0427.cn'
        runCheck.accept("achong0427.cn", results);

        results.append("\n--- 诊断结束 ---");

        return "<pre>" + results.toString().replace("\n", "<br/>") + "</pre>";
    }

    /**
     * 【新增】手动清理所有数据统计缓存的接口
     * @return 清理结果
     */
    @GetMapping("/statistics/clear-cache")
    public String clearStatisticsCache() {
        if (!isAdmin()) {
            return "FORBIDDEN";
        }
        try {
            // 1. 使用通配符 `*` 查找所有以 "Statistics:" 开头的缓存键
            Set<String> keysToDelete = JedisUtil.keys("Statistics:*");

            if (keysToDelete == null || keysToDelete.isEmpty()) {
                return "没有找到需要清理的统计缓存。";
            }

            // 2. 批量删除找到的键
            JedisUtil.delKeys(keysToDelete);

            return String.format("成功清理了 %d 个统计缓存条目。", keysToDelete.size());

        } catch (Exception e) {
            log.error("手动清理统计缓存时发生错误", e);
            return "清理缓存时发生错误: " + e.getMessage();
        }
    }


    /**
     * 查询网络资源消耗统计信息
     */
    @RateLimiter
    @GetMapping("queryResourceStatistics")
    public RespResult queryResourceStatistics(Long domainId, String startTime, String endTime) {
        Assert.assertNotEmpty(domainId, "域名ID");
        Assert.assertNotEmpty(startTime, "开始时间");
        Assert.assertNotEmpty(endTime, "结束时间");
        DateTime start, end;
        try {
            start = DateUtil.parse(startTime);
            end = DateUtil.parse(endTime);
        } catch (Exception e) {
            return RespResult.fail("解析时间错误，请传入正确时间格式：1999-07-29 12:30:00");
        }
        CdnDomain cdnDomain = service.queryById(domainId);
        if (Assert.isEmpty(cdnDomain)) {
            return RespResult.fail("域名不存在");
        }
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        try {
            return RespResult.success("查询成功", statisticsService.mergeAllPlatForm(Arrays.asList(cdnDomain), start, end, RESOURCE, loginUserId));
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
    }

    /**
     * 查询访问情况统计信息
     */
    @RateLimiter
    @GetMapping("queryVisitsStatistics")
    public RespResult queryVisitsStatistics(Long domainId, String startTime, String endTime) {
        Assert.assertNotEmpty(domainId, "域名ID");
        Assert.assertNotEmpty(startTime, "开始时间");
        Assert.assertNotEmpty(endTime, "结束时间");
        DateTime start, end;
        try {
            start = DateUtil.parse(startTime);
            end = DateUtil.parse(endTime);
        } catch (Exception e) {
            return RespResult.fail("解析时间错误，请传入正确时间格式：1999-07-29 12:30:00");
        }
        CdnDomain cdnDomain = service.queryById(domainId);
        if (Assert.isEmpty(cdnDomain)) {
            return RespResult.fail("域名不存在");
        }
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        try {
            return RespResult.success("查询成功", statisticsService.mergeAllPlatForm(Arrays.asList(cdnDomain), start, end, VISITS, loginUserId));
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
    }

    /**
     * 查询HTTP状态码统计信息
     */
    @RateLimiter
    @GetMapping("queryHttpCodeStatusStatistics")
    public RespResult queryHttpCodeStatusStatistics(Long domainId, String startTime, String endTime) {
        Assert.assertNotEmpty(domainId, "域名ID");
        Assert.assertNotEmpty(startTime, "开始时间");
        Assert.assertNotEmpty(endTime, "结束时间");
        DateTime start, end;
        try {
            start = DateUtil.parse(startTime);
            end = DateUtil.parse(endTime);
        } catch (Exception e) {
            return RespResult.fail("解析时间错误，请传入正确时间格式：1999-07-29 12:30:00");
        }
        CdnDomain cdnDomain = service.queryById(domainId);
        if (Assert.isEmpty(cdnDomain)) {
            return RespResult.fail("域名不存在");
        }
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        try {
            return RespResult.success("查询成功", statisticsService.mergeAllPlatForm(Arrays.asList(cdnDomain), start, end, HTTP_CODE_STATUS, loginUserId));
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
    }

    /**
     * 查询我的域名网络资源消耗统计信息
     */
    @RateLimiter
    @GetMapping("queryMyResourceStatistics")
    public RespResult queryMyResourceStatistics(String startTime, String endTime) {
        Assert.assertNotEmpty(startTime, "开始时间");
        Assert.assertNotEmpty(endTime, "结束时间");
        DateTime start, end;
        try {
            start = DateUtil.parse(startTime);
            end = DateUtil.parse(endTime);
        } catch (Exception e) {
            return RespResult.fail("解析时间错误，请传入正确时间格式：1999-07-29 12:30:00");
        }
        List<CdnDomain> cdnDomains = service.queryByUserId(loginUserId);
        if (Assert.isEmpty(cdnDomains)) {
            return RespResult.fail("当前用户尚未配置加速域名");
        }
        try {
            // TODO route
            return RespResult.success("查询成功", statisticsService.mergeAllPlatForm(cdnDomains, start, end, RESOURCE, loginUserId));
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
    }

    /**
     * 查询所有统计信息
     */
    @RateLimiter
    @GetMapping("queryStatistics")
    public RespResult queryStatistics(String startTime, String endTime, String domains, String type, Long userId) throws Exception {
        Assert.assertNotEmpty(startTime, "开始时间");
        Assert.assertNotEmpty(endTime, "结束时间");
        Assert.assertNotEmpty(domains, "查询域名");
        Assert.assertNotEmpty(type, "查询类型");
        if (isAdmin()) {
            userId = Assert.isEmpty(userId) ? loginUserId : userId;
        } else {
            userId = loginUserId;
        }
        if (!StatisticsType.okType(type)) {
            return RespResult.fail("不支持的查询类型");
        }
        DateTime start, end;
        try {
            start = DateUtil.parse(startTime);
            end = DateUtil.parse(endTime);
        } catch (Exception e) {
            return RespResult.fail("解析时间错误，请传入正确时间格式：1999-07-29 12:30:00");
        }
        SysUser sysUser = sysUserService.queryById(userId);
        List<CdnDomain> cdnDomains;
        if ("all".equals(domains)) {
            if (sysUser.isAdmin()) {
                cdnDomains = service.queryAll();
            } else {
                cdnDomains = service.queryByUserId(userId);
            }
            if (Assert.isEmpty(cdnDomains)) {
                return RespResult.fail("尚未配置加速域名");
            }
        } else {
            String[] domainArray = domains.split(",");
            ArrayList<String> domainList = ListUtil.toList(domainArray);
            RespResult accessResult = checkDomainNameAccess(domainList);
            if (accessResult != null) {
                return accessResult;
            }
            List<CdnDomain> cdnDomainList = service.queryByDomainNames(domainList);
            if (sysUser.isAdmin()) {
                cdnDomains = cdnDomainList;
            } else {
                for (CdnDomain cdnDomain : cdnDomainList) {
                    if (cdnDomain.getUserId().longValue() != userId) {
                        return RespResult.fail("无权限查询此域名信息：" + cdnDomain.getDomainName());
                    }
                }
                cdnDomains = cdnDomainList;
            }
        }
        try {
            // TODO route
            return RespResult.success("查询成功", statisticsService.mergeAllPlatForm(cdnDomains, start, end, type, userId));
        } catch (Exception e) {
            String message = e.getMessage();
            if (message.startsWith("您没有权限操作域名:")) {
                String domainName = message.replace("您没有权限操作域名: ", "");
                List<CdnDomain> deleteCdnDomains = cdnDomainService.queryByDomainNames(domainName);
                deleteCdnDomains.forEach(cdnDomain -> cdnDomainService.deleteById(cdnDomain.getId()));
                return RespResult.success("查询成功", statisticsService.mergeAllPlatForm(cdnDomains, start, end, type, userId));
            }
            log.error("queryStatistics 查询失败", e);
            throw new BusinessException(e.getMessage());
        }
    }
}
