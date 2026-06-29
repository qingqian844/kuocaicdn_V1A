package com.kuocai.cdn.api.baidu.cdn.vo;

import lombok.Data;

@Data
public class DomainTxtPair {
    public DomainTxtPair() {
    }

    String verifyDomain;
    String targetTxt;
}
