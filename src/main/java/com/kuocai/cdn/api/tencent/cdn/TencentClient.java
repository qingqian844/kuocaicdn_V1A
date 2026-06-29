package com.kuocai.cdn.api.tencent.cdn;

import com.kuocai.cdn.api.tencent.cdn.properties.TencentCdn;
import com.tencentcloudapi.cdn.v20180606.CdnClient;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;

public class TencentClient {

    public static CdnClient getCdnClient() {
        Credential credential = new Credential(TencentCdn.SecretId, TencentCdn.SecretKey);
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint(TencentCdn.END_POINT);
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        return new CdnClient(credential, "", clientProfile);
    }
}
