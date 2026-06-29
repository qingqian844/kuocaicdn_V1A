package com.kuocai.cdn.controller.rest;

import cn.hutool.core.util.ObjectUtil;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.annotation.SysLog;
import com.kuocai.cdn.api.huawei.cdn.constant.DomainStatus;
import com.kuocai.cdn.api.huawei.cdn.dto.ErrorCodeCacheDTO;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.enumeration.CacheTaskType;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.service.CacheTaskService;
import com.kuocai.cdn.service.CdnDomainCacheService;
import com.kuocai.cdn.service.CdnDomainService;
import com.kuocai.cdn.service.domain.operation.ICdnPlatformService;
import com.kuocai.cdn.service.domain.operation.KingsoftDomainServiceImpl;
import com.kuocai.cdn.service.factory.CdnPlatformFactory;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.BrowserUtils;
import com.kuocai.cdn.util.JedisUtil;
import com.kuocai.cdn.util.ValidatorUtils;
import com.kuocai.cdn.vo.IgnoreQueryStringDTO;
import com.kuocai.cdn.vo.SettingCacheVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.kuocai.cdn.constant.KuoCaiConstants.*;

/**
 * 域名Cache配置(CdnDomainCache)控制器
 *
 * @author ItYoung
 * @since 2023-03-13 17:30:24
 */
@Slf4j
@RestController
@RequestMapping(value = "CdnDomainCache")
@Scope(value = "session")
public class CdnDomainCacheController extends BaseController {

    @Autowired
    private CdnDomainService cdnDomainService;

    @Resource
    private CdnDomainCacheService cdnDomainCacheService;

    @Resource
    private CacheTaskService cacheTaskService;

    /**
     * 保存缓存规则
     *
     * @return {@code RespResult}
     */
    @RateLimiter
    @PostMapping("saveCacheRules")
    @SysLog(module = "站点管理", describe = "保存缓存规则")
    public RespResult saveCacheRules(@RequestBody SettingCacheVo config) {
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
            iCdnPlatformService.saveCacheRules(cdnDomain, config);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        // 修改本地加速域名状态为配置中
        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
        cdnDomainService.save(cdnDomain);
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }

    @PostMapping("saveIgnoreQueryString")
    @SysLog(module = "站点管理", describe = "保存过滤参数规则")
    public RespResult saveIgnoreQueryString(@RequestParam("doMainId") Long doMainId, @RequestBody IgnoreQueryStringDTO config) {
        if (Assert.isEmpty(doMainId)) {
            return RespResult.fail("参数错误");
        }
        CdnDomain cdnDomain = cdnDomainService.queryById(doMainId);
        if (Assert.isEmpty(cdnDomain)) {
            return RespResult.fail("没有对应的加速域名");
        }
        RespResult accessResult = checkDomainAccess(cdnDomain);
        if (accessResult != null) {
            return accessResult;
        }
        try {
            ICdnPlatformService iCdnPlatformService = CdnPlatformFactory.getCdnPlatform(cdnDomain.getRoute());
            if (iCdnPlatformService instanceof KingsoftDomainServiceImpl) {
                ((KingsoftDomainServiceImpl) iCdnPlatformService).saveIgnoreQueryString(cdnDomain, config);
            } else {
                return RespResult.fail("该线路不支持过滤参数功能");
            }
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
        cdnDomainService.save(cdnDomain);
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }

    /**
     * 缓存遵循源站
     *
     * @return {@code RespResult}
     */
    @RateLimiter
    @PostMapping("saveCacheFollowOriginStatusSwitch")
    @SysLog(module = "站点管理", describe = "缓存遵循源站")
    public RespResult saveCacheFollowOriginStatusSwitch(@RequestBody SettingCacheVo config) {
        if (Assert.isEmpty(config.getDoMainId()) || Assert.isEmpty(config.getCacheFollowOriginStatus())) {
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
            iCdnPlatformService.saveCacheFollowOriginStatusSwitch(cdnDomain, config);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        // 修改本地加速域名状态为配置中
        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
        cdnDomainService.save(cdnDomain);
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }

    /**
     * 状态码缓存时间
     *
     * @return {@code RespResult}
     */
    @RateLimiter
    @PostMapping("saveErrorCodeCache")
    @SysLog(module = "站点管理", describe = "状态码缓存时间")
    public RespResult saveErrorCodeCache(@RequestBody SettingCacheVo config) {
        if (Assert.isEmpty(config.getDoMainId())) {
            return RespResult.fail("参数错误");
        }
        List<Integer> codes = Arrays.asList(400, 403, 404, 405, 414, 500, 501, 502, 503, 504);
        // TODO [400, 403, 404, 405 , 414, 500, 501 , 502 , 503 , 504]
        List<ErrorCodeCacheDTO> errorCodeCaches = config.getErrorCodeCache();
        if (Assert.notEmpty(errorCodeCaches)) {
            for (ErrorCodeCacheDTO errorCodeCach : errorCodeCaches) {
                Integer code = errorCodeCach.getCode();
                if (!codes.contains(code)) {
                    return RespResult.fail("状态码参数异常，请检查！");
                }
            }
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
            iCdnPlatformService.saveErrorCodeCache(cdnDomain, config);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        // 修改本地加速域名状态为配置中
        cdnDomain.setDomainStatus(DomainStatus.CONFIGURING);
        cdnDomainService.save(cdnDomain);
        return RespResult.success("配置正在部署中，大约需要5分钟的时间完成部署，请稍后。");
    }

    /**
     * 提交缓存预热接口
     */
    @RateLimiter
    @PostMapping("submitCachePreheating")
    @SysLog(module = "站点管理", describe = "缓存预热设置")
    public RespResult submitCachePreheating(@RequestParam(value = "urls[]") String[] urls) {
        if (Assert.isEmpty(urls)) {
            return RespResult.fail("参数异常");
        }
        for (String url : urls) {
            if (!ValidatorUtils.isIncludeHttpOrHttps(url)) {
                return RespResult.fail("请检查URL是否以http://或 https://开头");
            }
        }
        List<String> domainNames = Arrays.stream(urls).map(url -> {
            try {
                return BrowserUtils.getDomainByUrl(url);
            } catch (URISyntaxException e) {
                return "";
            }
        }).collect(Collectors.toList());
        RespResult accessResult = checkDomainNameAccess(domainNames);
        if (accessResult != null) {
            return accessResult;
        }
        List<CdnDomain> cdnDomains = cdnDomainService.queryByDomainNames(domainNames);
        Map<String, String> domainRouteMap = cdnDomains.stream().collect(Collectors.toMap(CdnDomain::getDomainName, CdnDomain::getRoute));

        List<String> mergeUrlList = new ArrayList<>();
        List<String> huaweiUrlList = new ArrayList<>();
        List<String> volcengineUrlList = new ArrayList<>();
        List<String> qiNiuUrlList = new ArrayList<>();
        List<String> baiShanUrlList = new ArrayList<>();
        List<String> tencentUrlList = new ArrayList<>();
        List<String> tencentEdgeOneUrlList = new ArrayList<>();
        List<String> cdnetworksUrlList = new ArrayList<>();
        List<String> aliyunUrlList = new ArrayList<>();
        List<String> baiduUrlList = new ArrayList<>();
        List<String> kingsoftUrlList = new ArrayList<>();

        String key = String.format("%s:%s:%s:%s", CacheTaskType.PREHEATING, loginUserRoleCode, loginUserId, loginUserId);
        JedisUtil.delKey(key);

        for (String url : urls) {
            try {
                String domain = BrowserUtils.getDomainByUrl(url);
                String route = domainRouteMap.get(domain);
                if (Assert.isEmpty(route)) {
                    continue;
                }
                if (ObjectUtil.equal(route, CdnRoute.HUAWEI.getCode())) {
                    huaweiUrlList.add(url);
                }
                if (ObjectUtil.equal(route, CdnRoute.VOLCENGINE.getCode())) {
                    volcengineUrlList.add(url);
                }
                if (ObjectUtil.equal(route, CdnRoute.HUAWEI_VOLCENGINE.getCode())) {
                    mergeUrlList.add(url);
                }
                if (ObjectUtil.equal(route, CdnRoute.QINIU.getCode())) {
                    qiNiuUrlList.add(url);
                }
                if (ObjectUtil.equal(route, CdnRoute.BAISHAN.getCode())) {
                    baiShanUrlList.add(url);
                }
                if (ObjectUtil.equal(route, CdnRoute.TENCENT.getCode())) {
                    tencentUrlList.add(url);
                }
                if (ObjectUtil.equal(route, CdnRoute.TENCENT_EDGEONE.getCode())) {
                    tencentEdgeOneUrlList.add(url);
                }
                if (ObjectUtil.equal(route, CdnRoute.CDNETWORKS.getCode())) {
                    cdnetworksUrlList.add(url);
                }
                if (ObjectUtil.equal(route, CdnRoute.ALIYUN.getCode())) {
                    aliyunUrlList.add(url);
                }
                if (ObjectUtil.equal(route, CdnRoute.BAIDU.getCode())) {
                    baiduUrlList.add(url);
                }
                if (ObjectUtil.equal(route, CdnRoute.KINGSOFT.getCode())) kingsoftUrlList.add(url);
            } catch (URISyntaxException e) {
                return RespResult.fail("URL格式错误：" + url);
            }
        }
        try {
            String result = "";
            if (Assert.notEmpty(mergeUrlList)) {
                result += cdnDomainCacheService.purchaseTicket(CACHE_PREHEATING_KEY, mergeUrlList, "preheating", null, loginUserId, CdnRoute.HUAWEI_VOLCENGINE.getCode());
            }
            if (Assert.notEmpty(huaweiUrlList)) {
                result += cdnDomainCacheService.purchaseTicket(CACHE_PREHEATING_KEY, huaweiUrlList, "preheating", null, loginUserId, CdnRoute.HUAWEI.getCode());
            }
            if (Assert.notEmpty(volcengineUrlList)) {
                result += cdnDomainCacheService.purchaseTicket(CACHE_PREHEATING_KEY, volcengineUrlList, "preheating", null, loginUserId, CdnRoute.VOLCENGINE.getCode());
            }
            if (Assert.notEmpty(qiNiuUrlList)) {
                result += cdnDomainCacheService.purchaseTicket(CACHE_PREHEATING_KEY, qiNiuUrlList, "preheating", null, loginUserId, CdnRoute.QINIU.getCode());
            }
            if (Assert.notEmpty(baiShanUrlList)) {
                result += cdnDomainCacheService.purchaseTicket(CACHE_PREHEATING_KEY, baiShanUrlList, "preheating", null, loginUserId, CdnRoute.BAISHAN.getCode());
            }
            if (Assert.notEmpty(tencentUrlList)) {
                result += cdnDomainCacheService.purchaseTicket(CACHE_PREHEATING_KEY, tencentUrlList, "preheating", null, loginUserId, CdnRoute.TENCENT.getCode());
            }
            if (Assert.notEmpty(tencentEdgeOneUrlList)) {
                result += cdnDomainCacheService.purchaseTicket(CACHE_PREHEATING_KEY, tencentEdgeOneUrlList, "preheating", null, loginUserId, CdnRoute.TENCENT_EDGEONE.getCode());
            }
            if (Assert.notEmpty(cdnetworksUrlList)) {
                result += cdnDomainCacheService.purchaseTicket(CACHE_PREHEATING_KEY, cdnetworksUrlList, "preheating", null, loginUserId, CdnRoute.CDNETWORKS.getCode());
            }
            if (Assert.notEmpty(aliyunUrlList)) {
                result += cdnDomainCacheService.purchaseTicket(CACHE_PREHEATING_KEY, aliyunUrlList, "preheating", null, loginUserId, CdnRoute.ALIYUN.getCode());
            }
            if (Assert.notEmpty(baiduUrlList)) {
                result += cdnDomainCacheService.purchaseTicket(CACHE_PREHEATING_KEY, baiduUrlList, "preheating", null, loginUserId, CdnRoute.BAIDU.getCode());
            }
            if (Assert.notEmpty(kingsoftUrlList)) result += cdnDomainCacheService.purchaseTicket(CACHE_PREHEATING_KEY, kingsoftUrlList, "preheating", null, loginUserId, CdnRoute.KINGSOFT.getCode());
            if (Assert.isEmpty(result)) {
                return RespResult.success("添加成功");
            } else {
                return RespResult.fail("添加失败:" + result);
            }
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
    }

    /**
     * 提交文件刷新接口
     */
    @RateLimiter
    @PostMapping("submitCacheRefresh")
    @SysLog(module = "站点管理", describe = "文件目录刷新设置")
    public RespResult submitCacheRefresh(@RequestParam(value = "urls[]") String[] urls, @RequestParam(value = "type") String type) {
        if (Assert.isEmpty(urls) || Assert.isEmpty(type)) {
            return RespResult.fail("参数异常");
        }
        // 判断是还有余额
        String fileType = "";
        if (ObjectUtil.equal("file", type)) {
            fileType = CACHE_REFRESH_URL_KEY;
        } else if (ObjectUtil.equal("directory", type)) {
            fileType = CACHE_REFRESH_FOLDER_KEY;
        } else {
            return RespResult.fail("参数异常");
        }

        for (String url : urls) {
            if (!ValidatorUtils.isIncludeHttpOrHttps(url)) {
                return RespResult.fail("请检查URL是否以http://或 https://开头");
            }
        }
        List<String> domainNames = Arrays.stream(urls).map(url -> {
            try {
                return BrowserUtils.getDomainByUrl(url);
            } catch (URISyntaxException e) {
                return "";
            }
        }).collect(Collectors.toList());
        RespResult accessResult = checkDomainNameAccess(domainNames);
        if (accessResult != null) {
            return accessResult;
        }
        List<CdnDomain> cdnDomains = cdnDomainService.queryByDomainNames(domainNames);
        Map<String, String> domainRouteMap = cdnDomains.stream().collect(Collectors.toMap(CdnDomain::getDomainName, CdnDomain::getRoute));

        List<String> mergeUrlList = new ArrayList<>();
        List<String> huaweiUrlList = new ArrayList<>();
        List<String> volcengineUrlList = new ArrayList<>();
        List<String> qiNiuUrlList = new ArrayList<>();
        List<String> baiShanUrlList = new ArrayList<>();
        List<String> tencentUrlList = new ArrayList<>();
        List<String> tencentEdgeOneUrlList = new ArrayList<>();
        List<String> cdnetworksUrlList = new ArrayList<>();
        List<String> aliyunUrlList = new ArrayList<>();
        List<String> baiduUrlList = new ArrayList<>();
        List<String> kingsoftUrlList = new ArrayList<>();

        String key = String.format("%s:%s:%s:%s", CacheTaskType.REFRESH, loginUserRoleCode, loginUserId, loginUserId);
        JedisUtil.delKey(key);

        for (String url : urls) {
            try {
                String domain = BrowserUtils.getDomainByUrl(url);
                String route = domainRouteMap.get(domain);
                if (Assert.isEmpty(route)) {
                    continue;
                }
                if (ObjectUtil.equal(route, CdnRoute.HUAWEI.getCode())) {
                    huaweiUrlList.add(url);
                }
                if (ObjectUtil.equal(route, CdnRoute.VOLCENGINE.getCode())) {
                    volcengineUrlList.add(url);
                }
                if (ObjectUtil.equal(route, CdnRoute.HUAWEI_VOLCENGINE.getCode())) {
                    mergeUrlList.add(url);
                }
                if (ObjectUtil.equal(route, CdnRoute.QINIU.getCode())) {
                    qiNiuUrlList.add(url);
                }
                if (ObjectUtil.equal(route, CdnRoute.BAISHAN.getCode())) {
                    baiShanUrlList.add(url);
                }
                if (ObjectUtil.equal(route, CdnRoute.TENCENT.getCode())) {
                    tencentUrlList.add(url);
                }
                if (ObjectUtil.equal(route, CdnRoute.TENCENT_EDGEONE.getCode())) {
                    tencentEdgeOneUrlList.add(url);
                }
                if (ObjectUtil.equal(route, CdnRoute.CDNETWORKS.getCode())) {
                    cdnetworksUrlList.add(url);
                }
                if (ObjectUtil.equal(route, CdnRoute.ALIYUN.getCode())) {
                    aliyunUrlList.add(url);
                }
                if (ObjectUtil.equal(route, CdnRoute.BAIDU.getCode())) {
                    baiduUrlList.add(url);
                }
                if (ObjectUtil.equal(route, CdnRoute.KINGSOFT.getCode())) kingsoftUrlList.add(url);
            } catch (URISyntaxException e) {
                return RespResult.fail("URL格式错误：" + url);
            }
        }
        try {
            String result = "";

            if (Assert.notEmpty(mergeUrlList)) {
                result += cdnDomainCacheService.purchaseTicket(fileType, mergeUrlList, "refresh", type, loginUserId, CdnRoute.HUAWEI_VOLCENGINE.getCode());
            }
            if (Assert.notEmpty(huaweiUrlList)) {
                result += cdnDomainCacheService.purchaseTicket(fileType, huaweiUrlList, "refresh", type, loginUserId, CdnRoute.HUAWEI.getCode());
            }
            if (Assert.notEmpty(volcengineUrlList)) {
                result += cdnDomainCacheService.purchaseTicket(fileType, volcengineUrlList, "refresh", type, loginUserId, CdnRoute.VOLCENGINE.getCode());
            }
            if (Assert.notEmpty(qiNiuUrlList)) {
                result += cdnDomainCacheService.purchaseTicket(fileType, qiNiuUrlList, "refresh", type, loginUserId, CdnRoute.QINIU.getCode());
            }
            if (Assert.notEmpty(baiShanUrlList)) {
                result += cdnDomainCacheService.purchaseTicket(fileType, baiShanUrlList, "refresh", type, loginUserId, CdnRoute.BAISHAN.getCode());
            }
            if (Assert.notEmpty(tencentUrlList)) {
                result += cdnDomainCacheService.purchaseTicket(fileType, tencentUrlList, "refresh", type, loginUserId, CdnRoute.TENCENT.getCode());
            }
            if (Assert.notEmpty(tencentEdgeOneUrlList)) {
                result += cdnDomainCacheService.purchaseTicket(fileType, tencentEdgeOneUrlList, "refresh", type, loginUserId, CdnRoute.TENCENT_EDGEONE.getCode());
            }
            if (Assert.notEmpty(cdnetworksUrlList)) {
                result += cdnDomainCacheService.purchaseTicket(fileType, cdnetworksUrlList, "refresh", type, loginUserId, CdnRoute.CDNETWORKS.getCode());
            }
            if (Assert.notEmpty(aliyunUrlList)) {
                result += cdnDomainCacheService.purchaseTicket(fileType, aliyunUrlList, "refresh", type, loginUserId, CdnRoute.ALIYUN.getCode());
            }
            if (Assert.notEmpty(baiduUrlList)) {
                result += cdnDomainCacheService.purchaseTicket(fileType, baiduUrlList, "refresh", type, loginUserId, CdnRoute.BAIDU.getCode());
            }
            if (Assert.notEmpty(kingsoftUrlList)) result += cdnDomainCacheService.purchaseTicket(fileType, kingsoftUrlList, "refresh", type, loginUserId, CdnRoute.KINGSOFT.getCode());
            if (Assert.isEmpty(result)) {
                return RespResult.success("添加成功");
            } else {
                return RespResult.fail("添加失败:" + result);
            }
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
    }
}
