package com.kuocai.cdn.api.volcengine.cdn;

import com.alibaba.fastjson.JSON;
import com.kuocai.cdn.api.volcengine.cdn.properties.VolcengineCdn;
import com.volcengine.model.beans.CDN;
import com.volcengine.service.cdn.CDNService;
import com.volcengine.service.cdn.impl.CDNServiceImpl;

import java.util.Arrays;

public class AddCdnDomain {
    public static void main(String[] args) {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            CDN.AddCdnDomainRequest req = new CDN.AddCdnDomainRequest()
                    .setProject(VolcengineCdn.Project)
                    .setDomain("kedaya.site")
                    .setServiceType("web")
                    .setOriginProtocol("HTTP")
                    .setOrigin(Arrays.asList(new CDN.OriginRule().setOriginAction(new CDN.OriginAction().setOriginLines(Arrays.asList(
                                    new CDN.OriginLine()
                                            .setOriginType("primary")
                                            .setInstanceType("ip")
                                            .setAddress("124.220.182.8")
                                            .setHttpPort("80")
                                            .setHttpsPort("443")
                                            .setWeight("100")
                            )
                    ))));
            CDN.AddCdnDomainResponse resp = service.addCdnDomain(req);
        } catch (Exception e) {
            e.printStackTrace();
        }
        service.destroy();
    }
}