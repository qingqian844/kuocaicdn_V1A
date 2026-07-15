package com.kuocai.cdn.service.domain.statistics;

import cn.hutool.core.date.DateTime;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.service.CdnDomainStatisticsService;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class SelfHostedDomainStatisticsServiceImpl implements ICdnStatisticsPlatformService {
    private final CdnDomainStatisticsService statisticsService;

    public SelfHostedDomainStatisticsServiceImpl(CdnDomainStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @Override
    public Object queryResourceStatistics(String domainName, DateTime start, DateTime end) {
        JSONArray zeros = zeros(start, end);
        JSONObject detail = new JSONObject();
        detail.put("flux", zeros); detail.put("bw", zeros); detail.put("bs_flux", zeros); detail.put("bs_bw", zeros);
        JSONObject summary = new JSONObject();
        summary.put("flux", 0); summary.put("bw", 0); summary.put("bs_flux", 0); summary.put("bs_bw", 0);
        JSONObject result = new JSONObject();
        result.put("resource_detail", detail); result.put("resource_summary", summary);
        return result;
    }

    @Override
    public Object queryVisitsStatistics(String domainName, DateTime start, DateTime end) {
        JSONArray zeros = zeros(start, end);
        JSONObject detail = new JSONObject();
        detail.put("req_num", zeros); detail.put("hit_flux", zeros); detail.put("bs_num", zeros); detail.put("hit_num", zeros);
        JSONObject summary = new JSONObject();
        summary.put("req_num", 0); summary.put("hit_flux", 0); summary.put("bs_num", 0); summary.put("hit_num", 0);
        JSONObject result = new JSONObject();
        result.put("visits_detail", detail); result.put("visits_summary", summary);
        return result;
    }

    @Override
    public Object queryHttpCodeStatusStatistics(String domainName, DateTime start, DateTime end) {
        JSONObject result = new JSONObject();
        result.put("status_detail", new JSONArray()); result.put("status_summary", new JSONArray());
        result.put("bs_status_detail", new JSONArray()); result.put("bs_status_summary", new JSONArray());
        return result;
    }

    @Override
    public Object queryTopUri(String domainName, DateTime start, DateTime end) {
        JSONObject result = new JSONObject();
        result.put("fluxResult", new JSONArray()); result.put("reqNumResult", new JSONArray());
        return result;
    }

    private JSONArray zeros(DateTime start, DateTime end) {
        return new JSONArray(Collections.nCopies(statisticsService.getLabels(start, end).size(), 0));
    }
}
