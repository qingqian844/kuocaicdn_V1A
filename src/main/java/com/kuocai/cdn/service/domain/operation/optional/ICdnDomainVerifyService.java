package com.kuocai.cdn.service.domain.operation.optional;

import com.kuocai.cdn.api.DomainVerifyRecordInfo;
import com.kuocai.cdn.exception.BusinessException;

public interface ICdnDomainVerifyService {
    DomainVerifyRecordInfo createVerifyRecord(String domainName) throws BusinessException;

    default DomainVerifyRecordInfo createVerifyRecord(String domainName, String serviceArea) throws BusinessException {
        return createVerifyRecord(domainName);
    }

    void verifyDomainRecord(String domainName, String verifyType) throws BusinessException;
}
