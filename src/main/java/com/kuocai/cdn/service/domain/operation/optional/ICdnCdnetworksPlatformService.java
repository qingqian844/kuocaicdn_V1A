package com.kuocai.cdn.service.domain.operation.optional;

import com.kuocai.cdn.api.*;
import com.kuocai.cdn.exception.BusinessException;

public interface ICdnCdnetworksPlatformService {
    public DomainConfig getDomainBasicConfig(String domainName) throws BusinessException;
    public DomainBackSourceInfo getDomainBackSourceInfo(String domainName) throws BusinessException;
    public DomainHttpsInfo getDomainHttpsInfo(String domainName) throws BusinessException;
    public DomainCacheInfo getDomainCacheInfo(String domainName) throws BusinessException;
    public DomainVisitInfo getDomainVisitInfo(String domainName) throws BusinessException;
    public DomainAdvancedInfo getDomainAdvancedInfo(String domainName) throws BusinessException;
}
