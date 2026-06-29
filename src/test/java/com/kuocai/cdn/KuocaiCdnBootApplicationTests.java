package com.kuocai.cdn;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.huawei.cdn.PreheatingApi;
import com.kuocai.cdn.api.huawei.cdn.dto.PreheatingTaskDTO;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.Message;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.schedule.FlowBillingTask;
import com.kuocai.cdn.service.CdnDomainService;
import com.kuocai.cdn.service.CdnDomainStatisticsService;
import com.kuocai.cdn.service.MessageService;
import com.kuocai.cdn.service.domain.operation.BaiShanDomainServiceImpl;
import com.kuocai.cdn.service.domain.operation.QiNiuDomainServiceImpl;
import com.kuocai.cdn.service.domain.operation.YiFanDomainServiceImpl;
import com.kuocai.cdn.service.domain.statistics.QiNiuDomainStatisticsServiceImpl;
import com.kuocai.cdn.service.domain.statistics.VolCenGineDomainStatisticsServiceImpl;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

import static com.kuocai.cdn.constant.StatisticsType.RESOURCE;

@SpringBootTest
class KuocaiCdnBootApplicationTests {

    @Autowired
    private FlowBillingTask flowBillingConfig;

    @Resource
    private CdnDomainService cdnDomainService;

    @Resource
    private CdnDomainStatisticsService cdnDomainStatisticsService;

    @Autowired
    private MessageService service;

    @Autowired
    private BaiShanDomainServiceImpl baiShanDomainService;

    @Resource
    private QiNiuDomainStatisticsServiceImpl statisticsService;

    @Test
    void contextLoads() throws BusinessException, TencentCloudSDKException {
//        CdnDomain domain = cdnDomainService.queryByDomainName("bs.kedaya.site");
//        baiShanDomainService.configDNS(domain);
        flowBillingConfig.flowBillingLogic();
    }

}
