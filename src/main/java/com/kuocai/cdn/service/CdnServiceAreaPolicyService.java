package com.kuocai.cdn.service;

import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.SupportedVendorUtils;
import com.kuocai.cdn.vo.WebsiteBaseConfigVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class CdnServiceAreaPolicyService {
    public static final String MAINLAND = "mainland_china";
    public static final String OVERSEAS = "outside_mainland_china";
    public static final String GLOBAL = "global";
    public static final String ACCOUNT_TARGET_PREFIX = "account:";
    public static final String ROUTE_TARGET_PREFIX = "route:";

    public List<String> normalizeConfiguredRoutes(String routes) throws BusinessException {
        if (Assert.isEmpty(routes)) {
            return Collections.emptyList();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String item : routes.split(",")) {
            String route = item == null ? "" : item.trim();
            if (route.isEmpty()) {
                continue;
            }
            if (!SupportedVendorUtils.allVendorCodes().contains(route) || CdnRoute.isSelfHosted(route)) {
                throw new BusinessException("加速区域线路不存在或不可配置：" + route);
            }
            normalized.add(route);
        }
        return new ArrayList<>(normalized);
    }

    public List<String> normalizeConfiguredTargets(String targets) throws BusinessException {
        if (Assert.isEmpty(targets)) {
            return Collections.emptyList();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String item : targets.split(",")) {
            String target = item == null ? "" : item.trim();
            if (target.isEmpty()) {
                continue;
            }
            if (target.startsWith(ACCOUNT_TARGET_PREFIX)) {
                throw new BusinessException("开源版不支持厂商多账号区域配置");
            }
            if (target.startsWith(ROUTE_TARGET_PREFIX)) {
                String route = target.substring(ROUTE_TARGET_PREFIX.length()).trim();
                requireSupportedRoute(route);
                normalized.add(routeTarget(route));
                continue;
            }
            throw new BusinessException("加速区域配置目标格式不正确：" + target);
        }
        return new ArrayList<>(normalized);
    }

    public boolean isAllowed(String route, String serviceArea) {
        return isAllowed(route, null, serviceArea);
    }

    public boolean isAllowed(String route, Long vendorAccountId, String serviceArea) {
        if (Assert.isEmpty(route) || !isKnownArea(serviceArea)) {
            return false;
        }
        String fixedArea = CdnRoute.selfHostedServiceArea(route);
        if (fixedArea != null) {
            return fixedArea.equals(serviceArea);
        }
        // Legacy self_hosted users select a concrete product route on the create page.
        if (CdnRoute.SELF_HOSTED.getCode().equals(route)) {
            return true;
        }
        if (!SupportedVendorUtils.allVendorCodes().contains(route) && !CdnRoute.SELF_HOSTED.getCode().equals(route)) {
            return false;
        }
        if (MAINLAND.equals(serviceArea)) {
            return true;
        }
        WebsiteBaseConfigVo config = SystemConfig.websiteBaseConfig;
        if (config == null) {
            return false;
        }
        List<String> configuredTargets = OVERSEAS.equals(serviceArea)
                ? config.getOverseasEnabledTargets()
                : config.getGlobalEnabledTargets();
        if (configuredTargets != null) {
            if (vendorAccountId != null && configuredTargets.contains(accountTarget(vendorAccountId))) {
                return true;
            }
            return configuredTargets.contains(routeTarget(route));
        }
        List<String> legacyRoutes = OVERSEAS.equals(serviceArea)
                ? config.getOverseasEnabledRoutes()
                : config.getGlobalEnabledRoutes();
        return legacyRoutes != null && legacyRoutes.contains(route);
    }

    public Set<String> allowedAreas(String route) {
        Set<String> areas = new LinkedHashSet<>();
        if (isAllowed(route, MAINLAND)) {
            areas.add(MAINLAND);
        }
        if (isAllowed(route, OVERSEAS)) {
            areas.add(OVERSEAS);
        }
        if (isAllowed(route, GLOBAL)) {
            areas.add(GLOBAL);
        }
        return areas;
    }

    public void requireAllowed(String route, String serviceArea) throws BusinessException {
        requireAllowed(route, null, serviceArea);
    }

    public void requireAllowed(String route, Long vendorAccountId, String serviceArea) throws BusinessException {
        if (isAllowed(route, vendorAccountId, serviceArea)) {
            return;
        }
        if (!isKnownArea(serviceArea)) {
            throw new BusinessException("不支持的加速区域");
        }
        String areaName = MAINLAND.equals(serviceArea) ? "中国大陆加速"
                : OVERSEAS.equals(serviceArea) ? "海外加速" : "全球加速";
        throw new BusinessException("当前系统未开放" + vendorName(route)
                + "的" + areaName + "，请联系管理员");
    }

    public static String accountTarget(Long accountId) {
        return ACCOUNT_TARGET_PREFIX + accountId;
    }

    public static String routeTarget(String route) {
        return ROUTE_TARGET_PREFIX + route;
    }

    private void requireSupportedRoute(String route) throws BusinessException {
        if (!SupportedVendorUtils.allVendorCodes().contains(route) || CdnRoute.isSelfHosted(route)) {
            throw new BusinessException("加速区域线路不存在或不可配置：" + route);
        }
    }

    private String vendorName(String route) {
        return SupportedVendorUtils.vendorNameMap().getOrDefault(route, route);
    }

    private boolean isKnownArea(String serviceArea) {
        return MAINLAND.equals(serviceArea) || OVERSEAS.equals(serviceArea) || GLOBAL.equals(serviceArea);
    }
}
