package com.kuocai.cdn.service.domain.statistics;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.cdn20180510.Client;
import com.aliyun.cdn20180510.models.DescribeDomainUsageDataRequest;
import com.aliyun.cdn20180510.models.DescribeDomainUsageDataResponse;
import com.aliyun.cdn20180510.models.DescribeDomainUsageDataResponseBody;
import com.kuocai.cdn.api.aliyun.cdn.AliyunCdnClientFactory;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.CdnDomainStatisticsService;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.KuocaiDateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import static com.kuocai.cdn.api.aliyun.cdn.AliyunCdnErrorCodeHandler.catchException;

@Slf4j
@Service
public class AliyunDomainStatisticsServiceImpl extends BaseService<CdnDomain> implements ICdnStatisticsPlatformService {

    private final CdnDomainStatisticsService statisticsService;

    private final Client aliyunCdnClient;

    AliyunDomainStatisticsServiceImpl(@Qualifier("aliyunCdnClient") Client aliyunCdnClient, CdnDomainStatisticsService statisticsService) {
        this.aliyunCdnClient = aliyunCdnClient;
        this.statisticsService = statisticsService;
    }

    private Client getClient() throws BusinessException {
        try {
            Client client = AliyunCdnClientFactory.getClient();
            if (client == null) {
                throw new BusinessException("阿里云CDN配置未填写或未生效，请先在后台保存正确的阿里云 AccessKey");
            }
            return client;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("阿里云CDN客户端初始化失败，请检查后台阿里云配置");
        }
    }

    private String getDefaultDataUnit(DateTime start, DateTime end) {
        long between = DateUtil.between(start, end, DateUnit.DAY);
        if (between > 1) {
            return "86400";
        } else {
            long hours = DateUtil.between(start, end, DateUnit.HOUR);
            if (hours > 1) {
                return "3600";
            }
            return "300";
        }
    }

    public long parseLong(String str) {
        double d = Double.parseDouble(str);
        return (long) Math.ceil(d);
    }

    private JSONObject queryTotalUsage(String domainName, DateTime start, DateTime end, String field) throws BusinessException  {
        JSONObject result = new JSONObject();
        // 流量查询
        DescribeDomainUsageDataRequest request = new DescribeDomainUsageDataRequest();
        String unit = getDefaultDataUnit(start, end);
        request.setDomainName(domainName);
        request.setField(field);
        request.setType("all");
        request.setDataProtocol("all");
        request.setArea("all");
        request.setInterval(unit);
        // 格式：2024-04-30T00:00:00Z
        request.setStartTime(KuocaiDateUtil.toISOString(start));
        request.setEndTime(KuocaiDateUtil.toISOString(end));
        // 查询
        Client client = getClient();
        try {
            DescribeDomainUsageDataResponse response = client.describeDomainUsageData(request);
            List<DescribeDomainUsageDataResponseBody.DescribeDomainUsageDataResponseBodyUsageDataPerIntervalDataModule> data = response.getBody().getUsageDataPerInterval().getDataModule();
            JSONArray detail = new JSONArray();
            long sum = 0;
            if ("300".equals(unit)) {
                for (DescribeDomainUsageDataResponseBody.DescribeDomainUsageDataResponseBodyUsageDataPerIntervalDataModule item : data) {
                    sum += parseLong(item.getValue());
                }
                detail.add(sum);
            } else {
                for (DescribeDomainUsageDataResponseBody.DescribeDomainUsageDataResponseBodyUsageDataPerIntervalDataModule item : data) {
                    long value = parseLong(item.getValue());
                    detail.add(value);
                    sum += value;
                }
            }
            result.put("detail", detail);
            result.put("summary", sum);
        } catch (Exception e) {
            throw catchException(e);
        }
        return result;
    }

    @Override
    public JSONObject queryResourceStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        int sized = statisticsService.getLabels(start, end).size();
        // 默认数据
        JSONArray defaultData = new JSONArray(Collections.nCopies(sized, 0));
        JSONObject rd = new JSONObject();
        JSONObject rs = new JSONObject();
        // 访问流量
        JSONObject totalTraffic = queryTotalUsage(domainName, start, end, "traf");
        rd.put("flux", totalTraffic.getJSONArray("detail"));
        rs.put("flux", totalTraffic.getLongValue("summary"));
        // 带宽
        JSONObject bandwidth = queryTotalUsage(domainName, start, end, "bps");
        rd.put("bw", bandwidth.getJSONArray("detail"));
        rs.put("bw", bandwidth.getLongValue("summary"));
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
    public JSONObject queryVisitsStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
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
    public JSONObject queryHttpCodeStatusStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
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
