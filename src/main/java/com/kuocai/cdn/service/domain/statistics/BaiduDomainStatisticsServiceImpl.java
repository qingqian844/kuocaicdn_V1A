package com.kuocai.cdn.service.domain.statistics;

import cn.hutool.core.date.DateTime;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baidubce.services.cdn.CdnClient;
import com.baidubce.services.cdn.model.stat.GetMetricStatResponse;
import com.baidubce.services.cdn.model.stat.GetStatMetricRequest;
import com.baidubce.services.cdn.model.stat.StatDetail;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.CdnDomainStatisticsService;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.KuocaiDateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class BaiduDomainStatisticsServiceImpl extends BaseService<CdnDomain> implements ICdnStatisticsPlatformService {
    private final CdnDomainStatisticsService statisticsService;
    private final CdnClient baiduCdnClient;

    BaiduDomainStatisticsServiceImpl(@Qualifier("baiduCdnClient") CdnClient baiduCdnClient, CdnDomainStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
        this.baiduCdnClient = baiduCdnClient;
    }

    private CdnClient getClient() {
        return baiduCdnClient;
    }

    private JSONObject getUsageData(String domainName, DateTime start, DateTime end) {
        CdnClient client = getClient();
        JSONObject result = new JSONObject();
        JSONObject flux = new JSONObject();
        JSONObject bw = new JSONObject();
        String[] strings = domainName.split(",");
        GetStatMetricRequest request = new GetStatMetricRequest()
                .withMetric("flow")
                .withStartTime(KuocaiDateUtil.toISOString(start))
                .withEndTime(KuocaiDateUtil.toISOString(end))
                .withKey(Arrays.asList(strings))
                .withKeyType(0).withGroupBy("")
                .withPeriod(3600).withLevel("edge");
        GetMetricStatResponse response = client.getStatMetricData(request);
        List<StatDetail> details = response.getDetails();
        JSONArray fluxDetail = new JSONArray(); long fluxSum = 0;
        JSONArray bwDetail = new JSONArray(); long bwSum = 0;
        for (StatDetail detail : details) {
            fluxDetail.add(detail.getFlow());
            bwDetail.add(detail.getBps());
            fluxSum = fluxSum + detail.getFlow();
            bwSum = bwSum + detail.getBps();
//            System.out.println(detail.getKey());
        }
        flux.put("detail", fluxDetail);
        bw.put("detail", bwDetail);
        flux.put("summary", fluxSum);
        bw.put("summary", bwSum);
        result.put("flux", flux);
        result.put("bw", bw);
        return result;
    }

    @Override
    public Object queryResourceStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        int sized = statisticsService.getLabels(start, end).size();
        // 默认数据
        JSONArray defaultData = new JSONArray(Collections.nCopies(sized, 0));
        JSONObject rd = new JSONObject();
        JSONObject rs = new JSONObject();
        // 查询使用量
        JSONObject usageData = getUsageData(domainName, start, end);
        // 访问流量
        rd.put("flux", usageData.getJSONObject("flux").getJSONArray("detail"));
        rs.put("flux", usageData.getJSONObject("flux").getLongValue("summary"));
        // 带宽
        rd.put("bw", usageData.getJSONObject("bw").getJSONArray("detail"));
        rs.put("bw", usageData.getJSONObject("bw").getLongValue("summary"));
        // 回源流量
        rd.put("bs_flux", defaultData);
        rs.put("bs_flux", 0);
        // 回源带宽
        rd.put("bs_bw", defaultData);
        rs.put("bs_bw", 0);
        // 合成结果
        result.put("resource_detail", rd);
        result.put("resource_summary", rs);
        return result;
    }

    @Override
    public Object queryVisitsStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        JSONObject vs = new JSONObject();
        JSONObject vd = new JSONObject();
        int sized = statisticsService.getLabels(start, end).size();
        // 访问次数 req_num
        JSONArray defaultData = new JSONArray(Collections.nCopies(sized, 0));
        vd.put("req_num", defaultData);
        vs.put("req_num", 0);
        // 命中次数 hit_flux
        vd.put("hit_flux", defaultData);
        vs.put("hit_flux", 0);
        // 访问流量 bs_num
        vd.put("bs_num", defaultData);
        vs.put("bs_num", 0);
        // 命中流量 hit_num
        vd.put("hit_num", defaultData);
        vs.put("hit_num", 0);
        result.put("visits_detail", vd);
        result.put("visits_summary", vs);
        return result;
    }

    @Override
    public Object queryHttpCodeStatusStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        int sized = statisticsService.getLabels(start, end).size();
        JSONArray sd = new JSONArray();
        JSONArray ss = new JSONArray();
        JSONArray bsd = new JSONArray();
        JSONArray bss = new JSONArray();
        JSONArray defaultData = new JSONArray(Collections.nCopies(sized, 0));
        // 访问状态码 2xx
        sd.add(defaultData);
        ss.add(0);
        // 访问状态码 3xx
        bsd.add(defaultData);
        bss.add(0);
        result.put("status_detail", sd);
        result.put("status_summary", ss);
        result.put("bs_status_detail", bsd);
        result.put("bs_status_summary", bss);
        return result;
    }

    @Override
    public Object queryTopUri(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        JSONArray empty = new JSONArray();
        result.put("fluxResult", empty);
        result.put("reqNumResult", empty);
        return result;
    }
}
