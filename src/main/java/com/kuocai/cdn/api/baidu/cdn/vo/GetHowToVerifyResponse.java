package com.kuocai.cdn.api.baidu.cdn.vo;

import com.baidubce.services.cdn.model.CdnResponse;

import java.util.List;

public class GetHowToVerifyResponse extends CdnResponse {
    public GetHowToVerifyResponse() {
    }

    private List<HowToVerify> howToVerify;

    public List<HowToVerify> getHowToVerify() {
        return howToVerify;
    }

    public void setHowToVerify(List<HowToVerify> howToVerify) {
        this.howToVerify = howToVerify;
    }
}
