package com.kuocai.cdn.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DomainVerifyRecordInfo {
    private String domainName;
    private String subDomain;
    private String record;
    private String recordType;
    private String fileVerifyUrl;
    private String[] fileVerifyDomains;
    private String fileVerifyName;
    private String content;
}
