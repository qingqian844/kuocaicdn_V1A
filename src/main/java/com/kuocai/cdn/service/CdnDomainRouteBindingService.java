package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.CdnDomainRouteBinding;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.Assert;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class CdnDomainRouteBindingService extends BaseService<CdnDomainRouteBinding> {

    public static final String STATUS_ACTIVE = "active";

    public List<CdnDomainRouteBinding> listActiveByDomainId(Long domainId) {
        if (domainId == null) {
            return Collections.emptyList();
        }
        QueryWrapper<CdnDomainRouteBinding> wrapper = new QueryWrapper<>();
        wrapper.eq("domain_id", domainId)
                .eq("status", STATUS_ACTIVE)
                .orderByDesc("primary_binding")
                .orderByAsc("id");
        return queryByWrapper(wrapper);
    }

    public void persistBindings(CdnDomain parent) {
        if (parent == null || parent.getId() == null || Assert.isEmpty(parent.getRouteBindings())) {
            return;
        }
        deleteByDomainId(parent.getId());
        Date now = new Date();
        for (CdnDomainRouteBinding binding : parent.getRouteBindings()) {
            binding.setId(null);
            binding.setDomainId(parent.getId());
            binding.setUserId(parent.getUserId());
            binding.setDomainName(parent.getDomainName());
            binding.setServiceArea(parent.getServiceArea());
            binding.setStatus(STATUS_ACTIVE);
            binding.setCreateTime(now);
            binding.setUpdateTime(now);
            save(binding);
        }
    }

    public void deleteByDomainId(Long domainId) {
        if (domainId == null) {
            return;
        }
        QueryWrapper<CdnDomainRouteBinding> wrapper = new QueryWrapper<>();
        wrapper.eq("domain_id", domainId);
        dao.delete(wrapper);
    }

    public CdnDomain toChildDomain(CdnDomain parent, CdnDomainRouteBinding binding) {
        CdnDomain child = null;
        if (Assert.notEmpty(binding.getDomainSnapshotJson())) {
            try {
                child = JSON.parseObject(binding.getDomainSnapshotJson(), CdnDomain.class);
            } catch (Exception ignored) {
            }
        }
        if (child == null) {
            child = new CdnDomain();
        }
        child.setId(binding.getLocalDomainId());
        child.setUserId(parent.getUserId());
        child.setDomainName(parent.getDomainName());
        child.setBusinessType(parent.getBusinessType());
        child.setServiceArea(parent.getServiceArea());
        child.setDomainStatus(parent.getDomainStatus());
        child.setRoute(binding.getRoute());
        child.setDomainId(binding.getUpstreamDomainId());
        return child;
    }

    public List<CdnDomain> expandForStatistics(List<CdnDomain> domains) {
        if (domains == null || domains.isEmpty()) {
            return domains;
        }
        List<CdnDomain> expanded = new ArrayList<>();
        for (CdnDomain domain : domains) {
            if (!CdnRoute.MULTI_CDN.getCode().equals(domain.getRoute())) {
                expanded.add(domain);
                continue;
            }
            List<CdnDomainRouteBinding> bindings = listActiveByDomainId(domain.getId());
            for (CdnDomainRouteBinding binding : bindings) {
                expanded.add(toChildDomain(domain, binding));
            }
        }
        return expanded;
    }

    public String bindingNames(Long domainId) {
        List<String> names = new ArrayList<>();
        for (CdnDomainRouteBinding binding : listActiveByDomainId(domainId)) {
            names.add(com.kuocai.cdn.util.SupportedVendorUtils.vendorNameMap()
                    .getOrDefault(binding.getRoute(), binding.getRoute()));
        }
        return String.join(" + ", names);
    }
}
