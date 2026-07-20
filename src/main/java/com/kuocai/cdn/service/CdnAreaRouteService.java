package com.kuocai.cdn.service;

import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.api.tencent.edgeone.TencentEdgeOneClient;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.SupportedVendorUtils;
import com.kuocai.cdn.vo.AreaRouteTargetVo;
import com.kuocai.cdn.vo.ResolvedAreaRouteVo;
import com.kuocai.cdn.vo.WebsiteBaseConfigVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.CRC32;

@Slf4j
@Service
public class CdnAreaRouteService {

    public static final String MODE_LOAD_BALANCE = "load_balance";
    public static final String MODE_MULTI_CDN = "multi_cdn";

    public ResolvedAreaRouteVo resolve(Long userId, String fallbackRoute, String domainName,
                                       String serviceArea) throws BusinessException {
        List<AreaRouteTargetVo> targets = resolveConfiguredTargets(serviceArea);
        if (targets.isEmpty()) {
            if (!CdnServiceAreaPolicyService.MAINLAND.equals(serviceArea)) {
                throw new BusinessException(areaName(serviceArea) + "暂未配置可用线路，请联系管理员");
            }
            targets = Collections.singletonList(resolveFallbackTarget(userId, fallbackRoute, serviceArea));
        }
        String mode = configuredMode(serviceArea);
        if (MODE_LOAD_BALANCE.equals(mode) && targets.size() > 1) {
            int index = stableIndex(userId, domainName, serviceArea, targets.size());
            targets = Collections.singletonList(targets.get(index));
        }
        return ResolvedAreaRouteVo.builder()
                .serviceArea(serviceArea)
                .mode(mode)
                .targets(targets)
                .build();
    }

    public boolean isAreaAvailable(String serviceArea) {
        if (CdnServiceAreaPolicyService.MAINLAND.equals(serviceArea)) {
            return true;
        }
        try {
            return !resolveConfiguredTargets(serviceArea).isEmpty();
        } catch (BusinessException e) {
            return false;
        }
    }

    public boolean isAreaAvailable(Long userId, String fallbackRoute, String serviceArea) {
        try {
            ResolvedAreaRouteVo plan = resolve(userId, fallbackRoute, "availability-check.invalid", serviceArea);
            return plan != null && !Assert.isEmpty(plan.getTargets());
        } catch (BusinessException e) {
            return false;
        }
    }

    public List<AreaRouteTargetVo> resolveConfiguredTargets(String serviceArea) throws BusinessException {
        WebsiteBaseConfigVo config = SystemConfig.websiteBaseConfig;
        List<String> targetKeys = configuredTargetKeys(config, serviceArea);
        if (targetKeys.isEmpty()) {
            return Collections.emptyList();
        }
        List<AreaRouteTargetVo> targets = new ArrayList<>();
        for (String targetKey : targetKeys) {
            try {
                targets.add(resolveTarget(targetKey, serviceArea));
            } catch (BusinessException e) {
                log.warn("跳过不可用的区域线路目标，区域={}，目标={}，原因={}",
                        serviceArea, targetKey, e.getMessage());
            }
        }
        return targets;
    }

    public List<String> configuredSelfHostedRoutes() {
        Set<String> routes = new LinkedHashSet<>();
        for (String serviceArea : new String[]{
                CdnServiceAreaPolicyService.MAINLAND,
                CdnServiceAreaPolicyService.OVERSEAS,
                CdnServiceAreaPolicyService.GLOBAL}) {
            try {
                for (AreaRouteTargetVo target : resolveConfiguredTargets(serviceArea)) {
                    if (target != null && CdnRoute.isSelfHosted(target.getRoute())) {
                        routes.add(target.getRoute());
                    }
                }
            } catch (BusinessException e) {
                log.warn("读取自建 CDN 区域线路失败，区域={}，原因={}", serviceArea, e.getMessage());
            }
        }
        return new ArrayList<>(routes);
    }

    public String configuredMode(String serviceArea) {
        WebsiteBaseConfigVo config = SystemConfig.websiteBaseConfig;
        String mode = null;
        if (config != null) {
            if (CdnServiceAreaPolicyService.MAINLAND.equals(serviceArea)) {
                mode = config.getMainlandRouteMode();
            } else if (CdnServiceAreaPolicyService.OVERSEAS.equals(serviceArea)) {
                mode = config.getOverseasRouteMode();
            } else if (CdnServiceAreaPolicyService.GLOBAL.equals(serviceArea)) {
                mode = config.getGlobalRouteMode();
            }
        }
        return normalizeMode(mode);
    }

    public String normalizeMode(String mode) {
        return MODE_MULTI_CDN.equals(mode) ? MODE_MULTI_CDN : MODE_LOAD_BALANCE;
    }

    public String describeTargets(String serviceArea) {
        try {
            List<AreaRouteTargetVo> targets = resolveConfiguredTargets(serviceArea);
            List<String> names = new ArrayList<>();
            for (AreaRouteTargetVo target : targets) {
                names.add(target.getAccountName());
            }
            return String.join("、", names);
        } catch (BusinessException e) {
            return "";
        }
    }

    private AreaRouteTargetVo resolveFallbackTarget(Long userId, String fallbackRoute,
                                                     String serviceArea) throws BusinessException {
        if (Assert.isEmpty(fallbackRoute)) {
            throw new BusinessException("当前用户未配置默认 CDN 线路");
        }
        String fixedArea = CdnRoute.selfHostedServiceArea(fallbackRoute);
        if (fixedArea != null && !fixedArea.equals(serviceArea)) {
            throw new BusinessException("当前用户默认线路不支持" + areaName(serviceArea));
        }
        if (!SupportedVendorUtils.allVendorCodes().contains(fallbackRoute)
                && !CdnRoute.SELF_HOSTED.getCode().equals(fallbackRoute)) {
            throw new BusinessException("当前系统不支持线路：" + vendorName(fallbackRoute));
        }
        return routeTarget(fallbackRoute);
    }

    private AreaRouteTargetVo resolveTarget(String targetKey, String serviceArea) throws BusinessException {
        if (targetKey.startsWith(CdnServiceAreaPolicyService.ACCOUNT_TARGET_PREFIX)) {
            throw new BusinessException("开源版不支持厂商多账号区域目标");
        }
        if (targetKey.startsWith(CdnServiceAreaPolicyService.ROUTE_TARGET_PREFIX)) {
            String route = targetKey.substring(CdnServiceAreaPolicyService.ROUTE_TARGET_PREFIX.length());
            requireTargetArea(route, serviceArea);
            return routeTarget(route);
        }
        throw new BusinessException("区域线路目标格式不正确：" + targetKey);
    }

    private AreaRouteTargetVo routeTarget(String route) {
        return AreaRouteTargetVo.builder()
                .targetKey(CdnServiceAreaPolicyService.routeTarget(route))
                .route(route)
                .routeName(vendorName(route))
                .accountName(vendorName(route))
                .build();
    }

    private void requireTargetArea(String route, String serviceArea) throws BusinessException {
        if (!SupportedVendorUtils.allVendorCodes().contains(route)
                || CdnRoute.MULTI_CDN.getCode().equals(route)
                || CdnRoute.SELF_HOSTED.getCode().equals(route)) {
            throw new BusinessException("当前系统不支持线路：" + vendorName(route));
        }
        String fixedArea = CdnRoute.selfHostedServiceArea(route);
        if (fixedArea != null && !fixedArea.equals(serviceArea)) {
            throw new BusinessException(vendorName(route) + "不能用于" + areaName(serviceArea));
        }
    }

    private String vendorName(String route) {
        return SupportedVendorUtils.vendorNameMap().getOrDefault(route, route);
    }

    private List<String> configuredTargetKeys(WebsiteBaseConfigVo config, String serviceArea) {
        if (config == null) {
            return Collections.emptyList();
        }
        List<String> targets;
        if (CdnServiceAreaPolicyService.MAINLAND.equals(serviceArea)) {
            targets = config.getMainlandEnabledTargets();
        } else if (CdnServiceAreaPolicyService.OVERSEAS.equals(serviceArea)) {
            targets = config.getOverseasEnabledTargets();
            if (targets == null && config.getOverseasEnabledRoutes() != null) {
                targets = toRouteTargets(config.getOverseasEnabledRoutes());
            }
        } else if (CdnServiceAreaPolicyService.GLOBAL.equals(serviceArea)) {
            targets = config.getGlobalEnabledTargets();
            if (targets == null && config.getGlobalEnabledRoutes() != null) {
                targets = toRouteTargets(config.getGlobalEnabledRoutes());
            }
        } else {
            return Collections.emptyList();
        }
        return targets == null ? Collections.emptyList() : targets;
    }

    private List<String> toRouteTargets(List<String> routes) {
        List<String> targets = new ArrayList<>();
        for (String route : routes) {
            targets.add(CdnServiceAreaPolicyService.routeTarget(route));
        }
        return targets;
    }

    private int stableIndex(Long userId, String domainName, String serviceArea, int size) {
        String allocationDomain = domainName;
        try {
            allocationDomain = TencentEdgeOneClient.getRootDomain(domainName);
        } catch (Exception ignored) {
        }
        String identity = String.valueOf(userId) + ':' + String.valueOf(allocationDomain) + ':' + serviceArea;
        CRC32 crc32 = new CRC32();
        crc32.update(identity.getBytes(StandardCharsets.UTF_8));
        return (int) (crc32.getValue() % size);
    }

    private String areaName(String serviceArea) {
        if (CdnServiceAreaPolicyService.MAINLAND.equals(serviceArea)) {
            return "中国大陆加速";
        }
        if (CdnServiceAreaPolicyService.OVERSEAS.equals(serviceArea)) {
            return "中国境外加速";
        }
        return "全球加速";
    }
}
