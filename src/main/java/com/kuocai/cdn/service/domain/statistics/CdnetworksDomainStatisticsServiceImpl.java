package com.kuocai.cdn.service.domain.statistics;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.cdnetworks.cdn.dto.QueryBandwidthDTO;
import com.kuocai.cdn.api.cdnetworks.cdn.dto.QueryTotalTrafficDTO;
import com.kuocai.cdn.api.cdnetworks.cdn.vo.QueryBandwidthVO;
import com.kuocai.cdn.api.cdnetworks.cdn.vo.QueryTotalTrafficVO;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnetworksException;
import com.kuocai.cdn.service.CdnDomainStatisticsService;
import com.kuocai.cdn.service.base.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.kuocai.cdn.api.cdnetworks.cdn.CdnetworksClient.QueryBandwidth;
import static com.kuocai.cdn.api.cdnetworks.cdn.CdnetworksClient.QueryTotalTraffic;

@Slf4j
@Service
public class CdnetworksDomainStatisticsServiceImpl extends BaseService<CdnDomain> implements ICdnStatisticsPlatformService {

    @Resource
    private CdnDomainStatisticsService statisticsService;

    private String getDefaultDataUnit(DateTime start, DateTime end) {
        long between = DateUtil.between(start, end, DateUnit.DAY);
        if (between > 1) {
            return "daily";
        } else {
            return "hourly";
        }
    }

    private JSONArray fixArray(JSONArray data, int size) {
        int sized = data.size();
        if (sized < size) {
            for (int i = 0; i < size - sized; i++) {
                data.add(0);
            }
        }
        return data;
    }

    @Override
    public Object queryResourceStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        JSONObject result = new JSONObject();
        int sized = statisticsService.getLabels(start, end).size();
        // 默认数据
        JSONArray defaultData = new JSONArray(Collections.nCopies(sized, 0));
        JSONObject rd = new JSONObject();
        JSONObject rs = new JSONObject();
        System.out.printf("queryResourceStatistics: domainName=%s, start=%s, end=%s\n", domainName, start, end);
        // 访问流量
        JSONObject totalTraffic = queryTotalTraffic(domainName, start, end);
        rd.put("flux", fixArray(totalTraffic.getJSONArray("detail"), sized));
        rs.put("flux", totalTraffic.getLongValue("summary"));
        // 带宽
        JSONObject bandwidth = queryBandwidth(domainName, start, end);
        rd.put("bw", fixArray(bandwidth.getJSONArray("detail"), sized));
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

    private JSONObject queryTotalTraffic(String domainName, DateTime start, DateTime end) throws CdnetworksException {
        JSONObject result = new JSONObject();
        QueryTotalTrafficDTO dto = new QueryTotalTrafficDTO();
        // yyyy-MM-ddTHH:mm:ss+08:00
        dto.setDatefrom(String.format("%sT%s+08:00", start.toString("yyyy-MM-dd"), start.toString("HH:mm:ss")));
        dto.setDateto(String.format("%sT%s+08:00", end.toString("yyyy-MM-dd"), end.toString("HH:mm:ss")));
        QueryTotalTrafficDTO.DomainList domainList = new QueryTotalTrafficDTO.DomainList();
        List<String> domainNameList = Arrays.asList(domainName.split(","));
        domainList.setDomainName(domainNameList);
        dto.setDomainList(domainList);
        dto.setType(getDefaultDataUnit(start, end));
        JSONArray detail = new JSONArray();
        QueryTotalTrafficVO vo = QueryTotalTraffic(dto);
        for (QueryTotalTrafficVO.FlowData flowData : vo.getFlowData()) {
            double sum = Double.parseDouble(flowData.getFlow());
            detail.add(Math.round(sum * 1024 * 1024));
        }
        result.put("detail", detail);
        double sum = Double.parseDouble(vo.getFlowSummary());
        result.put("summary", Math.round(sum * 1024 * 1024));
        return result;
    }

    private JSONObject queryBandwidth(String domainName, DateTime start, DateTime end) throws CdnetworksException {
        JSONObject result = new JSONObject();
        QueryBandwidthDTO dto = new QueryBandwidthDTO();
        // yyyy-MM-ddTHH:mm:ss+08:00
        dto.setDatefrom(String.format("%sT%s+08:00", start.toString("yyyy-MM-dd"), start.toString("HH:mm:ss")));
        dto.setDateto(String.format("%sT%s+08:00", end.toString("yyyy-MM-dd"), end.toString("HH:mm:ss")));
        QueryTotalTrafficDTO.DomainList domainList = new QueryTotalTrafficDTO.DomainList();
        List<String> domainNameList = Arrays.asList(domainName.split(","));
        domainList.setDomainName(domainNameList);
        dto.setDomainList(domainList);
        dto.setType(getDefaultDataUnit(start, end));
        JSONArray detail = new JSONArray();
        QueryBandwidthVO vo = QueryBandwidth(dto);
        for (QueryBandwidthVO.BandwidthData bandwidthData : vo.getBandwidthReport()) {
            double sum = Double.parseDouble(bandwidthData.getBandwidth());
            detail.add(Math.round(sum * 1024 * 1024));
        }
        result.put("detail", detail);
        result.put("summary", Math.round(vo.getFlowSummary() * 1024 * 1024));
        System.out.println(result.toJSONString());
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
