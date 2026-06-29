package com.kuocai.cdn.service.domain.statistics;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.api.kingsoft.cdn.KingsoftApiService;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.CdnDomainService;
import com.kuocai.cdn.service.base.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KingsoftDomainStatisticsServiceImpl extends BaseService<CdnDomain> implements ICdnStatisticsPlatformService {

    @Autowired
    private KingsoftApiService kingsoftApiService;

    @Autowired
    private CdnDomainService cdnDomainService;

    private static final String CDN_API_VERSION = "2016-09-01";

    private Map<String, String> getDomainInfo(String domainName) throws BusinessException {
        String[] initialDomainNames = domainName.split(",");
        if (initialDomainNames.length == 0) {
            throw new BusinessException("域名不能为空");
        }

        log.info("开始查找金山云域名信息，输入域名: {}", domainName);

        // 验证所有域名是否具有相同的业务类型
        Set<String> businessTypes = new HashSet<>();
        List<CdnDomain> inputDomains = new ArrayList<>();

        for (String name : initialDomainNames) {
            CdnDomain domain = cdnDomainService.queryByDomainName(name);
            if (domain == null) {
                log.warn("在数据库中找不到域名: {}", name);
                throw new BusinessException("在数据库中找不到域名: " + name);
            }
            inputDomains.add(domain);
            businessTypes.add(domain.getBusinessType());
            log.debug("找到域名: {}, 业务类型: {}, 路线: {}, domainId: {}",
                    name, domain.getBusinessType(), domain.getRoute(), domain.getDomainId());
        }

        if (businessTypes.size() > 1) {
            log.error("传入的域名具有不同的业务类型: {}", businessTypes);
            throw new BusinessException("传入的域名具有不同的业务类型: " + businessTypes +
                    "，请确保每个API调用只包含相同业务类型的域名");
        }

        CdnDomain firstDomain = inputDomains.get(0);
        String cdnType = mapBusinessTypeToKingsoft(firstDomain.getBusinessType());
        log.info("业务类型映射: {} -> {}", firstDomain.getBusinessType(), cdnType);

        // 扩展域名查找范围，包括www和非www版本
        Set<String> domainsToLookUp = new HashSet<>();
        for (String name : initialDomainNames) {
            domainsToLookUp.add(name);
            if (name.toLowerCase().startsWith("www.")) {
                domainsToLookUp.add(name.substring(4));
            } else {
                domainsToLookUp.add("www." + name);
            }
        }
        log.debug("扩展后的域名查找列表: {}", domainsToLookUp);

        List<CdnDomain> foundDomains = cdnDomainService.queryByDomainNames(new ArrayList<>(domainsToLookUp));
        log.info("数据库中找到的相关域名数量: {}", foundDomains.size());

        // 记录所有找到的域名详情
        for (CdnDomain domain : foundDomains) {
            log.debug("找到域名: {}, 路线: {}, domainId: {}, 业务类型: {}",
                    domain.getDomainName(), domain.getRoute(), domain.getDomainId(), domain.getBusinessType());
        }

        // 过滤出有效的金山云域名ID
        List<CdnDomain> validKingsoftDomains = foundDomains.stream()
                .filter(d -> "kingsoft".equalsIgnoreCase(d.getRoute()))
                .collect(Collectors.toList());

        log.info("金山云路线的域名数量: {}", validKingsoftDomains.size());

        List<CdnDomain> domainsWithId = validKingsoftDomains.stream()
                .filter(d -> d.getDomainId() != null && !d.getDomainId().trim().isEmpty())
                .collect(Collectors.toList());

        log.info("有有效domainId的金山云域名数量: {}", domainsWithId.size());

        if (domainsWithId.isEmpty()) {
            // 详细记录问题
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("在数据库中找不到与 ").append(domainName).append(" 关联的、有效的金山云域名ID。");
            errorMsg.append("详细信息: ");

            if (validKingsoftDomains.isEmpty()) {
                errorMsg.append("没有找到金山云路线的域名。");
            } else {
                errorMsg.append("找到金山云域名但domainId为空: ");
                for (CdnDomain domain : validKingsoftDomains) {
                    errorMsg.append(domain.getDomainName()).append("(domainId: ")
                            .append(domain.getDomainId()).append("), ");
                }
            }

            log.error(errorMsg.toString());
            throw new BusinessException(errorMsg.toString());
        }

        String domainIds = domainsWithId.stream()
                .map(CdnDomain::getDomainId)
                .distinct()
                .collect(Collectors.joining(","));

        log.info("最终使用的金山云域名ID: {}", domainIds);

        Map<String, String> info = new HashMap<>();
        info.put("cdnType", cdnType);
        info.put("domainIds", domainIds);
        return info;
    }

    private JSONObject getUsageData(String domainIds, String cdnType, DateTime start, DateTime end) throws BusinessException {
        String granularity = calculateGranularity(start, end);
        log.info("获取使用数据 - 域名: {}, CDN类型: {}, 时间范围: {} 到 {}, 计算粒度: {}",
                domainIds, cdnType, start, end, granularity);

        Map<String, String> baseParams = new HashMap<>();
        baseParams.put("DomainIds", domainIds);
        baseParams.put("StartTime", formatDateTime(start));
        baseParams.put("EndTime", formatDateTime(end));
        baseParams.put("Granularity", granularity);
        baseParams.put("CdnType", cdnType);
        baseParams.put("ResultType", "1");
        baseParams.put("DataType", "edge,origin");

        JSONObject bwResponse = callStatisticsGetApi("GetBandwidthData", baseParams);
        JSONObject flowResponse = callStatisticsGetApi("GetFlowData", baseParams);

        // 记录原始响应数据大小
        JSONArray flowDatas = flowResponse.getJSONArray("Datas");
        JSONArray bwDatas = bwResponse.getJSONArray("Datas");
        log.info("流量数据点数: {}, 带宽数据点数: {}",
                flowDatas != null ? flowDatas.size() : 0,
                bwDatas != null ? bwDatas.size() : 0);

        JSONObject result = new JSONObject();
        JSONObject fluxData = new JSONObject();
        JSONObject bwData = new JSONObject();
        JSONObject bsFluxData = new JSONObject();
        JSONObject bsBwData = new JSONObject();

        JSONArray fluxDetailValues = new JSONArray();
        JSONArray bsFluxDetailValues = new JSONArray();
        long fluxSum = 0;
        long bsFluxSum = 0;

        if (flowDatas != null && !flowDatas.isEmpty()) {
            // 根据粒度进行不同的聚合策略
            if ("5".equals(granularity)) {
                // 5分钟粒度数据，需要聚合到小时级别
                fluxSum = aggregateFlowDataToHourly(flowDatas, "Flow", fluxDetailValues);
                bsFluxSum = aggregateFlowDataToHourly(flowDatas, "SrcFlow", bsFluxDetailValues);
                log.info("5分钟粒度流量数据聚合后 - 总流量: {}, 回源流量: {}, 聚合后数据点数: {}",
                        fluxSum, bsFluxSum, fluxDetailValues.size());
            } else {
                // 其他粒度数据，直接累加
                for (int i = 0; i < flowDatas.size(); i++) {
                    JSONObject dataPoint = flowDatas.getJSONObject(i);
                    long flowValue = dataPoint.getLongValue("Flow");
                    fluxDetailValues.add(flowValue);
                    fluxSum += flowValue;
                    long srcFlowValue = dataPoint.getLongValue("SrcFlow");
                    bsFluxDetailValues.add(srcFlowValue);
                    bsFluxSum += srcFlowValue;
                }
                log.info("{}分钟粒度流量数据 - 总流量: {}, 回源流量: {}, 数据点数: {}",
                        granularity, fluxSum, bsFluxSum, flowDatas.size());
            }
        }

        fluxData.put("detail", fluxDetailValues);
        fluxData.put("summary", fluxSum);
        bsFluxData.put("detail", bsFluxDetailValues);
        bsFluxData.put("summary", bsFluxSum);

        JSONArray bwDetailValues = new JSONArray();
        JSONArray bsBwDetailValues = new JSONArray();
        long bwPeak = 0;
        long bsBwPeak = 0;

        if (bwDatas != null && !bwDatas.isEmpty()) {
            // 带宽数据采用峰值策略，不需要聚合
            for (int i = 0; i < bwDatas.size(); i++) {
                JSONObject dataPoint = bwDatas.getJSONObject(i);
                long bwValue = dataPoint.getLongValue("Bw");
                bwDetailValues.add(bwValue);
                if (bwValue > bwPeak) {
                    bwPeak = bwValue;
                }
                long srcBwValue = dataPoint.getLongValue("SrcBw");
                bsBwDetailValues.add(srcBwValue);
                if (srcBwValue > bsBwPeak) {
                    bsBwPeak = srcBwValue;
                }
            }
            log.info("带宽数据 - 峰值带宽: {}, 回源峰值带宽: {}, 数据点数: {}",
                    bwPeak, bsBwPeak, bwDatas.size());
        }

        bwData.put("detail", bwDetailValues);
        bwData.put("summary", bwPeak);
        bsBwData.put("detail", bsBwDetailValues);
        bsBwData.put("summary", bsBwPeak);

        result.put("flux", fluxData);
        result.put("bw", bwData);
        result.put("bs_flux", bsFluxData);
        result.put("bs_bw", bsBwData);
        return result;
    }

    /**
     * 将5分钟粒度的流量数据聚合到小时级别
     * @param flowDatas 5分钟粒度的流量数据
     * @param fieldName 字段名 ("Flow" 或 "SrcFlow")
     * @param detailValues 用于存储聚合后数据的数组
     * @return 聚合后的总流量
     */
    private long aggregateFlowDataToHourly(JSONArray flowDatas, String fieldName, JSONArray detailValues) {
        if (flowDatas == null || flowDatas.isEmpty()) {
            return 0;
        }

        // 5分钟粒度，每小时12个点
        int pointsPerHour = 12;
        int totalHours = (int) Math.ceil((double) flowDatas.size() / pointsPerHour);

        log.info("开始聚合5分钟粒度数据到小时级别 - 总数据点: {}, 预计小时数: {}",
                flowDatas.size(), totalHours);

        long totalSum = 0;

        for (int hour = 0; hour < totalHours; hour++) {
            int startIndex = hour * pointsPerHour;
            int endIndex = Math.min(startIndex + pointsPerHour, flowDatas.size());

            long hourlySum = 0;
            for (int i = startIndex; i < endIndex; i++) {
                JSONObject dataPoint = flowDatas.getJSONObject(i);
                long value = dataPoint.getLongValue(fieldName);
                hourlySum += value;
            }

            detailValues.add(hourlySum);
            totalSum += hourlySum;

            log.debug("第{}小时聚合结果 - 数据点范围: {}-{}, 小时流量: {}, 累计总流量: {}",
                    hour + 1, startIndex, endIndex - 1, hourlySum, totalSum);
        }

        log.info("5分钟粒度数据聚合完成 - 字段: {}, 聚合后小时数: {}, 总流量: {}",
                fieldName, detailValues.size(), totalSum);

        return totalSum;
    }


    @Override
    public JSONObject queryResourceStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        log.info("[金山云统计] 资源统计查询开始 - 域名: {}, 开始时间: {}, 结束时间: {}", domainName, start, end);

        Map<String, String> domainInfo = getDomainInfo(domainName);
        String cdnType = domainInfo.get("cdnType");
        String domainIds = domainInfo.get("domainIds");

        log.info("[金山云统计] 域名信息获取成功 - 域名: {}, CDN类型: {}, 域名ID: {}", domainName, cdnType, domainIds);

        JSONObject result = new JSONObject();
        JSONObject rd = new JSONObject();
        JSONObject rs = new JSONObject();

        log.info("开始获取使用数据...");
        JSONObject usageData = getUsageData(domainIds, cdnType, start, end);
        log.info("使用数据获取完成，数据大小: {} 字符", usageData.toJSONString().length());

        rd.put("flux", usageData.getJSONObject("flux").getJSONArray("detail"));
        rs.put("flux", usageData.getJSONObject("flux").getLongValue("summary"));
        rd.put("bw", usageData.getJSONObject("bw").getJSONArray("detail"));
        rs.put("bw", usageData.getJSONObject("bw").getLongValue("summary"));

        rd.put("bs_flux", usageData.getJSONObject("bs_flux").getJSONArray("detail"));
        rs.put("bs_flux", usageData.getJSONObject("bs_flux").getLongValue("summary"));
        rd.put("bs_bw", usageData.getJSONObject("bs_bw").getJSONArray("detail"));
        rs.put("bs_bw", usageData.getJSONObject("bs_bw").getLongValue("summary"));

        result.put("resource_detail", rd);
        result.put("resource_summary", rs);

        log.info("=== 金山云资源统计查询完成 ===");
        log.info("返回结果摘要 - 流量: {} bytes, 带宽: {} bps, 回源流量: {} bytes, 回源带宽: {} bps",
                rs.getLongValue("flux"), rs.getLongValue("bw"),
                rs.getLongValue("bs_flux"), rs.getLongValue("bs_bw"));

        return result;
    }



    /**
     * [新方法] 使用 GetServerData 接口获取动态加速(wcdn)的数据。
     * 一次性获取指定时间段内、指定数据类型（服务/回源）的所有指标。
     */
    private JSONObject getWcdnDataFromServerApi(String domains, String cdnType, DateTime start, DateTime end, String dataType) throws BusinessException {
        Map<String, String> params = new HashMap<>();
        params.put("StartTime", formatDateTime(start));
        params.put("EndTime", formatDateTime(end));
        params.put("CdnType", cdnType); // 这里会是 "wcdn"
        params.put("Domains", domains); // 注意：新接口参数是 Domains
        params.put("Metric", "flow,bandwidth,request,qps"); // 一次获取所有指标
        params.put("DataType", dataType); // "edge" 或 "origin"
        params.put("Interval", calculateGranularity(start, end));

        // 调用通用的API请求方法，但指定新的 Action 和 Version
        try {
            // 确认您的 kingsoftApiService.callKingsoftApi 支持传入 Action 和 Version
            return kingsoftApiService.callKingsoftApi("GetServerData", "2020-06-30", params);
        } catch (Exception e) {
            log.error("调用金山云新接口 GetServerData 失败: DataType={}, Params={}", dataType, params, e);
            throw new BusinessException("调用金山云新接口GetServerData失败: " + e.getMessage());
        }
    }
    @Override
    public JSONObject queryVisitsStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        Map<String, String> domainInfo = getDomainInfo(domainName);
        String cdnType = domainInfo.get("cdnType");
        String domainIds = domainInfo.get("domainIds");

        // 根据业务类型（CdnType）进行分流处理
        if ("wcdn".equals(cdnType)) {
            // --- 动态加速(wcdn) 使用新接口 GetServerData 的逻辑 ---
            log.info("域名 {} 为动态加速(wcdn)，使用新的 GetServerData API 获取访问数据", domainName);

            // 1. 调用新接口，分别获取边缘(edge)和回源(origin)数据
            // getWcdnDataFromServerApi 是我们之前新增的用于调用新接口的私有方法
            JSONObject edgeResponse = getWcdnDataFromServerApi(domainName, cdnType, start, end, "edge");
            JSONArray edgeDataPoints = edgeResponse.getJSONArray("Datas").getJSONObject(0).getJSONArray("Data");

            JSONObject originResponse = getWcdnDataFromServerApi(domainName, cdnType, start, end, "origin");
            JSONArray originDataPoints = originResponse.getJSONArray("Datas").getJSONObject(0).getJSONArray("Data");

            log.info("GetServerData返回数据 - Edge数据点数: {}, Origin数据点数: {}",
                    edgeDataPoints != null ? edgeDataPoints.size() : 0,
                    originDataPoints != null ? originDataPoints.size() : 0);

            // 2. 解析数据并组装成统一格式的 JSONObject
            JSONObject result = new JSONObject();
            JSONObject vd = new JSONObject(); // visits_detail
            JSONObject vs = new JSONObject(); // visits_summary

            // 2.1 计算各项指标的总值
            long totalRequestSum = 0;
            long originRequestSum = 0;
            long hitNumSum = 0;

            // 检查是否需要聚合（GetServerData可能返回5分钟粒度数据）
            String granularity = calculateGranularity(start, end);
            if ("5".equals(granularity) && edgeDataPoints != null && edgeDataPoints.size() > 24) {
                // 5分钟粒度数据，需要聚合到小时级别
                log.info("检测到5分钟粒度数据，开始聚合到小时级别");

                JSONArray edgeHourlyData = new JSONArray();
                JSONArray originHourlyData = new JSONArray();

                totalRequestSum = aggregateRequestDataToHourly(edgeDataPoints, "Request", edgeHourlyData);
                originRequestSum = aggregateRequestDataToHourly(originDataPoints, "Request", originHourlyData);

                // 计算聚合后的命中数
                for (int i = 0; i < edgeHourlyData.size(); i++) {
                    long edgeReq = edgeHourlyData.getLongValue(i);
                    long originReq = (i < originHourlyData.size()) ? originHourlyData.getLongValue(i) : 0;
                    long hitNum = Math.max(0, edgeReq - originReq);
                    hitNumSum += hitNum;
                }

                // 填充detail部分
                vd.put("req_num", edgeHourlyData);
                vd.put("bs_num", originHourlyData);

                log.info("5分钟粒度数据聚合后 - 总请求数: {}, 回源请求数: {}, 命中数: {}, 聚合后小时数: {}",
                        totalRequestSum, originRequestSum, hitNumSum, edgeHourlyData.size());
            } else {
                // 其他粒度数据，直接累加
                log.info("使用{}分钟粒度数据，直接累加", granularity);
                totalRequestSum = edgeDataPoints.stream().mapToLong(obj -> ((JSONObject) obj).getLongValue("Request")).sum();
                originRequestSum = originDataPoints.stream().mapToLong(obj -> ((JSONObject) obj).getLongValue("Request")).sum();
                hitNumSum = totalRequestSum - originRequestSum;

                // 填充detail部分
                vd.put("req_num", edgeDataPoints.stream().map(obj -> ((JSONObject) obj).get("Request")).collect(Collectors.toList()));
                vd.put("bs_num", originDataPoints.stream().map(obj -> ((JSONObject) obj).get("Request")).collect(Collectors.toList()));

                log.info("{}分钟粒度数据 - 总请求数: {}, 回源请求数: {}, 命中数: {}",
                        granularity, totalRequestSum, originRequestSum, hitNumSum);
            }

            // 2.2 填充 summary 部分，遵循统一规范
            vs.put("req_num", totalRequestSum);     // req_num 始终为服务总请求数
            vs.put("bs_num", originRequestSum);     // bs_num 始终为回源请求数
            vs.put("hit_num", Math.max(0, hitNumSum)); // hit_num 为命中数

            // 2.3 计算详细的命中请求数 (如果之前没有计算)
            if (!vd.containsKey("hit_num")) {
                JSONArray hitNumDetail = new JSONArray();
                if ("5".equals(granularity) && edgeDataPoints != null && edgeDataPoints.size() > 24) {
                    // 使用聚合后的数据计算命中数
                    JSONArray edgeHourlyData = (JSONArray) vd.get("req_num");
                    JSONArray originHourlyData = (JSONArray) vd.get("bs_num");
                    for (int i = 0; i < edgeHourlyData.size(); i++) {
                        long edgeReq = edgeHourlyData.getLongValue(i);
                        long originReq = (i < originHourlyData.size()) ? originHourlyData.getLongValue(i) : 0;
                        hitNumDetail.add(Math.max(0, edgeReq - originReq));
                    }
                } else {
                    // 使用原始数据计算命中数
                    for (int i = 0; i < edgeDataPoints.size(); i++) {
                        long edgeReq = edgeDataPoints.getJSONObject(i).getLongValue("Request");
                        long originReq = (i < originDataPoints.size()) ? originDataPoints.getJSONObject(i).getLongValue("Request") : 0;
                        hitNumDetail.add(Math.max(0, edgeReq - originReq));
                    }
                }
                vd.put("hit_num", hitNumDetail);
            }

            // 2.4 其他指标（如回源失败数、命中流量）仍需按原逻辑获取
            // 这部分逻辑与旧API部分保持一致，以确保返回结构的完整性
            long hitFluxSum = 0;
            try {
                Map<String, String> hitRateParams = new HashMap<>();
                hitRateParams.put("DomainIds", domainIds);
                hitRateParams.put("StartTime", formatDateTime(start));
                hitRateParams.put("EndTime", formatDateTime(end));
                hitRateParams.put("CdnType", cdnType);
                JSONObject hitRateResponse = callStatisticsGetApi("GetHitRateData", hitRateParams);
                JSONArray hitRateDatas = hitRateResponse.getJSONArray("Datas");
                if (hitRateDatas != null && !hitRateDatas.isEmpty()) {
                    hitFluxSum = hitRateDatas.getJSONObject(0).getLongValue("HitFlow");
                }
            } catch (Exception e) {
                log.warn("获取命中流量(HitFlow)时发生错误，该指标将为0。错误: {}", e.getMessage());
            }
            vs.put("hit_flux", hitFluxSum);
            vd.put("hit_flux", new JSONArray(Collections.nCopies(edgeDataPoints.size(), 0L)));

            long bsFailNumSum = 0;
            try {
                Map<String, String> srcHttpCodeParams = new HashMap<>();
                srcHttpCodeParams.put("DomainIds", domainIds);
                srcHttpCodeParams.put("StartTime", formatDateTime(start));
                srcHttpCodeParams.put("EndTime", formatDateTime(end));
                srcHttpCodeParams.put("CdnType", cdnType);
                JSONObject srcHttpCodeResponse = callStatisticsGetApi("GetSrcHttpCodeData", srcHttpCodeParams);
                JSONArray srcHttpCodeDatas = srcHttpCodeResponse.getJSONArray("Datas");
                if (srcHttpCodeDatas != null) {
                    for (int i = 0; i < srcHttpCodeDatas.size(); i++) {
                        JSONObject codeTypeObject = srcHttpCodeDatas.getJSONObject(i);
                        if ("5xx".equals(codeTypeObject.getString("SrcCodeType"))) {
                            bsFailNumSum = codeTypeObject.getLongValue("Pv");
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("获取回源状态码(用于计算回源失败数)时发生错误，该指标将为0。错误: {}", e.getMessage());
            }
            vs.put("bs_fail_num", bsFailNumSum);
            vd.put("bs_fail_num", new JSONArray(Collections.nCopies(edgeDataPoints.size(), 0L)));

            // 3. 组装最终结果并返回
            result.put("visits_detail", vd);
            result.put("visits_summary", vs);
            return result;

        } else {
            // --- 静态加速等其他类型，保持原有旧接口逻辑不变 ---
            log.info("域名 {} 为非动态加速(CdnType={})，继续使用旧版API获取访问数据", domainName, cdnType);

            Map<String, String> params = new HashMap<>();
            params.put("DomainIds", domainIds);
            params.put("StartTime", formatDateTime(start));
            params.put("EndTime", formatDateTime(end));
            String granularity = calculateGranularity(start, end);
            params.put("Granularity", granularity);
            params.put("CdnType", cdnType);
            params.put("ResultType", "1");
            params.put("DataType", "edge,origin");

            log.info("调用GetPvData接口 - 域名: {}, 粒度: {}, 时间范围: {} 到 {}",
                    domainName, granularity, start, end);

            JSONObject response = callStatisticsGetApi("GetPvData", params);

            JSONObject result = new JSONObject();
            JSONObject vs = new JSONObject();
            JSONObject vd = new JSONObject();

            JSONArray responseDatas = response.getJSONArray("Datas");
            log.info("GetPvData返回数据点数: {}", responseDatas != null ? responseDatas.size() : 0);

            JSONArray reqNumDetailValues = new JSONArray();
            JSONArray bsNumDetailValues = new JSONArray();
            JSONArray hitNumDetailValues = new JSONArray();

            long reqNumSum = 0;
            long bsNumSum = 0;
            long hitNumSum = 0;

            if (responseDatas != null && !responseDatas.isEmpty()) {
                // 根据粒度进行不同的聚合策略
                if ("5".equals(granularity)) {
                    // 5分钟粒度数据，需要聚合到小时级别
                    log.info("检测到5分钟粒度数据，开始聚合到小时级别");
                    reqNumSum = aggregateRequestDataToHourly(responseDatas, "Pv", reqNumDetailValues);
                    bsNumSum = aggregateRequestDataToHourly(responseDatas, "SrcPv", bsNumDetailValues);

                    // 计算聚合后的命中数
                    for (int i = 0; i < reqNumDetailValues.size(); i++) {
                        long reqNum = reqNumDetailValues.getLongValue(i);
                        long bsNum = bsNumDetailValues.getLongValue(i);
                        long hitNum = Math.max(0, reqNum - bsNum);
                        hitNumDetailValues.add(hitNum);
                        hitNumSum += hitNum;
                    }

                    log.info("5分钟粒度请求数据聚合后 - 总请求数: {}, 回源请求数: {}, 命中数: {}, 聚合后小时数: {}",
                            reqNumSum, bsNumSum, hitNumSum, reqNumDetailValues.size());
                } else {
                    // 其他粒度数据，直接累加
                    log.info("使用{}分钟粒度数据，直接累加", granularity);
                    for (int i = 0; i < responseDatas.size(); i++) {
                        JSONObject dataPoint = responseDatas.getJSONObject(i);

                        long reqNumValue = dataPoint.getLongValue("Pv");
                        long bsReqNumValue = dataPoint.getLongValue("SrcPv");
                        long hitNumValue = reqNumValue - bsReqNumValue;

                        reqNumDetailValues.add(reqNumValue);
                        bsNumDetailValues.add(bsReqNumValue);
                        hitNumDetailValues.add(Math.max(0, hitNumValue));

                        reqNumSum += reqNumValue;
                        bsNumSum += bsReqNumValue;
                        hitNumSum += hitNumValue;
                    }
                    log.info("{}分钟粒度请求数据 - 总请求数: {}, 回源请求数: {}, 命中数: {}, 数据点数: {}",
                            granularity, reqNumSum, bsNumSum, hitNumSum, responseDatas.size());
                }
            }

            vd.put("req_num", reqNumDetailValues);
            vs.put("req_num", reqNumSum);

            vd.put("bs_num", bsNumDetailValues);
            vs.put("bs_num", bsNumSum);

            vd.put("hit_num", hitNumDetailValues);
            vs.put("hit_num", Math.max(0, hitNumSum));

            long hitFluxSum = 0;
            try {
                Map<String, String> hitRateParams = new HashMap<>();
                hitRateParams.put("DomainIds", domainIds);
                hitRateParams.put("StartTime", formatDateTime(start));
                hitRateParams.put("EndTime", formatDateTime(end));
                hitRateParams.put("CdnType", cdnType);

                JSONObject hitRateResponse = callStatisticsGetApi("GetHitRateData", hitRateParams);
                JSONArray hitRateDatas = hitRateResponse.getJSONArray("Datas");

                if (hitRateDatas != null && !hitRateDatas.isEmpty()) {
                    hitFluxSum = hitRateDatas.getJSONObject(0).getLongValue("HitFlow");
                }
            } catch (Exception e) {
                log.warn("获取命中流量(HitFlow)时发生错误，该指标将为0。错误: {}", e.getMessage());
            }


            long bsFailNumSum = 0;
            try {
                Map<String, String> srcHttpCodeParams = new HashMap<>();
                srcHttpCodeParams.put("DomainIds", domainIds);
                srcHttpCodeParams.put("StartTime", formatDateTime(start));
                srcHttpCodeParams.put("EndTime", formatDateTime(end));
                srcHttpCodeParams.put("CdnType", cdnType);
                JSONObject srcHttpCodeResponse = callStatisticsGetApi("GetSrcHttpCodeData", srcHttpCodeParams);
                JSONArray srcHttpCodeDatas = srcHttpCodeResponse.getJSONArray("Datas");
                if (srcHttpCodeDatas != null) {
                    for (int i = 0; i < srcHttpCodeDatas.size(); i++) {
                        JSONObject codeTypeObject = srcHttpCodeDatas.getJSONObject(i);
                        if ("5xx".equals(codeTypeObject.getString("SrcCodeType"))) {
                            bsFailNumSum = codeTypeObject.getLongValue("Pv");
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("获取回源状态码(用于计算回源失败数)时发生错误，该指标将为0。错误: {}", e.getMessage());
            }

            vs.put("bs_fail_num", bsFailNumSum);

            int dataPointSize = reqNumDetailValues.size();
            JSONArray defaultDetailData = new JSONArray(Collections.nCopies(dataPointSize, 0L));
            vd.put("hit_flux", defaultDetailData);
            vs.put("hit_flux", hitFluxSum);

            vd.put("bs_fail_num", defaultDetailData);

            result.put("visits_detail", vd);
            result.put("visits_summary", vs);
            return result;
        }
    }
    @Override
    public JSONObject queryHttpCodeStatusStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        Map<String, String> domainInfo = getDomainInfo(domainName);
        String cdnType = domainInfo.get("cdnType");
        String domainIds = domainInfo.get("domainIds");

        Map<String, String> params = new HashMap<>();
        params.put("DomainIds", domainIds);
        params.put("StartTime", formatDateTime(start));
        params.put("EndTime", formatDateTime(end));
        params.put("CdnType", cdnType);

        JSONObject edgeResponse = callStatisticsGetApi("GetHttpCodeData", params);
        JSONObject edgeResult = parseHttpCodeData(edgeResponse.getJSONArray("Datas"));

        JSONObject srcResponse = callStatisticsGetApi("GetSrcHttpCodeData", params);
        JSONObject srcResult = parseHttpCodeData(srcResponse.getJSONArray("Datas"));

        JSONObject result = new JSONObject();
        result.put("status_detail", edgeResult.getJSONArray("detail"));
        result.put("status_summary", edgeResult.getJSONArray("summary"));
        result.put("bs_status_detail", srcResult.getJSONArray("detail"));
        result.put("bs_status_summary", srcResult.getJSONArray("summary"));

        return result;
    }

    private JSONObject parseHttpCodeData(JSONArray responseDatas) {
        JSONObject result = new JSONObject();
        JSONArray status_detail = new JSONArray();
        JSONArray status_summary = new JSONArray();

        Map<String, Long> summaryMap = new HashMap<>();
        if (responseDatas != null) {
            for (int i = 0; i < responseDatas.size(); i++) {
                JSONObject codeTypeObject = responseDatas.getJSONObject(i);
                String codeType = codeTypeObject.containsKey("CodeType") ?
                        codeTypeObject.getString("CodeType") : codeTypeObject.getString("SrcCodeType");
                long pv = codeTypeObject.getLongValue("Pv");
                if (codeType != null) {
                    summaryMap.put(codeType, pv);
                }
            }
        }

        long summary2xx = summaryMap.getOrDefault("2xx", 0L);
        long summary3xx = summaryMap.getOrDefault("3xx", 0L);
        long summary4xx = summaryMap.getOrDefault("4xx", 0L);
        long summary5xx = summaryMap.getOrDefault("5xx", 0L);

        status_summary.add(summary2xx);
        status_summary.add(summary3xx);
        status_summary.add(summary4xx);
        status_summary.add(summary5xx);

        status_detail.add(new JSONArray(Collections.singletonList(summary2xx)));
        status_detail.add(new JSONArray(Collections.singletonList(summary3xx)));
        status_detail.add(new JSONArray(Collections.singletonList(summary4xx)));
        status_detail.add(new JSONArray(Collections.singletonList(summary5xx)));

        result.put("detail", status_detail);
        result.put("summary", status_summary);
        return result;
    }

    @Override
    public JSONObject queryTopUri(String domainName, DateTime start, DateTime end) throws BusinessException {
        Map<String, String> baseParams = new HashMap<>();
        baseParams.put("DomainName", domainName);
        baseParams.put("StartTime", formatDateTime(start));
        baseParams.put("EndTime", formatDateTime(end));
        baseParams.put("Limit", "100");

        baseParams.put("SortBy", "flow");
        JSONObject fluxResponse = callStatisticsGetApi("GetTopUrlData", baseParams);

        baseParams.put("SortBy", "req_num");
        JSONObject reqNumResponse = callStatisticsGetApi("GetTopUrlData", baseParams);

        JSONObject result = new JSONObject();
        result.put("fluxResult", fluxResponse.getJSONArray("TopUrlData")); // 字段名以官方文档为准
        result.put("reqNumResult", reqNumResponse.getJSONArray("TopUrlData"));
        return result;
    }

    public String checkAndFixDomainId(String domainName) throws BusinessException {
        CdnDomain dbDomain = cdnDomainService.queryByDomainName(domainName);
        if (dbDomain == null) {
            return "诊断失败：在我们的数据库中找不到域名 " + domainName;
        }
        String dbDomainId = dbDomain.getDomainId();

        Map<String, String> params = new HashMap<>();
        params.put("DomainName", domainName);
        JSONObject apiResponse;
        try {
            apiResponse = kingsoftApiService.callKingsoftApi("GetCdnDomainBasicInfo", "2016-09-01", params);
        } catch (Exception e) {
            return "诊断失败：调用金山云API(GetCdnDomainBasicInfo)时出错: " + e.getMessage();
        }

        JSONObject domainInfo = apiResponse.getJSONObject("DomainBasicInfo");
        if (domainInfo == null) {
            return "诊断失败：金山云API的返回结果中没有找到 DomainBasicInfo 部分。返回: " + apiResponse.toJSONString();
        }
        String officialDomainId = domainInfo.getString("DomainId");
        if (officialDomainId == null) {
            return "诊断失败：金山云API返回的 DomainBasicInfo 中没有 DomainId 字段。返回: " + domainInfo.toJSONString();
        }

        if (officialDomainId.equals(dbDomainId)) {
            return String.format("诊断结果：域名 %s 的ID匹配！数据库ID和金山云官方ID均为: %s。数据是正确的。", domainName, dbDomainId);
        } else {
            dbDomain.setDomainId(officialDomainId);
            cdnDomainService.save(dbDomain);
            return String.format("诊断并修复：域名 %s 的ID已从【%s】更新为金山云官方ID【%s】。", domainName, dbDomainId, officialDomainId);
        }
    }

    private String mapBusinessTypeToKingsoft(String businessType) {
        switch (businessType) {
            case "web":
                return "page";
            case "download":
                return "file";
            case "video":
                return "video";
            case "fullsite":
                return "wcdn";
            default:
                return "page";
        }
    }
    /**
     * 调用金山云统计类API的统一入口（GET请求）
     */
    private JSONObject callStatisticsGetApi(String action, Map<String, String> params) throws BusinessException {
        try {
            return kingsoftApiService.callKingsoftApi(action, CDN_API_VERSION, params);
        } catch (Exception e) {
            String errorMessage = e.getMessage();

            // 检查是否是域名类型不匹配错误
            if (errorMessage != null && errorMessage.contains("DomainCdnTypeNotMatch")) {
                log.warn("域名CDN类型不匹配，尝试使用其他CDN类型: {}", errorMessage);

                // 尝试其他CDN类型
                String originalCdnType = params.get("CdnType");
                String[] cdnTypes = {"page", "file", "video", "wcdn"};

                for (String cdnType : cdnTypes) {
                    if (!cdnType.equals(originalCdnType)) {
                        try {
                            log.info("尝试使用CDN类型: {} 重新调用API", cdnType);
                            Map<String, String> newParams = new HashMap<>(params);
                            newParams.put("CdnType", cdnType);
                            return kingsoftApiService.callKingsoftApi(action, CDN_API_VERSION, newParams);
                        } catch (Exception retryException) {
                            log.debug("使用CDN类型 {} 调用失败: {}", cdnType, retryException.getMessage());
                            // 继续尝试下一个类型
                        }
                    }
                }

                // 如果所有类型都失败了，抛出原始错误
                log.error("所有CDN类型都尝试失败，抛出原始错误");
            }

            log.error("调用金山云CDN统计API失败: Action={}, Params={}", action, params, e);
            throw new BusinessException("调用金山云CDN统计API(" + action + ")失败: " + e.getMessage());
        }
    }



    private String formatDateTime(DateTime dateTime) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        String formattedDateTime = sdf.format(dateTime);

        return formattedDateTime + "+0800";
    }


    private String calculateGranularity(DateTime start, DateTime end) throws BusinessException {
        long hoursBetween = DateUtil.between(start, end, DateUnit.HOUR, true);
        if (hoursBetween > 90 * 24) {
            throw new BusinessException("查询时间范围超过90天，金山云不支持此操作");
        }

        if (hoursBetween > 31 * 24) {
            return "1440";
        }

        if (hoursBetween > 3 * 24) {
            return "240";
        }

        if (hoursBetween <= 3 * 24) {
            if (hoursBetween <= 25) {
                return "5";
            } else {
                return "60";
            }
        }

        return "1440";
    }

    /**
     * 将5分钟粒度的请求数据聚合到小时级别
     * @param responseDatas 5分钟粒度的请求数据
     * @param fieldName 字段名 ("Pv" 或 "SrcPv")
     * @param detailValues 用于存储聚合后数据的数组
     * @return 聚合后的总请求数
     */
    private long aggregateRequestDataToHourly(JSONArray responseDatas, String fieldName, JSONArray detailValues) {
        if (responseDatas == null || responseDatas.isEmpty()) {
            return 0;
        }

        // 5分钟粒度，每小时12个点
        int pointsPerHour = 12;
        int totalHours = (int) Math.ceil((double) responseDatas.size() / pointsPerHour);

        log.info("开始聚合5分钟粒度请求数据到小时级别 - 总数据点: {}, 预计小时数: {}",
                responseDatas.size(), totalHours);

        long totalSum = 0;

        for (int hour = 0; hour < totalHours; hour++) {
            int startIndex = hour * pointsPerHour;
            int endIndex = Math.min(startIndex + pointsPerHour, responseDatas.size());

            long hourlySum = 0;
            for (int i = startIndex; i < endIndex; i++) {
                JSONObject dataPoint = responseDatas.getJSONObject(i);
                long value = dataPoint.getLongValue(fieldName);
                hourlySum += value;
            }

            detailValues.add(hourlySum);
            totalSum += hourlySum;

            log.debug("第{}小时聚合结果 - 数据点范围: {}-{}, 小时请求数: {}, 累计总请求数: {}",
                    hour + 1, startIndex, endIndex - 1, hourlySum, totalSum);
        }

        log.info("5分钟粒度请求数据聚合完成 - 字段: {}, 聚合后小时数: {}, 总请求数: {}",
                fieldName, detailValues.size(), totalSum);

        return totalSum;
    }
}