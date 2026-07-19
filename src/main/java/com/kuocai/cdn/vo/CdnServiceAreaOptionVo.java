package com.kuocai.cdn.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CdnServiceAreaOptionVo {
    private String targetKey;

    private String route;

    private String routeName;

    private Long accountId;

    private String accountName;

    private Boolean defaultAccount;

    private String accountStatus;

    private Boolean selectable;

    private String fixedArea;

    private Boolean mainlandEnabled;

    private Boolean overseasEnabled;

    private Boolean globalEnabled;
}
