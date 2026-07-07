package com.kuocai.cdn.controller.rest;

import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.annotation.SysLog;
import com.kuocai.cdn.api.huawei.cdn.constant.DomainStatus;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.service.CdnDomainService;
import com.kuocai.cdn.service.domain.operation.ICdnPlatformService;
import com.kuocai.cdn.service.factory.CdnPlatformFactory;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.EdgeOneSecurityPolicyVo;
import com.kuocai.cdn.vo.SettingAccessVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 域名管理访问配置
 *
 * @author ItYoung
 * @since 2023-03-13 17:30:24
 */
@RestController
@RequestMapping(value = "CdnDomainAccess")
@Scope(value = "session")
public class CdnDomainAccessController extends BaseController {

    @Autowired
    private CdnDomainService cdnDomainService;

    /**
     * 保存域名管理访问配置-防盗链信息
     *
     * @return {@code RespResult}
     */
    @RateLimiter
    @PostMapping("saveHotlinkPrevention")
    @SysLog(module = "站点管理", describe = "保存防盗链信息")
    public RespResult saveHotlinkPrevention(@RequestBody SettingAccessVo config) {
        if (Assert.isEmpty(config.getDoMainId()) || Assert.isEmpty(config.getReferer())) {
            return RespResult.fail("参数错误");
        }
        // 查询对应的加速域名
        CdnDomain cdnDomain = cdnDomainService.queryById(config.getDoMainId());
        if (Assert.isEmpty(cdnDomain)) {
            return RespResult.fail("没有对应的加速域名");
        }
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        try {
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(cdnDomain.getRoute());
            iCdnPlatformService.saveHotlinkPrevention(cdnDomain, config);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        // 修改本地加速域名状态为配置中
        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
        cdnDomainService.save(cdnDomain);
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }

    /**
     * 保存域名管理访问配置-IP黑白名单
     *
     * @return {@code RespResult}
     */
    @RateLimiter
    @PostMapping("saveIpBlackWhiteList")
    @SysLog(module = "站点管理", describe = "保存IP黑白名单")
    public RespResult saveIpBlackWhiteList(@RequestBody SettingAccessVo config) {
        if (Assert.isEmpty(config.getDoMainId())) {
            return RespResult.fail("参数错误");
        }
        // 查询对应的加速域名
        CdnDomain cdnDomain = cdnDomainService.queryById(config.getDoMainId());
        if (Assert.isEmpty(cdnDomain)) {
            return RespResult.fail("没有对应的加速域名");
        }
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        try {
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(cdnDomain.getRoute());
            iCdnPlatformService.saveIpBlackWhiteList(cdnDomain, config);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        // 修改本地加速域名状态为配置中
        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
        cdnDomainService.save(cdnDomain);
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }

    /**
     * 保存域名管理访问配置-User-Agent黑白名单信息
     *
     * @return {@code RespResult}
     */
    @RateLimiter
    @PostMapping("saveUserAgentFilter")
    @SysLog(module = "站点管理", describe = "保存User-Agent黑白名单信息")
    public RespResult saveUserAgentFilter(@RequestBody SettingAccessVo config) {
        if (Assert.isEmpty(config.getDoMainId()) || Assert.isEmpty(config.getUserAgentBlackAndWhiteListDTO())) {
            return RespResult.fail("参数错误");
        }
        // 查询对应的加速域名
        CdnDomain cdnDomain = cdnDomainService.queryById(config.getDoMainId());
        if (Assert.isEmpty(cdnDomain)) {
            return RespResult.fail("没有对应的加速域名");
        }
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        try {
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(cdnDomain.getRoute());
            iCdnPlatformService.saveUserAgentFilter(cdnDomain, config);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        // 修改本地加速域名状态为配置中
        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
        cdnDomainService.save(cdnDomain);
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }

    /**
     * 保存URL鉴权
     *
     * @return {@code RespResult}
     */
    @RateLimiter
    @PostMapping("saveUrlAuth")
    @SysLog(module = "站点管理", describe = "保存URL鉴权")
    public RespResult saveUrlAuth(@RequestBody SettingAccessVo config) {
        if (Assert.isEmpty(config.getDoMainId()) || Assert.isEmpty(config.getUrlAuth())) {
            return RespResult.fail("参数错误");
        }
        // 查询对应的加速域名
        CdnDomain cdnDomain = cdnDomainService.queryById(config.getDoMainId());
        if (Assert.isEmpty(cdnDomain)) {
            return RespResult.fail("没有对应的加速域名");
        }
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        try {
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(cdnDomain.getRoute());
            iCdnPlatformService.saveUrlAuth(cdnDomain, config);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        // 修改本地加速域名状态为配置中
        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
        cdnDomainService.save(cdnDomain);
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }

    @RateLimiter
    @PostMapping("saveEdgeOneSecurityPolicy")
    @SysLog(module = "站点管理", describe = "保存 EdgeOne 安全防护策略")
    public RespResult saveEdgeOneSecurityPolicy(@RequestBody EdgeOneSecurityPolicyVo config) {
        if (Assert.isEmpty(config) || Assert.isEmpty(config.getDoMainId())) {
            return RespResult.fail("参数错误");
        }
        CdnDomain cdnDomain = cdnDomainService.queryById(config.getDoMainId());
        if (Assert.isEmpty(cdnDomain)) {
            return RespResult.fail("没有对应的加速域名");
        }
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        try {
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(cdnDomain.getRoute());
            iCdnPlatformService.saveEdgeOneSecurityPolicy(cdnDomain, config);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
        cdnDomainService.save(cdnDomain);
        return RespResult.success("EdgeOne 安全防护策略正在部署中，请稍后刷新查看。");
    }
}
