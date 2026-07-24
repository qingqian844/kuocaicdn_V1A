package com.kuocai.cdn.service.domain.operation;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.*;
import com.kuocai.cdn.api.huawei.cdn.dto.*;
import com.kuocai.cdn.api.kingsoft.cdn.KingsoftApiService;
import com.kuocai.cdn.api.kingsoft.cdn.model.*;
import com.kuocai.cdn.api.tencent.dns.CreateRecordResponse;
import com.kuocai.cdn.api.tencent.dns.TencentApi;
import com.kuocai.cdn.api.tencent.dns.dto.CreateRecordDTO;
import com.kuocai.cdn.api.tencent.dns.properties.TencentDns;
import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.CdnDomainSources;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.enumeration.domainmerage.domain.BusinessTypeEnum;
import com.kuocai.cdn.enumeration.domainmerage.domain.OriginTypeEnum;
import com.kuocai.cdn.enumeration.domainmerage.domain.ServiceAreaEnum;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.KingsoftCertificateService;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.service.domain.operation.optional.ICdnDomainVerifyService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.DomainUtil;
import com.kuocai.cdn.util.KuocaiBaseUtil;
import com.kuocai.cdn.util.KuocaiDateUtil;
import com.kuocai.cdn.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KingsoftDomainServiceImpl extends BaseService<CdnDomain> implements ICdnPlatformService, ICdnDomainVerifyService {

    @Autowired
    private KingsoftApiService kingsoftApiService;

    @Autowired
    private KingsoftCertificateService kingsoftCertificateService;


    private BusinessException handleKingsoftException(Exception e) {
        String message = e.getMessage();
        if (message != null) {
            if (message.contains("InvalidEnable.ValueNotSupported")) {
                return new BusinessException("HTTPS启用参数(Enable)的值不合法，应为'on'或'off'");
            } else if (message.contains("ServerCertificate.MissingParameter")) {
                return new BusinessException("缺少必需的证书内容(ServerCertificate)参数");
            } else if (message.contains("PrivateKey.MissingParameter")) {
                return new BusinessException("缺少必需的私钥(PrivateKey)参数");
            } else if (message.contains("InvalidServerCertificate") && !message.contains("Name") && !message.contains("TooLong")) {
                return new BusinessException("提供的证书内容(ServerCertificate)格式不正确");
            } else if (message.contains("InvalidPrivateKey") && !message.contains("TooLong")) {
                return new BusinessException("提供的私钥内容(PrivateKey)格式不正确");
            } else if (message.contains("ServerCertificate.MissMatch")) {
                return new BusinessException("证书和私钥不匹配");
            } else if (message.contains("InvalidServerCertificate.TooLong")) {
                return new BusinessException("证书内容超过了16K的长度限制");
            } else if (message.contains("InvalidServerCertificateName.TooLong")) {
                return new BusinessException("证书名称超过了128个字符的长度限制");
            } else if (message.contains("Certificate.Duplicated")) {
                return new BusinessException("证书名称已存在，请使用其他名称");
            } else if (message.contains("Certificate.CertNull")) {
                return new BusinessException("证书内容不能为空");
            } else if (message.contains("Certificate.FormatError")) {
                return new BusinessException("证书格式错误");
            } else if (message.contains("Certificate.KeyNull")) {
                return new BusinessException("私钥内容不能为空");
            } else if (message.contains("Key.Malformed")) {
                return new BusinessException("私钥格式错误");
            } else if (message.contains("ServerCertificateName.NameNull")) {
                return new BusinessException("证书名称不能为空");
            } else if (message.contains("notRunningDomain")) {
                return new BusinessException("操作失败：域名未处于“运行中”状态，无法配置HTTPS。请先启用域名或等待配置完成。");
            }else if (message.contains("The certificate name has already existed")) {
                return new BusinessException("操作失败：证书名称已存在。");
            }

            // === 基于金山云错误代码表的全面错误处理 ===

            // 400错误 - 认证相关
            else if (message.contains("InvalidClientTokenId")) {
                return new BusinessException("ak/sk不正确");
            } else if (message.contains("IllegalOperation")) {
                if (message.contains("does not open CDN service")) {
                    return new BusinessException("未开通CDN服务");
                } else {
                    return new BusinessException("没有权限执行当前操作");
                }
            }

            // CDN类型相关错误
            else if (message.contains("UnSupportCdnType")) {
                return new BusinessException("不支持的CDN业务类型，请检查CDN类型参数");
            } else if (message.contains("InvalidCdnType")) {
                return new BusinessException("产品类型参数错误，支持的类型：file/live/page/download/video/wcdn");
            } else if (message.contains("DomainCdnTypeNotMatch")) {
                return new BusinessException("域名与产品类型不符");
            } else if (message.contains("InvalidCdnSubType")) {
                return new BusinessException("产品子类型参数错误");
            } else if (message.contains("InvalidCdnSubType.Live")) {
                return new BusinessException("无效的直播子类型，应为：live");
            } else if (message.contains("InvalidCdnSubType.Download")) {
                return new BusinessException("无效的下载子类型，应为：web/download/video");
            }

            // 时间参数相关错误
            else if (message.contains("InvalidStartTime.Malformed")) {
                return new BusinessException("开始时间格式不正确，请使用正确的时间格式");
            } else if (message.contains("InvalidEndTime.Malformed")) {
                return new BusinessException("结束时间格式不正确，请使用正确的时间格式");
            } else if (message.contains("InvalidEndTime.Mismatch")) {
                return new BusinessException("结束时间不能早于开始时间");
            } else if (message.contains("InvalidStartTime.ValueNotSupported")) {
                return new BusinessException("开始时间和结束时间的差值不能超过93天");
            } else if (message.contains("NoDataProvided")) {
                return new BusinessException("查询数据不能超过1年范围");
            }

            // 统计参数相关错误
            else if (message.contains("InvalidGranularity")) {
                return new BusinessException("统计粒度参数错误，支持的粒度：5,10,15...1440(分钟)");
            } else if (message.contains("InvalidInterval")) {
                return new BusinessException("时间粒度填写错误，支持的间隔：1,5,60,1440(分钟)");
            } else if (message.contains("InvalidDataType.EdgeOrigin")) {
                return new BusinessException("数据类型参数错误，应为：edge/origin");
            } else if (message.contains("InvalidDataType.ReqhitrateFlowhitrate")) {
                return new BusinessException("命中率数据类型参数错误，应为：reqhitrate/flowhitrate");
            } else if (message.contains("InvalidResultType")) {
                return new BusinessException("返回结果参数错误，请检查结果类型参数");
            } else if (message.contains("InvalidMetricType")) {
                return new BusinessException("查询类型错误，支持的类型：flow/bandwidth/request/qps");
            } else if (message.contains("InvalidHitType")) {
                return new BusinessException("命中率类型填写错误，应为：reqhitrate/flowhitrate");
            }

            // 域名相关错误
            else if (message.contains("InvalidDomain.NotFound")) {
                return new BusinessException("域名不属于当前用户或域名类型填写错误");
            } else if (message.contains("InvalidDomain.Offline")) {
                return new BusinessException("域名处于非法状态");
            } else if (message.contains("NoAvailableDomain")) {
                return new BusinessException("没有可用的域名信息");
            } else if (message.contains("DomainNotFound")) {
                return new BusinessException("通过域名ID没有找到域名信息");
            } else if (message.contains("DomainNameHasBeUsed")) {
                return new BusinessException("域名已存在");
            } else if (message.contains("RecordQueryFailed")) {
                return new BusinessException("域名未备案");
            } else if (message.contains("StatusCannotOrNoneedModified")) {
                return new BusinessException("当前状态不能或无需修改");
            } else if (message.contains("InvalidDomainStatus")) {
                return new BusinessException("无效域名状态，支持的状态：online/offline/configuring/configure_failed/icp_checking/icp_check_failed");
            } else if (message.contains("InvalidActionType")) {
                return new BusinessException("无效域动作类型，应为：start/stop");
            } else if (message.contains("InvalidDomainName")) {
                return new BusinessException("域名格式不符合规则");
            }

            // 区域和运营商相关错误
            else if (message.contains("InvalidRegion")) {
                return new BusinessException("加速区域参数错误，支持的区域：CN,NA,AS,EU,SA,AU,AF,HK,TW等");
            } else if (message.contains("InvalidArea")) {
                return new BusinessException("区域填写错误，支持的区域：CN,HK,TW,MO,US,JP,SG等");
            } else if (message.contains("InvalidIsp")) {
                return new BusinessException("运营商参数错误，支持的运营商：UN,CM,CT,CTT,PBS,CE,Other");
            } else if (message.contains("InvalidProvince")) {
                return new BusinessException("省份参数错误");
            }

            // 数据量限制相关错误
            else if (message.contains("TooManyDataPoints")) {
                return new BusinessException("超出接口最大吞吐量范围，数据点总数不能超过10000");
            } else if (message.contains("PageSizeOutOfRange")) {
                return new BusinessException("页面大小超出范围，应在1-500之间");
            } else if (message.contains("pageNumberOutOfRange")) {
                return new BusinessException("页面编号超出范围，应在1-10000之间");
            } else if (message.contains("TooManyReferList")) {
                return new BusinessException("refer URL数量过多，应在1-100之间");
            } else if (message.contains("QuotaOverfull")) {
                return new BusinessException("超过配额");
            }

            // 源站相关错误
            else if (message.contains("InvalidOriginType")) {
                return new BusinessException("无效源站类型，支持的类型：ipaddr/domain/ksvideo/KS3");
            } else if (message.contains("InvalidOriginType.Live")) {
                return new BusinessException("无效直播源站类型，应为：ipaddr/domain/ksvideo");
            } else if (message.contains("InvalidOriginType.Download")) {
                return new BusinessException("无效下载源站类型，应为：ipaddr/domain/KS3");
            } else if (message.contains("InvalidOriginProtocol")) {
                return new BusinessException("回源协议参数错误");
            } else if (message.contains("InvalidOriginProtocol.Live")) {
                return new BusinessException("无效的直播回源协议，应为：rtmp");
            } else if (message.contains("InvalidOriginProtocol.Download")) {
                return new BusinessException("无效的下载回源协议，应为：http");
            } else if (message.contains("InvalidOriginPort")) {
                return new BusinessException("无效的回源端口号，应为：80");
            } else if (message.contains("InvalidOriginAdress")) {
                return new BusinessException("源地址不规范");
            } else if (message.contains("OriginLineRepeat")) {
                return new BusinessException("线路不能重复");
            } else if (message.contains("OriginTypeNotUnique")) {
                return new BusinessException("回源类型必须唯一");
            } else if (message.contains("InvalidOriginPolicyType")) {
                return new BusinessException("无效轮询类型，应为：rr/quality");
            } else if (message.contains("PolicyBestCountOutOfRange")) {
                return new BusinessException("超出计数范围，应在1-10之间");
            } else if (message.contains("InvalidSourceStationLine")) {
                return new BusinessException("无效源站线路，应为：default/un/ct/cm");
            } else if (message.contains("InvalidSourceStationType")) {
                return new BusinessException("无效源站类型，应为：ipaddr/domain");
            } else if (message.contains("CanNotSetByOriginType")) {
                return new BusinessException("源站类型为KS3时，不能修改回源host");
            }

            // 协议相关错误
            else if (message.contains("InvalidCdnProtocol")) {
                return new BusinessException("访问协议参数错误");
            } else if (message.contains("InvalidCdnProtocol.Live")) {
                return new BusinessException("无效的直播协议，应为：http+flv/hls/rtmp");
            } else if (message.contains("InvalidCdnProtocol.Download")) {
                return new BusinessException("无效下载协议，应为：HTTP");
            } else if (message.contains("InvalidHttpProtocol")) {
                return new BusinessException("错误协议类型，应为：http/https/quic");
            }

            // 缓存相关错误
            else if (message.contains("InvalidCacheRuleType")) {
                return new BusinessException("无效的缓存规则类型，应为：file_suffix/directory/exact/url_regex");
            } else if (message.contains("InvalidCacheRuleValue")) {
                return new BusinessException("无效缓存规则值");
            } else if (message.contains("InvalidCacheTime")) {
                return new BusinessException("无效的缓存时间值，必须是大于0的整数");
            } else if (message.contains("CacheRuleListIsEmpty")) {
                return new BusinessException("缓存规则列表是空的");
            } else if (message.contains("RepeatedCacheValue")) {
                return new BusinessException("同一个缓存类型的值不能重复");
            }

            // 开关和配置相关错误
            else if (message.contains("InvalidSwitchValue")) {
                return new BusinessException("无效开关值，应为：on/off");
            } else if (message.contains("InvalidConfigInfoQueryCondition")) {
                return new BusinessException("无效的配置信息查询条件");
            } else if (message.contains("RequiresInputValue")) {
                return new BusinessException("要求输入值，该字段为必填项");
            }

            // Refer相关错误
            else if (message.contains("InvalidReferType")) {
                return new BusinessException("无效的refer类型，应为：block/allow");
            } else if (message.contains("NotCorrectURL")) {
                return new BusinessException("URL不正确");
            }

            // IP类型相关错误
            else if (message.contains("InvalidIpType")) {
                return new BusinessException("IpType输入错误，应为：ipv4/ipv6");
            }

            // 状态码相关错误
            else if (message.contains("InvalidCodeType")) {
                return new BusinessException("状态码类型填写错误，应为：2xx/3xx/4xx/5xx");
            }

            // 时间间隔相关错误
            else if (message.contains("DurationTimeNotMatch")) {
                return new BusinessException("当前和之前的间隔不一致");
            }

            // 参数绑定错误
            else if (message.contains("BindException")) {
                return new BusinessException("参数类型错误");
            }

            else if (message.contains("InvalidDomain.Found")) {
                return new BusinessException("域名格式不正确");
            } else if (message.contains("InvalidDomain.Malformed")) {
                return new BusinessException("域名格式不规范，请检查域名");
            } else if (message.contains("DomainAlreadyExist")) {
                return new BusinessException("域名已存在，请勿重复添加");
            } else if (message.contains("CdnDomainBelongToOtherUser")) {
                return new BusinessException("域名已被其他用户占用");
            } else if (message.contains("InvalidParameter")) {
                return new BusinessException("参数错误，请检查输入信息");
            } else if (message.contains("AccessDenied")) {
                return new BusinessException("权限不足，请检查您的访问密钥(AK/SK)");
            } else if (message.contains("Throttling")) {
                return new BusinessException("API调用频率过高，请稍后再试");
            } else if (message.contains("InternalFailure")) {
                return new BusinessException("金山云内部错误，请稍后重试或联系客服");
            } else if (message.contains("has not been auth")) {
                return new BusinessException("创建时发生错误，域名解析未进行验证");
            } else if (message.contains("InvalidOrigin")) {
                return new BusinessException("源站地址配置不正确");
            } else if (message.contains("QuotaExceeded.CdnDomain")) {
                return new BusinessException("您账户下的域名数量已达上限");
            } else if (message.contains("AddDomainException") || message.contains("not been auth")) {
                return new BusinessException("创建时发生错误，域名解析未进行验证");
            } else {
                String errorMsg = message.length() > 300 ? message.substring(0, 300) + "..." : message;
                return new BusinessException("Kingsoft CDN API error: " + errorMsg);
            }
        }

        String errorMsg = e.getMessage() != null ? e.getMessage() : "未知错误";
        if (errorMsg.length() > 100) {
            errorMsg = errorMsg.substring(0, 100) + "...";
        }
        log.error("未映射的金山云CDN错误: {}", errorMsg, e);
        return new BusinessException("操作失败: " + errorMsg);
    }
    private String getAndCheckDomainId(CdnDomain cdnDomain) throws BusinessException {
        if (cdnDomain == null || Assert.isEmpty(cdnDomain.getDomainId())) {
            throw new BusinessException("操作失败：缺少与金山云平台关联的域名ID，请检查域名状态");
        }
        return cdnDomain.getDomainId();
    }


    private JSONObject postKingsoftApiWithRetry(String action, String version, String path, Map<String, Object> body) throws BusinessException {
        BusinessException lastException = null;
        int maxAttempts = 4;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return kingsoftApiService.postKingsoftApi(action, version, path, body);
            } catch (BusinessException e) {
                lastException = e;
                if (!isRetryableKingsoftException(e) || attempt == maxAttempts) {
                    throw e;
                }
                sleepBeforeRetry(action, attempt);
            }
        }
        throw lastException == null ? new BusinessException("Kingsoft CDN API request failed") : lastException;
    }

    private JSONObject callKingsoftApiWithRetry(String action, String version, Map<String, String> params) throws BusinessException {
        BusinessException lastException = null;
        int maxAttempts = 4;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return kingsoftApiService.callKingsoftApi(action, version, params);
            } catch (BusinessException e) {
                lastException = e;
                if (!isRetryableKingsoftException(e) || attempt == maxAttempts) {
                    throw e;
                }
                sleepBeforeRetry(action, attempt);
            }
        }
        throw lastException == null ? new BusinessException("Kingsoft CDN API request failed") : lastException;
    }

    private boolean isRetryableKingsoftException(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return message.contains("\u4efb\u52a1\u961f\u5217")
                || message.contains("\u62e5\u585e")
                || message.contains("\u7a0d\u540e\u91cd\u8bd5")
                || message.contains("\u7cfb\u7edf\u7e41\u5fd9")
                || lower.contains("throttl")
                || lower.contains("rate")
                || lower.contains("too many")
                || lower.contains("busy")
                || lower.contains("queue")
                || lower.contains("timeout")
                || lower.contains("timed out")
                || lower.contains("temporar")
                || lower.contains("serviceunavailable")
                || lower.contains("internalerror");
    }

    private void sleepBeforeRetry(String action, int attempt) throws BusinessException {
        long delayMillis = 1500L * attempt;
        log.warn("Kingsoft CDN API {} hit a temporary error, retrying after {} ms (attempt {})", action, delayMillis, attempt + 1);
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Kingsoft CDN API retry interrupted");
        }
    }

    private GetCdnDomainsResult.DomainInfo getDomainInfoByName(String domainName) throws BusinessException {
        log.debug("开始通过域名 {} 查询DomainInfo", domainName);
        try {
            Map<String, String> params = new HashMap<>();
            params.put("DomainName", domainName);
            params.put("FuzzyMatch", "off");
            params.put("PageSize", "1");

            JSONObject responseJson = kingsoftApiService.callKingsoftApi("GetCdnDomains", "2019-06-01", params);

            if (responseJson == null) {
                throw new BusinessException("通过域名查询ID失败：API返回为空");
            }

            GetCdnDomainsResult result = JSON.toJavaObject(responseJson, GetCdnDomainsResult.class);

            if (result == null || result.getDomains() == null || result.getDomains().isEmpty()) {
                log.info("域名 {} 在金山云平台未找到", domainName);
                return null;
            }

            GetCdnDomainsResult.DomainInfo domainInfo = result.getDomains().get(0);
            log.debug("成功获取到DomainInfo: {}", domainInfo);
            return domainInfo;

        } catch (Exception e) {
            log.error("通过域名 {} 查询DomainId失败", domainName, e);
            if (e instanceof BusinessException) {
                throw (BusinessException) e;
            }
            throw handleKingsoftException(e);
        }
    }


    @Override
    public CdnDomain create(Long userId, String domainName, String businessType, String serviceArea, String originType, String ipOrDomain) throws BusinessException {
        log.info("金山云CDN创建域名开始，域名：{}，业务类型：{}，服务区域：{}，源站类型：{}，源站地址：{}",
                domainName, businessType, serviceArea, originType, ipOrDomain);

        try {
            if (Assert.isEmpty(domainName) || Assert.isEmpty(businessType) || Assert.isEmpty(serviceArea) ||
                    Assert.isEmpty(originType) || Assert.isEmpty(ipOrDomain)) {
                throw new BusinessException("参数不能为空");
            }

            Map<String, Object> params = new HashMap<>();
            params.put("DomainName", domainName);

            BusinessTypeEnum businessTypeEnum = BusinessTypeEnum.getOtherParam(businessType);
            if (businessTypeEnum == null) throw new BusinessException("不支持的业务类型: " + businessType);
            String kingsoftBusinessType = mapBusinessTypeToKingsoft(businessTypeEnum);
            kingsoftBusinessType = optimizeCdnTypeByDomain(domainName, kingsoftBusinessType);
            params.put("CdnType", kingsoftBusinessType);

            OriginTypeEnum originTypeEnum = OriginTypeEnum.getOtherParam(originType);
            if (originTypeEnum == null) throw new BusinessException("不支持的源站类型: " + originType);
            params.put("OriginType", mapOriginTypeToKingsoft(originTypeEnum));

            params.put("CdnProtocol", "http");
            params.put("OriginProtocol", "http");
            params.put("Origin", ipOrDomain);

            ServiceAreaEnum serviceAreaEnum = ServiceAreaEnum.getOtherParam(serviceArea);
            if (serviceAreaEnum == null) throw new BusinessException("不支持的服务区域: " + serviceArea);
            params.put("Regions", mapServiceAreaToKingsoft(serviceAreaEnum));
            String projectId = SystemConfig.kingsoftCdnConfig == null ? null : SystemConfig.kingsoftCdnConfig.getProjectId();
            if (!Assert.isEmpty(projectId)) {
                params.put("ProjectId", projectId.trim());
            }

            JSONObject result = postKingsoftApiWithRetry("AddCdnDomain", "V3", "/V3/AddCdnDomain", params);

            if (result == null) throw new BusinessException("金山云CDN API返回空响应");
            String domainId = result.getString("DomainId");
            if (Assert.isEmpty(domainId)) throw new BusinessException("金山云CDN API未返回域名ID");

            GetCdnDomainBasicInfoResult detailInfo = null;
            try {
                Map<String, String> basicInfoParams = new HashMap<>();
                basicInfoParams.put("DomainId", domainId);
                JSONObject basicInfoJson = callKingsoftApiWithRetry("GetCdnDomainBasicInfo", "2016-09-01", basicInfoParams);
                detailInfo = JSON.toJavaObject(basicInfoJson, GetCdnDomainBasicInfoResult.class);
            } catch (Exception e) {
                log.warn("Kingsoft domain {} was created, but immediate basic-info query failed. Continue with AddCdnDomain response. Error: {}",
                        domainName, e.getMessage());
                try {
                    GetCdnDomainsResult.DomainInfo domainInfo = getDomainInfoByName(domainName);
                    if (domainInfo != null) {
                        detailInfo = new GetCdnDomainBasicInfoResult();
                        detailInfo.setCname(domainInfo.getCname());
                        detailInfo.setDomainStatus(domainInfo.getDomainStatus());
                    }
                } catch (Exception queryException) {
                    log.warn("Kingsoft domain {} fallback query by name failed. Error: {}", domainName, queryException.getMessage());
                }
            }
            String officialCname = detailInfo == null ? null : detailInfo.getCname();
            if (Assert.isEmpty(officialCname)) {
                log.warn("创建域名 {} 成功，但未能获取到官方CNAME，将使用拼接规则", domainName);
                officialCname = domainName + ".download.ks-cdn.com";
            }

            String domainStatus = detailInfo == null ? result.getString("DomainStatus") : detailInfo.getDomainStatus();
            if (Assert.isEmpty(domainStatus)) {
                domainStatus = "configuring";
            }

            CdnDomain cdnDomain = CdnDomain.builder()
                    .userId(userId)
                    .domainName(domainName)
                    .businessType(businessType)
                    .serviceArea(serviceArea)
                    .domainId(domainId)
                    .domainStatus(mapKingsoftStatusToSystem(domainStatus))
                    .failureReason(kingsoftFailureReason(domainStatus))
                    .cnameKingsoft(officialCname)
                    .route(CdnRoute.KINGSOFT.getCode())
                    .build();

            String cdnTypeInfo = getCdnTypeDescription(kingsoftBusinessType);
            log.info("域名 {} 使用CDN类型: {}", domainName, cdnTypeInfo);
            log.info("金山云CDN创建域名成功，域名ID：{}，状态：{}，CDN类型：{}，官方CNAME：{}",
                    domainId, cdnDomain.getDomainStatus(), cdnTypeInfo, officialCname);

            // 保存域名信息
            CdnDomain savedDomain = save(cdnDomain);

            try {
                // 延迟一段时间再开启日志，等待域名信息同步
                Thread.sleep(2000); // 等待2秒
                enableLogging(domainId, domainName);
                log.info("域名 {} 默认开启日志功能成功", domainName);
            } catch (Exception e) {
                log.info("域名 {} 默认开启日志功能失败，但不影响域名创建: {}", domainName, e.getMessage());
            }

            return savedDomain;

        } catch (Exception e) {
            log.error("[金山云CDN] 创建域名失败 - 用户ID: {}, 域名: {}", userId, domainName, e);
            if (e instanceof BusinessException) {
                throw (BusinessException) e;
            }
            throw handleKingsoftException(e);
        }
    }

    @Override
    public CdnDomain configDNS(CdnDomain cdnDomain) throws BusinessException {
        if (cdnDomain == null || Assert.isEmpty(cdnDomain.getCnameKingsoft())) {
            throw new BusinessException("域名信息或金山云官方CNAME不能为空");
        }
        log.info("[金山云CDN] 配置DNS - 用户ID: {}, 域名: {}", cdnDomain.getUserId(), cdnDomain.getDomainName());
        try {
            String subDomain = DomainUtil.convertSubDomain(cdnDomain.getDomainName());
            CreateRecordDTO createRecordDTO = new CreateRecordDTO();
            createRecordDTO.setDomain(TencentDns.LOCAL_DOMAIN_NAME)
                    .setSubDomain(subDomain)
                    .setValue(cdnDomain.getCnameKingsoft());

            CreateRecordResponse resp = TencentApi.createRecord(createRecordDTO);
            if (resp == null || Assert.isEmpty(resp.getRecordId())) {
                log.error("创建域名 {} 解析失败", cdnDomain.getDomainName());
                throw new BusinessException("dns 解析失败");
            }
            cdnDomain.setCname(subDomain + "." + TencentDns.LOCAL_DOMAIN_NAME);
            cdnDomain.setTencentDnsId(resp.getRecordId());
            log.info("创建域名 {} 解析成功，系统 CNAME = {}", cdnDomain.getDomainName(), cdnDomain.getCname());
            return save(cdnDomain);
        } catch (Exception e) {
            log.error("金山云CDN配置DNS失败，域名：{}", cdnDomain.getDomainName(), e);
            throw handleKingsoftException(e);
        }
    }

    @Override
    public void disable(CdnDomain cdnDomain) throws BusinessException {
        String domainId = getAndCheckDomainId(cdnDomain);
        log.info("金山云CDN停用域名，域名：{}", cdnDomain.getDomainName());
        try {
            Map<String, String> params = new HashMap<>();
            params.put("DomainId", domainId);
            params.put("ActionType", "stop");
            JSONObject result = kingsoftApiService.callKingsoftApi("StartStopCdnDomain", "2016-09-01", params);
            log.info("金山云CDN停用域名成功，域名：{}，结果：{}", cdnDomain.getDomainName(), result);
        } catch (Exception e) {
            log.error("金山云CDN停用域名失败，域名：{}", cdnDomain.getDomainName(), e);
            throw handleKingsoftException(e);
        }
    }

    @Override
    public void enable(CdnDomain cdnDomain) throws BusinessException {
        String domainId = getAndCheckDomainId(cdnDomain);
        log.info("[金山云CDN] 启用域名 - 用户ID: {}, 域名: {}", cdnDomain.getUserId(), cdnDomain.getDomainName());
        try {
            Map<String, String> params = new HashMap<>();
            params.put("DomainId", domainId);
            params.put("ActionType", "start");
            JSONObject result = kingsoftApiService.callKingsoftApi("StartStopCdnDomain", "2016-09-01", params);
            log.info("[金山云CDN] 启用域名成功 - 用户ID: {}, 域名: {}, 结果: {}", cdnDomain.getUserId(), cdnDomain.getDomainName(), result);
        } catch (Exception e) {
            log.error("[金山云CDN] 启用域名失败 - 用户ID: {}, 域名: {}", cdnDomain.getUserId(), cdnDomain.getDomainName(), e);
            throw handleKingsoftException(e);
        }
    }

    @Override
    public void delete(CdnDomain cdnDomain) throws BusinessException {
        String domainId = getAndCheckDomainId(cdnDomain);
        log.info("金山云CDN删除域名，域名：{}", cdnDomain.getDomainName());
        try {
            Map<String, String> params = new HashMap<>();
            params.put("DomainId", domainId);
            JSONObject result = kingsoftApiService.callKingsoftApi("DeleteCdnDomain", "2016-09-01", params);
            log.info("金山云CDN删除域名成功，域名：{}，结果：{}", cdnDomain.getDomainName(), result);
        } catch (Exception e) {
            log.error("金山云CDN删除域名失败，域名：{}", cdnDomain.getDomainName(), e);
            throw handleKingsoftException(e);
        }
    }

    /**
     * 获取当前域名支持的回源协议列表（为前端提供）
     *
     * @param cdnDomain CDN域名
     * @return 支持的协议列表
     */
    public List<Map<String, String>> getSupportedOriginProtocols(CdnDomain cdnDomain) {
        List<Map<String, String>> protocols = new ArrayList<>();
        String cdnType = getCurrentCdnType(cdnDomain);

        log.info("获取域名 {} 支持的回源协议，CDN类型: {}", cdnDomain.getDomainName(), cdnType);

        if ("live".equalsIgnoreCase(cdnType)) {
            // 流媒体直播不可修改回源协议，返回空列表
            log.info("流媒体直播CDN类型不支持修改回源协议");
            return protocols;
        }

        // 其他类型支持所有协议
        Map<String, String> httpProtocol = new HashMap<>();
        httpProtocol.put("value", "http");
        httpProtocol.put("label", "HTTP");
        httpProtocol.put("description", "强制使用HTTP协议回源");
        protocols.add(httpProtocol);

        Map<String, String> httpsProtocol = new HashMap<>();
        httpsProtocol.put("value", "https");
        httpsProtocol.put("label", "HTTPS");
        httpsProtocol.put("description", "强制使用HTTPS协议回源");
        protocols.add(httpsProtocol);

        Map<String, String> followProtocol = new HashMap<>();
        followProtocol.put("value", "follow");
        followProtocol.put("label", "协议跟随");
        followProtocol.put("description", "跟随用户请求协议回源（推荐）");
        protocols.add(followProtocol);

        return protocols;
    }

    /**
     * 获取CDN类型的详细信息（为前端提供）
     */
    public Map<String, Object> getCdnTypeInfo(CdnDomain cdnDomain) {
        Map<String, Object> info = new HashMap<>();
        String cdnType = getCurrentCdnType(cdnDomain);

        info.put("cdnType", cdnType);
        info.put("cdnTypeName", getCdnTypeDescription(cdnType));
        info.put("supportOriginProtocolModify", !"live".equalsIgnoreCase(cdnType));
        info.put("supportedProtocols", getSupportedOriginProtocols(cdnDomain));

        return info;
    }

    /**
     * 获取当前域名的CDN类型
     */
    private String getCurrentCdnType(CdnDomain cdnDomain) {
        try {
            // 从数据库中获取业务类型，然后映射到金山云类型
            if (cdnDomain != null && Assert.notEmpty(cdnDomain.getBusinessType())) {
                BusinessTypeEnum businessType = BusinessTypeEnum.getOtherParam(cdnDomain.getBusinessType());
                if (businessType != null) {
                    return mapBusinessTypeToKingsoft(businessType);
                }
            }

            // 如果无法确定，尝试通过API获取
            GetCdnDomainsResult.DomainInfo domainInfo = getDomainInfoByName(cdnDomain.getDomainName());
            if (domainInfo != null && Assert.notEmpty(domainInfo.getCdnType())) {
                return domainInfo.getCdnType();
            }

            // 默认返回page类型
            return "page";
        } catch (Exception e) {
            log.warn("获取域名 {} CDN类型失败，使用默认类型: {}", cdnDomain.getDomainName(), e.getMessage());
            return "page";
        }
    }

    /**
     * 验证回源协议是否被CDN类型支持
     *
     * @param originProtocol 回源协议 (http, https, follow)
     * @param cdnType CDN类型 (page, file, video, wcdn, live)
     * @return 是否支持
     */
    private boolean isOriginProtocolSupported(String originProtocol, String cdnType) {
        if (Assert.isEmpty(originProtocol) || Assert.isEmpty(cdnType)) {
            return true; // 空值默认支持
        }

        // 验证协议值是否合法
        if (!("http".equalsIgnoreCase(originProtocol) ||
                "https".equalsIgnoreCase(originProtocol) ||
                "follow".equalsIgnoreCase(originProtocol))) {
            log.warn("不支持的回源协议: {}", originProtocol);
            return false;
        }

        // 根据CDN类型判断协议支持情况
        switch (cdnType.toLowerCase()) {
            case "page":    // 图片小文件
            case "file":    // 大文件下载
            case "video":   // 音视频点播
            case "wcdn":    // 全站加速
                // 这些类型支持所有协议: http, https, follow
                return true;

            case "live":    // 流媒体直播
                // 流媒体直播不可修改回源协议
                log.warn("流媒体直播CDN类型不支持修改回源协议，当前协议: {}", originProtocol);
                return false;

            default:
                // 未知类型，默认支持
                log.warn("未知的CDN类型: {}，默认支持协议: {}", cdnType, originProtocol);
                return true;
        }
    }

    private String mapBusinessTypeToKingsoft(BusinessTypeEnum businessType) {
        String businessTypeParam = businessType.getParam();
        switch (businessTypeParam) {
            case "web":
                return "page";
            case "download":
                return "file";
            case "video":
                return "video";
            case "fullsite":
                return "wcdn";
            default:
                return "wcdn";
        }
    }

    private String optimizeCdnTypeByDomain(String domainName, String originalCdnType) {
        String lowerDomainName = domainName.toLowerCase();
        if ("wcdn".equals(originalCdnType)) {
            return originalCdnType;
        }
        boolean needFullSiteAcceleration = false;
        if (lowerDomainName.contains("api") || lowerDomainName.startsWith("api.") ||
                lowerDomainName.contains("admin") || lowerDomainName.contains("manage") ||
                lowerDomainName.contains("console") || lowerDomainName.startsWith("admin.") ||
                lowerDomainName.startsWith("m.") || lowerDomainName.startsWith("mobile.") ||
                lowerDomainName.contains("app") || lowerDomainName.contains("wap") ||
                lowerDomainName.contains("service") || lowerDomainName.contains("gateway") ||
                lowerDomainName.contains("portal")) {
            needFullSiteAcceleration = true;
        } else if (lowerDomainName.contains("static") || lowerDomainName.contains("cdn") ||
                lowerDomainName.contains("img") || lowerDomainName.contains("assets")) {
            return originalCdnType;
        }

        if (needFullSiteAcceleration) {
            log.info("域名 {} 包含动态特征，从 {} 升级为全站加速(wcdn)", domainName, originalCdnType);
            return "wcdn";
        }
        return originalCdnType;
    }

    private String getCdnTypeDescription(String cdnType) {
        switch (cdnType) {
            case "wcdn": return "全站加速(WCDN) - 动静分离，智能路由优化";
            case "file": return "大文件下载加速 - 适用于软件、游戏等大文件分发";
            case "video": return "音视频点播加速 - 适用于视频、音频内容分发";
            case "page": return "图片小文件加速 - 适用于网站静态资源";
            case "live": return "流媒体直播加速 - 适用于直播推流分发";
            default: return "标准CDN加速";
        }
    }

    private String mapOriginTypeToKingsoft(OriginTypeEnum originType) {
        String param = originType.getParam();
        return "ipaddr".equals(param) ? "ipaddr" : "domain";
    }

    private String mapServiceAreaToKingsoft(ServiceAreaEnum serviceArea) {
        switch (serviceArea.getParam()) {
            case "mainland_china": return "CN";
            case "outside_mainland_china":
            case "overseas":
                return "OverSea";
            case "global": return "Global";
            default: return "CN";
        }
    }

    static String mapKingsoftStatusToSystem(String kingsoftStatus) {
        if (kingsoftStatus == null) return "configuring";
        switch (kingsoftStatus.trim().toLowerCase(Locale.ROOT)) {
            case "online": return "online";
            case "offline": return "offline";
            case "configuring":
            case "icp_checking":
                return "configuring";
            case "configure_failed":
            case "icp_check_failed":
            case "locked":
                return "configure_failed";
            default:
                return "configure_failed";
        }
    }

    static String kingsoftFailureReason(String kingsoftStatus) {
        if (kingsoftStatus == null) {
            return null;
        }
        String normalized = kingsoftStatus.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "locked":
                return "域名已被金山云锁定，请联系管理员或金山云处理";
            case "icp_check_failed":
                return "金山云备案检查失败，请检查域名备案信息";
            case "configure_failed":
                return "金山云域名配置失败，请联系管理员处理";
            case "online":
            case "offline":
            case "configuring":
            case "icp_checking":
                return null;
            default:
                return "金山云返回了无法识别的域名状态：" + kingsoftStatus;
        }
    }

    @Override
    public void saveIgnoreQueryString(CdnDomain cdnDomain, IgnoreQueryStringDTO config) throws BusinessException {
        String domainId = getAndCheckDomainId(cdnDomain);
        log.info("金山云CDN更新过滤参数配置，域名：{}，配置：{}", cdnDomain.getDomainName(), JSON.toJSONString(config));

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("DomainId", domainId);
            body.put("Enable", config.getEnable());

            if ("on".equalsIgnoreCase(config.getEnable())) {
                if (Assert.notEmpty(config.getType()) && Assert.notEmpty(config.getHashKeyArgs())) {
                    body.put("Type", config.getType());
                    body.put("HashKeyArgs", config.getHashKeyArgs());
                } else if (Assert.notEmpty(config.getType()) || Assert.notEmpty(config.getHashKeyArgs())) {
                    throw new BusinessException("当启用过滤参数类型时，类型和参数列表必须同时填写。");
                }
            }
            kingsoftApiService.postKingsoftApi(
                    "SetIgnoreQueryStringConfig", "2016-09-01",
                    "/2016-09-01/domain/SetIgnoreQueryStringConfig", body
            );
            log.info("金山云CDN成功更新过滤参数配置，域名：{}", cdnDomain.getDomainName());

        } catch (Exception e) {
            log.error("金山云CDN更新过滤参数配置失败，域名：{}", cdnDomain.getDomainName(), e);
            throw handleKingsoftException(e);
        }
    }


    @Override
    public void saveSourceStationConfig(CdnDomain cdnDomain, CdnDomainSourcesVo config) throws BusinessException {
        String domainId = getAndCheckDomainId(cdnDomain);
        log.info("金山云CDN更新源站配置（支持主备），域名：{}，配置：{}", cdnDomain.getDomainName(), JSON.toJSONString(config));

        try {
            CdnDomainSources mainSource = config.getMain();
            CdnDomainSources standbySource = config.getBack();

            if (mainSource == null || Assert.isEmpty(mainSource.getIpOrDomain())) {
                throw new BusinessException("主源站信息不能为空");
            }

            if (standbySource != null && Assert.notEmpty(standbySource.getIpOrDomain())) {
                log.info("检测到备源站配置，将启用高级回源策略。");


                Map<String, String> basicParams = new HashMap<>();
                basicParams.put("DomainId", domainId);
                basicParams.put("Origin", mainSource.getIpOrDomain());
                basicParams.put("OriginType", mainSource.getOriginType());

                String backOriginHost = mainSource.getHostName();
                if (backOriginHost == null && standbySource.getHostName() != null) {
                    backOriginHost = standbySource.getHostName();
                }
                if (backOriginHost != null) {
                    basicParams.put("BackOriginHost", backOriginHost);
                }

                String originProtocol = config.getOriginProtocol();
                if (originProtocol != null) {
                    basicParams.put("OriginProtocol", originProtocol);

                    Integer httpPort = mainSource.getHttpPort();
                    Integer httpsPort = mainSource.getHttpsPort();
                    if (httpPort == null && standbySource.getHttpPort() != null) {
                        httpPort = standbySource.getHttpPort();
                    }
                    if (httpsPort == null && standbySource.getHttpsPort() != null) {
                        httpsPort = standbySource.getHttpsPort();
                    }

                    int finalHttpPort = httpPort != null ? httpPort : 80;
                    int finalHttpsPort = httpsPort != null ? httpsPort : 443;

                    if ("http".equalsIgnoreCase(originProtocol)) {
                        basicParams.put("OriginPort", String.valueOf(finalHttpPort));
                    } else if ("https".equalsIgnoreCase(originProtocol)) {
                        basicParams.put("OriginPort", String.valueOf(finalHttpsPort));
                    } else {
                        if (finalHttpPort == finalHttpsPort) {
                            finalHttpsPort = finalHttpPort == 443 ? 8443 : 443;
                        }
                        basicParams.put("OriginPort", finalHttpPort + "," + finalHttpsPort);
                    }

                    log.info("设置基础回源配置: HTTP={}, HTTPS={}, Host={}, Protocol={}",
                            finalHttpPort, finalHttpsPort, backOriginHost, originProtocol);
                }

                kingsoftApiService.callKingsoftApi("ModifyCdnDomainBasicInfo", "2016-09-01", basicParams);
                log.info("已设置基础回源配置，现在启用高级回源策略");

                Map<String, Object> advancedBody = new HashMap<>();
                advancedBody.put("DomainId", domainId);
                advancedBody.put("Enable", "on");
                advancedBody.put("OriginPolicy", "quality");
                advancedBody.put("OriginPolicyBestCount", 1);
                advancedBody.put("OriginType", mainSource.getOriginType());
                advancedBody.put("Origin", mainSource.getIpOrDomain());
                advancedBody.put("BackupOriginType", standbySource.getOriginType());
                advancedBody.put("BackupOrigin", standbySource.getIpOrDomain());

                kingsoftApiService.postKingsoftApi(
                        "SetOriginAdvancedConfig", "2016-09-01",
                        "/2016-09-01/domain/SetOriginAdvancedConfig", advancedBody
                );

                log.info("已启用高级回源策略，基础配置（端口、协议、Host）将被保留");

                log.info("金山云CDN成功更新主备源站配置，域名：{}", cdnDomain.getDomainName());

            } else {
                log.info("未检测到备源站配置，将使用基础回源配置。");

                Map<String, Object> disableAdvancedBody = new HashMap<>();
                disableAdvancedBody.put("DomainId", domainId);
                disableAdvancedBody.put("Enable", "off");
                kingsoftApiService.postKingsoftApi(
                        "SetOriginAdvancedConfig", "2016-09-01",
                        "/2016-09-01/domain/SetOriginAdvancedConfig", disableAdvancedBody
                );

                Map<String, String> params = new HashMap<>();
                params.put("DomainId", domainId);
                params.put("Origin", mainSource.getIpOrDomain());
                params.put("OriginType", mainSource.getOriginType());
                if (mainSource.getHostName() != null) {
                    params.put("BackOriginHost", mainSource.getHostName());
                }
                String originProtocol = config.getOriginProtocol();

                String currentCdnType = getCurrentCdnType(cdnDomain);

                if (originProtocol != null && !isOriginProtocolSupported(originProtocol, currentCdnType)) {
                    throw new BusinessException(String.format("当前CDN类型(%s)不支持回源协议(%s)，请选择支持的协议", currentCdnType, originProtocol));
                }

                Integer httpPort = mainSource.getHttpPort();
                Integer httpsPort = mainSource.getHttpsPort();

                if (originProtocol == null) {
                    try {
                        Map<String, String> currentInfoParams = new HashMap<>();
                        currentInfoParams.put("DomainId", domainId);
                        JSONObject currentInfoJson = kingsoftApiService.callKingsoftApi("GetCdnDomainBasicInfo", "2016-09-01", currentInfoParams);
                        GetCdnDomainBasicInfoResult currentInfo = JSON.toJavaObject(currentInfoJson, GetCdnDomainBasicInfoResult.class);

                        if (currentInfo != null && Assert.notEmpty(currentInfo.getOriginProtocol())) {
                            originProtocol = currentInfo.getOriginProtocol();
                            log.info("未指定回源协议，保持当前协议: {}", originProtocol);
                        } else {
                            originProtocol = "follow";
                            log.info("无法获取当前协议，使用默认协议: follow");
                        }
                    } catch (Exception e) {
                        log.warn("获取当前回源协议失败，使用默认协议: follow", e);
                        originProtocol = "follow";
                    }
                }

                int finalHttpPort = httpPort != null ? httpPort : 80;
                int finalHttpsPort = httpsPort != null ? httpsPort : 443;

                if (finalHttpPort < 0 || finalHttpPort > 65535 || finalHttpsPort < 0 || finalHttpsPort > 65535) {
                    throw new BusinessException("回源端口必须在0-65535范围内");
                }

                if ("http".equalsIgnoreCase(originProtocol)) {
                    params.put("OriginProtocol", "http");
                    params.put("OriginPort", String.valueOf(finalHttpPort));
                    log.info("HTTP协议模式，设置回源配置: Protocol=http, Port={}", finalHttpPort);

                } else if ("https".equalsIgnoreCase(originProtocol)) {
                    params.put("OriginProtocol", "https");
                    params.put("OriginPort", String.valueOf(finalHttpsPort));
                    log.info("HTTPS协议模式，设置回源配置: Protocol=https, Port={}", finalHttpsPort);

                } else {
                    params.put("OriginProtocol", "follow");

                    if (finalHttpPort == finalHttpsPort) {
                        finalHttpsPort = finalHttpPort == 443 ? 8443 : 443;
                        log.warn("HTTP和HTTPS端口不能相同，自动调整HTTPS端口: {} -> {}", finalHttpPort, finalHttpsPort);
                    }

                    params.put("OriginPort", finalHttpPort + "," + finalHttpsPort);
                    log.info("协议跟随模式，设置回源配置: Protocol=follow, HTTP={}, HTTPS={}", finalHttpPort, finalHttpsPort);
                }


                boolean needUpdateBasicConfig = true; // 默认需要更新，确保用户的修改生效
                try {
                    Map<String, String> currentInfoParams = new HashMap<>();
                    currentInfoParams.put("DomainId", domainId);
                    JSONObject currentInfoJson = kingsoftApiService.callKingsoftApi("GetCdnDomainBasicInfo", "2016-09-01", currentInfoParams);
                    GetCdnDomainBasicInfoResult currentInfo = JSON.toJavaObject(currentInfoJson, GetCdnDomainBasicInfoResult.class);

                    if (currentInfo != null) {
                        boolean originChanged = !mainSource.getIpOrDomain().equals(currentInfo.getOrigin()) ||
                                !mainSource.getOriginType().equals(currentInfo.getOriginType());

                        boolean protocolChanged = !originProtocol.equals(currentInfo.getOriginProtocol());

                        boolean portChanged = false;
                        if ("follow".equalsIgnoreCase(originProtocol)) {
                            String currentPortStr = currentInfo.getOriginHttpPort() + "," + currentInfo.getOriginHttpsPort();
                            String newPortStr = finalHttpPort + "," + finalHttpsPort;
                            portChanged = !newPortStr.equals(currentPortStr);
                        } else if ("http".equalsIgnoreCase(originProtocol)) {
                            portChanged = !Integer.valueOf(finalHttpPort).equals(currentInfo.getOriginHttpPort());
                        } else if ("https".equalsIgnoreCase(originProtocol)) {
                            portChanged = !Integer.valueOf(finalHttpsPort).equals(currentInfo.getOriginHttpsPort());
                        }

                        boolean hostChanged = (mainSource.getHostName() != null);

                        if (!originChanged && !protocolChanged && !portChanged && !hostChanged) {
                            needUpdateBasicConfig = false;
                            String currentPortDisplay = currentInfo.getOriginHttpPort() + "," + currentInfo.getOriginHttpsPort();
                            log.info("配置无变化，跳过更新: Origin={}, Protocol={}, Port={}",
                                    currentInfo.getOrigin(), currentInfo.getOriginProtocol(), currentPortDisplay);
                        } else {
                            log.info("检测到配置变化: Origin={}, Protocol={}, Port={}, Host={}",
                                    originChanged, protocolChanged, portChanged, hostChanged);
                        }
                    }
                } catch (Exception e) {
                    log.warn("获取当前域名基础配置失败，将强制更新: {}", e.getMessage());
                    needUpdateBasicConfig = true;
                }

                if (needUpdateBasicConfig) {
                    Map<String, String> basicParams = new HashMap<>();
                    basicParams.put("DomainId", domainId);
                    basicParams.put("Origin", mainSource.getIpOrDomain());
                    basicParams.put("OriginType", mainSource.getOriginType());
                    if (mainSource.getHostName() != null) {
                        basicParams.put("BackOriginHost", mainSource.getHostName());
                    }
                    basicParams.put("OriginProtocol", originProtocol);
                    basicParams.put("OriginPort", params.get("OriginPort"));

                    kingsoftApiService.callKingsoftApi("ModifyCdnDomainBasicInfo", "2016-09-01", basicParams);
                    log.info("金山云CDN已更新源站地址和类型，保持端口配置不变。域名：{}", cdnDomain.getDomainName());
                } else {
                    log.info("源站地址和类型无变化，跳过基础配置更新，保持当前端口设置。域名：{}", cdnDomain.getDomainName());
                }

                log.info("金山云CDN成功更新单源站配置，并已禁用高级回源策略。域名：{}", cdnDomain.getDomainName());
            }
        } catch (Exception e) {
            log.error("金山云CDN更新源站配置失败，域名：{}", cdnDomain.getDomainName(), e);
            throw handleKingsoftException(e);
        }
    }


    @Override
    public void httpsConfiguration(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        String domainId = getAndCheckDomainId(cdnDomain);
        log.info("金山云CDN更新HTTPS配置，域名：{}", cdnDomain.getDomainName());
        try {
            HttpPutBodyDTO httpsConfig = config.getHttps();

            if (httpsConfig == null || "off".equalsIgnoreCase(httpsConfig.getHttps_status())) {
                kingsoftCertificateService.configCertificate(domainId, "off", null, null, null, null);
            } else {
                String certificateName = httpsConfig.getCertificate_name();
                String serverCertificate = httpsConfig.getCertificate_value();
                String privateKey = httpsConfig.getPrivate_key();

                kingsoftCertificateService.configCertificate(
                        domainId,
                        "on",
                        null,
                        certificateName,
                        serverCertificate,
                        privateKey
                );
            }

            log.info("金山云CDN更新HTTPS配置成功，域名：{}", cdnDomain.getDomainName());
        } catch (Exception e) {
            log.error("金山云CDN更新HTTPS配置失败，域名：{}", cdnDomain.getDomainName(), e);
            throw handleKingsoftException(e);
        }
    }

    @Override
    public void httpsConfigurationOther(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        String domainId = getAndCheckDomainId(cdnDomain);
        HttpPutBodyDTO httpsConfig = config.getHttps();
        log.info("金山云CDN更新HTTPS高级配置，域名：{}", cdnDomain.getDomainName());

        try {
            if (httpsConfig != null && httpsConfig.getHttp2_status() != null) {
                Map<String, Object> body = new HashMap<>();
                body.put("DomainId", domainId);
                body.put("Enable", httpsConfig.getHttp2_status());

                kingsoftApiService.postKingsoftApi(
                        "SetHttp2OptionConfig",
                        "2016-09-01",
                        "/2016-09-01/domain/SetHttp2OptionConfig",
                        body
                );
                log.info("金山云CDN更新HTTP/2配置成功，状态为：{}，域名：{}", httpsConfig.getHttp2_status(), cdnDomain.getDomainName());
            }

        } catch (Exception e) {
            log.error("金山云CDN更新HTTPS高级配置失败，域名：{}", cdnDomain.getDomainName(), e);
            throw handleKingsoftException(e);
        }
    }

    @Override
    public void saveCacheRules(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        String domainId = getAndCheckDomainId(cdnDomain);
        log.info("金山云CDN更新缓存规则，域名：{}，传入规则数：{}", cdnDomain.getDomainName(), config.getCacheRules() != null ? config.getCacheRules().size() : 0);

        try {
            Map<String, String> params = new HashMap<>();
            params.put("DomainId", domainId);
            JSONObject responseJson = kingsoftApiService.callKingsoftApi("GetDomainConfigs", "2016-09-01", params);
            GetDomainConfigsResult onlineConfigs = JSON.toJavaObject(responseJson, GetDomainConfigsResult.class);

            List<CacheRuleConfig.CacheRule> onlineRules = new ArrayList<>();
            if (onlineConfigs != null && onlineConfigs.getCacheRuleConfig() != null && onlineConfigs.getCacheRuleConfig().getCacheRules() != null) {
                onlineRules = onlineConfigs.getCacheRuleConfig().getCacheRules();
            }

            List<Map<String, Object>> rulesToSubmit = new ArrayList<>();
            if (config.getCacheRules() != null) {
                for (CacheRuleDTO dto : config.getCacheRules()) {
                    if ("all".equals(dto.getMatch_type()) || "global".equals(dto.getMatch_type())) {
                        log.info("已跳过'所有文件(global)'类型的规则，该规则不可通过API修改。");
                        continue;
                    }
                    rulesToSubmit.add(adaptDtoToKingsoftCacheRule(dto));
                }
            }


            if (rulesToSubmit.isEmpty()) {
                log.info("没有需要提交的（非全局）缓存规则。如果想清空所有规则，请提交一个空列表。");
            }

            Map<String, Object> body = new HashMap<>();
            body.put("DomainId", domainId);
            body.put("CacheRules", rulesToSubmit);

            kingsoftApiService.postKingsoftApi(
                    "SetCacheRuleConfig", "2016-09-01",
                    "/2016-09-01/domain/SetCacheRuleConfig", body
            );
            log.info("金山云CDN成功更新 {} 条自定义缓存规则，域名：{}", rulesToSubmit.size(), cdnDomain.getDomainName());

        } catch (Exception e) {
            log.error("金山云CDN更新缓存规则失败，域名：{}", cdnDomain.getDomainName(), e);
            throw handleKingsoftException(e);
        }
    }

    private Map<String, Object> adaptDtoToKingsoftCacheRule(CacheRuleDTO dto) {
        Map<String, Object> rule = new HashMap<>();

        String matchType = mapSystemToCacheRuleType(dto.getMatch_type());
        String matchValue = dto.getMatch_value();

        if ("file_suffix".equals(matchType) && matchValue != null) {
            matchValue = Arrays.stream(matchValue.split("[;,]"))
                    .map(suffix -> suffix.trim().startsWith(".") ? suffix.trim().substring(1) : suffix.trim())
                    .collect(Collectors.joining(","));
        }
        if ("directory".equals(matchType) && matchValue != null && !matchValue.endsWith("/")) {
            matchValue = matchValue + "/";
        }

        rule.put("CacheRuleType", matchType);
        rule.put("Value", matchValue);

        if (dto.getTtl() != null && dto.getTtl() == 0) {
            rule.put("CacheEnable", "on");
            rule.put("CacheTime", 0);
            if (dto.getFollow_origin() != null) {
                rule.put("RespectOrigin", dto.getFollow_origin());
            }
        } else if (dto.getTtl() != null && dto.getTtl() > 0) {
            rule.put("CacheEnable", "on");
            rule.put("CacheTime", KuocaiBaseUtil.toSeconds(dto.getTtl(), dto.getTtl_unit()));
            if (dto.getFollow_origin() != null) {
                rule.put("RespectOrigin", dto.getFollow_origin());
            }
        } else {
            rule.put("CacheEnable", "off");
        }

        return rule;
    }

    private String mapSystemToCacheRuleType(String systemType) {
        if (systemType == null) return null;
        switch (systemType) {
            case "file_extension": return "file_suffix";
            case "catalog": return "directory";
            case "full_path": return "exact";
            default: return systemType;
        }
    }

    @Override
    public DomainConfig getDomainConfig(String domainName) throws BusinessException {
        log.info("金山云CDN获取域名配置，域名：{}", domainName);
        try {
            GetCdnDomainsResult.DomainInfo basicDomainInfo = getDomainInfoByName(domainName);
            if (basicDomainInfo == null) {
                throw new BusinessException("在金山云平台未找到域名: " + domainName);
            }
            String domainId = basicDomainInfo.getDomainId();

            Map<String, String> params = new HashMap<>();
            params.put("DomainId", domainId);
            JSONObject responseJson = kingsoftApiService.callKingsoftApi("GetDomainConfigs", "2016-09-01", params);
            if (responseJson == null) {
                throw new BusinessException("获取金山云域名配置失败：API返回为空");
            }
            GetDomainConfigsResult configsResult = JSON.toJavaObject(responseJson, GetDomainConfigsResult.class);

            GetCertificatesResult certificatesResult = null;
            boolean httpsEnabled = configsResult.getCertificateConfig() != null && "on".equalsIgnoreCase(configsResult.getCertificateConfig().getEnable());

            if (httpsEnabled) {
                log.info("检测到域名 {} 已开启HTTPS，正在获取证书详情...", domainName);
                Map<String, Object> certParams = new HashMap<>();
                certParams.put("DomainName", domainName);
                certParams.put("PageSize", 10);
                certParams.put("PageNum", 1);

                String certPath = "/2016-09-01/cert/GetCertificates";
                JSONObject certsJson = kingsoftApiService.postKingsoftApi("GetCertificates", "2016-09-01", certPath, certParams);

                if (certsJson != null) {
                    certificatesResult = JSON.toJavaObject(certsJson, GetCertificatesResult.class);
                }
            }

            return adaptToDomainConfig(basicDomainInfo, configsResult, certificatesResult);

        } catch (Exception e) {
            log.error("获取金山云域名配置失败，域名：{}", domainName, e);
            throw handleKingsoftException(e);
        }
    }

    private String mapKingsoftTypeToSystem(String kingsoftType) {
        if (kingsoftType == null) return null;
        switch (kingsoftType) {
            case "page":
            case "wcdn":
                return BusinessTypeEnum.WEB.getParam();
            case "file":
                return BusinessTypeEnum.DOWNLOAD.getParam();
            case "video":
                return BusinessTypeEnum.VIDEO.getParam();
            default:
                return kingsoftType;
        }
    }

    private String mapKingsoftRegionToSystem(String kingsoftRegion) {
        if (kingsoftRegion == null) return null;
        switch (kingsoftRegion) {
            case "CN":
                return ServiceAreaEnum.MAINLAND_CHINA.getParam();
            case "OverSea":
                return ServiceAreaEnum.OUTSIDE_MAINLAND_CHINA.getParam();
            case "Global":
                return ServiceAreaEnum.GLOBAL.getParam();
            default:
                return kingsoftRegion;
        }
    }
    private DomainConfig adaptToDomainConfig(GetCdnDomainsResult.DomainInfo listInfo, GetDomainConfigsResult configs,GetCertificatesResult certificatesResult) throws BusinessException {
        Map<String, String> basicInfoParams = new HashMap<>();
        basicInfoParams.put("DomainId", listInfo.getDomainId());
        JSONObject basicInfoJson = kingsoftApiService.callKingsoftApi("GetCdnDomainBasicInfo", "2016-09-01", basicInfoParams);
        GetCdnDomainBasicInfoResult detailInfo = JSON.toJavaObject(basicInfoJson, GetCdnDomainBasicInfoResult.class);

        List<DomainAdvancedInfo.ErrorPage> errorPages = new ArrayList<>();
        try {
            if (configs.getErrorPageConfig() != null && configs.getErrorPageConfig().getErrorPages() != null) {
                for (GetDomainConfigsResult.ErrorPage errorPage : configs.getErrorPageConfig().getErrorPages()) {
                    String errorHttpCode = errorPage.getErrorHttpCode();
                    String customPageUrl = errorPage.getCustomPageUrl();

                    if (Assert.notEmpty(errorHttpCode) && Assert.notEmpty(customPageUrl)) {
                        errorPages.add(DomainAdvancedInfo.ErrorPage.builder()
                                .errorHttpCode(errorHttpCode)
                                .customPageUrl(customPageUrl)
                                .build());
                    }
                }
                log.info("金山云域名 {} 从API响应中获取到 {} 个自定义错误页面配置", listInfo.getDomainName(), errorPages.size());
            } else {
                log.info("金山云域名 {} 没有错误页面配置", listInfo.getDomainName());
            }
        } catch (Exception e) {
            log.warn("获取金山云域名 {} 的自定义错误页面配置失败: {}", listInfo.getDomainName(), e.getMessage());
        }

        String backOriginHost = (configs.getBackOriginHostConfig() != null) ? configs.getBackOriginHostConfig().getBackOriginHost() : null;

        String httpPortStr = "80";
        String httpsPortStr = "443";

        if (detailInfo.getOriginHttpPort() != null) {
            httpPortStr = String.valueOf(detailInfo.getOriginHttpPort());
        }
        if (detailInfo.getOriginHttpsPort() != null) {
            httpsPortStr = String.valueOf(detailInfo.getOriginHttpsPort());
        }

        log.debug("获取到的端口信息: HTTP={}, HTTPS={}", httpPortStr, httpsPortStr);

        DomainBasicInfo.SourceStationPrimaryInfo primarySource = DomainBasicInfo.SourceStationPrimaryInfo.builder()
                .sourceStationType(detailInfo.getOriginType())
                .ipOrDomain(detailInfo.getOrigin())
                .sourceHost(backOriginHost)
                .httpPort(httpPortStr)
                .httpsPort(httpsPortStr)
                .build();
        DomainBasicInfo.SourceStationStandbyInfo standbySource = DomainBasicInfo.SourceStationStandbyInfo.builder().build();
        GetDomainConfigsResult.OriginAdvancedConfig advancedConfig = configs.getOriginAdvancedConfig();

        if (advancedConfig != null && "on".equalsIgnoreCase(advancedConfig.getEnable()) && Assert.notEmpty(advancedConfig.getBackupOrigin())) {
            log.info("检测到已启用的高级回源配置，正在解析备源站信息...");
            standbySource = DomainBasicInfo.SourceStationStandbyInfo.builder()
                    .sourceStationType(advancedConfig.getBackupOriginType())
                    .ipOrDomain(advancedConfig.getBackupOrigin())
                    .sourceHost(backOriginHost)
                    .httpPort(httpPortStr)
                    .httpsPort(httpsPortStr)
                    .build();
            log.info("备源站信息解析成功：{}", JSON.toJSONString(standbySource));
        }

        boolean httpsEnabled = (configs.getCertificateConfig() != null) && "on".equalsIgnoreCase(configs.getCertificateConfig().getEnable());
        DomainBasicInfo domainBasicInfo = DomainBasicInfo.builder()
                .domainName(listInfo.getDomainName())
                .domainStatus(mapKingsoftStatusToSystem(listInfo.getDomainStatus()))
                .failureReason(kingsoftFailureReason(listInfo.getDomainStatus()))
                .cname(listInfo.getCname())
                .httpsStatus(httpsEnabled ? "1" : "0")
                .businessType(mapKingsoftTypeToSystem(listInfo.getCdnType()))
                .serviceArea(mapKingsoftRegionToSystem(listInfo.getRegion()))
                .createTime(KuocaiDateUtil.isoStrToDate(listInfo.getCreatedTime()))
                .updateTime(KuocaiDateUtil.isoStrToDate(listInfo.getModifiedTime()))
                .sourceStationPrimaryInfo(primarySource)
                .sourceStationStandbyInfo(standbySource)
                .build();

        GetDomainConfigsResult.VideoSeekConfig videoSeekConfig = configs.getVideoSeekConfig();
        GetDomainConfigsResult.OriginAdvancedConfig originAdvancedConfig = configs.getOriginAdvancedConfig();
        DomainBackSourceInfo domainBackSourceInfo = DomainBackSourceInfo.builder()
                .origin_protocol(detailInfo.getOriginProtocol())
                .origin_range_status((videoSeekConfig != null) ? videoSeekConfig.getEnable() : "off")
                .origin_receive_timeout((originAdvancedConfig != null && originAdvancedConfig.getOriginReadTimeout() != null) ?
                        String.valueOf(originAdvancedConfig.getOriginReadTimeout() / 1000) : "60")
                .build();

        ArrayList<DomainCacheInfo.CacheRule> cacheRules = new ArrayList<>();
        if (configs.getCacheRuleConfig() != null && configs.getCacheRuleConfig().getCacheRules() != null) {
            for (CacheRuleConfig.CacheRule rule : configs.getCacheRuleConfig().getCacheRules()) {
                // 跳过全局默认规则
                String cacheRuleType = rule.getCacheRuleType();
                if ("all".equalsIgnoreCase(cacheRuleType) || cacheRuleType == null) {
                    log.debug("跳过全局默认缓存规则，该规则不在UI中显示");
                    continue;
                }

                DomainCacheInfo.CacheRule.CacheRuleBuilder ruleBuilder = DomainCacheInfo.CacheRule.builder()
                        .match_type(mapCacheRuleTypeToSystem(rule.getCacheRuleType()))
                        .match_value(rule.getValue())
                        .priority(0)
                        .follow_origin(rule.getRespectOrigin());

                if ("on".equalsIgnoreCase(rule.getCacheEnable())) {
                    ruleBuilder.ttl(KuocaiBaseUtil.getUnitCacheTime(rule.getCacheTime()))
                            .ttl_unit(KuocaiBaseUtil.getCacheTimeUnit(rule.getCacheTime()));
                } else {
                    ruleBuilder.ttl(null)
                            .ttl_unit(null);
                }

                cacheRules.add(ruleBuilder.build());
            }
        }
        DomainCacheInfo domainCacheInfo = DomainCacheInfo.builder()
                .cache_rules(cacheRules)
                .error_code_cache(new ArrayList<>())
                .ignore_query_string(new IgnoreQueryStringDTO())
                .build();

        // 填充过滤参数信息
        GetDomainConfigsResult.IgnoreQueryStringConfig iqsConfig = configs.getIgnoreQueryStringConfig();
        if (iqsConfig != null) {
            IgnoreQueryStringDTO iqsDto = domainCacheInfo.getIgnore_query_string();
            iqsDto.setEnable(iqsConfig.getEnable());
            iqsDto.setType(iqsConfig.getType());
            iqsDto.setHashKeyArgs(iqsConfig.getHashKeyArgs());
        }

        GetDomainConfigsResult.IpProtectionConfig ipConfig = configs.getIpProtectionConfig();
        GetDomainConfigsResult.ReferProtectionConfig referConfig = configs.getReferProtectionConfig();
        DomainVisitInfo.IpFilter ipFilter = DomainVisitInfo.IpFilter.builder()
                .type((ipConfig != null && "on".equalsIgnoreCase(ipConfig.getEnable())) ? ipConfig.getIpType() : "off")
                .value((ipConfig != null) ? ipConfig.getIpList() : "")
                .build();
        DomainVisitInfo.Referer referer = DomainVisitInfo.Referer.builder()
                .type((referConfig != null && "on".equalsIgnoreCase(referConfig.getEnable())) ? referConfig.getReferType() : "off")
                .referer_type((referConfig != null && "on".equalsIgnoreCase(referConfig.getEnable())) ?
                        ("block".equals(referConfig.getReferType()) ? 1 : 2) : 0)
                .value((referConfig != null) ? referConfig.getReferList() : "")
                .include_empty((referConfig != null) && "on".equalsIgnoreCase(referConfig.getAllowEmpty()))
                .build();
        DomainVisitInfo.UserAgentFilter userAgentFilter = DomainVisitInfo.UserAgentFilter.builder()
                .type("off")
                .ua_list(Collections.emptyList())
                .build();

        // 处理URL鉴权配置
        DomainVisitInfo.UrlAuth.UrlAuthBuilder urlAuthBuilder = DomainVisitInfo.UrlAuth.builder().status("off");
        if (configs.getRequestAuthConfig() != null && "on".equalsIgnoreCase(configs.getRequestAuthConfig().getEnable())) {
            GetDomainConfigsResult.RequestAuthConfig authConfig = configs.getRequestAuthConfig();
            // 将金山云API的鉴权类型映射回前端类型
            String frontendType = mapApiUrlAuthType(authConfig.getAuthType());

            urlAuthBuilder.status("on")
                    .type(frontendType)
                    .primary_key(authConfig.getKey1())
                    .secondary_key(authConfig.getKey2())
                    .expire_time(authConfig.getExpirationTime());
        }

        DomainVisitInfo domainVisitInfo = DomainVisitInfo.builder()
                .ip_filter(ipFilter)
                .referer(referer)
                .user_agent_filter(userAgentFilter)
                .url_auth(urlAuthBuilder.build())
                .build();

        List<String> compressTypes = new ArrayList<>();
        if (configs.getPageCompressConfig() != null && "on".equalsIgnoreCase(configs.getPageCompressConfig().getEnable())) compressTypes.add("gzip");
        if (configs.getBrCompressConfig() != null && "on".equalsIgnoreCase(configs.getBrCompressConfig().getEnable())) compressTypes.add("br");
        DomainAdvancedInfo.Compress.CompressBuilder compressBuilder = DomainAdvancedInfo.Compress.builder().status("off");
        if (!compressTypes.isEmpty()) compressBuilder.status("on").type(String.join(",", compressTypes));
        ArrayList<DomainAdvancedInfo.HttpResponseHeader> httpResponseHeaders = new ArrayList<>();
        if (configs.getHttpHeadersConfig() != null && configs.getHttpHeadersConfig().getHttpHeaderRules() != null) {
            for (HttpHeadersConfig.HttpHeaderRule rule : configs.getHttpHeadersConfig().getHttpHeaderRules()) {

                httpResponseHeaders.add(DomainAdvancedInfo.HttpResponseHeader.builder()
                        .name(rule.getHeader())
                        .value(rule.getValue())
                        .action(rule.getAction())
                        .build());
            }
        }

        DomainAdvancedInfo domainAdvancedInfo = DomainAdvancedInfo.builder()
                .compress(compressBuilder.build())
                .http_response_header(httpResponseHeaders)
                .error_pages(errorPages)
                .build();


        GetDomainConfigsResult.TLSVersionConfig tlsConfig = configs.getTlsVersionConfig();
        DomainHttpsInfo.HttpGetBody.HttpGetBodyBuilder httpGetBodyBuilder = DomainHttpsInfo.HttpGetBody.builder().https_status("off");


        if (httpsEnabled) {
            httpGetBodyBuilder.https_status("on");

            if (certificatesResult != null && certificatesResult.getCertificates() != null && !certificatesResult.getCertificates().isEmpty()) {
                GetCertificatesResult.Certificate cert = certificatesResult.getCertificates().get(0);
                log.info("成功匹配到证书: {}", cert.getCertificateName());

                httpGetBodyBuilder.certificate_name(cert.getCertificateName());
                httpGetBodyBuilder.certificate_value(cert.getCertificateContent());
                httpGetBodyBuilder.certificate_type(cert.getCertificateType());
                if (Assert.notEmpty(cert.getExpirationTime())) {
                    httpGetBodyBuilder.expire_time(Long.parseLong(cert.getExpirationTime()) * 1000);
                }
                httpGetBodyBuilder.certificate_source(1);
            } else {
                log.warn("域名 {} 已开启HTTPS，但未能获取到关联的证书信息。", listInfo.getDomainName());
            }
        }

        if (configs.getHttp2OptionConfig() != null) {
            httpGetBodyBuilder.http2_status(configs.getHttp2OptionConfig().getEnable());
        }
        if (tlsConfig != null && tlsConfig.getTlsVersion() != null) {
            httpGetBodyBuilder.tls_version(String.join(",", tlsConfig.getTlsVersion()));
        }

        DomainHttpsInfo.ForceRedirect.ForceRedirectBuilder forceRedirectBuilder = DomainHttpsInfo.ForceRedirect.builder().status("off");
        if (configs.getForceRedirectConfig() != null && !"off".equalsIgnoreCase(configs.getForceRedirectConfig().getRedirectType())) {
            String redirectType = configs.getForceRedirectConfig().getRedirectType();
            int redirectCode = configs.getForceRedirectConfig().getRedirectCode();
            forceRedirectBuilder.status("on")
                    .type(redirectType)
                    .redirectType(redirectType)
                    .redirect_code(String.valueOf(redirectCode))
                    .redirectCode(redirectCode);
        }
        DomainHttpsInfo domainHttpsInfo = DomainHttpsInfo.builder().https(httpGetBodyBuilder.build()).force_redirect(forceRedirectBuilder.build()).build();

        return DomainConfig.builder()
                .domainBasicInfo(domainBasicInfo)
                .domainBackSourceInfo(domainBackSourceInfo)
                .domainCacheInfo(domainCacheInfo)
                .domainVisitInfo(domainVisitInfo)
                .domainAdvancedInfo(domainAdvancedInfo)
                .domainHttpsInfo(domainHttpsInfo)
                .build();
    }
    private String mapCacheRuleTypeToSystem(String kingsoftType) {
        if (kingsoftType == null) return null;
        switch (kingsoftType) {
            case "file_suffix":
                return "file_extension";
            case "directory":
                return "catalog";

            case "exact":
                return "full_path";
            default:
                return kingsoftType;
        }
    }



    @Override
    public DomainVerifyRecordInfo createVerifyRecord(String domainName) throws BusinessException {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("DomainName", domainName);
            JSONObject resp = kingsoftApiService.callKingsoftApi("GetDomainAuthContent", "2020-06-30", params);
            String content = resp.getString("Content");
            if (Assert.isEmpty(content)) {
                throw new BusinessException("无法获取域名验证内容");
            }
            return DomainVerifyRecordInfo.builder()
                    .subDomain("ksy-cdnauth").record(content).recordType("TXT")
                    .fileVerifyUrl("#").fileVerifyDomains(new String[]{domainName})
                    .fileVerifyName("[不可用]").content(content)
                    .build();
        } catch (Exception e) {
            throw handleKingsoftException(e);
        }
    }

    @Override
    public void verifyDomainRecord(String domainName, String verifyType) throws BusinessException {
        final int MAX_ATTEMPTS = 3;
        final long WAIT_MS = 10_000L;
        String authType;
        if (Assert.isEmpty(verifyType) || "dnsCheck".equalsIgnoreCase(verifyType) || "dns".equalsIgnoreCase(verifyType)) {
            authType = "dnsCheck";
        } else if ("file".equalsIgnoreCase(verifyType) || "fileCheck".equalsIgnoreCase(verifyType)) {
            authType = "fileCheck";
        } else {
            throw new BusinessException("非法的验证类型，仅支持 dns / file");
        }

        try {
            Map<String, String> p = new HashMap<>();
            p.put("DomainName", domainName);
            JSONObject ac = kingsoftApiService.callKingsoftApi("GetDomainAuthContent", "2020-06-30", p);
            log.info("[验证前检查] 域名 {} 的验证内容(Content): {}", domainName, ac.toJSONString());
        } catch (Exception ex) {
            log.warn("[验证前检查] 获取域名 {} 的Content失败: {}", domainName, ex.getMessage());
        }

        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("DomainName", domainName);
                body.put("AuthType", authType);
                JSONObject resp = kingsoftApiService.postKingsoftApi("AuthDomainOwner", "2020-06-30", "/2020-06-30/domain/AuthDomainOwner", body);
                log.info("[AuthDomainOwner] 第 {}/{} 次尝试，域名: {}, 响应: {}", i, MAX_ATTEMPTS, domainName, resp.toJSONString());

                String result = resp.getString("Result");
                if ("pass".equalsIgnoreCase(result)) {
                    log.info("🎉 域名 {} 归属验证通过", domainName);
                    return;
                }
            } catch (Exception inner) {
                log.warn("[AuthDomainOwner] 第 {}/{} 次尝试出现异常: {}", i, MAX_ATTEMPTS, inner.getMessage());
            }

            if (i < MAX_ATTEMPTS) {
                try {
                    log.info("域名 {} 验证未通过，{}秒后进行第 {} 次重试...", domainName, WAIT_MS / 1000, i + 1);
                    Thread.sleep(WAIT_MS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw new BusinessException("域名归属验证连续 " + MAX_ATTEMPTS + " 次失败，请检查DNS/文件配置或稍后再试");
    }

    @Override
    public void saveIpBlackWhiteList(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        String domainId = getAndCheckDomainId(cdnDomain);
        log.info("金山云CDN更新IP黑白名单，域名：{}，配置：{}", cdnDomain.getDomainName(), JSON.toJSONString(config));

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("DomainId", domainId);

            if (config.getType() == null || config.getType() == 0) {
                body.put("Enable", "off");
            } else {
                body.put("Enable", "on");
                body.put("IpType", config.getType() == 1 ? "block" : "allow");

                if (config.getIps() != null && !config.getIps().isEmpty()) {
                    body.put("IpList", String.join(",", config.getIps()));
                } else {
                    body.put("IpList", "");
                }
            }

            kingsoftApiService.postKingsoftApi(
                    "SetIpProtectionConfig",
                    "2016-09-01",
                    "/2016-09-01/domain/SetIpProtectionConfig",
                    body
            );
            log.info("金山云CDN更新IP黑白名单成功，域名：{}", cdnDomain.getDomainName());

        } catch (Exception e) {
            log.error("金山云CDN更新IP黑白名单失败，域名：{}", cdnDomain.getDomainName(), e);
            throw handleKingsoftException(e);
        }
    }
    @Override
    public void saveHotlinkPrevention(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        String domainId = getAndCheckDomainId(cdnDomain);
        RefererDTO refererConfig = config.getReferer();
        log.info("金山云CDN更新Referer防盗链，域名：{}，配置：{}", cdnDomain.getDomainName(), JSON.toJSONString(refererConfig));

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("DomainId", domainId);

            if (refererConfig == null || refererConfig.getReferer_type() == null || refererConfig.getReferer_type() == 0) {
                body.put("Enable", "off");
            } else {
                body.put("Enable", "on");
                body.put("ReferType", refererConfig.getReferer_type() == 1 ? "block" : "allow");

                if (refererConfig.getReferers() != null && !refererConfig.getReferers().isEmpty()) {
                    body.put("ReferList", String.join(",", refererConfig.getReferers()));
                } else {
                    body.put("ReferList", "");
                }

                body.put("AllowEmpty", Boolean.TRUE.equals(refererConfig.getInclude_empty()) ? "on" : "off");
            }

            kingsoftApiService.postKingsoftApi(
                    "SetReferProtectionConfig",
                    "2016-09-01",
                    "/2016-09-01/domain/SetReferProtectionConfig",
                    body
            );
            log.info("金山云CDN更新Referer防盗链成功，域名：{}", cdnDomain.getDomainName());

        } catch (Exception e) {
            log.error("金山云CDN更新Referer防盗链失败，域名：{}", cdnDomain.getDomainName(), e);
            throw handleKingsoftException(e);
        }
    }

    @Override
    public void saveUserAgentFilter(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        log.warn("金山云CDN平台不支持直接配置User-Agent黑白名单，此功能通过更复杂的'访问控制规则引擎'实现。当前版本暂不支持。域名：{}", cdnDomain.getDomainName());
        throw new BusinessException("功能暂未支持：金山云平台不支持直接配置User-Agent黑白名单");
    }

    @Override
    public void saveUrlAuth(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        String domainId = getAndCheckDomainId(cdnDomain);
        UrlAuthDTO urlAuthConfig = config.getUrlAuth();
        log.info("金山云CDN更新URL鉴权配置，域名：{}，配置：{}", cdnDomain.getDomainName(), JSON.toJSONString(urlAuthConfig));

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("DomainId", domainId);
            body.put("Enable", urlAuthConfig.getStatus());

            if ("on".equalsIgnoreCase(urlAuthConfig.getStatus())) {
                if (Assert.isEmpty(urlAuthConfig.getType()) || Assert.isEmpty(urlAuthConfig.getPrimary_key()) || urlAuthConfig.getExpire_time() == null) {
                    throw new BusinessException("启用URL鉴权时，鉴权类型、主密钥和过期时间为必填项。");
                }

                // 确保AuthType值是正确的格式（typeA、typeB、typeC）
                String authType = urlAuthConfig.getType();
                if (authType != null) {
                    // 处理可能的下划线格式转换
                    if (authType.equals("type_a")) {
                        authType = "typeA";
                    } else if (authType.equals("type_b")) {
                        authType = "typeB";
                    } else if (authType.equals("type_c")) {
                        authType = "typeC";
                    }
                    // 确保首字母大写
                    if (authType.equalsIgnoreCase("typea")) {
                        authType = "typeA";
                    } else if (authType.equalsIgnoreCase("typeb")) {
                        authType = "typeB";
                    } else if (authType.equalsIgnoreCase("typec")) {
                        authType = "typeC";
                    }
                } else {
                    authType = "typeA"; // 默认值
                }

                body.put("AuthType", authType);
                body.put("Key1", urlAuthConfig.getPrimary_key());
                body.put("ExpirationTime", urlAuthConfig.getExpire_time());

                if (Assert.notEmpty(urlAuthConfig.getSecondary_key())) {
                    body.put("Key2", urlAuthConfig.getSecondary_key());
                }
            }

            // 打印请求体，用于调试
            log.info("金山云CDN URL鉴权请求体：{}", JSON.toJSONString(body));

            kingsoftApiService.postKingsoftApi(
                    "SetRequestAuthConfig",
                    "2016-09-01",
                    "/2016-09-01/domain/SetRequestAuthConfig",
                    body
            );
            log.info("金山云CDN更新URL鉴权配置成功，域名：{}", cdnDomain.getDomainName());

        } catch (Exception e) {
            log.error("金山云CDN更新URL鉴权配置失败，域名：{}", cdnDomain.getDomainName(), e);
            throw handleKingsoftException(e);
        }
    }

    /**
     * 映射URL鉴权类型
     * 将前端的typeA、typeB映射到金山云API需要的实际值
     *
     * @param frontendType 前端类型值
     * @return 金山云API需要的类型值
     */
    private String mapUrlAuthType(String frontendType) {
        if (frontendType == null) {
            return "typeA"; // 默认使用typeA
        }

        // 直接返回原始值，保持大小写，不做转换
        // 金山云API要求typeA、typeB、typeC（大写A、B、C）
        return frontendType;
    }

    @Override
    public void saveHttpHeader(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        String domainId = getAndCheckDomainId(cdnDomain);
        log.info("金山云CDN更新HTTP Header，域名：{}，配置：{}", cdnDomain.getDomainName(), JSON.toJSONString(config.getHttpResponseHeaders()));

        if (config.getHttpResponseHeaders() == null || config.getHttpResponseHeaders().isEmpty()) {
            log.info("没有需要更新的HTTP Header规则，域名：{}", cdnDomain.getDomainName());
            return;
        }

        try {
            for (HttpResponseHeaderDTO headerDto : config.getHttpResponseHeaders()) {
                if (Assert.isEmpty(headerDto.getName())) {
                    log.warn("跳过一个无效的Header规则（缺少名称），域名：{}", cdnDomain.getDomainName());
                    continue;
                }

                Map<String, Object> body = new HashMap<>();
                body.put("DomainId", domainId);
                body.put("HeaderKey", headerDto.getName());

                if ("delete".equalsIgnoreCase(headerDto.getAction())) {
                    log.debug("准备删除HTTP Header: {}，域名：{}", headerDto.getName(), cdnDomain.getDomainName());
                    kingsoftApiService.postKingsoftApi(
                            "DeleteHttpHeadersConfig",
                            "2016-09-01",
                            "/2016-09-01/domain/DeleteHttpHeadersConfig",
                            body
                    );
                    log.info("成功删除HTTP Header: {}，域名：{}", headerDto.getName(), cdnDomain.getDomainName());

                } else {
                    if (Assert.isEmpty(headerDto.getValue())) {
                        log.warn("跳过一个无效的Header规则（缺少值），Header: {}，域名：{}", headerDto.getName(), cdnDomain.getDomainName());
                        continue;
                    }
                    body.put("HeaderValue", headerDto.getValue());

                    log.debug("准备设置HTTP Header: {} -> {}，域名：{}", headerDto.getName(), headerDto.getValue(), cdnDomain.getDomainName());
                    kingsoftApiService.postKingsoftApi(
                            "SetHttpHeadersConfig",
                            "2016-09-01",
                            "/2016-09-01/domain/SetHttpHeadersConfig",
                            body
                    );
                    log.info("成功设置HTTP Header: {} -> {}，域名：{}", headerDto.getName(), headerDto.getValue(), cdnDomain.getDomainName());
                }
            }
            log.info("金山云CDN批量更新HTTP Header成功，域名：{}", cdnDomain.getDomainName());

        } catch (Exception e) {
            log.error("金山云CDN更新HTTP Header失败，域名：{}", cdnDomain.getDomainName(), e);
            throw handleKingsoftException(e);
        }
    }

    @Override
    public void saveCompress(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        String domainId = getAndCheckDomainId(cdnDomain);
        CompressDTO compressConfig = config.getCompress();
        log.info("金山云CDN更新智能压缩，域名：{}，配置：{}", cdnDomain.getDomainName(), JSON.toJSONString(compressConfig));

        try {
            boolean gzipEnabled = false;
            boolean brEnabled = false;

            if (compressConfig != null && "on".equalsIgnoreCase(compressConfig.getStatus())) {
                String types = compressConfig.getType();
                if (types != null) {
                    gzipEnabled = types.contains("gzip");
                    brEnabled = types.contains("br");
                }
            }

            Map<String, Object> gzipBody = new HashMap<>();
            gzipBody.put("DomainId", domainId);
            gzipBody.put("Enable", gzipEnabled ? "on" : "off");

            log.debug("准备设置Gzip压缩为: {}，域名：{}", gzipBody.get("Enable"), cdnDomain.getDomainName());
            kingsoftApiService.postKingsoftApi(
                    "SetPageCompressConfig",
                    "2016-09-01",
                    "/2016-09-01/domain/SetPageCompressConfig",
                    gzipBody
            );
            log.info("成功设置Gzip压缩为: {}，域名：{}", gzipBody.get("Enable"), cdnDomain.getDomainName());


            Map<String, Object> brBody = new HashMap<>();
            brBody.put("DomainId", domainId);
            brBody.put("Enable", brEnabled ? "on" : "off");

            log.debug("准备设置Br压缩为: {}，域名：{}", brBody.get("Enable"), cdnDomain.getDomainName());
            kingsoftApiService.postKingsoftApi(
                    "SetBrCompressConfig",
                    "2021-12-01",
                    "/2021-12-01/domain/SetBrCompressConfig",
                    brBody
            );
            log.info("成功设置Br压缩为: {}，域名：{}", brBody.get("Enable"), cdnDomain.getDomainName());

        } catch (Exception e) {
            log.error("金山云CDN更新智能压缩失败，域名：{}", cdnDomain.getDomainName(), e);
            throw handleKingsoftException(e);
        }
    }

    @Override
    public void saveOriginProtocol(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        String domainId = getAndCheckDomainId(cdnDomain);
        String originProtocol = domainOriginSettingVo.getOriginProtocol();
        log.info("金山云CDN更新回源协议，域名：{}，协议：{}", cdnDomain.getDomainName(), originProtocol);

        if (Assert.isEmpty(originProtocol)) {
            throw new BusinessException("回源协议不能为空");
        }

        try {
            Map<String, String> params = new HashMap<>();
            params.put("DomainId", domainId);
            params.put("OriginProtocol", originProtocol);

            kingsoftApiService.callKingsoftApi("ModifyCdnDomainBasicInfo", "2016-09-01", params);
            log.info("金山云CDN更新回源协议成功，域名：{}", cdnDomain.getDomainName());

        } catch (Exception e) {
            log.error("金山云CDN更新回源协议失败，域名：{}", cdnDomain.getDomainName(), e);
            throw handleKingsoftException(e);
        }
    }

    @Override
    public void saveOriginHost(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        String domainId = getAndCheckDomainId(cdnDomain);

        log.error("saveOriginHost 方法的参数 DomainOriginSettingVo 不包含回源Host信息，这是一个设计缺陷。");
        throw new BusinessException("接口参数不匹配：无法从当前VO中获取回源Host");

    }

    @Override
    public void saveRangeSwitch(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        String domainId = getAndCheckDomainId(cdnDomain);

        String rangeStatus = domainOriginSettingVo.getStatus();

        log.info("金山云CDN更新Range回源配置，域名：{}，状态：{}", cdnDomain.getDomainName(), rangeStatus);

        if (Assert.isEmpty(rangeStatus)) {
            throw new BusinessException("Range回源状态不能为空");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("DomainId", domainId);
            body.put("Enable", rangeStatus);

            kingsoftApiService.postKingsoftApi(
                    "SetVideoSeekConfig",
                    "2016-09-01",
                    "/2016-09-01/domain/SetVideoSeekConfig",
                    body
            );
            log.info("金山云CDN更新Range回源配置成功，域名：{}", cdnDomain.getDomainName());

        } catch (Exception e) {
            log.error("金山云CDN更新Range回源配置失败，域名：{}", cdnDomain.getDomainName(), e);
            throw handleKingsoftException(e);
        }
    }

    @Override
    public void saveCacheFollowOriginStatusSwitch(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        log.warn("金山云CDN的'遵循源站'功能需在每条具体的缓存规则中设置，没有全局开关。当前版本暂不支持此操作。域名：{}", cdnDomain.getDomainName());
        throw new BusinessException("功能实现不兼容：金山云的'遵循源站'功能需在每条缓存规则中单独设置");
    }

    @Override
    public void saveErrorCodeCache(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        String domainId = getAndCheckDomainId(cdnDomain);
        log.info("金山云CDN状态码缓存配置，域名：{}，配置: {}", cdnDomain.getDomainName(), JSON.toJSONString(config.getErrorCodeCache()));

        if (config.getErrorCodeCache() == null || config.getErrorCodeCache().isEmpty()) {
            log.info("没有需要更新的状态码缓存规则，跳过。域名: {}", cdnDomain.getDomainName());
            return;
        }

        try {
            for (ErrorCodeCacheDTO rule : config.getErrorCodeCache()) {
                Map<String, Object> body = new HashMap<>();
                body.put("DomainId", domainId);
                body.put("ErrorCode", rule.getCode());
                body.put("Ttl", rule.getTtl());

                body.put("RedirectUrl", "");

                kingsoftApiService.postKingsoftApi(
                        "SetErrorPageConfig",
                        "2016-09-01",
                        "/2016-09-01/domain/SetErrorPageConfig",
                        body
                );
                log.info("成功设置状态码 {} 的缓存时间为 {}秒，域名: {}", rule.getCode(), rule.getTtl(), cdnDomain.getDomainName());
            }
            log.info("金山云CDN批量更新状态码缓存成功，域名：{}", cdnDomain.getDomainName());
        } catch (Exception e) {
            log.error("金山云CDN更新状态码缓存失败，接口返回错误。域名：{}，错误：{}", cdnDomain.getDomainName(), e.getMessage());
            throw handleKingsoftException(e);
        }
    }
//    @Override
//    public void httpsConfigurationOther(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
//        String domainId = getAndCheckDomainId(cdnDomain);
//        HttpPutBodyDTO httpsConfig = config.getHttps();
//        log.info("金山云CDN更新HTTPS高级配置，域名：{}", cdnDomain.getDomainName());
//
//        try {
//            // 处理HTTP/2配置
//            if (httpsConfig != null && httpsConfig.getHttp2_status() != null) {
//                Map<String, Object> body = new HashMap<>();
//                body.put("DomainId", domainId);
//                body.put("Enable", httpsConfig.getHttp2_status());
//
//                kingsoftApiService.postKingsoftApi(
//                        "SetHttp2OptionConfig",
//                        "2016-09-01",
//                        "/2016-09-01/domain/SetHttp2OptionConfig",
//                        body
//                );
//                log.info("金山云CDN更新HTTP/2配置成功，状态为：{}，域名：{}", httpsConfig.getHttp2_status(), cdnDomain.getDomainName());
//            }
//
//            // 处理TLS版本配置
//            if (httpsConfig != null && httpsConfig.getTls_version() != null) {
//                String[] tlsVersions = httpsConfig.getTls_version().split(",");
//                kingsoftCertificateService.setTlsVersions(domainId, tlsVersions);
//                log.info("金山云CDN更新TLS版本配置成功，版本为：{}，域名：{}", httpsConfig.getTls_version(), cdnDomain.getDomainName());
//            }
//
//            // 处理OCSP Stapling配置
//            if (httpsConfig != null && httpsConfig.getOcsp_status() != null) {
//                kingsoftCertificateService.setOcspStapling(domainId, httpsConfig.getOcsp_status());
//                log.info("金山云CDN更新OCSP Stapling配置成功，状态为：{}，域名：{}", httpsConfig.getOcsp_status(), cdnDomain.getDomainName());
//            }
//
//        } catch (Exception e) {
//            log.error("金山云CDN更新HTTPS高级配置失败，域名：{}", cdnDomain.getDomainName(), e);
//            throw handleKingsoftException(e);
//        }
//    }

    @Override
    public void forcedToJump(CdnDomain cdnDomain, DomainHttpsSettingVo config, String redirectCode) throws BusinessException {
        String domainId = getAndCheckDomainId(cdnDomain);
        ForceRedirectConfigDTO redirectConfig = config.getForceRedirect();
        log.info("金山云CDN更新强制跳转，域名：{}，配置：{}", cdnDomain.getDomainName(), JSON.toJSONString(redirectConfig));

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("DomainId", domainId);

            // 根据status确定RedirectType
            String status = (redirectConfig != null && "on".equalsIgnoreCase(redirectConfig.getStatus())) ? "https" : "off";
            body.put("RedirectType", status);

            // 只有在启用强制跳转且有重定向码的情况下才设置RedirectCode
            if ("https".equals(status) && redirectCode != null && !redirectCode.isEmpty()) {
                body.put("RedirectCode", redirectCode);
            }

            kingsoftApiService.postKingsoftApi(
                    "SetForceRedirectConfig",
                    "2016-09-01",
                    "/2016-09-01/domain/SetForceRedirectConfig",
                    body
            );
            log.info("金山云CDN更新强制跳转成功，域名：{}", cdnDomain.getDomainName());

        } catch (Exception e) {
            log.error("金山云CDN更新强制跳转失败，域名：{}", cdnDomain.getDomainName(), e);
            throw handleKingsoftException(e);
        }
    }
    @Override
    public void change(CdnDomain cdnDomain) throws BusinessException {
        log.warn("金山云CDN的主备源站切换是基于健康检查自动进行的，不支持通过API进行手动切换。域名：{}", cdnDomain.getDomainName());
        throw new BusinessException("功能不适用：金山云平台主备切换为自动模式，不支持手动触发。");
    }



    @Override public void save(CdnDomain cdnDomain, String businessType, String serviceArea) {}
    @Override public void ipv6(CdnDomain cdnDomain, Integer status) {}
    @Override
    public void saveRangeTimeOut(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        String domainId = getAndCheckDomainId(cdnDomain);
        Integer timeoutSeconds = domainOriginSettingVo.getOriginReceiveTimeOut();
        log.info("金山云CDN更新回源超时时间，域名：{}，超时时间：{}秒", cdnDomain.getDomainName(), timeoutSeconds);

        if (timeoutSeconds == null || timeoutSeconds <= 0) {
            throw new BusinessException("回源超时时间必须大于0秒");
        }

        if (timeoutSeconds > 300) {
            throw new BusinessException("回源超时时间不能超过300秒");
        }

        try {
            // 获取当前的基本域名信息
            Map<String, String> basicInfoParams = new HashMap<>();
            basicInfoParams.put("DomainId", domainId);
            JSONObject basicInfoJson = kingsoftApiService.callKingsoftApi("GetCdnDomainBasicInfo", "2016-09-01", basicInfoParams);
            GetCdnDomainBasicInfoResult detailInfo = JSON.toJavaObject(basicInfoJson, GetCdnDomainBasicInfoResult.class);

            if (detailInfo == null) {
                throw new BusinessException("无法获取域名基本信息");
            }

            // 金山云的高级回源配置中包含超时时间设置
            // 需要保持其他高级回源配置不变，只更新超时时间
            Map<String, String> currentParams = new HashMap<>();
            currentParams.put("DomainId", domainId);
            JSONObject currentConfigJson = kingsoftApiService.callKingsoftApi("GetDomainConfigs", "2016-09-01", currentParams);
            GetDomainConfigsResult currentConfigs = JSON.toJavaObject(currentConfigJson, GetDomainConfigsResult.class);

            if (currentConfigs == null || currentConfigs.getOriginAdvancedConfig() == null) {
                throw new BusinessException("无法获取当前高级回源配置，请先配置回源策略");
            }

            GetDomainConfigsResult.OriginAdvancedConfig currentAdvanced = currentConfigs.getOriginAdvancedConfig();

            // 构建更新请求，保持原有配置不变
            Map<String, Object> body = new HashMap<>();
            body.put("DomainId", domainId);

            // 为了设置超时时间，必须启用高级回源配置
            body.put("Enable", "on");

            // 保持原有配置或设置默认值
            body.put("OriginType", currentAdvanced.getOriginType() != null ? currentAdvanced.getOriginType() : "ipaddr");
            body.put("Origin", currentAdvanced.getOrigin() != null ? currentAdvanced.getOrigin() : detailInfo.getOrigin());
            body.put("OriginPolicy", currentAdvanced.getOriginPolicy() != null ? currentAdvanced.getOriginPolicy() : "rr");

            if (currentAdvanced.getOriginPolicyBestCount() != null) {
                body.put("OriginPolicyBestCount", currentAdvanced.getOriginPolicyBestCount());
            }
            if (currentAdvanced.getBackupOriginType() != null) {
                body.put("BackupOriginType", currentAdvanced.getBackupOriginType());
            }
            if (currentAdvanced.getBackupOrigin() != null) {
                body.put("BackupOrigin", currentAdvanced.getBackupOrigin());
            }

            // 更新超时时间（金山云API需要毫秒单位）
            body.put("OriginReadTimeout", timeoutSeconds * 1000);
            body.put("OriginConnectTimeout", 5000); // 保持连接超时为5秒

            kingsoftApiService.postKingsoftApi(
                    "SetOriginAdvancedConfig", "2016-09-01",
                    "/2016-09-01/domain/SetOriginAdvancedConfig", body
            );
            log.info("金山云CDN更新回源超时时间成功，域名：{}，设置为{}秒", cdnDomain.getDomainName(), timeoutSeconds);

        } catch (Exception e) {
            log.error("金山云CDN更新回源超时时间失败，域名：{}", cdnDomain.getDomainName(), e);
            throw handleKingsoftException(e);
        }
    }
    @Override public void saveOriginRequestHeader(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) {}



    @Override
    public void saveCustomErrorPageConfiguration(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        String domainId = getAndCheckDomainId(cdnDomain);
        log.info("金山云CDN设置自定义错误页面，域名：{}，配置：{}", cdnDomain.getDomainName(), JSON.toJSONString(config));

        try {
            List<Map<String, String>> errorPages = new ArrayList<>();

            // 从config中获取错误页面配置
            if (config != null && config.getErrorPages() != null) {
                for (SettingHigherVo.ErrorPage errorPage : config.getErrorPages()) {
                    if (Assert.notEmpty(errorPage.getErrorHttpCode()) && Assert.notEmpty(errorPage.getCustomPageUrl())) {
                        // 验证状态码是否支持
                        if (!isValidErrorCode(errorPage.getErrorHttpCode())) {
                            throw new BusinessException("不支持的错误状态码: " + errorPage.getErrorHttpCode() +
                                    "，支持的状态码：400,403,404,405,406,414,416,500,501,502,503,504");
                        }

                        // 验证URL格式
                        if (!isValidUrl(errorPage.getCustomPageUrl())) {
                            throw new BusinessException("自定义页面URL格式不正确，必须以 https:// 或 http:// 开头");
                        }

                        Map<String, String> errorPageMap = new HashMap<>();
                        errorPageMap.put("ErrorHttpCode", errorPage.getErrorHttpCode());
                        errorPageMap.put("CustomPageUrl", errorPage.getCustomPageUrl());
                        errorPages.add(errorPageMap);
                    }
                }
            }

            Map<String, Object> body = new HashMap<>();
            body.put("DomainId", domainId);
            body.put("ErrorPages", errorPages);

            kingsoftApiService.postKingsoftApi(
                    "SetErrorPageConfig",
                    "2016-09-01",
                    "/2016-09-01/domain/SetErrorPageConfig",
                    body
            );
            log.info("金山云CDN设置自定义错误页面成功，域名：{}", cdnDomain.getDomainName());

        } catch (Exception e) {
            log.error("金山云CDN设置自定义错误页面失败，域名：{}", cdnDomain.getDomainName(), e);
            throw handleKingsoftException(e);
        }
    }

    /**
     * 验证错误状态码是否支持
     */
    private boolean isValidErrorCode(String code) {
        Set<String> validCodes = new HashSet<>(Arrays.asList(
                "400", "403", "404", "405", "406", "414", "416",
                "500", "501", "502", "503", "504"
        ));
        return validCodes.contains(code);
    }

    /**
     * 验证URL格式是否正确
     */
    private boolean isValidUrl(String url) {
        if (Assert.isEmpty(url)) {
            return false;
        }
        return url.startsWith("https://") || url.startsWith("http://");
    }
    @Override public void saveOriginRequestUrlRewrite(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) {}
    @Override public void saveAdvancedReturnSource(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) {}
    @Override public void saveRangeVerifyETag(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) {}

    /**
     * 将金山云API的鉴权类型映射回前端类型
     *
     * @param apiType API返回的类型值
     * @return 前端使用的类型值
     */
    private String mapApiUrlAuthType(String apiType) {
        if (apiType == null) {
            return "typeA"; // 默认使用typeA
        }

        switch (apiType.toLowerCase()) {
            case "typea":
                return "typeA"; // 时间戳+共享密钥防盗链
            case "typeb":
                return "typeB"; // 带参数的鉴权
            case "typec":
                return "typeC"; // 其他鉴权类型
            default:
                return "typeA"; // 默认使用typeA
        }
    }

//    /**
//     * 更新回源端口配置
//     *
//     * @param cdnDomain 加速域名
//     * @param httpPort HTTP端口
//     * @param httpsPort HTTPS端口
//     * @throws BusinessException 业务异常
//     */
//    public void updateOriginPorts(CdnDomain cdnDomain, Integer httpPort, Integer httpsPort) throws BusinessException {
//        String domainId = getAndCheckDomainId(cdnDomain);
//        log.info("金山云CDN更新回源端口配置，域名：{}，HTTP端口：{}，HTTPS端口：{}", cdnDomain.getDomainName(), httpPort, httpsPort);
//
//        try {
//            // 首先获取当前的基本信息
//            Map<String, String> basicInfoParams = new HashMap<>();
//            basicInfoParams.put("DomainId", domainId);
//            JSONObject basicInfoJson = kingsoftApiService.callKingsoftApi("GetCdnDomainBasicInfo", "2016-09-01", basicInfoParams);
//            GetCdnDomainBasicInfoResult detailInfo = JSON.toJavaObject(basicInfoJson, GetCdnDomainBasicInfoResult.class);
//
//            if (detailInfo == null) {
//                throw new BusinessException("无法获取域名基本信息");
//            }
//
//            // 构建请求参数
//            Map<String, String> params = new HashMap<>();
//            params.put("DomainId", domainId);
//
//            // 保留原有配置
//            params.put("Origin", detailInfo.getOrigin());
//            params.put("OriginType", detailInfo.getOriginType());
//            params.put("BackOriginHost", detailInfo.getBackOriginHost());
//
//            // 更新端口配置
//            String portConfig = "";
//            if (httpPort != null && httpsPort != null) {
//                portConfig = httpPort + "," + httpsPort;
//            } else if (httpPort != null) {
//                portConfig = String.valueOf(httpPort);
//            } else if (httpsPort != null) {
//                portConfig = "," + httpsPort;
//            }
//
//            if (!portConfig.isEmpty()) {
//                params.put("OriginPort", portConfig);
//                log.info("设置回源端口：{}", portConfig);
//            }
//
//            // 调用API更新配置
//            kingsoftApiService.callKingsoftApi("ModifyCdnDomainBasicInfo", "2016-09-01", params);
//            log.info("金山云CDN更新回源端口成功，域名：{}", cdnDomain.getDomainName());
//
//        } catch (Exception e) {
//            log.error("金山云CDN更新回源端口失败，域名：{}", cdnDomain.getDomainName(), e);
//            throw handleKingsoftException(e);
//        }
//    }

    /**
     * 默认开启域名日志功能
     *
     * @param domainId 域名ID
     * @param domainName 域名名称
     * @throws BusinessException 业务异常
     */
    private void enableLogging(String domainId, String domainName) throws BusinessException {
        log.info("为域名 {} 开启日志功能，域名ID：{}", domainName, domainId);

        int maxRetries = 3;
        int retryDelay = 2000; // 2秒

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // 验证域名是否存在
                if (!isDomainExists(domainId)) {
                    if (attempt < maxRetries) {
                        log.info("域名 {} 尚未同步到金山云系统，等待 {} 毫秒后重试 (尝试 {}/{})",
                                domainName, retryDelay, attempt, maxRetries);
                        Thread.sleep(retryDelay);
                        continue;
                    } else {
                        throw new BusinessException("域名信息同步超时，无法开启日志功能");
                    }
                }

                Map<String, Object> logConfig = new HashMap<>();
                logConfig.put("DomainIds", domainId);
                logConfig.put("ActionType", "start"); // 操作类型：启用
                logConfig.put("Granularity", "1440"); // 日志存储粒度：按天粒度存储

                // 调用金山云API开启日志
                kingsoftApiService.postKingsoftApi(
                        "SetDomainLogService",
                        "2016-09-01",
                        "/2016-09-01/log/SetDomainLogService",
                        logConfig
                );

                log.info("域名 {} 日志功能开启成功 (尝试 {}/{})", domainName, attempt, maxRetries);
                return; // 成功则退出

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException("开启日志功能被中断");
            } catch (Exception e) {
                if (attempt < maxRetries) {
                    log.warn("域名 {} 开启日志功能失败 (尝试 {}/{}): {}，等待 {} 毫秒后重试",
                            domainName, attempt, maxRetries, e.getMessage(), retryDelay);
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BusinessException("开启日志功能被中断");
                    }
                } else {
                    log.error("域名 {} 开启日志功能最终失败 (尝试 {}/{})", domainName, attempt, maxRetries, e);
                    throw new BusinessException("开启日志功能失败: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 检查域名是否存在于金山云系统中
     *
     * @param domainId 域名ID
     * @return 是否存在
     */
    private boolean isDomainExists(String domainId) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("DomainId", domainId);

            // 尝试获取域名基本信息，如果能获取到说明域名已存在
            JSONObject result = kingsoftApiService.callKingsoftApi("GetCdnDomainBasicInfo", "2016-09-01", params);
            return result != null && !result.containsKey("Error");

        } catch (Exception e) {
            log.info("检查域名 {} 是否存在时发生异常: {}", domainId, e.getMessage());
            return false;
        }
    }
}
