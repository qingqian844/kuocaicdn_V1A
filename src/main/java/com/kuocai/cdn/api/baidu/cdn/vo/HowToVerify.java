package com.kuocai.cdn.api.baidu.cdn.vo;

import lombok.Data;

import java.util.List;

@Data
public class HowToVerify {
    public HowToVerify() {
    }

    String domain;
    String type;
    List<DomainTxtPair> details;
}
