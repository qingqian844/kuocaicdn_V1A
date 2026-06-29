package com.kuocai.cdn.service.domain.operation;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.DomainBasicInfo;
import com.kuocai.cdn.api.DomainConfig;
import com.kuocai.cdn.api.huawei.cdn.dto.CacheRuleDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.ForceRedirectConfigDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.HttpPutBodyDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.RefererDTO;
import com.kuocai.cdn.api.tencent.dns.CreateRecordResponse;
import com.kuocai.cdn.api.tencent.dns.TencentApi;
import com.kuocai.cdn.api.tencent.dns.dto.CreateRecordDTO;
import com.kuocai.cdn.api.tencent.dns.properties.TencentDns;
import com.kuocai.cdn.api.yifan.cdn.YiFanOperationApi;
import com.kuocai.cdn.api.yifan.cdn.YiFanRequest;
import com.kuocai.cdn.api.yifan.cdn.dto.*;
import com.kuocai.cdn.api.yifan.cdn.enums.OriginProtocol;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.CdnDomainSources;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.enumeration.domainmerage.domain.DomainStatus;
import com.kuocai.cdn.enumeration.domainmerage.route.CdnOperationRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.CdnDomainService;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.*;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

@Service
@Slf4j
public class YiFanDomainServiceImpl extends BaseService<CdnDomain> implements ICdnPlatformService {


    @Resource
    private CdnDomainService domainService;

    /**
     * 创建域名信息
     *
     * @param userId
     * @param domainName
     * @param businessType
     * @param serviceArea
     * @param originType
     * @param ipOrDomain
     * @return
     * @throws BusinessException
     * @throws InterruptedException
     */
    @Override
    public CdnDomain create(Long userId, String domainName, String businessType, String serviceArea, String originType, String ipOrDomain) throws BusinessException, InterruptedException {
        // 封装源站信息
        JSONObject domain = null;
        try {
            domain = YiFanOperationApi.createDomain(domainName, businessType, serviceArea, originType, ipOrDomain);
        } catch (Exception e) {
            log.error("易凡创建加速域名失败，用户：{}，域名：{}，业务类型：{}，服务范围：{}，源站类别：{}，源站：{}",
                    userId, domainName, businessType, serviceArea, originType, ipOrDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        //保存域名信息
        if (Assert.isEmpty(domain)) {
            throw new BusinessException("创建加速域名失败");
        }
        String domainIdResult = domain.getString("id");
        String domainNameResult = domain.getString("domainName");
        String businessTypeResult = domain.getString("businessType");
        String serviceAreaResult = domain.getString("serviceArea");
        String state = domain.getString("state");
        String cname = domain.getString("cname");
        CdnDomain cdnDomain = CdnDomain.builder()
                .domainId(domainIdResult)
                .domainName(domainNameResult)
                .businessType(businessTypeResult)
                .serviceArea(serviceAreaResult)
                .domainStatus(DomainStatus.getSelfParam(state, CdnOperationRoute.YIFAN))
                .cnameYifan(cname)
                .userId(userId)
                .route(CdnRoute.YIFAN.getCode())
                .build();
        return save(cdnDomain);
    }

    /**
     * {
     * "id": "64a422ead3eec25c6c590b95",
     * "domainName": "yifan2.kedaya.site",
     * "cdnLineId": "6406b6717a19386f9babbd86",
     * "cdnLineName": null,
     * "businessType": "web",
     * "serviceArea": "mainland_china",
     * "state": "CONFIGURING",
     * "cname": "yifan2.kedaya.site.cdn.yifancdn.com",
     * "createDate": "2023-07-04 21:47:22",
     * "modifyDate": "2023-07-04 21:47:27",
     * "disable": false,
     * "memberId": "641585097a19386f9bac00ab",
     * "memberName": null,
     * "domainConfig": {
     * "sources": [
     * {
     * "ipOrDomain": "124.220.182.8",
     * "originType": "ipaddr",
     * "httpPort": 80,
     * "httpsPort": 443,
     * "hostName": "yifan2.kedaya.site"
     * }
     * ],
     * "originRequestHeader": [],
     * "httpResponseHeader": [],
     * "urlAuth": {
     * "type": null,
     * "status": "off",
     * "key": null,
     * "expireTime": null
     * },
     * "compress": {
     * "status": "off"
     * },
     * "forceRedirect": {
     * "status": "off",
     * "type": null
     * },
     * "originProtocol": "http",
     * "referer": {
     * "refererType": 0,
     * "refererList": "",
     * "includeEmpty": false
     * },
     * "ipFilter": {
     * "type": 0,
     * "list": []
     * },
     * "cacheConfig": {
     * "ignoreUrlParameter": false,
     * "followOrigin": false,
     * "compress": 0,
     * "rules": [
     * {
     * "ruleType": 0,
     * "content": "",
     * "ttl": 2592000,
     * "ttlType": 1,
     * "priority": 1
     * },
     * {
     * "ruleType": 1,
     * "content": ".php;.jsp;.asp;.aspx",
     * "ttl": 0,
     * "ttlType": 1,
     * "priority": 2
     * }
     * ]
     * },
     * "https": {
     * "expirationTime": null,
     * "httpsStatus": "off",
     * "certName": null,
     * "certificate": null,
     * "privateKey": null
     * },
     * "rangeStatus": "off"
     * },
     * "preCreateChecks": [
     * {
     * "accountId": "63f22bc76768b8670213fbfb",
     * "type": "HUAWEI",
     * "skipCheck": true,
     * "domainUserId": "2c3434779804483f80fa39decb091748"
     * },
     * {
     * "accountId": "6406b6197a19386f9babbd83",
     * "type": "ALIYUN",
     * "skipCheck": false,
     * "domainUserId": "5799070378167119"
     * }
     * ]
     * }
     * <p>
     * /**
     * 配置域名CNAME的DNS解析
     *
     * @param cdnDomain
     * @return
     * @throws TencentCloudSDKException
     * @throws BusinessException
     */
    @Override
    public CdnDomain configDNS(CdnDomain cdnDomain) throws TencentCloudSDKException, BusinessException {
        String domainName = cdnDomain.getDomainName();
        String cnameYifan = cdnDomain.getCnameYifan();
        CreateRecordDTO createRecordRequest = new CreateRecordDTO();
        createRecordRequest.setDomain(TencentDns.LOCAL_DOMAIN_NAME).setSubDomain(domainName + "." + RandomUtil.randomString(8)).setValue(cnameYifan);
        CreateRecordResponse recordResponse = TencentApi.createRecord(createRecordRequest);
        // 如果返回有记录id，说明返回成功
        if (!Assert.isEmpty(recordResponse.getRecordId())) {
            // 自己定义括彩云的
            cdnDomain.setCname(createRecordRequest.getSubDomain() + "." + TencentDns.LOCAL_DOMAIN_NAME);
            cdnDomain.setTencentDnsId(recordResponse.getRecordId());
            cdnDomain = save(cdnDomain);
            return cdnDomain;
        }
        // 如果返回没有记录id,说明失败 TODO**这里如果失败了，dns解析不到
        // 如果返回没有记录id,说明失败 TODO**这里如果失败了，dns解析不到
        if (Assert.isEmpty(recordResponse.getRecordId())) {
            log.error("易凡dns解析失败，域名：{}", cdnDomain.getDomainName());
            throw new BusinessException("dns解析失败");
        }
        return null;
    }

    /**
     * 修改域名的一些基础信息
     *
     * @param cdnDomain
     * @param businessType
     * @param serviceArea
     * @throws BusinessException
     */
    @Override
    public void save(CdnDomain cdnDomain, String businessType, String serviceArea) throws BusinessException {
        // TODO 不支持修改域名加速类型，以及服务范围
    }

    /**
     * 停用域名
     *
     * @param cdnDomain
     * @throws BusinessException
     */
    @Override
    public void disable(CdnDomain cdnDomain) throws BusinessException {
        try {
            YiFanOperationApi.disable(Arrays.asList(cdnDomain.getDomainId()));
        } catch (Exception e) {
            log.error("易凡停用域名失败，用户：{}，域名：{}", cdnDomain.getUserId(), cdnDomain.getDomainId());
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 启用域名
     *
     * @param cdnDomain
     * @throws BusinessException
     */
    @Override
    public void enable(CdnDomain cdnDomain) throws BusinessException {
        try {
            YiFanOperationApi.enable(Arrays.asList(cdnDomain.getDomainId()));
        } catch (Exception e) {
            log.error("易凡启用域名失败，用户：{}，域名：{}", cdnDomain.getUserId(), cdnDomain.getDomainId());
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 删除域名信息
     *
     * @param cdnDomain
     * @throws BusinessException
     */
    @Override
    public void delete(CdnDomain cdnDomain) throws BusinessException {
        try {
            YiFanOperationApi.delete(Arrays.asList(cdnDomain.getDomainId()));
        } catch (Exception e) {
            log.error("易凡删除域名失败，用户：{}，域名：{}", cdnDomain.getUserId(), cdnDomain.getDomainId());
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 打开域名的IPV6
     *
     * @param cdnDomain
     * @param status
     * @throws BusinessException
     */
    @Override
    public void ipv6(CdnDomain cdnDomain, Integer status) throws BusinessException {
        // TODO 不支持IPV6修改
    }

    /**
     * 保存域名的源站信息
     *
     * @param cdnDomain
     * @param config
     * @throws BusinessException
     */
    @Override
    public void saveSourceStationConfig(CdnDomain cdnDomain, CdnDomainSourcesVo config) throws BusinessException {
        CdnDomainSources main = config.getMain();
        SourceElement sourceElement = new SourceElement();
        sourceElement.setOriginType(main.getOriginType());
        sourceElement.setIpOrDomain(main.getIpOrDomain());
        sourceElement.setHttpPort(main.getHttpPort());
        sourceElement.setHttpsPort(main.getHttpsPort());
        sourceElement.setHostName(main.getHostName());
        try {
            YiFanOperationApi.saveSourceStationInfos(cdnDomain.getDomainId(), sourceElement);
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 主备域名信息切换
     *
     * @param cdnDomain
     * @throws BusinessException
     */
    @Override
    public void change(CdnDomain cdnDomain) throws BusinessException {
        // TODO 没有备用源站，所以不能切换
    }


    /**
     * 修改回源类型
     *
     * @param cdnDomain
     * @param domainOriginSettingVo
     * @throws BusinessException
     */
    @Override
    public void saveOriginProtocol(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        try {
            YiFanOperationApi.saveOriginProtocol(cdnDomain.getDomainId(), OriginProtocol.valueOf(domainOriginSettingVo.getOriginProtocol()));
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    @Override
    public void saveOriginRequestUrlRewrite(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveAdvancedReturnSource(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    /**
     * 修改Range回源
     *
     * @param cdnDomain
     * @param domainOriginSettingVo
     * @throws BusinessException
     */
    @Override
    public void saveRangeSwitch(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        try {
            YiFanOperationApi.rangeSwitch(cdnDomain.getDomainId(), domainOriginSettingVo.getStatus());
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    @Override
    public void saveRangeVerifyETag(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveOriginHost(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveRangeTimeOut(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveOriginRequestHeader(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void httpsConfiguration(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        HttpPutBodyDTO https = config.getHttps();
        HttpsConfigDTO httpsConfigDTO = HttpsConfigDTO.builder()
                .httpsStatus(https.getHttps_status())
                .certName(https.getCertificate_name())
                .certificate(https.getCertificate_value())
                .privateKey(https.getPrivate_key())
                .build();
        try {
            YiFanOperationApi.httpsConfig(cdnDomain.getDomainId(), httpsConfigDTO);
        } catch (Exception e) {
            log.error("易凡HTTPS配置失败->{}", e.getMessage());
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    @Override
    public void httpsConfigurationOther(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {

    }

    /**
     * HTTPS强制跳转
     *
     * @param cdnDomain
     * @param config
     * @throws BusinessException
     */
    @Override
    public void forcedToJump(CdnDomain cdnDomain, DomainHttpsSettingVo config,String redirectCode) throws BusinessException {
        ForceRedirectConfigDTO forceRedirect = config.getForceRedirect();
        String type = forceRedirect.getType();
        String status = forceRedirect.getStatus();
        ForceRedirectDTO forceRedirectDTO = ForceRedirectDTO.builder()
                .status(status)
                .type(type)
                .build();
        try {
            YiFanOperationApi.forceRedirect(cdnDomain.getDomainId(), forceRedirectDTO);
        } catch (Exception e) {
            log.error("易凡强制跳转失败->{}", e.getMessage());
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    @Override
    public void saveCacheRules(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        // TODO
        List<CacheRuleDTO> cacheRules = config.getCacheRules();

//        YiFanOperationApi.cacheRuleConfig(cdnDomain.getDomainId(), );
    }

    @Override
    public void saveCacheFollowOriginStatusSwitch(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {

    }

    @Override
    public void saveErrorCodeCache(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {

    }

    @Override
    public void saveHotlinkPrevention(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        RefererDTO referer = config.getReferer();
        HotlinkPreventionDTO hotlinkPreventionDTO = HotlinkPreventionDTO.builder()
                .includeEmpty(referer.getInclude_empty()).refererType(referer.getReferer_type()).refererList(String.join(";", referer.getReferers())).build();
        try {
            YiFanOperationApi.saveHotlinkPrevention(cdnDomain.getDomainId(), hotlinkPreventionDTO);
        } catch (Exception e) {
            log.error("易凡防盗链配置失败->{}", e.getMessage());
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    @Override
    public void saveIpBlackWhiteList(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        IpAclDTO ipAclDTO = IpAclDTO.builder()
                .type(config.getType())
                .list(config.getIps())
                .build();
        try {
            YiFanOperationApi.saveIpAcl(cdnDomain.getDomainId(), ipAclDTO);
        } catch (Exception e) {
            log.error("易凡IP黑白名单配置失败->{}", e.getMessage());
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    @Override
    public void saveUserAgentFilter(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        // TODO 没有这个功能
    }

    @Override
    public void saveUrlAuth(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {

    }

    /**
     * 配置HTTP响应头信息
     *
     * @param cdnDomain
     * @param config
     * @throws BusinessException
     */
    @Override
    public void saveHttpHeader(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        List<ResponseHeaderDTO> httpResponseHeaders = Convert.toList(ResponseHeaderDTO.class, config.getHttpResponseHeaders());
        try {
            YiFanOperationApi.responseHeader(cdnDomain.getDomainId(), httpResponseHeaders);
        } catch (Exception e) {
            log.error("易凡HTTP响应头配置失败->{}", e.getMessage());
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    @Override
    public void saveCustomErrorPageConfiguration(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {

    }

    @Override
    public void saveCompress(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {

    }

    @Override
    public DomainConfig getDomainConfig(String domainName) throws BusinessException {
        // 1.通过domainName查询domainId
        List<CdnDomain> cdnDomains = domainService.queryByDomainNames(Arrays.asList(domainName));
        if (Assert.isEmpty(cdnDomains)) {
            throw new BusinessException("查询域名信息详情失败");
        }
        CdnDomain domain = cdnDomains.get(0);
        String getUrl = "/api/v1.0/domain/{domainId}/detail";
        String url = getUrl.replace("{domainId}", domain.getDomainId());
        JSONObject yiFanInfos = null;
        try {
            JSONObject response = YiFanRequest.request(url, "GET", new TreeMap<>(), new byte[0]);
            yiFanInfos = response.getJSONObject("data");
        } catch (Exception e) {
            log.error("易凡查询域名信息详情失败->{}", e.getMessage());
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        String httpsStatus = yiFanInfos.getJSONObject("domainConfig").getJSONObject("https").getString("httpsStatus");
        JSONObject source = yiFanInfos.getJSONObject("domainConfig").getJSONArray("sources").getJSONObject(0);
        DomainBasicInfo.SourceStationPrimaryInfo sourceStationPrimaryInfo = DomainBasicInfo.SourceStationPrimaryInfo.builder()
                .sourceStationType(source.getString("originType"))
                .sourceHost(source.getString("hostName"))
                .httpPort(source.getString("httpPort"))
                .httpsPort(source.getString("httpsPort"))
                .ipOrDomain(source.getString("ipOrDomain"))
                .build();
        DomainBasicInfo domainBasicInfo = DomainBasicInfo.builder()
                .domainName(yiFanInfos.getString("domainName"))
                .businessType(yiFanInfos.getString("businessType"))
                .serviceArea(yiFanInfos.getString("serviceArea"))
                .cname(domain.getCname())
                .domainStatus(DomainStatus.getSelfParam(yiFanInfos.getString("state"), CdnOperationRoute.YIFAN))
                .httpsStatus(ObjectUtil.equal(httpsStatus, "off") ? "0" : "1")
                .sourceStationPrimaryInfo(sourceStationPrimaryInfo)
                .sourceStationStandbyInfo(new DomainBasicInfo.SourceStationStandbyInfo())
                .createTime(yiFanInfos.getDate("createDate"))
                .updateTime(yiFanInfos.getDate("modifyDate"))
                .build();
        DomainConfig domainConfig = DomainConfig.builder()
                .domainBasicInfo(domainBasicInfo).build();
        return domainConfig;
    }
}
