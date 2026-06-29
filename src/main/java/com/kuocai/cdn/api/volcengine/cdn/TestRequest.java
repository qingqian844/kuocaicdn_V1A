package com.kuocai.cdn.api.volcengine.cdn;

import cn.hutool.core.date.DateUtil;
import com.kuocai.cdn.api.volcengine.cdn.properties.VolcengineCdn;
import com.volcengine.model.beans.CDN;
import com.volcengine.service.cdn.CDNService;
import com.volcengine.service.cdn.impl.CDNServiceImpl;

public class TestRequest {
    public static void main(String[] args) {
        CDNService service = CDNServiceImpl.getInstance();
        service.setAccessKey(VolcengineCdn.AK);
        service.setSecretKey(VolcengineCdn.SK);
        try {
            long start = DateUtil.yesterday().getTime() / 1000;
            long end = DateUtil.date().getTime() / 1000;
//            CDN.DescribeEdgeNrtDataSummaryRequest req = new CDN.DescribeEdgeNrtDataSummaryRequest()
//                    .setDomain("kedaya.site,pengyuyan.plus")
//                    .setStartTime(start)
//                    .setEndTime(end)
//                    .setMetric("flux")
//                    .setAggregate("aggregate");
//            CDN.DescribeEdgeNrtDataSummaryResponse resp = null;
//            resp = service.describeEdgeNrtDataSummary(req);
            CDN.DescribeCdnDataRequest req = new CDN.DescribeCdnDataRequest()
                    .setDomain("kedaya.site,pengyuyan.plus")
                    .setStartTime(start)
                    .setEndTime(end)
                    .setMetric("flux")
                    .setAggregate("aggregate");
            CDN.DescribeCdnDataResponse resp = null;
            resp = service.describeCdnData(req);
        } catch (Exception e) {
        } finally {
            service.destroy();
        }
    }
}
