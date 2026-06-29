package com.kuocai.cdn.controller.rest;

import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.annotation.SysLog;
import com.kuocai.cdn.api.huawei.cdn.constant.DomainStatus;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.CdnDomainService;
import com.kuocai.cdn.service.domain.operation.ICdnPlatformService;
import com.kuocai.cdn.service.domain.operation.KingsoftDomainServiceImpl;
import com.kuocai.cdn.service.factory.CdnPlatformFactory;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.CdnDomainSourcesVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 域名源站(CdnDomainSources)控制器
 *
 * @author XUEW
 * @since 2023-02-26 22:37:19
 */
@Slf4j
@RestController
@RequestMapping(value = "CdnDomainSources")
@Scope(value = "session")
public class CdnDomainSourcesController extends BaseController {

    @Autowired
    protected CdnDomainService cdnDomainService;
    
    @Autowired
    private KingsoftDomainServiceImpl kingsoftDomainService;


    /**
     * 获取域名支持的回源协议列表
     *
     * @param domainId 域名ID
     * @return 支持的协议列表
     */
    @GetMapping("supportedProtocols")
    public RespResult getSupportedOriginProtocols(@RequestParam Long domainId) {
        if (Assert.isEmpty(domainId)) {
            return RespResult.fail("域名ID不能为空");
        }
        
        CdnDomain cdnDomain = cdnDomainService.queryById(domainId);
        if (Assert.isEmpty(cdnDomain)) {
            return RespResult.fail("域名不存在");
        }
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        
        try {
            // 只有金山云需要这种限制，其他平台暂时都支持所有协议
            if (CdnRoute.KINGSOFT.getCode().equals(cdnDomain.getRoute())) {
                List<Map<String, String>> protocols = kingsoftDomainService.getSupportedOriginProtocols(cdnDomain);
                return RespResult.success("获取成功", protocols);
            } else {
                // 其他平台返回所有协议选项
                return RespResult.success("获取成功", getAllOriginProtocols());
            }
        } catch (Exception e) {
            log.error("获取域名 {} 支持的协议列表失败", cdnDomain.getDomainName(), e);
            return RespResult.fail("获取支持协议列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取CDN类型信息
     *
     * @param domainId 域名ID
     * @return CDN类型信息
     */
    @GetMapping("cdnTypeInfo")
    public RespResult getCdnTypeInfo(@RequestParam Long domainId) {
        if (Assert.isEmpty(domainId)) {
            return RespResult.fail("域名ID不能为空");
        }
        
        CdnDomain cdnDomain = cdnDomainService.queryById(domainId);
        if (Assert.isEmpty(cdnDomain)) {
            return RespResult.fail("域名不存在");
        }
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        
        try {
            if (CdnRoute.KINGSOFT.getCode().equals(cdnDomain.getRoute())) {
                Map<String, Object> info = kingsoftDomainService.getCdnTypeInfo(cdnDomain);
                return RespResult.success("获取成功", info);
            } else {
                // 其他平台的默认信息
                Map<String, Object> info = new HashMap<>();
                info.put("cdnType", "unknown");
                info.put("cdnTypeName", "标准CDN");
                info.put("supportOriginProtocolModify", true);
                info.put("supportedProtocols", getAllOriginProtocols());
                return RespResult.success("获取成功", info);
            }
        } catch (Exception e) {
            log.error("获取域名 {} CDN类型信息失败", cdnDomain.getDomainName(), e);
            return RespResult.fail("获取CDN类型信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有回源协议选项（默认支持）
     */
    private List<Map<String, String>> getAllOriginProtocols() {
        List<Map<String, String>> protocols = new ArrayList<>();
        
        Map<String, String> httpProtocol = new HashMap<>();
        httpProtocol.put("value", "http");
        httpProtocol.put("label", "HTTP");
        httpProtocol.put("description", "强制使用HTTP协议回源");
        protocols.add(httpProtocol);
        
        Map<String, String> httpsProtocol = new HashMap<>();
        httpsProtocol.put("value", "https");
        httpsProtocol.put("label", "HTTPS");
        httpsProtocol.put("description", "强制使用HTTPS协议回源");
        protocols.add(httpsProtocol);
        
        Map<String, String> followProtocol = new HashMap<>();
        followProtocol.put("value", "follow");
        followProtocol.put("label", "协议跟随");
        followProtocol.put("description", "跟随用户请求协议回源（推荐）");
        protocols.add(followProtocol);
        
        return protocols;
    }

    /**
     * 修改域名源站信息，直接修改华为云（本地没有做这个表的存储）
     *
     * @return {@code RespResult}
     */
    @RateLimiter
    @PostMapping("save")
    @SysLog(module = "站点管理", describe = "修改域名源站信息")
    public RespResult save(@RequestBody CdnDomainSourcesVo vo) {
        if (Assert.isEmpty(vo) || Assert.isEmpty(vo.getCdnDomainId())) {
            return RespResult.fail("参数错误");
        }
        // 查询对应的加速域名
        CdnDomain cdnDomain = cdnDomainService.queryById(vo.getCdnDomainId());
        if (Assert.isEmpty(cdnDomain)) {
            return RespResult.fail("没有对应的加速域名");
        }
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        try {
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(cdnDomain.getRoute());
            iCdnPlatformService.saveSourceStationConfig(cdnDomain, vo);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        // 修改本地加速域名状态为配置中
        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
        cdnDomainService.save(cdnDomain);
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }

    /**
     * 主源站 备源站 切换
     *
     * @param id
     * @return
     */
    @RateLimiter
    @PostMapping("change")
    @SysLog(module = "站点管理", describe = "主源站备源站切换")
    public RespResult change(Long id) {
        if (Assert.isEmpty(id)) {
            return RespResult.fail("参数错误");
        }
        // 查询对应的加速域名
        CdnDomain cdnDomain = cdnDomainService.queryById(id);
        if (Assert.isEmpty(cdnDomain)) {
            return RespResult.fail("没有对应的加速域名");
        }
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        try {
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(cdnDomain.getRoute());
            iCdnPlatformService.change(cdnDomain);
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
        // 修改本地加速域名状态为配置中
        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
        cdnDomainService.save(cdnDomain);
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }
}
