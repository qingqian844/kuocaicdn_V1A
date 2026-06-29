package com.kuocai.cdn.service.domain.operation;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.cdn20180510.Client;
import com.aliyun.cdn20180510.models.*;
import com.aliyun.tea.TeaException;
import com.aliyun.teautil.models.RuntimeOptions;
import com.kuocai.cdn.api.*;
import com.kuocai.cdn.api.aliyun.cdn.AliyunCdnClientFactory;
import com.kuocai.cdn.api.aliyun.cdn.AliyunCdnConfigFunctions;
import com.kuocai.cdn.api.aliyun.cdn.AliyunCdnErrorCodeHandler;
import com.kuocai.cdn.api.huawei.cdn.dto.*;
import com.kuocai.cdn.api.tencent.dns.CreateRecordResponse;
import com.kuocai.cdn.api.tencent.dns.TencentApi;
import com.kuocai.cdn.api.tencent.dns.dto.CreateRecordDTO;
import com.kuocai.cdn.api.tencent.dns.properties.TencentDns;
import com.kuocai.cdn.common.mongo.entity.AliyunSetCdnDomainConfig;
import com.kuocai.cdn.config.MyRabbitConfig;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.CdnDomainSources;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.enumeration.domainmerage.domain.*;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.service.domain.operation.optional.ICdnDomainVerifyService;
import com.kuocai.cdn.util.*;
import com.kuocai.cdn.vo.*;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static com.kuocai.cdn.api.aliyun.cdn.AliyunCdnErrorCodeHandler.catchException;

@Slf4j
@Service
public class AliyunDomainServiceImpl extends BaseService<CdnDomain> implements ICdnPlatformService, ICdnDomainVerifyService {

    private final MongoTemplate mongoTemplate;

    private final RabbitTemplate rabbitTemplate;

    private final Client aliyunCdnClient;

    private final Executor executorService;

    AliyunDomainServiceImpl(MongoTemplate mongoTemplate, RabbitTemplate rabbitTemplate,
                            @Qualifier("aliyunCdnClient") Client aliyunCdnClient,
                            @Qualifier("cdnDomainExecutor") Executor executorService) {
        this.mongoTemplate = mongoTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.aliyunCdnClient = aliyunCdnClient;
        this.executorService = executorService;
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

    private String getSources(String originType, String ipOrDomain) {
        JSONArray jsonArray = new JSONArray();
        // 是域名还是IP ？？
        String type = "domain".equals(originType) ? "domain" : "ipaddr";
        String[] strings = ipOrDomain.split(";");
        for (String str : strings) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", type);
            jsonObject.put("content", str);
            jsonArray.add(jsonObject);
        }
        return jsonArray.toJSONString();
    }

    public String getCname(String domainName) throws BusinessException {
        Client client = getClient();
        DescribeCdnDomainDetailRequest request = new DescribeCdnDomainDetailRequest();
        request.setDomainName(domainName);
        DescribeCdnDomainDetailResponseBody.DescribeCdnDomainDetailResponseBodyGetDomainDetailModel domainDetailModel;
        try {
            DescribeCdnDomainDetailResponse response = client.describeCdnDomainDetail(request);
            domainDetailModel = response.getBody().getGetDomainDetailModel();
        } catch (Exception e) {
            log.error("获取域名 {} 的 cname 信息失败", domainName);
            throw catchException(e);
        }
        return domainDetailModel.getCname();
    }

    @Override
    public CdnDomain create(Long userId, String domainName, String businessType, String serviceArea, String originType, String ipOrDomain) throws BusinessException {
        Client client = getClient();
        AddCdnDomainRequest request = new AddCdnDomainRequest();
        request.setCdnType(BusinessTypeEnum.getOtherParam(businessType).getAliyun());
        request.setDomainName(domainName);
        request.setSources(getSources(originType, ipOrDomain));
        request.setScope("domestic");
        if ("outside_mainland_china".equals(serviceArea)) {
            request.setScope("overseas");
        }
        try {
            client.addCdnDomain(request);
        } catch (TeaException error) {
            log.error("阿里云CDN创建域名 {} 失败", domainName);
            if ("DomainOwnerVerifyFail".equals(error.getCode())) {
                throw new BusinessException("创建时发生错误，域名解析未进行验证");
            }
            throw catchException(error);
        } catch (Exception e) {
            throw catchException(e);
        }
        log.info("阿里云CDN创建域名 {} 成功", domainName);
        // 创建时没错误
        DescribeCdnDomainDetailRequest detailRequest = new DescribeCdnDomainDetailRequest();
        detailRequest.setDomainName(domainName);
        // 前置
        DescribeCdnDomainDetailResponseBody.DescribeCdnDomainDetailResponseBodyGetDomainDetailModel domainDetailModel = null;
        RuntimeOptions runtimeOptions = new RuntimeOptions().setAutoretry(true).setMaxAttempts(3);
        try {
            DescribeCdnDomainDetailResponse detailResponse = client.describeCdnDomainDetailWithOptions(detailRequest, runtimeOptions);
            domainDetailModel = detailResponse.getBody().getGetDomainDetailModel();
        } catch (Exception e) {
            throw catchException(e);
        }
        // 保存信息
        CdnDomain cdnDomain = CdnDomain.builder()
                .userId(userId)
                .domainName(domainDetailModel.getDomainName())
                .businessType(businessType)
                .serviceArea(serviceArea)
                .domainId(null)
                .cnameAliyun(domainDetailModel.getCname())
                .domainStatus(DomainStatus.CONFIGURING.getParam())
                .route(CdnRoute.ALIYUN.getCode())
                .build();
        return save(cdnDomain);
    }

    @Override
    public CdnDomain configDNS(CdnDomain cdnDomain) throws TencentCloudSDKException, BusinessException {
        String domainName = cdnDomain.getDomainName();
        String cnameAliyun = cdnDomain.getCnameAliyun();
        if ("outside_mainland_china".equals(cdnDomain.getServiceArea())) {
            cnameAliyun = "asia.vip.kuocaidns.com";
        }
        if (Assert.isEmpty(cnameAliyun)) {
            log.error("域名 {} 的 cname 信息为空", domainName);
            cdnDomain.setCname("审核中");
            return save(cdnDomain);
        }
        CreateRecordDTO createRecordDTO = new CreateRecordDTO();
        createRecordDTO.setDomain(TencentDns.LOCAL_DOMAIN_NAME).setSubDomain(DomainUtil.convertSubDomain(domainName)).setValue(cnameAliyun);
        CreateRecordResponse createRecordResponse = TencentApi.createRecord(createRecordDTO);
        if (Assert.isEmpty(createRecordResponse.getRecordId())) {
            log.error("创建域名 {} 解析失败", domainName);
            throw new BusinessException("dns 解析失败");
        }
        log.info("创建域名 {} 解析成功", domainName);
        cdnDomain.setCname(createRecordDTO.getSubDomain() + "." + TencentDns.LOCAL_DOMAIN_NAME);
        cdnDomain.setTencentDnsId(createRecordResponse.getRecordId());
        return save(cdnDomain);
    }

    @Override
    public void save(CdnDomain cdnDomain, String businessType, String serviceArea) throws BusinessException {

    }

    @Override
    public void disable(CdnDomain cdnDomain) throws BusinessException {
        Client client = getClient();
        String domain = cdnDomain.getDomainName();
        StopCdnDomainRequest request = new StopCdnDomainRequest();
        request.setDomainName(domain);
        try {
            client.stopCdnDomain(request);
            log.info("阿里云CDN停用域名 {} 成功", domain);
        } catch (Exception e) {
            log.error("阿里云CDN停用域名 {} 失败", domain);
            throw catchException(e);
        }
    }

    @Override
    public void enable(CdnDomain cdnDomain) throws BusinessException {
        Client client = getClient();
        StartCdnDomainRequest request = new StartCdnDomainRequest();
        String domain = cdnDomain.getDomainName();
        request.setDomainName(domain);
        try {
            client.startCdnDomain(request);
            log.info("阿里云CDN启用域名 {} 成功", domain);
        } catch (Exception e) {
            log.error("阿里云CDN启用域名 {} 失败", domain);
            throw catchException(e);
        }
    }

    @Override
    public void delete(CdnDomain cdnDomain) throws BusinessException {
        Client client = getClient();
        DeleteCdnDomainRequest request = new DeleteCdnDomainRequest();
        String domain = cdnDomain.getDomainName();
        request.setDomainName(domain);
        try {
            client.deleteCdnDomain(request);
            log.info("阿里云CDN删除域名 {} 成功", domain);
        } catch (Exception e) {
            log.error("阿里云CDN删除域名 {} 失败", domain);
            throw catchException(e);
        }
    }

    public void deleteBatchCdnDomainConfig(String domainNames, String functionNames) throws BusinessException {
        Client client = getClient();
        BatchDeleteCdnDomainConfigRequest deleteRequest = new BatchDeleteCdnDomainConfigRequest();
        deleteRequest.setDomainNames(domainNames);
        deleteRequest.setFunctionNames(functionNames);
        try {
            client.batchDeleteCdnDomainConfig(deleteRequest);
        } catch (Exception e) {
            log.error("阿里云CDN批量删除域名配置 {} 失败", domainNames);
        }
    }

    private void updateOnlyBatchCdnDomainConfig(AliyunSetCdnDomainConfig config) throws BusinessException {
        Client client = getClient();
        String domainNames = config.getDomain();
        BatchSetCdnDomainConfigRequest setRequest = new BatchSetCdnDomainConfigRequest();
        setRequest.setDomainNames(domainNames);
        setRequest.setFunctions(config.getFunctions());
        try {
            client.batchSetCdnDomainConfig(setRequest);
        } catch (Exception e) {
            log.error("阿里云CDN批量添加域名配置 {} 失败", domainNames);
            throw catchException(e);
        }
    }

    public void updateBatchCdnDomainConfig(AliyunSetCdnDomainConfig config) throws BusinessException {
        String domainNames = config.getDomain();
        // 先删除
        deleteBatchCdnDomainConfig(domainNames, config.getFunctionNames());
        // 再添加
        updateOnlyBatchCdnDomainConfig(config);
    }

    private AliyunSetCdnDomainConfig aliyunSetCdnDomainConfigBuilder(String domainNames, AliyunCdnConfigFunctions functions) {
        AliyunSetCdnDomainConfig config = new AliyunSetCdnDomainConfig();
        config.setDomain(domainNames);
        config.setFunctions(functions.getJson());
        config.setFunctionNames(functions.getFunctionNames());
        return config;
    }

    private void updateBatchCdnDomainConfig(String domainNames, AliyunCdnConfigFunctions functions) throws BusinessException {
        updateBatchCdnDomainConfig(aliyunSetCdnDomainConfigBuilder(domainNames, functions));
    }

    private void updateOnlyBatchCdnDomainConfig(String domainNames, AliyunCdnConfigFunctions functions) throws BusinessException {
        updateOnlyBatchCdnDomainConfig(aliyunSetCdnDomainConfigBuilder(domainNames, functions));
    }

    private AliyunSetCdnDomainConfig saveConfig(AliyunSetCdnDomainConfig config) {
        // 查询当前域名是否有未完成的配置 promise=pending 修改 cancel
        Query query = new Query();
        query.addCriteria(Criteria.where("domain").is(config.getDomain())
                .and("functionNames").is(config.getFunctionNames())
                .and("promise").is("pending")
        );
        mongoTemplate.find(query, AliyunSetCdnDomainConfig.class).forEach(item -> {
            item.setPromise("cancel");
            mongoTemplate.save(item);
        });
        // 保存新的配置
        return mongoTemplate.save(config);
    }

    private void pushConfig(String domainName, AliyunCdnConfigFunctions functions) {
        String functionNames = functions.getFunctionNames();
        AliyunSetCdnDomainConfig config = new AliyunSetCdnDomainConfig();
        config.setDomain(domainName);
        config.setFunctions(functions.getJson());
        config.setFunctionNames(functionNames);
        // 保存
        AliyunSetCdnDomainConfig saved = saveConfig(config);
        // 提交到队列
        rabbitTemplate.convertAndSend(MyRabbitConfig.EXCHANGE_NAME, MyRabbitConfig.ALIYUN_CDN_CONFIG_QUEUE_NAME, saved.getId());
    }

    @Override
    public void ipv6(CdnDomain cdnDomain, Integer status) throws BusinessException {
        AliyunCdnConfigFunctions functions = new AliyunCdnConfigFunctions();
        AliyunCdnConfigFunctions.Function function = new AliyunCdnConfigFunctions.Function("ipv6");
        AliyunCdnConfigFunctions.FunctionArg functionArgSwitch = AliyunCdnConfigFunctions.FunctionArg.builder()
                .argName("switch").argValue("off").build();
        AliyunCdnConfigFunctions.FunctionArg functionArgRegion = AliyunCdnConfigFunctions.FunctionArg.builder()
                .argName("region").argValue("*").build();
        if (1 == status) {
            functionArgSwitch.setArgValue("on");
        }
        function.setFunctionArgs(functionArgSwitch, functionArgRegion);
        functions.setFunction(function);
        updateBatchCdnDomainConfig(cdnDomain.getDomainName(), functions);
        // pushConfig(cdnDomain.getDomainName(), functions);
    }

    private JSONObject enSource(String s) {
        JSONObject object = new JSONObject();
        String[] strings = s.split(":");
        String type = strings[0];
        if (Assert.notEmpty(type)) {
            switch (type) {
                case "DN":
                    object.put("type", "domain");
                    break;
                case "OSS":
                    object.put("type", "oss");
                    break;
                default:
                    object.put("type", "ipaddr");
            }
        }
        String content = strings[1];
        if (Assert.notEmpty(content)) {
            object.put("content", content);
        }
        String port = strings[2];
        if (Assert.notEmpty(port)) {
            object.put("port", port);
        }
        String weight = strings[3];
        if (Assert.notEmpty(weight)) {
            object.put("weight", weight);
        }
        String priority = strings[4];
        if (Assert.notEmpty(priority)) {
            if ("B".equals(priority)) {
                object.put("priority", "30");
            } else {
                object.put("priority", "20");
            }
        }
        return object;
    }

    private JSONArray convertSource(CdnDomainSources sources) {
        JSONArray array = new JSONArray();
        String ipOrDomain = sources.getIpOrDomain();
        if (ipOrDomain.isEmpty()) {
            return array;
        }
        if (ipOrDomain.contains(";")) {
            for (String it : ipOrDomain.split(";")) {
                array.add(enSource(it));
            }
        } else {
            array.add(enSource(ipOrDomain));
        }
        return array;
    }

    @Override
    public void saveSourceStationConfig(CdnDomain cdnDomain, CdnDomainSourcesVo config) throws BusinessException {
        String domainName = cdnDomain.getDomainName();
        Client client = getClient();
        CdnDomainSources main = config.getMain();
        ModifyCdnDomainRequest request = new ModifyCdnDomainRequest();
        request.setDomainName(domainName);
        JSONArray array = convertSource(main);
        request.setSources(array.toJSONString());
        try {
            client.modifyCdnDomain(request);
            log.info("域名 {} 回源配置：{}", domainName, config);
        } catch (Exception e) {
            log.error("域名 {} 回源配置失败：{}", domainName, e.getMessage());
            throw catchException(e);
        }
        AliyunCdnConfigFunctions functions = new AliyunCdnConfigFunctions();
        // 修改 host
        String hostName = main.getHostName();
        // set_req_host_header
        AliyunCdnConfigFunctions.Function function = new AliyunCdnConfigFunctions.Function("set_req_host_header");
        // domain_name
        AliyunCdnConfigFunctions.FunctionArg functionArgDomainName = AliyunCdnConfigFunctions.FunctionArg.builder()
                .argName("domain_name").argValue(domainName).build();
        if (Assert.notEmpty(hostName)) {
            functionArgDomainName.setArgValue(hostName);
        }
        function.setFunctionArg(functionArgDomainName);
        functions.setFunction(function);
        // END 修改 host
        updateBatchCdnDomainConfig(domainName, functions);
    }

    @Override
    public void change(CdnDomain cdnDomain) throws BusinessException {

    }

    @Override
    public void saveOriginProtocol(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        String protocol = domainOriginSettingVo.getOriginProtocol();
        AliyunCdnConfigFunctions functions = new AliyunCdnConfigFunctions();
        AliyunCdnConfigFunctions.Function function = new AliyunCdnConfigFunctions.Function("forward_scheme");
        AliyunCdnConfigFunctions.FunctionArg functionArgEnable = AliyunCdnConfigFunctions.FunctionArg.builder()
                .argName("enable").argValue("on").build();
        AliyunCdnConfigFunctions.FunctionArg functionArgSchemeOrigin = AliyunCdnConfigFunctions.FunctionArg.builder()
                .argName("scheme_origin").argValue("follow").build();
        switch (protocol) {
            case "https":
                functionArgSchemeOrigin.setArgValue("https");
                break;
            case "http":
                functionArgSchemeOrigin.setArgValue("http");
                break;
            default:
                functionArgSchemeOrigin.setArgValue("follow");
        }
        function.setFunctionArgs(functionArgEnable, functionArgSchemeOrigin);
        functions.setFunction(function);
        updateBatchCdnDomainConfig(cdnDomain.getDomainName(), functions);
    }

    @Override
    public void saveOriginRequestUrlRewrite(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveAdvancedReturnSource(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveRangeSwitch(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        AliyunCdnConfigFunctions functions = new AliyunCdnConfigFunctions();
        AliyunCdnConfigFunctions.Function function = new AliyunCdnConfigFunctions.Function("range");
        AliyunCdnConfigFunctions.FunctionArg functionArg = AliyunCdnConfigFunctions.FunctionArg.builder()
                .argName("enable").argValue("off").build();
        if ("on".equals(domainOriginSettingVo.getStatus())) {
            functionArg.setArgValue("on");
        }
        function.setFunctionArg(functionArg);
        functions.setFunction(function);
        updateBatchCdnDomainConfig(cdnDomain.getDomainName(), functions);
    }

    @Override
    public void saveRangeVerifyETag(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveOriginHost(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {

    }

    @Override
    public void saveRangeTimeOut(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        // forward_timeout
        AliyunCdnConfigFunctions functions = new AliyunCdnConfigFunctions();
        AliyunCdnConfigFunctions.Function function = new AliyunCdnConfigFunctions.Function("forward_timeout");
        AliyunCdnConfigFunctions.FunctionArg functionArg = AliyunCdnConfigFunctions.FunctionArg.builder()
                .argName("forward_timeout").argValue("30").build();
        functionArg.setArgValue(String.valueOf(domainOriginSettingVo.getOriginReceiveTimeOut()));
        function.setFunctionArg(functionArg);
        functions.setFunction(function);
        updateBatchCdnDomainConfig(cdnDomain.getDomainName(), functions);
    }

    @Override
    public void saveOriginRequestHeader(CdnDomain cdnDomain, DomainOriginSettingVo domainOriginSettingVo) throws BusinessException {
        String domainName = cdnDomain.getDomainName();
        List<OriginRequestHeaderDTO> originRequestHeaders = domainOriginSettingVo.getOriginRequestHeader();
        if (Assert.isEmpty(originRequestHeaders)) {
            deleteBatchCdnDomainConfig(domainName, "origin_request_header");
        } else {
            AliyunCdnConfigFunctions functions = new AliyunCdnConfigFunctions();
            for (OriginRequestHeaderDTO originRequestHeaderDTO : originRequestHeaders) {
                AliyunCdnConfigFunctions.Function function = new AliyunCdnConfigFunctions.Function("origin_request_header");
                // header_operation_type
                AliyunCdnConfigFunctions.FunctionArg functionArgHeaderOperationType = AliyunCdnConfigFunctions.FunctionArg.builder()
                        .argName("header_operation_type").argValue("add").build();
                if ("delete".equals(originRequestHeaderDTO.getAction())) {
                    functionArgHeaderOperationType.setArgValue("delete");
                }
                // header_name
                AliyunCdnConfigFunctions.FunctionArg functionArgHeaderName = AliyunCdnConfigFunctions.FunctionArg.builder()
                        .argName("header_name").argValue(originRequestHeaderDTO.getName()).build();
                // header_value
                AliyunCdnConfigFunctions.FunctionArg functionArgHeaderValue = AliyunCdnConfigFunctions.FunctionArg.builder()
                        .argName("header_value").argValue(originRequestHeaderDTO.getValue()).build();
                // duplicate
                AliyunCdnConfigFunctions.FunctionArg functionArgDuplicate = AliyunCdnConfigFunctions.FunctionArg.builder()
                        .argName("duplicate").argValue("off").build();
                function.setFunctionArgs(functionArgHeaderOperationType, functionArgHeaderName, functionArgHeaderValue, functionArgDuplicate);
                functions.setFunction(function);
            }
            updateBatchCdnDomainConfig(domainName, functions);
        }
    }

    @Override
    public void httpsConfiguration(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        Client client = getClient();
        SetCdnDomainSSLCertificateRequest request = new SetCdnDomainSSLCertificateRequest();
        HttpPutBodyDTO httpPutBodyDTO = config.getHttps();
        request.setDomainName(cdnDomain.getDomainName());
        if ("on".equals(httpPutBodyDTO.getHttps_status())) {
            request.setSSLProtocol("on");
            request.setCertType("upload");
            request.setSSLPub(httpPutBodyDTO.getCertificate_value());
            request.setSSLPri(httpPutBodyDTO.getPrivate_key());
        } else {
            request.setSSLProtocol("off");
        }
        try {
            client.setCdnDomainSSLCertificate(request);
        } catch (Exception e) {
            throw catchException(e);
        }
    }

    private List<DescribeCdnDomainConfigsResponseBody.DescribeCdnDomainConfigsResponseBodyDomainConfigsDomainConfig> getDescribeConfigs(String domainName, String functionNames) throws BusinessException {
        Client client = getClient();
        DescribeCdnDomainConfigsRequest request = new DescribeCdnDomainConfigsRequest();
        request.setDomainName(domainName);
        request.setFunctionNames(functionNames);
        try {
            DescribeCdnDomainConfigsResponse response = client.describeCdnDomainConfigs(request);
            return response.getBody().getDomainConfigs().getDomainConfig();
        } catch (Exception e) {
            throw catchException(e);
        }
    }

    private String getDescribeConfigId(String domainName, String functionName) throws BusinessException {
        List<DescribeCdnDomainConfigsResponseBody.DescribeCdnDomainConfigsResponseBodyDomainConfigsDomainConfig> domainConfigs = getDescribeConfigs(domainName, functionName);
        if (Assert.isEmpty(domainConfigs)) {
            return null;
        }
        return domainConfigs.get(0).getConfigId();
    }

    @Override
    public void httpsConfigurationOther(CdnDomain cdnDomain, DomainHttpsSettingVo config) throws BusinessException {
        HttpPutBodyDTO httpPutBodyDTO = config.getHttps();
        String http2Status = httpPutBodyDTO.getHttp2_status();
        String ocspStaplingStatus = httpPutBodyDTO.getOcsp_stapling_status();
        String tlsVersion = httpPutBodyDTO.getTls_version();
        String domainName = cdnDomain.getDomainName();
        AliyunCdnConfigFunctions functions = new AliyunCdnConfigFunctions();
        // HTTP 2
        if (Assert.notEmpty(http2Status)) {
            // https_option
            AliyunCdnConfigFunctions.Function httpsOption = new AliyunCdnConfigFunctions.Function("https_option");
            // http2
            AliyunCdnConfigFunctions.FunctionArg functionArgHttp2 = AliyunCdnConfigFunctions.FunctionArg.builder()
                    .argName("http2").argValue(http2Status).build();
            // ocsp_stapling
//            AliyunCdnConfigFunctions.FunctionArg functionArgOcspStapling = AliyunCdnConfigFunctions.FunctionArg.builder()
//            .argName("ocsp_stapling").argValue("off").build();
            // 查询 https_option 是否存在
//            String configId = getDescribeConfigId(domainName, "https_option");
//            if (Assert.notEmpty(configId)) {
//                httpsOption.setParentId(configId);
//                httpsOption.setFunctionArg(functionArgHttp2);
//            } else {
                httpsOption.setFunctionArgs(functionArgHttp2);
//            }
            functions.setFunction(httpsOption);
            updateOnlyBatchCdnDomainConfig(domainName, functions);
        }
        // OCSP Stapling
        if (Assert.notEmpty(ocspStaplingStatus)) {
            // https_option
            AliyunCdnConfigFunctions.Function httpsOption = new AliyunCdnConfigFunctions.Function("https_option");
            // ocsp_stapling
            AliyunCdnConfigFunctions.FunctionArg functionArgOcspStapling = AliyunCdnConfigFunctions.FunctionArg.builder()
                    .argName("ocsp_stapling").argValue(ocspStaplingStatus).build();
            // http2
//            AliyunCdnConfigFunctions.FunctionArg functionArgHttp2 = AliyunCdnConfigFunctions.FunctionArg.builder()
//                    .argName("http2").argValue("off").build();
            // 查询 https_option 是否存在
//            String configId = getDescribeConfigId(domainName, "https_option");
//            if (Assert.notEmpty(configId)) {
//                httpsOption.setParentId(configId);
//                httpsOption.setFunctionArg(functionArgOcspStapling);
//            } else {
                httpsOption.setFunctionArgs(functionArgOcspStapling);
//            }
            functions.setFunction(httpsOption);
            updateOnlyBatchCdnDomainConfig(domainName, functions);
        }
        // TLS 版本
        if (Assert.notEmpty(tlsVersion)) {
            // https_tls_version
            AliyunCdnConfigFunctions.Function httpsTlsVersion = new AliyunCdnConfigFunctions.Function("https_tls_version");
            // tls10
            AliyunCdnConfigFunctions.FunctionArg functionArgTls10 = AliyunCdnConfigFunctions.FunctionArg.builder()
                    .argName("tls10").argValue("off").build();
            // tls11
            AliyunCdnConfigFunctions.FunctionArg functionArgTls11 = AliyunCdnConfigFunctions.FunctionArg.builder()
                    .argName("tls11").argValue("off").build();
            // tls12
            AliyunCdnConfigFunctions.FunctionArg functionArgTls12 = AliyunCdnConfigFunctions.FunctionArg.builder()
                    .argName("tls12").argValue("off").build();
            // tls13
            AliyunCdnConfigFunctions.FunctionArg functionArgTls13 = AliyunCdnConfigFunctions.FunctionArg.builder()
                    .argName("tls13").argValue("off").build();
            // tlsVersion
            if (!tlsVersion.isEmpty()) {
                for (String it : tlsVersion.split(",")) {
                    switch (it) {
                        case "TLSv1":
                            functionArgTls10.setArgValue("on");
                            break;
                        case "TLSv1.1":
                            functionArgTls11.setArgValue("on");
                            break;
                        case "TLSv1.2":
                            functionArgTls12.setArgValue("on");
                            break;
                        case "TLSv1.3":
                            functionArgTls13.setArgValue("on");
                            break;
                    }
                }
            }
            httpsTlsVersion.setFunctionArgs(functionArgTls10, functionArgTls11, functionArgTls12, functionArgTls13);
            functions.setFunction(httpsTlsVersion);
            updateBatchCdnDomainConfig(domainName, functions);
        }
    }

    @Override
    public void forcedToJump(CdnDomain cdnDomain, DomainHttpsSettingVo config,String redirectCode) throws BusinessException {
        ForceRedirectConfigDTO forceRedirectConfigDTO = config.getForceRedirect();
        String domainName = cdnDomain.getDomainName();
        deleteBatchCdnDomainConfig(domainName, "http_force,https_force");
        if ("on".equals(forceRedirectConfigDTO.getStatus())) {
            AliyunCdnConfigFunctions functions = new AliyunCdnConfigFunctions();
            if ("https".equals(forceRedirectConfigDTO.getType())) {
                // https_force
                AliyunCdnConfigFunctions.Function httpsForce = new AliyunCdnConfigFunctions.Function("https_force");
                // enable
                AliyunCdnConfigFunctions.FunctionArg httpsForceArgEnable = AliyunCdnConfigFunctions.FunctionArg.builder()
                        .argName("enable").argValue("on").build();
                // https_rewrite
                AliyunCdnConfigFunctions.FunctionArg functionArgHttpsRewrite = AliyunCdnConfigFunctions.FunctionArg.builder()
                        .argName("https_rewrite").argValue("308").build();
                httpsForce.setFunctionArgs(httpsForceArgEnable, functionArgHttpsRewrite);
                if (301 == forceRedirectConfigDTO.getRedirect_code()) {
                    functionArgHttpsRewrite.setArgValue("301");
                }
                functions.setFunctions(httpsForce);
            } else {
                // http_force
                AliyunCdnConfigFunctions.Function httpForce = new AliyunCdnConfigFunctions.Function("http_force");
                // enable
                AliyunCdnConfigFunctions.FunctionArg httpForceArgEnable = AliyunCdnConfigFunctions.FunctionArg.builder()
                        .argName("enable").argValue("on").build();
                // http_rewrite
                AliyunCdnConfigFunctions.FunctionArg functionArgHttpRewrite = AliyunCdnConfigFunctions.FunctionArg.builder()
                        .argName("http_rewrite").argValue("308").build();
                httpForce.setFunctionArgs(httpForceArgEnable, functionArgHttpRewrite);
                if (301 == forceRedirectConfigDTO.getRedirect_code()) {
                    functionArgHttpRewrite.setArgValue("301");
                }
                functions.setFunctions(httpForce);
            }
            updateBatchCdnDomainConfig(domainName, functions);
        }
    }

    @Override
    public void saveCacheRules(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        String compareMaxAge = config.getCacheFollowOriginStatus();
        if (Assert.isEmpty(compareMaxAge)) {
            compareMaxAge = "off";
        }
        String domainName = cdnDomain.getDomainName();
        deleteBatchCdnDomainConfig(domainName, "filetype_based_ttl_set,path_based_ttl_set");
        List<CacheRuleDTO> cacheRules = config.getCacheRules();
        int weight = cacheRules.size();
        if (0 == weight) {
            return;
        }
        AliyunCdnConfigFunctions functions = new AliyunCdnConfigFunctions();
        for (CacheRuleDTO cacheRule : cacheRules) {
            String matchType = cacheRule.getMatch_type();
            String matchValue = cacheRule.getMatch_value();
            AliyunCdnConfigFunctions.Function function;
            switch (matchType) {
                case "catalog":
                    function = new AliyunCdnConfigFunctions.Function("path_based_ttl_set");
                    // path
                    AliyunCdnConfigFunctions.FunctionArg functionArgPath = AliyunCdnConfigFunctions.FunctionArg.builder()
                            .argName("path").argValue("/").build();
                    functionArgPath.setArgValue(matchValue);
                    function.setFunctionArg(functionArgPath);
                    break;
                case "file_extension":
                    function = new AliyunCdnConfigFunctions.Function("filetype_based_ttl_set");
                    // file_type
                    AliyunCdnConfigFunctions.FunctionArg functionArgFileType = AliyunCdnConfigFunctions.FunctionArg.builder()
                            .argName("file_type").argValue("").build();
                    functionArgFileType.setArgValue(matchValue.replace(".", "").replace(";", ","));
                    function.setFunctionArg(functionArgFileType);
                    break;
                default:
                    continue;
            }
            AliyunCdnConfigFunctions.FunctionArg functionArgTtl = AliyunCdnConfigFunctions.FunctionArg.builder()
                    .argName("ttl").argValue("0").build();
            functionArgTtl.setArgValue(String.valueOf(KuocaiBaseUtil.toSeconds(cacheRule.getTtl(), cacheRule.getTtl_unit())));
            AliyunCdnConfigFunctions.FunctionArg functionArgWeight = AliyunCdnConfigFunctions.FunctionArg.builder()
                    .argName("weight").argValue(String.valueOf(weight)).build();
            // swift_origin_cache_high
            AliyunCdnConfigFunctions.FunctionArg functionArgSwiftOriginCacheHigh = AliyunCdnConfigFunctions.FunctionArg.builder()
                    .argName("swift_origin_cache_high").argValue(compareMaxAge).build();
            // args
            function.setFunctionArgs(functionArgTtl, functionArgWeight, functionArgSwiftOriginCacheHigh);
            functions.setFunctions(function);
            weight = weight - 1;
        }
        updateBatchCdnDomainConfig(domainName, functions);
    }

    @Override
    public void saveCacheFollowOriginStatusSwitch(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        // 未使用
    }

    @Override
    public void saveErrorCodeCache(CdnDomain cdnDomain, SettingCacheVo config) throws BusinessException {
        // default_ttl_code
        String domainName = cdnDomain.getDomainName();
        List<ErrorCodeCacheDTO> errorCodeCache = config.getErrorCodeCache();
        if (errorCodeCache.isEmpty()) {
            deleteBatchCdnDomainConfig(domainName, "default_ttl_code");
            return;
        }
        AliyunCdnConfigFunctions functions = new AliyunCdnConfigFunctions();
        AliyunCdnConfigFunctions.Function function = new AliyunCdnConfigFunctions.Function("default_ttl_code");
        AliyunCdnConfigFunctions.FunctionArg functionArg = AliyunCdnConfigFunctions.FunctionArg.builder()
                .argName("default_ttl_code").argValue("").build();
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (ErrorCodeCacheDTO item : errorCodeCache) {
            if (first) {
                first = false;
            } else {
                builder.append(",");
            }
            builder.append(item.getCode()).append("=").append(item.getTtl());
        }
        functionArg.setArgValue(builder.toString());
        function.setFunctionArg(functionArg);
        functions.setFunction(function);
        updateBatchCdnDomainConfig(domainName, functions);
    }

    @Override
    public void saveHotlinkPrevention(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        RefererDTO refererDTO = config.getReferer();
        String domainName = cdnDomain.getDomainName();
        Integer refererType = refererDTO.getReferer_type();
        deleteBatchCdnDomainConfig(domainName, "referer_black_list_set,referer_white_list_set");
        if (0 == refererType) {
            return;
        }
        AliyunCdnConfigFunctions functions = new AliyunCdnConfigFunctions();
        List<String> referers = refererDTO.getReferers();
        // allow_empty
        AliyunCdnConfigFunctions.FunctionArg allowEmpty = AliyunCdnConfigFunctions.FunctionArg.builder()
                .argName("allow_empty").argValue("off").build();
        if (refererDTO.getInclude_empty()) {
            allowEmpty.setArgValue("on");
        }
        if (2 == refererType) {
            // referer_white_list_set
            AliyunCdnConfigFunctions.Function refererWhiteListSet = new AliyunCdnConfigFunctions.Function("referer_white_list_set");
            // refer_domain_allow_list
            AliyunCdnConfigFunctions.FunctionArg referDomainAllowList = AliyunCdnConfigFunctions.FunctionArg.builder()
                    .argName("refer_domain_allow_list").argValue(String.join(",", referers)).build();
            refererWhiteListSet.setFunctionArgs(referDomainAllowList, allowEmpty);
            functions.setFunction(refererWhiteListSet);
        } else {
            // referer_black_list_set
            AliyunCdnConfigFunctions.Function refererBlackListSet = new AliyunCdnConfigFunctions.Function("referer_black_list_set");
            // refer_domain_deny_list
            AliyunCdnConfigFunctions.FunctionArg referDomainDenyList = AliyunCdnConfigFunctions.FunctionArg.builder()
                    .argName("refer_domain_deny_list").argValue(String.join(",", referers)).build();
            refererBlackListSet.setFunctionArgs(referDomainDenyList, allowEmpty);
            functions.setFunction(refererBlackListSet);
        }
        updateBatchCdnDomainConfig(domainName, functions);
    }

    @Override
    public void saveIpBlackWhiteList(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        Integer banIpType = config.getType();
        String domainName = cdnDomain.getDomainName();
        deleteBatchCdnDomainConfig(domainName, "ip_black_list_set,ip_allow_list_set");
        if (0 == banIpType) {
            return;
        }
        AliyunCdnConfigFunctions functions = new AliyunCdnConfigFunctions();
        // ip_list
        AliyunCdnConfigFunctions.FunctionArg ipList = AliyunCdnConfigFunctions.FunctionArg.builder()
                .argName("ip_list").argValue(String.join(",", config.getIps())).build();
        // ip_acl_xfwd
        // AliyunCdnConfigFunctions.FunctionArg ipAclXfwd = AliyunCdnConfigFunctions.FunctionArg.builder().argName("ip_acl_xfwd").argValue("all").build();
        if (2 == banIpType) {
            // ip_allow_list_set
            AliyunCdnConfigFunctions.Function ipAllowListSet = new AliyunCdnConfigFunctions.Function("ip_allow_list_set");
            // ip_list
            ipAllowListSet.setFunctionArg(ipList);
            functions.setFunction(ipAllowListSet);
        } else {
            // ip_black_list_set
            AliyunCdnConfigFunctions.Function ipBlackListSet = new AliyunCdnConfigFunctions.Function("ip_black_list_set");
            // ip_list
            ipBlackListSet.setFunctionArg(ipList);
            functions.setFunction(ipBlackListSet);
        }
        updateBatchCdnDomainConfig(domainName, functions);
    }

    @Override
    public void saveUserAgentFilter(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {
        UserAgentBlackAndWhiteListDTO userAgentBlackAndWhiteListDTO = config.getUserAgentBlackAndWhiteListDTO();
        Integer filterType = userAgentBlackAndWhiteListDTO.getType();
        String domainName = cdnDomain.getDomainName();
        if (0 == filterType) {
            deleteBatchCdnDomainConfig(domainName, "ali_ua");
            return;
        }
        AliyunCdnConfigFunctions functions = new AliyunCdnConfigFunctions();
        // ali_ua
        AliyunCdnConfigFunctions.Function aliUa = new AliyunCdnConfigFunctions.Function("ali_ua");
        // ua
        AliyunCdnConfigFunctions.FunctionArg ua = AliyunCdnConfigFunctions.FunctionArg.builder()
                .argName("ua").argValue(String.join("|", userAgentBlackAndWhiteListDTO.getUa_list())).build();
        // type
        AliyunCdnConfigFunctions.FunctionArg type = AliyunCdnConfigFunctions.FunctionArg.builder()
                .argName("type").argValue("black").build();
        if (2 == filterType) {
            type.setArgValue("white");
        }
        aliUa.setFunctionArgs(ua, type);
        functions.setFunction(aliUa);
        updateBatchCdnDomainConfig(domainName, functions);
    }

    @Override
    public void saveUrlAuth(CdnDomain cdnDomain, SettingAccessVo config) throws BusinessException {

    }

    @Override
    public void saveHttpHeader(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        String domainName = cdnDomain.getDomainName();
        // set_resp_header
        List<HttpResponseHeaderDTO> httpResponseHeaders = config.getHttpResponseHeaders();
        if (Assert.isEmpty(httpResponseHeaders)) {
            deleteBatchCdnDomainConfig(domainName, "set_resp_header");
        } else {
            AliyunCdnConfigFunctions functions = new AliyunCdnConfigFunctions();
            for (HttpResponseHeaderDTO httpResponseHeaderDTO : httpResponseHeaders) {
                AliyunCdnConfigFunctions.Function function = new AliyunCdnConfigFunctions.Function("set_resp_header");
                // key
                AliyunCdnConfigFunctions.FunctionArg functionArgKey = AliyunCdnConfigFunctions.FunctionArg.builder()
                        .argName("key").argValue(httpResponseHeaderDTO.getName()).build();
                // value
                AliyunCdnConfigFunctions.FunctionArg functionArgValue = AliyunCdnConfigFunctions.FunctionArg.builder()
                        .argName("value").argValue(httpResponseHeaderDTO.getValue()).build();
                // header_operation_type
                AliyunCdnConfigFunctions.FunctionArg functionArgHeaderOperationType = AliyunCdnConfigFunctions.FunctionArg.builder()
                        .argName("header_operation_type").argValue("add").build();
                if ("delete".equals(httpResponseHeaderDTO.getAction())) {
                    functionArgHeaderOperationType.setArgValue("delete");
                }
                function.setFunctionArgs(functionArgKey, functionArgValue, functionArgHeaderOperationType);
                functions.setFunction(function);
            }
            updateBatchCdnDomainConfig(domainName, functions);
        }
    }

    @Override
    public void saveCustomErrorPageConfiguration(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        // 暂未使用
    }

    @Override
    public void saveCompress(CdnDomain cdnDomain, SettingHigherVo config) throws BusinessException {
        CompressDTO compressDTO = config.getCompress();
        String status = compressDTO.getStatus();
        String type = compressDTO.getType();
        String domainName = cdnDomain.getDomainName();
        // brotli gzip
        deleteBatchCdnDomainConfig(domainName, "brotli,gzip");
        if ("off".equals(status)) {
            return;
        }
        AliyunCdnConfigFunctions functions = new AliyunCdnConfigFunctions();
        // enable
        AliyunCdnConfigFunctions.FunctionArg functionArgEnable = AliyunCdnConfigFunctions.FunctionArg.builder()
                .argName("enable").argValue("on").build();
        if (type.contains("br")) {
            // brotli
            AliyunCdnConfigFunctions.Function brotli = new AliyunCdnConfigFunctions.Function("brotli");
            brotli.setFunctionArg(functionArgEnable);
            functions.setFunction(brotli);
        }
        if (type.contains("gzip")) {
            // gzip
            AliyunCdnConfigFunctions.Function gzip = new AliyunCdnConfigFunctions.Function("gzip");
            gzip.setFunctionArg(functionArgEnable);
            functions.setFunction(gzip);
        }
        updateBatchCdnDomainConfig(domainName, functions);
    }

    private HashMap<String, String> functionArgsToMap(List<DescribeCdnDomainConfigsResponseBody.DescribeCdnDomainConfigsResponseBodyDomainConfigsDomainConfigFunctionArgsFunctionArg> functionArgs) {
        HashMap<String, String> map = new HashMap<>();
        for (DescribeCdnDomainConfigsResponseBody.DescribeCdnDomainConfigsResponseBodyDomainConfigsDomainConfigFunctionArgsFunctionArg functionArg : functionArgs) {
            map.put(functionArg.getArgName(), functionArg.getArgValue());
        }
        return map;
    }

    private HashMap<String, String> functionArgsToMap(DescribeCdnDomainConfigsResponseBody.DescribeCdnDomainConfigsResponseBodyDomainConfigsDomainConfigFunctionArgs functionArgs) {
        return functionArgsToMap(functionArgs.getFunctionArg());
    }

    private ArrayList<DomainCacheInfo.ErrorCodeCache> ttlCode(String rule) {
        ArrayList<DomainCacheInfo.ErrorCodeCache> list = new ArrayList<>();
        if (rule.isEmpty()) {
            return list;
        }
        // 4xx=3,200=3600,5xx=1
        for (String item : rule.split(",")) {
            String[] split = item.split("=");
            DomainCacheInfo.ErrorCodeCache errorCodeCache = DomainCacheInfo.ErrorCodeCache.builder()
                    .code(Integer.valueOf(split[0])).ttl(Integer.valueOf(split[1]))
                    .build();
            list.add(errorCodeCache);
        }
        return list;
    }

    private <T> CompletableFuture<T> executeAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(ThreadMdcUtils.wrapAsync(supplier, MDC.getCopyOfContextMap()), executorService);
    }

    @Override
    public DomainConfig getDomainConfig(String domainName) throws BusinessException {
        Client client = getClient();

        DomainBasicInfo.SourceStationPrimaryInfo sourceStationPrimaryInfo = DomainBasicInfo.SourceStationPrimaryInfo.builder()
                .sourceStationType(OriginTypeEnum.IPADDR.getParam())
                .ipOrDomain("").httpPort("80").httpsPort("443").sourceHost(domainName)
                .build();

        DomainBasicInfo.SourceStationStandbyInfo sourceStationStandbyInfo = DomainBasicInfo.SourceStationStandbyInfo.builder()
                .sourceStationType("").ipOrDomain("").httpPort("").httpsPort("").sourceHost("")
                .build();

        DomainBasicInfo domainBasicInfo = DomainBasicInfo.builder()
                .domainName(domainName)
                .domainStatus(DomainStatus.CONFIGURING.getParam())
                .httpsStatus("0")
                .cname("")
                .businessType(BusinessTypeEnum.WEB.getParam())
                .serviceArea(ServiceAreaEnum.MAINLAND_CHINA.getParam())
                .isIpv6("0")
                .createTime(KuocaiDateUtil.strToDate("2021-01-01 00:00:00"))
                .updateTime(KuocaiDateUtil.strToDate("2021-01-01 00:00:00"))
                .sourceStationPrimaryInfo(sourceStationPrimaryInfo)
                .sourceStationStandbyInfo(sourceStationStandbyInfo)
                .build();

        ArrayList<DomainBackSourceInfo.BackSourceRequestInfo> backSourceRequestInfos = new ArrayList<>();
        ArrayList<DomainAdvancedInfo.HttpResponseHeader> httpResponseHeaders = new ArrayList<>();
        DomainBackSourceInfo domainBackSourceInfo = DomainBackSourceInfo.builder()
                .origin_protocol(OriginProtocolEnum.HTTP.getParam())
                .port(80)  // 无该配置
                .origin_receive_timeout("30")
                .origin_range_status("off")
                .slice_etag_status("off")  // 无该配置
                .origin_request_url_rewrite(new ArrayList<>())  // 无该配置
                .flexible_origin(new ArrayList<>())  // 无该配置
                .origin_request_header(backSourceRequestInfos)
                .build();

        ArrayList<DomainCacheInfo.CacheRule> cacheRules = new ArrayList<>();
        ArrayList<DomainCacheInfo.ErrorCodeCache> errorCodeCaches = new ArrayList<>();
        DomainCacheInfo domainCacheInfo = DomainCacheInfo.builder()
                .cache_rules(cacheRules).error_code_cache(errorCodeCaches)
                .build();

        DomainVisitInfo.Referer referer = DomainVisitInfo.Referer.builder()
                .type("off").referer_type(0).value("").include_empty(false)
                .build();
        DomainVisitInfo.IpFilter ipFilter = DomainVisitInfo.IpFilter.builder()
                .type("off").value("")
                .build();
        DomainVisitInfo.UserAgentFilter userAgentFilter = DomainVisitInfo.UserAgentFilter.builder()
                .type("off").value("")
                .build();
        DomainVisitInfo domainVisitInfo = DomainVisitInfo.builder()
                .referer(referer).ip_filter(ipFilter).user_agent_filter(userAgentFilter)
                .build();

        ArrayList<DomainAdvancedInfo.ErrorCodeRedirectRules> errorCodeRedirectRules = new ArrayList<>();
        DomainAdvancedInfo.Compress compress = DomainAdvancedInfo.Compress.builder()
                .status("off").type("").file_type("")
                .build();
        DomainAdvancedInfo domainAdvancedInfo = DomainAdvancedInfo.builder()
                .http_response_header(httpResponseHeaders).error_code_redirect_rules(errorCodeRedirectRules).compress(compress)
                .build();

        DomainHttpsInfo.HttpGetBody httpGetBody = DomainHttpsInfo.HttpGetBody.builder()
                .https_status("off").certificate_name("").certificate_value("").expire_time(0L).certificate_source(0).certificate_type("").http2_status("off").tls_version("").ocsp_stapling_status("off").certId(0)
                .build();
        DomainHttpsInfo.ForceRedirect forceRedirect = DomainHttpsInfo.ForceRedirect.builder()
                .status("off").type("https").redirect_code("301")
                .build();
        DomainHttpsInfo domainHttpsInfo = DomainHttpsInfo.builder()
                .https(httpGetBody).force_redirect(forceRedirect)
                .build();

        DescribeCdnDomainDetailRequest detailRequest = new DescribeCdnDomainDetailRequest();
        detailRequest.setDomainName(domainName);
        CompletableFuture<DescribeCdnDomainDetailResponse> detailResponseCompletableFuture = executeAsync(() -> {
            try {
                return client.describeCdnDomainDetail(detailRequest);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        DescribeCdnDomainConfigsRequest request = new DescribeCdnDomainConfigsRequest();
        request.setDomainName(domainName);
        CompletableFuture<DescribeCdnDomainConfigsResponse> responseCompletableFuture = executeAsync(() -> {
            try {
                return client.describeCdnDomainConfigs(request);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        DescribeDomainCertificateInfoRequest certificateInfoRequest = new DescribeDomainCertificateInfoRequest();
        certificateInfoRequest.setDomainName(domainName);
        CompletableFuture<DescribeDomainCertificateInfoResponse> certificateInfoResponseCompletableFuture = executeAsync(() -> {
            try {
                return client.describeDomainCertificateInfo(certificateInfoRequest);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture.allOf(detailResponseCompletableFuture, responseCompletableFuture).join();

        try {
            DescribeCdnDomainDetailResponse detailResponse = detailResponseCompletableFuture.get();
            DescribeCdnDomainConfigsResponse response = responseCompletableFuture.get();
            DescribeCdnDomainDetailResponseBody.DescribeCdnDomainDetailResponseBodyGetDomainDetailModel detailModel = detailResponse.getBody().getGetDomainDetailModel();
            domainBasicInfo.setCname(detailModel.getCname());
            domainBasicInfo.setServiceArea(convertServiceArea(detailModel.getCdnType()));
            domainBasicInfo.setDomainStatus(convertDomainStatus(detailModel.getDomainStatus()));
            // 时间
            domainBasicInfo.setCreateTime(KuocaiDateUtil.toDate(detailModel.getGmtCreated()));
            domainBasicInfo.setUpdateTime(KuocaiDateUtil.toDate(detailModel.getGmtModified()));
            sourceStationPrimaryInfo.setIpOrDomain(convertSource(detailModel.getSourceModels()));
            // 功能
            List<DescribeCdnDomainConfigsResponseBody.DescribeCdnDomainConfigsResponseBodyDomainConfigsDomainConfig> configs = response.getBody().getDomainConfigs().getDomainConfig();
            for (DescribeCdnDomainConfigsResponseBody.DescribeCdnDomainConfigsResponseBodyDomainConfigsDomainConfig config : configs) {
                HashMap<String, String> configMap = functionArgsToMap(config.getFunctionArgs());
                switch (config.getFunctionName()) {
                    case "set_req_host_header":
                        sourceStationPrimaryInfo.setSourceHost(configMap.getOrDefault("domain_name", ""));
                        break;
                    case "forward_scheme":
                        if ("on".equals(configMap.getOrDefault("enable", "off"))) {
                            // 一样的 不使用 OriginProtocolEnum
                            domainBackSourceInfo.setOrigin_protocol(configMap.getOrDefault("scheme_origin", "http"));
                        }
                        break;
                    case "ipv6":
                        if ("on".equals(configMap.getOrDefault("switch", "off"))) {
                            domainBasicInfo.setIsIpv6("1");
                        }
                        break;
                    case "range":
                        domainBackSourceInfo.setOrigin_range_status(configMap.getOrDefault("enable", "off"));
                        break;
                    case "forward_timeout":
                        domainBackSourceInfo.setOrigin_receive_timeout(configMap.getOrDefault("forward_timeout", "30"));
                        break;
                    case "origin_request_header":
                        DomainBackSourceInfo.BackSourceRequestInfo backSourceRequestInfo = DomainBackSourceInfo.BackSourceRequestInfo.builder()
                                .name(configMap.getOrDefault("header_name", ""))
                                .value(configMap.getOrDefault("header_value", ""))
                                .action("delete")
                                .build();
                        if ("add".equals(configMap.getOrDefault("header_operation_type", "delete"))) {
                            backSourceRequestInfo.setAction("set");
                        }
                        backSourceRequestInfos.add(backSourceRequestInfo);
                        break;
                    case "default_ttl_code":
                        domainCacheInfo.setError_code_cache(ttlCode(configMap.getOrDefault("default_ttl_code", "")));
                        break;
                    case "set_resp_header":
                        DomainAdvancedInfo.HttpResponseHeader httpResponseHeader = DomainAdvancedInfo.HttpResponseHeader.builder()
                                .name(configMap.getOrDefault("key", ""))
                                .value(configMap.getOrDefault("value", ""))
                                .action("delete")
                                .build();
                        if ("add".equals(configMap.getOrDefault("header_operation_type", "delete"))) {
                            httpResponseHeader.setAction("set");
                        }
                        httpResponseHeaders.add(httpResponseHeader);
                        break;
                    case "gzip":
                        if ("on".equals(configMap.getOrDefault("enable", "off"))) {
                            compress.setStatus("on");
                            String s = compress.getType();
                            if (s.isEmpty()) {
                                compress.setType("gzip");
                            } else {
                                compress.setType(s + ",gzip");
                            }
                        }
                        break;
                    case "brotli":
                        if ("on".equals(configMap.getOrDefault("enable", "off"))) {
                            compress.setStatus("on");
                            String s = compress.getType();
                            if (s.isEmpty()) {
                                compress.setType("br");
                            } else {
                                compress.setType(s + ",br");
                            }
                        }
                        break;
                    case "referer_black_list_set":
                        referer.setType("black");
                        referer.setReferer_type(1);
                        referer.setValue(configMap.getOrDefault("refer_domain_deny_list", "").replace(",", "\n"));
                        referer.setInclude_empty("on".equals(configMap.getOrDefault("allow_empty", "off")));
                        break;
                    case "referer_white_list_set":
                        referer.setType("white");
                        referer.setReferer_type(2);
                        referer.setValue(configMap.getOrDefault("refer_domain_allow_list", "").replace(",", "\n"));
                        referer.setInclude_empty("on".equals(configMap.getOrDefault("allow_empty", "off")));
                        break;
                    case "ip_black_list_set":
                        ipFilter.setType("black");
                        ipFilter.setValue(configMap.getOrDefault("ip_list", "").replace(",", "\n"));
                        break;
                    case "ip_allow_list_set":
                        ipFilter.setType("white");
                        ipFilter.setValue(configMap.getOrDefault("ip_list", "").replace(",", "\n"));
                        break;
                    case "ali_ua":
                        userAgentFilter.setType(configMap.getOrDefault("type", "off"));
                        userAgentFilter.setValue(configMap.getOrDefault("ua", "").replace("|", "\n"));
                        break;
                    case "path_based_ttl_set":
                        Integer pathTtl = Integer.valueOf(configMap.getOrDefault("ttl", "0"));
                        DomainCacheInfo.CacheRule pathCacheRule = DomainCacheInfo.CacheRule.builder()
                                .match_type("catalog")
                                .match_value(configMap.getOrDefault("path", ""))
                                .ttl(KuocaiBaseUtil.getUnitCacheTime(pathTtl))
                                .ttl_unit(KuocaiBaseUtil.getCacheTimeUnit(pathTtl))
                                .priority(Integer.valueOf(configMap.getOrDefault("weight", "1")))
                                .follow_origin(configMap.getOrDefault("swift_origin_cache_high", "off"))
                                .build();
                        cacheRules.add(pathCacheRule);
                        break;
                    case "filetype_based_ttl_set":
                        Integer filetypeTtl = Integer.valueOf(configMap.getOrDefault("ttl", "0"));
                        String fileType = configMap.getOrDefault("file_type", "");
                        DomainCacheInfo.CacheRule filetypeCacheRule = DomainCacheInfo.CacheRule.builder()
                                .match_type("file_extension")
                                .ttl(KuocaiBaseUtil.getUnitCacheTime(filetypeTtl))
                                .ttl_unit(KuocaiBaseUtil.getCacheTimeUnit(filetypeTtl))
                                .priority(Integer.valueOf(configMap.getOrDefault("weight", "1")))
                                .follow_origin(configMap.getOrDefault("swift_origin_cache_high", "off"))
                                .build();
                        if (fileType.isEmpty()) {
                            filetypeCacheRule.setMatch_value("");
                        } else {
                            filetypeCacheRule.setMatch_value("." + fileType.replace(",", ";."));
                        }
                        cacheRules.add(filetypeCacheRule);
                        break;
                    case "https_option":
                        httpGetBody.setCertificate_name("unknown");
                        // http2
                        httpGetBody.setHttp2_status(configMap.getOrDefault("http2", "off"));
                        // ocsp_stapling
                        httpGetBody.setOcsp_stapling_status(configMap.getOrDefault("ocsp_stapling", "off"));
                        break;
                    case "https_tls_version":
                        StringBuilder tlsVersions = new StringBuilder();
                        for (int i = 0; i < 4; i++) {
                            if ("on".equals(configMap.getOrDefault("tls1" + i, "off"))) {
                                if (tlsVersions.length() > 0) {
                                    tlsVersions.append(";");
                                }
                                tlsVersions.append("TLSv1.").append(i);
                            }
                        }
                        httpGetBody.setTls_version(tlsVersions.toString());
                        break;
                    case "http_force":
                        forceRedirect.setStatus("on");
                        forceRedirect.setType("HTTP");
                        forceRedirect.setRedirect_code(configMap.getOrDefault("http_rewrite", "301"));
                        break;
                    case "https_force":
                        forceRedirect.setStatus("on");
                        forceRedirect.setType("HTTPS");
                        forceRedirect.setRedirect_code(configMap.getOrDefault("https_rewrite", "301"));
                        break;
                }
            }
            DescribeDomainCertificateInfoResponse certificateInfoResponse = certificateInfoResponseCompletableFuture.get();
            DescribeDomainCertificateInfoResponseBody.DescribeDomainCertificateInfoResponseBodyCertInfosCertInfo certInfosCertInfo = certificateInfoResponse.getBody().getCertInfos().getCertInfo().get(0);
            if (Assert.notEmpty(certInfosCertInfo) && "on".equals(certInfosCertInfo.getServerCertificateStatus())) {
                domainBasicInfo.setHttpsStatus("1");
                httpGetBody.setHttps_status("on");
                httpGetBody.setCertificate_name(certInfosCertInfo.getCertName());
            }
        } catch (Exception e) {
            throw catchException(e);
        }

        return DomainConfig.builder()
                .domainBasicInfo(domainBasicInfo)
                .domainBackSourceInfo(domainBackSourceInfo)
                .domainCacheInfo(domainCacheInfo)
                .domainVisitInfo(domainVisitInfo)
                .domainAdvancedInfo(domainAdvancedInfo)
                .domainHttpsInfo(domainHttpsInfo)
                .build();
    }

    private String convertSource(DescribeCdnDomainDetailResponseBody.DescribeCdnDomainDetailResponseBodyGetDomainDetailModelSourceModels modelSourceModels) {
        StringBuilder s = new StringBuilder();
        List<DescribeCdnDomainDetailResponseBody.DescribeCdnDomainDetailResponseBodyGetDomainDetailModelSourceModelsSourceModel> sourceModels = modelSourceModels.getSourceModel();
        for (DescribeCdnDomainDetailResponseBody.DescribeCdnDomainDetailResponseBodyGetDomainDetailModelSourceModelsSourceModel sourceModel : sourceModels) {
            if (s.length() > 0) {
                s.append(";");
            }
            switch (sourceModel.getType()) {
                case "domain":
                    s.append("DN");
                    break;
                case "oss":
                    s.append("OSS");
                    break;
                default:
                    s.append("IP");
                    break;
            }
            s.append(":");
            s.append(sourceModel.getContent());
            s.append(":");
            s.append(sourceModel.getPort());
            s.append(":");
            s.append(sourceModel.getWeight());
            s.append(":");
            if ("30".equals(sourceModel.getPriority())) {
                s.append("B");
            } else {
                s.append("A");
            }
        }
        return s.toString();
    }

    private String convertServiceArea(String s) {
        switch (s) {
            case "global":
                return "global";
            case "overseas":
                return "outside_mainland_china";
            default:
                return "mainland_china";
        }
    }

    private String convertDomainStatus(String s) {
        switch (s) {
            case "online":
                return "online";
            case "offline":
                return "offline";
            default:
                return "configuring";
        }
    }

    private String getTopLevelDomain(String domainName) {
        String[] split = domainName.split("\\.");
        return split[split.length - 2] + "." + split[split.length - 1];
    }

    @Override
    public DomainVerifyRecordInfo createVerifyRecord(String domainName) throws BusinessException {
        Client client = getClient();
        DescribeVerifyContentRequest request = new DescribeVerifyContentRequest();
        request.setDomainName(domainName);
        try {
            DescribeVerifyContentResponse response = client.describeVerifyContent(request);
            DescribeVerifyContentResponseBody body = response.getBody();
            String content = body.getContent();
            String topLevelDomain = getTopLevelDomain(domainName);
            return DomainVerifyRecordInfo.builder()
                    .subDomain("verification")
                    .record(content)
                    .recordType("TXT")
                    .fileVerifyUrl("http://" + topLevelDomain + "/verification.html")
                    .fileVerifyDomains(new String[]{topLevelDomain, domainName})
                    .fileVerifyName("verification.html")
                    .content(content)
                    .build();
        } catch (TeaException error) {
            log.error("阿里云CDN查询归属校验内容 {} 失败：{} : {}", domainName, error.getCode(), error.getMessage());
            throw new BusinessException(error.getMessage());
        } catch (Exception e) {
            throw new BusinessException("操作失败，请稍后再试");
        }
    }

    @Override
    public void verifyDomainRecord(String domainName, String verifyType) throws BusinessException {
        Client client = getClient();
        VerifyDomainOwnerRequest request = new VerifyDomainOwnerRequest();
        request.setDomainName(domainName);
        request.setVerifyType(verifyType + "Check");
        try {
            VerifyDomainOwnerResponse response = client.verifyDomainOwner(request);
            VerifyDomainOwnerResponseBody body = response.getBody();
            if (Assert.notEmpty(body.getContent())) {
                throw new BusinessException("验证失败，请检查后重试");
            }
        } catch (TeaException error) {
            log.error("阿里云CDN验证域名 {} 失败：{} : {}", domainName, error.getCode(), error.getMessage());
            throw new BusinessException(AliyunCdnErrorCodeHandler.getErrorDescription(error));
        } catch (Exception e) {
            throw new BusinessException("操作失败，请稍后再试");
        }
    }
}
