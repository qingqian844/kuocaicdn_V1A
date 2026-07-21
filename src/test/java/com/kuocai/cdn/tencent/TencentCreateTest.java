package com.kuocai.cdn.tencent;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.CdnDomainService;
import com.kuocai.cdn.service.CdnDomainStatisticsService;
import com.kuocai.cdn.service.domain.operation.TencentDomainServiceImpl;
import com.kuocai.cdn.service.domain.statistics.TencentDomainStatisticsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest
@Disabled("Manual Tencent Cloud integration test; requires live credentials and infrastructure")
public class TencentCreateTest {

    @Autowired
    private TencentDomainServiceImpl tencentDomainService;

    @Autowired
//    private VolCenGineDomainStatisticsServiceImpl service;
    private TencentDomainStatisticsServiceImpl service;

    // cdnDomainService
    @Autowired
    private CdnDomainService cdnDomainService;

    @Autowired
    private CdnDomainStatisticsService cdnDomainStatisticsService;

    @Test
    public void testCreate() {
        System.out.println("Test Tencent Create");
        try {
            CdnDomain domain = tencentDomainService.create(1L, "fastly.truimo.com", "web", "mainland_china", "ipaddr", "127.0.0.1");
            System.out.println(domain);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void testVerifyRecord() {
        System.out.println("Test Tencent Verify");
        try {
            tencentDomainService.createVerifyRecord("fastly.truimo.com");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    public void test() throws BusinessException {
        DateTime start = new DateTime("2024-02-25 17:00:00");
        DateTime end = new DateTime("2024-02-25 18:00:00");
//        JSONObject object = service.queryResourceStatistics("zma.com.jishangsw.xyz", start, end);
        JSONObject object = service.queryResourceStatistics("new.xianbao.fun", start, end);
        System.out.println(object);
    }

    private DateTime getStartHourTime() {
        return DateUtil.offsetHour(getNowHourTime(), -1);
    }

    private DateTime getEndHourTime() {
        return getNowHourTime();
    }

    private DateTime getNowHourTime() {
        return DateUtil.beginOfHour(new DateTime());
    }


    @Test
    public void test02() throws BusinessException {
        List<CdnDomain> cdnDomains = cdnDomainService.queryAll();
        Map<Long, List<CdnDomain>> userIdMaps = cdnDomains.stream().collect(Collectors.groupingBy(CdnDomain::getUserId));
        for (Map.Entry<Long, List<CdnDomain>> userIdEntry : userIdMaps.entrySet()) {
            Long userId = userIdEntry.getKey();
            if (userId != 1655041628341899265L) {
                continue;
            }
            List<CdnDomain> userCdnDomains = userIdEntry.getValue();
            String domainNames = userCdnDomains.stream().map(CdnDomain::getDomainName).sorted().collect(Collectors.joining(","));
            DateTime start = getStartHourTime();
            DateTime end = getEndHourTime();
            System.out.printf("start: %s, end: %s\n", start, end);
            JSONObject resource = null;
            resource = cdnDomainStatisticsService.mergeAllPlatForm(userCdnDomains, start, end, "Resource", userId);
            System.out.println(resource);
        }
    }
}
