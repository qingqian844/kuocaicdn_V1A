package com.kuocai.cdn.controller.system;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.*;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.domain.operation.CdnetworksDomainServiceImpl;
import com.kuocai.cdn.service.domain.operation.ICdnPlatformService;
import com.kuocai.cdn.service.factory.CdnPlatformFactory;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.CdnDomainVo;
import com.kuocai.cdn.vo.EdgeOneSecurityPolicyVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * 域名配置页面跳转控制器
 *
 * @author XUEW
 * @date 下午9:00 2023/2/12
 */
@Slf4j
@Controller
@Scope(value = "session")
public class DomainSettingsPageController extends BaseController {

    /**
     * 域名配置 基础配置
     */
    @GetMapping("/domain-setting-basic")
    public String domainBasicSettings(Long id, Map<String, Object> map) {
        if (Assert.isEmpty(id)) {
            return "redirect:/400";
        }
        CdnDomainVo cdnDomainVo = cdnDomainService.getCdnDomainVoById(id);
        if (Assert.isEmpty(cdnDomainVo)) {
            return "redirect:/404";
        }
        // 获取域名的详细配置
        try {
            String domainRoute = cdnDomainVo.getRoute();
            ICdnPlatformService cdnPlatformService = CdnPlatformFactory.getCdnPlatform(domainRoute);
            DomainConfig domainConfig;
            if ("cdnetworks".equals(domainRoute)) {
                CdnetworksDomainServiceImpl service = (CdnetworksDomainServiceImpl) cdnPlatformService;
                domainConfig = service.getDomainBasicConfig(cdnDomainVo.getDomainName());
            } else {
                domainConfig = cdnPlatformService.getDomainConfig(cdnDomainVo.getDomainName());
            }
            map.put("domainConfig", JSONObject.parseObject(JSON.toJSONString(domainConfig)));

        } catch (BusinessException e) {
            log.error("获取域名基础配置失败，domain={}, route={}", cdnDomainVo.getDomainName(), cdnDomainVo.getRoute(), e);
            return "redirect:/500";
        }
        map.put("domain", cdnDomainVo);
        return "admin/domain/domain-setting-basic";
    }

    /**
     * 域名配置 回源配置
     */
    @GetMapping("/domain-setting-origin")
    public String domainOriginSettings(Long id, Map<String, Object> map) {
        if (Assert.isEmpty(id)) {
            return "redirect:/400";
        }
        CdnDomainVo cdnDomainVo = cdnDomainService.getCdnDomainVoById(id);
        if (Assert.isEmpty(cdnDomainVo)) {
            return "redirect:/404";
        }
        // 获取域名的详细配置
        try {
            String domainRoute = cdnDomainVo.getRoute();
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(domainRoute);
            DomainBackSourceInfo domainBackSourceInfo;
            if ("cdnetworks".equals(domainRoute)) {
                CdnetworksDomainServiceImpl service = (CdnetworksDomainServiceImpl) iCdnPlatformService;
                domainBackSourceInfo = service.getDomainBackSourceInfo(cdnDomainVo.getDomainName());
            } else {
                DomainConfig domainConfig = iCdnPlatformService.getDomainConfig(cdnDomainVo.getDomainName());
                domainBackSourceInfo = domainConfig.getDomainBackSourceInfo();
            }
            JSONObject backSourceInfoJson = JSONObject.parseObject(JSON.toJSONString(domainBackSourceInfo));
            log.info("获取到配置信息{}", backSourceInfoJson);
            map.put("domainConfig", backSourceInfoJson);
        } catch (BusinessException e) {
            return "redirect:/500";
        }
        map.put("domain", cdnDomainVo);
        return "admin/domain/domain-setting-origin";
    }

    /**
     * 域名配置 HTTPS配置
     */
    @GetMapping("/domain-setting-https")
    public String domainHttpsSettings(Long id, Map<String, Object> map) {
        if (Assert.isEmpty(id)) {
            return "redirect:/400";
        }
        CdnDomainVo cdnDomainVo = cdnDomainService.getCdnDomainVoById(id);
        if (Assert.isEmpty(cdnDomainVo)) {
            return "redirect:/404";
        }
        // 获取域名的详细配置
        try {
            String domainRoute = cdnDomainVo.getRoute();
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(domainRoute);
            DomainHttpsInfo domainHttpsInfo;
            if ("cdnetworks".equals(domainRoute)) {
                CdnetworksDomainServiceImpl service = (CdnetworksDomainServiceImpl) iCdnPlatformService;
                domainHttpsInfo = service.getDomainHttpsInfo(cdnDomainVo.getDomainName());
            } else {
                DomainConfig domainConfig = iCdnPlatformService.getDomainConfig(cdnDomainVo.getDomainName());
                domainHttpsInfo = domainConfig.getDomainHttpsInfo();
            }
            JSONObject httpsInfoJson = JSONObject.parseObject(JSON.toJSONString(domainHttpsInfo));
            log.info("获取到配置信息{}", httpsInfoJson);
            map.put("domainConfig", httpsInfoJson);
        } catch (BusinessException e) {
            return "redirect:/500";
        }
        map.put("domain", cdnDomainVo);
        return "admin/domain/domain-setting-https";
    }

    /**
     * 域名配置 缓存配置
     */
    @GetMapping("/domain-setting-cache")
    public String domainCacheSettings(Long id, Map<String, Object> map) {
        if (Assert.isEmpty(id)) {
            return "redirect:/400";
        }
        CdnDomainVo cdnDomainVo = cdnDomainService.getCdnDomainVoById(id);
        if (Assert.isEmpty(cdnDomainVo)) {
            return "redirect:/404";
        }
        // 获取域名的详细配置
        try {
            String domainRoute = cdnDomainVo.getRoute();
            DomainCacheInfo domainCacheInfo;
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(domainRoute);
            if ("cdnetworks".equals(domainRoute)) {
                CdnetworksDomainServiceImpl service = (CdnetworksDomainServiceImpl) iCdnPlatformService;
                domainCacheInfo = service.getDomainCacheInfo(cdnDomainVo.getDomainName());
            } else {
                DomainConfig domainConfig = iCdnPlatformService.getDomainConfig(cdnDomainVo.getDomainName());
                domainCacheInfo = domainConfig.getDomainCacheInfo();
            }
            JSONObject cacheInfoJson = JSONObject.parseObject(JSON.toJSONString(domainCacheInfo));
            log.info("获取到配置信息{}", cacheInfoJson);
            map.put("domainConfig", cacheInfoJson);
        } catch (BusinessException e) {
            return "redirect:/500";
        }
        map.put("domain", cdnDomainVo);
        return "admin/domain/domain-setting-cache";
    }

    /**
     * 域名配置 访问配置
     */
    @GetMapping("/domain-setting-access")
    public String domainAccessSettings(Long id, Map<String, Object> map) {
        if (Assert.isEmpty(id)) {
            return "redirect:/400";
        }
        CdnDomainVo cdnDomainVo = cdnDomainService.getCdnDomainVoById(id);
        if (Assert.isEmpty(cdnDomainVo)) {
            return "redirect:/404";
        }
        // 获取域名的详细配置
        try {
            String domainRoute = cdnDomainVo.getRoute();
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(domainRoute);
            DomainVisitInfo domainVisitInfo;
            if ("cdnetworks".equals(domainRoute)) {
                CdnetworksDomainServiceImpl service = (CdnetworksDomainServiceImpl) iCdnPlatformService;
                domainVisitInfo = service.getDomainVisitInfo(cdnDomainVo.getDomainName());
            } else {
                DomainConfig domainConfig = iCdnPlatformService.getDomainConfig(cdnDomainVo.getDomainName());
                domainVisitInfo = domainConfig.getDomainVisitInfo();
            }
            if (domainVisitInfo == null) {
                domainVisitInfo = new DomainVisitInfo();
            }
            if (domainVisitInfo.getReferer() == null) {
                domainVisitInfo.setReferer(DomainVisitInfo.Referer.builder()
                        .type("off")
                        .referer_type(0)
                        .include_empty(false)
                        .value("")
                        .build());
            }
            if (domainVisitInfo.getIp_filter() == null) {
                domainVisitInfo.setIp_filter(DomainVisitInfo.IpFilter.builder()
                        .type("off")
                        .value("")
                        .build());
            }
            if (domainVisitInfo.getUser_agent_filter() == null) {
                domainVisitInfo.setUser_agent_filter(DomainVisitInfo.UserAgentFilter.builder()
                        .type("off")
                        .value("")
                        .build());
            }

            // 确保URL鉴权配置存在，如果不存在则初始化一个空对象
            if (domainVisitInfo.getUrl_auth() == null) {
                domainVisitInfo.setUrl_auth(DomainVisitInfo.UrlAuth.builder()
                        .status("off")
                        .type("typeA")
                        .primary_key("")
                        .secondary_key("")
                        .expire_time(3600L)
                        .build());
            }
            if (domainVisitInfo.getEdgeone_security_policy() == null) {
                domainVisitInfo.setEdgeone_security_policy(EdgeOneSecurityPolicyVo.builder()
                        .managedRulesEnabled("off")
                        .managedRulesDetectionOnly("off")
                        .managedRulesSemanticAnalysis("off")
                        .managedRulesAutoUpdate("on")
                        .botManagementEnabled("off")
                        .captchaPageChallengeEnabled("off")
                        .aiCrawlerDetectionEnabled("off")
                        .aiCrawlerDetectionAction("Monitor")
                        .httpDdosAdaptiveFrequencyControlEnabled("off")
                        .httpDdosAdaptiveFrequencyControlSensitivity("medium")
                        .httpDdosClientFilteringEnabled("off")
                        .httpDdosBandwidthAbuseDefenseEnabled("off")
                        .httpDdosSlowAttackDefenseEnabled("off")
                        .rateLimitEnabled("off")
                        .rateLimitCountBy("http.request.ip")
                        .rateLimitThreshold(1000L)
                        .rateLimitPeriod("1m")
                        .rateLimitMode("Monitor")
                        .rateLimitActionDuration("10m")
                        .rateLimitAction("Monitor")
                        .rateLimitChallengeOption("ManagedChallenge")
                        .exceptionEnabled("off")
                        .exceptionModules("waf,rateLimiting,bot")
                        .build());
            }
            
            JSONObject visitInfoJson = JSONObject.parseObject(JSON.toJSONString(domainVisitInfo));
            JSONObject referer = JSONObject.parseObject(JSON.toJSONString(domainVisitInfo.getReferer()));
            JSONObject ipAcl = JSONObject.parseObject(JSON.toJSONString(domainVisitInfo.getIp_filter()));
            JSONObject userAgentFilter = JSONObject.parseObject(JSON.toJSONString(domainVisitInfo.getUser_agent_filter()));
            JSONObject edgeOneSecurityPolicy = JSONObject.parseObject(JSON.toJSONString(domainVisitInfo.getEdgeone_security_policy()));
            log.info("获取到IP黑白名单信息{}", ipAcl);
            
            // 创建一个包含所有必要属性的domainConfig
            JSONObject domainConfig = new JSONObject();
            domainConfig.put("visit", visitInfoJson);
            domainConfig.put("user_agent_filter", userAgentFilter);
            domainConfig.put("edgeone_security_policy", edgeOneSecurityPolicy);
            
            map.put("domainConfig", domainConfig);
            map.put("referer", referer);
            map.put("ipAcl", ipAcl);
            map.put("edgeOneSecurityPolicy", edgeOneSecurityPolicy);
        } catch (BusinessException e) {
            log.error("获取域名访问配置失败", e);
            return "redirect:/500";
        }
        map.put("domain", cdnDomainVo);
        return "admin/domain/domain-setting-access";
    }

    /**
     * 域名配置 高级配置
     */
    @GetMapping("/domain-setting-higher")
    public String domainHigherSettings(Long id, Map<String, Object> map) {
        if (Assert.isEmpty(id)) {
            return "redirect:/400";
        }
        CdnDomainVo cdnDomainVo = cdnDomainService.getCdnDomainVoById(id);
        if (Assert.isEmpty(cdnDomainVo)) {
            return "redirect:/404";
        }
        // 获取域名的详细配置
        try {
            String domainRoute = cdnDomainVo.getRoute();
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(domainRoute);
            DomainAdvancedInfo domainAdvancedInfo;
            if ("cdnetworks".equals(domainRoute)) {
                CdnetworksDomainServiceImpl service = (CdnetworksDomainServiceImpl) iCdnPlatformService;
                domainAdvancedInfo = service.getDomainAdvancedInfo(cdnDomainVo.getDomainName());
            } else {
                DomainConfig domainConfig = iCdnPlatformService.getDomainConfig(cdnDomainVo.getDomainName());
                domainAdvancedInfo = domainConfig.getDomainAdvancedInfo();
            }
            JSONObject advancedInfoJson = JSONObject.parseObject(JSON.toJSONString(domainAdvancedInfo));
            log.info("获取到配置信息{}", advancedInfoJson);
            
            // 添加调试日志，确保错误页面配置正确传递到前端
            if (advancedInfoJson.containsKey("error_pages")) {
                Object errorPages = advancedInfoJson.get("error_pages");
                log.info("错误页面配置数据: {}", errorPages);
                if (errorPages != null) {
                    log.info("错误页面配置类型: {}", errorPages.getClass().getName());
                }
            } else {
                log.info("未找到错误页面配置数据");
            }
            
            map.put("domainConfig", advancedInfoJson);
        } catch (BusinessException e) {
            return "redirect:/500";
        }
        map.put("domain", cdnDomainVo);
        return "admin/domain/domain-setting-higher";
    }

}
