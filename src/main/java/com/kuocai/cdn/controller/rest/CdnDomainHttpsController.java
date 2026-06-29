package com.kuocai.cdn.controller.rest;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.annotation.SysLog;
import com.kuocai.cdn.api.huawei.cdn.DomainConfigureApi;
import com.kuocai.cdn.api.huawei.cdn.constant.DomainStatus;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.service.CdnDomainService;
import com.kuocai.cdn.service.domain.operation.ICdnPlatformService;
import com.kuocai.cdn.service.factory.CdnPlatformFactory;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.CertificationCreateVo;
import com.kuocai.cdn.vo.CertificationListVo;
import com.kuocai.cdn.vo.DomainHttpsSettingVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 域名Https配置(CdnDomainHttpsController)控制器
 *
 * @author XUEW
 * @since 2023-02-26 23:30:24
 */
@Slf4j
@RestController
@RequestMapping(value = "CdnDomainHttps")
@Scope(value = "session")
public class CdnDomainHttpsController extends BaseController {

    @Autowired
    private CdnDomainService cdnDomainService;

    /**
     * Https配置和TLS版本配置和HTTP/2配置和OCSP Stapling配置
     *
     * @return {@code RespResult}
     */
    @RateLimiter
    @PostMapping("httpsConfiguration")
    @SysLog(module = "站点管理", describe = "Https配置")
    public RespResult httpsConfiguration(@RequestBody DomainHttpsSettingVo config) {
        if (Assert.isEmpty(config.getDoMainId()) || Assert.isEmpty(config.getHttps())) {
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
            iCdnPlatformService.httpsConfiguration(cdnDomain, config);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        // 修改本地加速域名状态为配置中
        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
        cdnDomainService.save(cdnDomain);
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }

    /**
     * Https配置和TLS版本配置和HTTP/2配置和OCSP Stapling配置
     *
     * @return {@code RespResult}
     */
    @RateLimiter
    @PostMapping("httpsConfigurationOther")
    @SysLog(module = "站点管理", describe = "TLS版本配置和HTTP/2配置和OCSP Stapling配置")
    public RespResult httpsConfigurationOther(@RequestBody DomainHttpsSettingVo config) {
        if (Assert.isEmpty(config.getDoMainId()) || Assert.isEmpty(config.getHttps())) {
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
            iCdnPlatformService.httpsConfigurationOther(cdnDomain, config);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        // 修改本地加速域名状态为配置中
        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
        cdnDomainService.save(cdnDomain);
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }


    /**
     * 强制跳转
     *
     * @return {@code RespResult}
     */
    @RateLimiter
    @PostMapping("forcedToJump")
    @SysLog(module = "站点管理", describe = "强制跳转")
    public RespResult forcedToJump(@RequestBody DomainHttpsSettingVo config) {
        if (Assert.isEmpty(config.getDoMainId()) || Assert.isEmpty(config.getForceRedirect())) {
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
            String redirectCode = null;
            if (config.getForceRedirect() != null && "on".equals(config.getForceRedirect().getStatus())) {
                redirectCode = String.valueOf(config.getForceRedirect().getRedirect_code());
                // 如果redirectCode为空或null，设置默认值302
                if (redirectCode == null || "null".equals(redirectCode) || redirectCode.isEmpty()) {
                    redirectCode = "302";
                }
                log.info("强制跳转配置: status={}, redirectCode={}", 
                         config.getForceRedirect().getStatus(), redirectCode);
            }
            iCdnPlatformService.forcedToJump(cdnDomain, config, redirectCode);
        } catch (Exception e) {
            log.error("强制跳转配置失败: {}", e.getMessage(), e);
            return RespResult.fail(e.getMessage());
        }
        // 修改本地加速域名状态为配置中
        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
        cdnDomainService.save(cdnDomain);
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }

    /**
     * 新增配置证书
     *
     * @return {@code RespResult}
     */
    @RateLimiter
    @PostMapping("certificateNewConfiguration")
    @SysLog(module = "站点管理", describe = "新增配置证书")
    public RespResult certificateNewConfiguration(@RequestBody CertificationCreateVo paramVo) {
        if (Assert.isEmpty(paramVo.getDomainMultiCertificates()) || Assert.isEmpty(paramVo.getDomainMultiCertificates().getDomain_name())) {
            return RespResult.fail("参数错误");
        }
        List<String> domainNames = Arrays.stream(paramVo.getDomainMultiCertificates().getDomain_name().split(","))
                .map(String::trim)
                .filter(domainName -> !domainName.isEmpty())
                .collect(Collectors.toList());
        RespResult accessResult = checkDomainNameAccess(domainNames);
        if (accessResult != null) {
            return accessResult;
        }
        try {
            DomainConfigureApi.updateConfigHttpsInfo(paramVo.getDomainMultiCertificates());
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }

    /**
     * 修改配置证书
     *
     * @return {@code RespResult}
     */
    @RateLimiter
    @RequestMapping(value = "modifyCertificate", method = {RequestMethod.POST, RequestMethod.PUT})
    @SysLog(module = "站点管理", describe = "修改配置证书")
    public RespResult modifyCertificate(@RequestBody CertificationListVo paramVo) {
        if (Assert.isEmpty(paramVo.getHttps()) || Assert.isEmpty(paramVo.getDomainId())) {
            return RespResult.fail("参数错误");
        }
        CdnDomain cdnDomain = cdnDomainService.queryByDomainId(paramVo.getDomainId()).orElse(null);
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        try {
            DomainConfigureApi.updateHttpsInfo(paramVo.getDomainId(), paramVo.getHttps());
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }

    /**
     * 查询HTTPS证书关联域名接口
     *
     * @return {@code RespResult}
     */
    @RateLimiter
    @PostMapping("certificateAssociatedDomainName")
    @SysLog(module = "站点管理", describe = "查询HTTPS证书关联域名接口")
    public RespResult certificateAssociatedDomainName(@RequestBody CertificationCreateVo paramVo) {
        if (Assert.isEmpty(paramVo.getCertificateDomainName())) {
            return RespResult.fail("参数错误");
        }
        try {
            JSONObject jsonObject = DomainConfigureApi.certificateAssociatedDomainName(paramVo.getCertificateDomainName());
            return RespResult.success("查询成功", jsonObject);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
    }

}
