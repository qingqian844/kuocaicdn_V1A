package com.kuocai.cdn.service;

import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.license.LicenseService;
import com.kuocai.cdn.license.LicenseVendorRegistry;
import com.kuocai.cdn.util.Assert;
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

    private final LicenseService licenseService;

    public CdnServiceAreaPolicyService(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

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
            if (!LicenseVendorRegistry.isSupported(route) || CdnRoute.isSelfHosted(route)) {
                throw new BusinessException("加速区域线路不存在或不可配置：" + route);
            }
            if (!licenseService.isVendorAuthorized(route)) {
                throw new BusinessException("当前授权不包含加速区域线路：" + LicenseVendorRegistry.getName(route));
            }
            normalized.add(route);
        }
        return new ArrayList<>(normalized);
    }

    public boolean isAllowed(String route, String serviceArea) {
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
        if (!licenseService.isVendorAuthorized(route)) {
            return false;
        }
        if (MAINLAND.equals(serviceArea)) {
            return true;
        }
        WebsiteBaseConfigVo config = SystemConfig.websiteBaseConfig;
        if (config == null) {
            return false;
        }
        List<String> configuredRoutes = OVERSEAS.equals(serviceArea)
                ? config.getOverseasEnabledRoutes()
                : config.getGlobalEnabledRoutes();
        return configuredRoutes != null && configuredRoutes.contains(route);
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
        if (isAllowed(route, serviceArea)) {
            return;
        }
        if (!isKnownArea(serviceArea)) {
            throw new BusinessException("不支持的加速区域");
        }
        String areaName = MAINLAND.equals(serviceArea) ? "中国大陆加速"
                : OVERSEAS.equals(serviceArea) ? "海外加速" : "全球加速";
        throw new BusinessException("当前系统未开放" + LicenseVendorRegistry.getName(route)
                + "的" + areaName + "，请联系管理员");
    }

    private boolean isKnownArea(String serviceArea) {
        return MAINLAND.equals(serviceArea) || OVERSEAS.equals(serviceArea) || GLOBAL.equals(serviceArea);
    }
}
