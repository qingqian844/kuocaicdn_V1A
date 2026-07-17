package com.kuocai.cdn.controller.system;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.CacheTaskType;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.enumeration.domainmerage.cachesetting.PreheatingCount;
import com.kuocai.cdn.enumeration.domainmerage.cachesetting.RefreshFolderCount;
import com.kuocai.cdn.enumeration.domainmerage.cachesetting.RefreshUrlCount;
import com.kuocai.cdn.service.CacheTaskService;
import com.kuocai.cdn.service.EdgeOneDomainQuotaService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import com.kuocai.cdn.vo.CdnDomainVo;
import com.kuocai.cdn.vo.CertificateVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.kuocai.cdn.constant.KuoCaiConstants.*;

/**
 * 站点管理页面跳转控制器
 *
 * @author XUEW
 * @date 下午9:00 2023/2/12
 */
@Slf4j
@Controller
@Scope(value = "session")
public class CdnManagePageController extends BaseController {
    private static final String CACHE_HISTORY_PREFIX = "CacheHistory:v2:";
    private static final int ACTIVE_HISTORY_CACHE_SECONDS = 10;
    private static final int TERMINAL_HISTORY_CACHE_SECONDS = 300;

    @Resource
    private CacheTaskService cacheTaskService;

    @Resource
    private EdgeOneDomainQuotaService edgeOneDomainQuotaService;

    /**
     * 站点管理 域名管理
     */
    @GetMapping("/domain-list")
    public String domainList(Map<String, Object> map) {
        if (isAdmin()) {
            return adminDomainList(map);
        }
        return userDomainList(map);
    }

    @GetMapping("/agent-domain")
    public String agentDomain(Map<String, Object> map) {
        List<SysUser> sysUsers = sysUserService.queryUserByAgentId(loginUserId);
        List<CdnDomainVo> cdnDomainVos = cdnDomainService.queryVoByUserIds(sysUsers.stream().map(SysUser::getId).collect(Collectors.toList()));
        map.put("sysUsers", sysUsers);
        map.put("cdnDomainVos", cdnDomainVos);
        return "user/agent/agent-domain";
    }

    /**
     * 管理员域名管理
     */
    public String adminDomainList(Map<String, Object> map) {
        List<SysUser> sysUsers = sysUserService.queryAll();
        map.put("sysUsers", sysUsers);
        return "admin/domain/domain-list";
    }

    /**
     * 用户域名列表
     */
    public String userDomainList(Map<String, Object> map) {
        List<CdnDomainVo> cdnDomainVo = cdnDomainService.getMyCdnDomainVo(loginUserId);
        map.put("domains", cdnDomainVo);
        return "user/domain/domain-list";
    }

    // #########################################################################################################

    /**
     * 站点管理 证书管理
     */
    @GetMapping("/certification-list")
    public String certificationList(CertificateVo certificateVo, Map<String, Object> map) {
        if (isAdmin()) {
            return adminCertificationList(certificateVo, map);
        }
        return userCertificationList(certificateVo, map);
    }

    /**
     * 站点管理 证书管理
     */
    public String adminCertificationList(CertificateVo certificateVo, Map<String, Object> map) {
        try {
            JSONObject certificateInfo = httpsCertificateService.queryCertificateInfosByPage(certificateVo);
            // 过滤出自己平台的加速域名
            JSONObject result = new JSONObject();
            JSONArray resultHttps = new JSONArray();
            List<CdnDomain> cdnDomains = cdnDomainService.queryAll();
            Map<String, Long> doMainUserIdMap = cdnDomains.stream().collect(Collectors.toMap(CdnDomain::getDomainId, CdnDomain::getUserId));
            List<SysUser> sysUsers = sysUserService.queryAll();
            Map<Long, SysUser> userIdMap = sysUsers.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
            if (Assert.notEmpty(cdnDomains)) {
                List<String> domainIds = cdnDomains.stream().map(CdnDomain::getDomainId).collect(Collectors.toList());
                JSONArray https = (JSONArray) certificateInfo.get("https");
                for (Object http : https) {
                    JSONObject httpObject = (JSONObject) http;
                    String domainId = httpObject.get("domainId").toString();
                    if (domainIds.contains(domainId)) {
                        Long userId = doMainUserIdMap.get(domainId);
                        SysUser sysUser = userIdMap.get(userId);
                        ((JSONObject) http).put("userId", sysUser.getId());
                        ((JSONObject) http).put("img", sysUser.getImg());
                        ((JSONObject) http).put("userName", sysUser.getUserName());
                        resultHttps.add(http);
                    }
                }
            }
            result.put("total", resultHttps.size());
            result.put("https", resultHttps);
            map.put("certificateInfo", result);
        } catch (Exception e) {
            return "redirect:/500";
        }
        return "admin/certification/certification-list";
    }

    /**
     * 站点管理 证书管理
     */
    public String userCertificationList(CertificateVo certificateVo, Map<String, Object> map) {
        try {
            JSONObject certificateInfo = httpsCertificateService.queryCertificateInfosByPage(certificateVo);
            // 过滤出自己平台的加速域名
            JSONObject result = new JSONObject();
            JSONArray resultHttps = new JSONArray();
            List<CdnDomain> cdnDomains = cdnDomainService.queryByObj(CdnDomain.builder().userId(loginUserId).build());
            if (Assert.notEmpty(cdnDomains)) {
                List<String> domainIds = cdnDomains.stream().filter(item -> ObjectUtil.equal(item.getUserId(), loginUserId)).map(CdnDomain::getDomainId).collect(Collectors.toList());
                JSONArray https = (JSONArray) certificateInfo.get("https");
                for (Object http : https) {
                    JSONObject httpObject = (JSONObject) http;
                    if (domainIds.contains(httpObject.get("domainId").toString())) {
                        ((JSONObject) http).put("userId", loginUser.getId());
                        ((JSONObject) http).put("img", loginUser.getImg());
                        ((JSONObject) http).put("userName", loginUser.getUserName());
                        resultHttps.add(http);
                    }
                }
            }
            result.put("total", resultHttps.size());
            result.put("https", resultHttps);
            map.put("certificateInfo", result);
        } catch (Exception e) {
            return "redirect:/500";
        }
        return "admin/certification/certification-list";
    }


    // #########################################################################################################

    /**
     * 站点管理 创建域名
     */
    @GetMapping("/domain-create")
    public String domainCreate(Map<String, Object> map) {
        // 获取可用额度
        int usedSize = cdnDomainService.queryByUserId(loginUserId).size();
        Integer maxDomainSize = sysUserService.queryById(loginUserId).getMaxDomainCount();
        String verifyCode = RandomUtil.randomNumbers(6);
        JedisUtil.setStr("SubmitVerifyCode:" + verifyCode, "1", 300);
        map.put("availableCount", maxDomainSize - usedSize);
        map.put("verifyCode", verifyCode);
        map.put("route", route);
        if ("tencent_edgeone".equals(route)) {
            map.put("edgeOneQuotaSummary", edgeOneDomainQuotaService.summary(loginUserId));
        }
        map.put("routeAuthorized", true);
        SysUser loginUser = getLoginUser();
        map.put("enableOverseas", loginUser.getEnableOverseas());
        map.put("enableGlobal", loginUser.getEnableGlobal());
        return "admin/domain/domain-create";
    }

    /**
     * 站点管理 创建证书
     */
    @GetMapping("/certification-create")
    public String certificationCreate(Map<String, Object> map) {
        return "admin/certification/certification-create";
    }

    /**
     * 站点管理 缓存预热
     */
    @GetMapping("/cache-preheating")
    public String cachePreheating(Map<String, Object> map) {
        for (CdnRoute value : CdnRoute.values()) {
            String code = value.getCode();
            String key = CACHE_PREHEATING_KEY + "_" + code + ":" + DateUtil.today();
            if (JedisUtil.exists(key)) {
                String count = JedisUtil.getStr(key);
            } else {
                JedisUtil.setStr(key, PreheatingCount.convert(code).getNum(), 60 * 60 * 24 * 2);
                String count = JedisUtil.getStr(key);
                map.put("count", count);
            }
        }
        return "admin/cache/cache-preheating";
    }

    /**
     * 站点管理 缓存刷新url
     */
    @GetMapping("/cache-refresh-url")
    public String cacheRefreshUrl(Map<String, Object> map) {
        for (CdnRoute value : CdnRoute.values()) {
            String code = value.getCode();
            String key = CACHE_REFRESH_URL_KEY + "_" + code + ":" + DateUtil.today();
            if (JedisUtil.exists(key)) {
                String count = JedisUtil.getStr(key);
            } else {
                JedisUtil.setStr(key, RefreshUrlCount.convert(code).getNum(), 60 * 60 * 24 * 2);
                String count = JedisUtil.getStr(key);
                map.put("count", count);
            }
        }
        return "admin/cache/cache-refresh-url";
    }

    /**
     * 站点管理 缓存刷新folder
     */
    @GetMapping("/cache-refresh-folder")
    public String cacheRefreshFolder(Map<String, Object> map) {
        for (CdnRoute value : CdnRoute.values()) {
            String code = value.getCode();
            String key = CACHE_REFRESH_FOLDER_KEY + "_" + code + ":" + DateUtil.today();
            if (JedisUtil.exists(key)) {
                String count = JedisUtil.getStr(key);
            } else {
                JedisUtil.setStr(key, RefreshFolderCount.convert(code).getNum(), 60 * 60 * 24 * 2);
                String count = JedisUtil.getStr(key);
                map.put("count", count);
            }
        }
        return "admin/cache/cache-refresh-folder";
    }

    /**
     * 站点管理 缓存预热历史
     */
    @GetMapping("/cache-history-preheating")
    public String cacheHistoryPreheating(Map<String, Object> map, Long userId) {
        if (!isAdmin()) {
            userId = loginUserId;
        }
        try {
            String key = cacheHistoryKey(CacheTaskType.PREHEATING, userId);
            String cacheData = JedisUtil.getStr(key);
            if (Assert.notEmpty(cacheData) && !"[]".equals(cacheData)) {
                map.put("results", JSONArray.parseArray(cacheData));
            } else {
                List<JSONObject> results = cacheTaskService.queryCdnInfos(CacheTaskType.PREHEATING, loginUserRoleCode, loginUserId, userId);
                cacheHistory(key, results);
                map.put("results", results);
            }
            map.put("users", sysUserService.queryAll());
            map.put("currentUserId", userId);

        } catch (Exception e) {
            return "redirect:/500";
        }
        return "admin/cache/cache-history-preheating";
    }

    /**
     * 站点管理 缓存刷新历史
     */
    @GetMapping("/cache-history-refresh")
    public String cacheHistoryRefresh(Map<String, Object> map, Long userId) {
        if (!isAdmin()) {
            userId = loginUserId;
        }
        try {
            String key = cacheHistoryKey(CacheTaskType.REFRESH, userId);
            String cacheData = JedisUtil.getStr(key);
            if (Assert.notEmpty(cacheData) && !"[]".equals(cacheData)) {
                map.put("results", JSONArray.parseArray(cacheData));
            } else {
                List<JSONObject> results = cacheTaskService.queryCdnInfos(CacheTaskType.REFRESH, loginUserRoleCode, loginUserId, userId);
                cacheHistory(key, results);
                map.put("results", results);
            }
            map.put("users", sysUserService.queryAll());
            map.put("currentUserId", userId);
        } catch (Exception e) {
            return "redirect:/500";
        }
        return "admin/cache/cache-history-refresh";
    }

    private String cacheHistoryKey(CacheTaskType taskType, Long userId) {
        return String.format("%s%s:%s:%s:%s",
                CACHE_HISTORY_PREFIX, taskType, loginUserRoleCode, loginUserId, userId);
    }

    private void cacheHistory(String key, List<JSONObject> results) {
        if (Assert.isEmpty(results)) {
            return;
        }
        boolean processing = results.stream()
                .anyMatch(item -> "处理中".equals(item.getString("status")));
        JedisUtil.setStr(key, JSONObject.toJSONString(results),
                processing ? ACTIVE_HISTORY_CACHE_SECONDS : TERMINAL_HISTORY_CACHE_SECONDS);
    }
}
