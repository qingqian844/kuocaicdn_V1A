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
import com.kuocai.cdn.vo.DomainOriginSettingVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 域名回源配置(CdnDomainOriginHost)控制器
 *
 * @author XUEW
 * @since 2023-02-26 23:30:24
 */
@Slf4j
@RestController
@RequestMapping(value = "CdnDomainOriginHost")
@Scope(value = "session")
public class CdnDomainOriginHostController extends BaseController {

    @Autowired
    protected CdnDomainService cdnDomainService;
    
    /**
     * 修改回源URL改写
     *
     * @return {@code RespResult}
     */
    @RateLimiter
    @PostMapping("saveOriginRequestUrlRewrite")
    @SysLog(module = "站点管理", describe = "修改回源URL改写")
    public RespResult DomainOriginSettingVo(@RequestBody DomainOriginSettingVo config) {
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
            iCdnPlatformService.saveOriginRequestUrlRewrite(cdnDomain, config);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        // 修改本地加速域名状态为配置中
        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
        cdnDomainService.save(cdnDomain);
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }

    /**
     * 修改高级回源
     *
     * @return {@code RespResult}
     */
    @RateLimiter
    @PostMapping("saveAdvancedReturnSource")
    @SysLog(module = "站点管理", describe = "修改高级回源")
    public RespResult saveAdvancedReturnSource(@RequestBody DomainOriginSettingVo config) {
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
            iCdnPlatformService.saveAdvancedReturnSource(cdnDomain, config);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        // 修改本地加速域名状态为配置中
        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
        cdnDomainService.save(cdnDomain);
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }


    /**
     * 修改Range回源
     *
     * @return {@code RespResult}
     */
    @RateLimiter
    @PostMapping("saveRangeSwitch")
    @SysLog(module = "站点管理", describe = "修改Range回源")
    public RespResult saveRangeSwitch(@RequestBody DomainOriginSettingVo config) {
        if (Assert.isEmpty(config.getDoMainId()) || Assert.isEmpty(config.getStatus())) {
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
            iCdnPlatformService.saveRangeSwitch(cdnDomain, config);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        // 修改本地加速域名状态为配置中
        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
        cdnDomainService.save(cdnDomain);
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }


    /**
     * 修改回源是否校验ETag
     *
     * @return {@code RespResult}
     */
    @RateLimiter
    @PostMapping("saveRangeVerifyETag")
    @SysLog(module = "站点管理", describe = "修改回源是否校验ETag")
    public RespResult saveRangeVerifyETag(@RequestBody DomainOriginSettingVo config) {
        if (Assert.isEmpty(config.getDoMainId()) || Assert.isEmpty(config.getStatus())) {
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
            iCdnPlatformService.saveRangeVerifyETag(cdnDomain, config);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        // 修改本地加速域名状态为配置中
        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
        cdnDomainService.save(cdnDomain);
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }

    /**
     * 修改回源超时时间
     *
     * @return {@code RespResult}
     */
    @RateLimiter
    @PostMapping("saveRangeTimeOut")
    @SysLog(module = "站点管理", describe = "修改回源超时时间")
    public RespResult saveRangeTimeOut(@RequestBody DomainOriginSettingVo config) {
        if (Assert.isEmpty(config.getDoMainId()) || Assert.isEmpty(config.getOriginReceiveTimeOut())) {
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
            iCdnPlatformService.saveRangeTimeOut(cdnDomain, config);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        // 修改本地加速域名状态为配置中
        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
        cdnDomainService.save(cdnDomain);
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }

    /**
     * 修改回源请求头
     *
     * @return {@code RespResult}
     */
    @RateLimiter
    @PostMapping("saveOriginRequestHeader")
    @SysLog(module = "站点管理", describe = "修改回源请求头")
    public RespResult saveOriginRequestHeader(@RequestBody DomainOriginSettingVo config) {
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
            iCdnPlatformService.saveOriginRequestHeader(cdnDomain, config);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        // 修改本地加速域名状态为配置中
        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
        cdnDomainService.save(cdnDomain);
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }

    /**
     * 修改回源协议
     *
     * @return {@code RespResult}
     */
    @RateLimiter
    @PostMapping("saveOriginProtocol")
    @SysLog(module = "站点管理", describe = "修改回源协议")
    public RespResult saveOriginProtocol(@RequestBody DomainOriginSettingVo config) {
        if (Assert.isEmpty(config.getDoMainId()) || Assert.isEmpty(config.getOriginProtocol())) {
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
            iCdnPlatformService.saveOriginProtocol(cdnDomain, config);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }

        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
        cdnDomainService.save(cdnDomain);
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }

//    /**
//     * 修改回源协议
//     *
//     * @return {@code RespResult}
//     */
//    @RateLimiter
//    @PostMapping("saveOriginProtocol")
//    @SysLog(module = "站点管理", describe = "修改回源协议")
//    public RespResult saveOriginProtocol(@RequestBody DomainOriginSettingVo config) {
//        if (Assert.isEmpty(config.getDoMainId()) || Assert.isEmpty(config.getOriginProtocol())) {
//            return RespResult.fail("参数错误");
//        }
//
//        // 查询对应的加速域名
//        CdnDomain cdnDomain = cdnDomainService.queryById(config.getDoMainId());
//        if (Assert.isEmpty(cdnDomain)) {
//            return RespResult.fail("没有对应的加速域名");
//        }
//
//        try {
//            // 如果前端传递了端口信息，同时更新端口配置
//            if (config.getHttpPort() != null || config.getHttpsPort() != null) {
//                log.info("同时更新回源端口配置：HTTP={}, HTTPS={}", config.getHttpPort(), config.getHttpsPort());
//
//                // 这里可以调用保存源站配置的方法，但需要先获取当前的源站配置
//                if (cdnDomain.getRoute() == CdnRoute.KINGSOFT) {
//                    try {
//                        kingsoftDomainService.updateOriginPorts(cdnDomain, config.getHttpPort(), config.getHttpsPort());
//                    } catch (Exception e) {
//                        log.error("更新回源端口失败", e);
//                        // 端口更新失败不影响协议更新
//                    }
//                }
//            }
//
//            // 更新回源协议
//            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(cdnDomain.getRoute());
//            iCdnPlatformService.saveOriginProtocol(cdnDomain, config);
//        } catch (Exception e) {
//            return RespResult.fail(e.getMessage());
//        }
//
//        // 修改本地加速域名状态为配置中
//        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
//        cdnDomainService.save(cdnDomain);
//        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
//    }

}
