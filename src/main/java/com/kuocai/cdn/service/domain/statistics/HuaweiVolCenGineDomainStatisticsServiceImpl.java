package com.kuocai.cdn.service.domain.statistics;

import cn.hutool.core.date.DateTime;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.CdnDomainStatisticsService;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.vo.statistics.HttpCodeStatusStatistics;
import com.kuocai.cdn.vo.statistics.ResourceStatistics;
import com.kuocai.cdn.vo.statistics.TopUri;
import com.kuocai.cdn.vo.statistics.VisitsStatistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 华为火山云加速域名统计(CdnDomain)服务
 */

@Slf4j
@Service
public class HuaweiVolCenGineDomainStatisticsServiceImpl extends BaseService<CdnDomain> implements ICdnStatisticsPlatformService {

    @Resource
    private HuaweiDomainStatisticsServiceImpl huawei;

    @Resource
    private VolCenGineDomainStatisticsServiceImpl volCenGine;

    @Resource
    private CdnDomainStatisticsService statisticsService;

    @Override
    public JSONObject queryResourceStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject huaweiResourceStatistics = huawei.queryResourceStatistics(domainName, start, end);
        JSONObject volCenGineResourceStatistics = volCenGine.queryResourceStatistics(domainName, start, end);
        ResourceStatistics huaweiObj = JSONObject.parseObject(huaweiResourceStatistics.toJSONString(), ResourceStatistics.class);
        ResourceStatistics volCenGineObj = JSONObject.parseObject(volCenGineResourceStatistics.toJSONString(), ResourceStatistics.class);
        ResourceStatistics resourceStatistics = statisticsService.mergeResourceData(huaweiObj, volCenGineObj);
        return JSON.parseObject(JSON.toJSONString(resourceStatistics));
    }

    @Override
    public JSONObject queryVisitsStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject huaweiVisitsStatistics = huawei.queryVisitsStatistics(domainName, start, end);
        JSONObject volCenGineVisitsStatistics = volCenGine.queryVisitsStatistics(domainName, start, end);
        VisitsStatistics huaweiObj = JSONObject.parseObject(huaweiVisitsStatistics.toJSONString(), VisitsStatistics.class);
        VisitsStatistics volCenGineObj = JSONObject.parseObject(volCenGineVisitsStatistics.toJSONString(), VisitsStatistics.class);
        VisitsStatistics visitsStatistics = statisticsService.mergeVisitsData(huaweiObj, volCenGineObj);
        return JSON.parseObject(JSON.toJSONString(visitsStatistics));
    }

    @Override
    public JSONObject queryHttpCodeStatusStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject huaweiHttpCodeStatusStatistics = huawei.queryHttpCodeStatusStatistics(domainName, start, end);
        JSONObject volCenGineHttpCodeStatusStatistics = volCenGine.queryHttpCodeStatusStatistics(domainName, start, end);
        HttpCodeStatusStatistics huaweiObj = JSONObject.parseObject(huaweiHttpCodeStatusStatistics.toJSONString(), HttpCodeStatusStatistics.class);
        HttpCodeStatusStatistics volCenGineObj = JSONObject.parseObject(volCenGineHttpCodeStatusStatistics.toJSONString(), HttpCodeStatusStatistics.class);
        HttpCodeStatusStatistics httpCodeStatusStatistics = statisticsService.mergeHttpCodeStatusData(huaweiObj, volCenGineObj);
        return JSON.parseObject(JSON.toJSONString(httpCodeStatusStatistics));
    }

    /**
     * 暂未使用
     * @param domainName
     * @param start
     * @param end
     * @return
     * @throws BusinessException
     */
    @Override
    public JSONObject queryTopUri(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject huaweiTopUri = huawei.queryTopUri(domainName, start, end);
        JSONObject volCenGineTopUri = volCenGine.queryTopUri(domainName, start, end);
        TopUri huaweiObj = JSONObject.parseObject(huaweiTopUri.toJSONString(), TopUri.class);
        TopUri volCenGineObj = JSONObject.parseObject(volCenGineTopUri.toJSONString(), TopUri.class);
        TopUri topUri = statisticsService.mergeTopUriData(huaweiObj, volCenGineObj);
        return JSON.parseObject(JSON.toJSONString(topUri));
    }
}
