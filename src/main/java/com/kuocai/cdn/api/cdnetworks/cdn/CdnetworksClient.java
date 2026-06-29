package com.kuocai.cdn.api.cdnetworks.cdn;

import com.kuocai.cdn.api.cdnetworks.cdn.dto.*;
import com.kuocai.cdn.api.cdnetworks.cdn.properties.CdnetworksCdn;
import com.kuocai.cdn.api.cdnetworks.cdn.vo.*;
import com.kuocai.cdn.exception.CdnetworksException;
import com.kuocai.cdn.util.CryptoUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.*;

import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeSet;

@Slf4j
public class CdnetworksClient {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    private static class Config {
        private String uri;
        private String method;
        private String signedHeaders;
    }

    private static String getSignedHeaders(String signedHeaders) {
        if (StringUtils.isBlank(signedHeaders)) {
            return "content-type;host";
        }
        String[] headers = signedHeaders.split(";");
        TreeSet<String> signedHeaderSet = new TreeSet<>();
        for (String header : headers) {
            signedHeaderSet.add(header.toLowerCase());
        }
        StringBuilder newSignedHeaders = new StringBuilder();
        for (String s : signedHeaderSet) {
            newSignedHeaders.append(s).append(";");
        }
        return newSignedHeaders.substring(0, newSignedHeaders.length() - 1);
    }

    private static String getCanonicalHeaders(Map<String, String> headers, String signedHeaders) {
        String[] headerNames = signedHeaders.split(";");
        StringBuilder canonicalHeaders = new StringBuilder();
        for (String headerName : headerNames) {
            canonicalHeaders.append(headerName).append(":").append(getValueByHeader(headerName, headers).toLowerCase()).append("\n");
        }
        return canonicalHeaders.toString();
    }

    private static String getValueByHeader(String name, Map<String, String> customHeaderMap) {
        String value = null;
        for (Map.Entry<String, String> entry : customHeaderMap.entrySet()) {
            if (StringUtils.equalsIgnoreCase(entry.getKey(), name)) {
                value = entry.getValue();
                break;
            }
        }
        return value;
    }

    private static String getSignature(CdnetworksRequest request, String timestamp) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
        String bodyStr = request.getBody();
        if (HttpGet.METHOD_NAME.equals(request.getMethod()) || HttpDelete.METHOD_NAME.equals(request.getMethod()) || StringUtils.isBlank(bodyStr)) {
            bodyStr = "";
        }
        String hashedRequestPayload = CryptoUtils.sha256Hex(bodyStr);
        String canonicalRequest = request.getMethod() + "\n" +
                request.getUri().split("\\?")[0] + "\n" +
                URLDecoder.decode(request.getQueryString(), CharEncoding.UTF_8) + "\n" +
                getCanonicalHeaders(request.getHeaders(), getSignedHeaders(request.getSignedHeaders())) + "\n" +
                getSignedHeaders(request.getSignedHeaders()) + "\n" + hashedRequestPayload;
        String stringToSign = CdnetworksCdn.HEAD_SIGN_ALGORITHM + "\n" + timestamp + "\n" + CryptoUtils.sha256Hex(canonicalRequest);
        return DatatypeConverter.printHexBinary(CryptoUtils.hmac256(CdnetworksCdn.SecretKey.getBytes(StandardCharsets.UTF_8), stringToSign)).toLowerCase();
    }

    private static String genAuthorization(String signedHeaders, String signature) {
        return CdnetworksCdn.HEAD_SIGN_ALGORITHM + " " + "Credential=" + CdnetworksCdn.AccessKey + ", " + "SignedHeaders=" + signedHeaders + ", " + "Signature=" + signature;
    }

    private static void getAuthAndSetHeaders(CdnetworksRequest request) {
        try {
            String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
            request.putHeader(HttpHeaders.HOST, request.getHost());
            request.putHeader(CdnetworksCdn.HEAD_SIGN_ACCESS_KEY, CdnetworksCdn.AccessKey);
            request.putHeader(CdnetworksCdn.HEAD_SIGN_TIMESTAMP, timeStamp);
            String signature = getSignature(request, timeStamp);
            request.putHeader(HttpHeaders.AUTHORIZATION, genAuthorization(getSignedHeaders(request.getSignedHeaders()), signature));
        } catch (Exception e) {
            log.error("CDNetworks: aksk get authorization fail.", e);
        }
    }

    private static CdnetworksRequest transferHttpRequest(Config config, String jsonBody) {
        CdnetworksRequest request = new CdnetworksRequest();
        request.setUri(config.getUri());
        request.setHost(CdnetworksCdn.END_POINT);
        request.setUrl(CdnetworksCdn.HTTPS_REQUEST_PREFIX + CdnetworksCdn.END_POINT + request.getUri());
        request.setMethod(config.getMethod());
        request.setSignedHeaders(getSignedHeaders(config.getSignedHeaders()));
        String requestMethod = config.getMethod();
        if (requestMethod.equals(HttpPost.METHOD_NAME) || requestMethod.equals(HttpPut.METHOD_NAME) || requestMethod.equals(HttpPatch.METHOD_NAME) || requestMethod.equals(HttpDelete.METHOD_NAME)) {
            request.setBody(jsonBody);
        }
        return request;
    }

    private static CdnetworksHttp.Response invoke(Config config, IRequestDTO requestDTO) throws CdnetworksException {
        try {
            CdnetworksRequest request = transferHttpRequest(config, requestDTO.toJson());
            getAuthAndSetHeaders(request);
            return CdnetworksHttp.call(request);
        } catch (Exception e) {
            log.error("CDNetworks: invoke fail.", e);
            throw new CdnetworksException("api invoke fail.", e);
        }
    }

    /**
     * 新增加速域名
     *
     * @param addCdnDomainDTO AddCdnDomainDTO
     * @return AddCdnDomainVO
     */
    public static AddCdnDomainVO AddCdnDomain(AddCdnDomainDTO addCdnDomainDTO) throws CdnetworksException {
        Config config = Config.builder()
                .uri("/cdnw/api/domain")
                .method(HttpPost.METHOD_NAME)
                .build();
        addCdnDomainDTO.setContractId(CdnetworksCdn.ContractId);
        addCdnDomainDTO.setItemId(CdnetworksCdn.ItemId);
//        addCdnDomainDTO.setReferencedDomainName("cdn.example.com");
        CdnetworksHttp.Response response = invoke(config, addCdnDomainDTO);
        return AddCdnDomainVO.convert(response);
    }

    /**
     * 禁用加速域名
     *
     * @param disableDomainDTO DisableDomainDTO
     * @return DisableDomainVO
     */
    public static DisableDomainVO DisableDomain(DisableDomainDTO disableDomainDTO) throws CdnetworksException {
        String uri = "/api/domain/%s/disable";
        Config config = Config.builder()
                .uri(String.format(uri, disableDomainDTO.getDomainId()))
                .method(HttpPut.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, disableDomainDTO);
        return DisableDomainVO.convert(response);
    }


    /**
     * 启用加速域名
     *
     * @param enableDomainDTO EnableDomainDTO
     * @return EnableDomainVO
     */
    public static EnableDomainVO EnableDomain(EnableDomainDTO enableDomainDTO) throws CdnetworksException {
        String uri = "/api/domain/%s/enable";
        Config config = Config.builder()
                .uri(String.format(uri, enableDomainDTO.getDomainId()))
                .method(HttpPut.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, enableDomainDTO);
        return EnableDomainVO.convert(response);
    }

    /**
     * 删除加速域名
     *
     * @param deleteDomainDTO DeleteDomainDTO
     * @return DeleteDomainVO
     */
    public static DeleteDomainVO DeleteDomain(DeleteDomainDTO deleteDomainDTO) throws CdnetworksException {
        // 这里 域名 和 域名 ID 都可以
        String uri = "/api/domain/%s";
        Config config = Config.builder()
                .uri(String.format(uri, deleteDomainDTO.getDomainName()))
                .method(HttpDelete.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, deleteDomainDTO);
        return DeleteDomainVO.convert(response);
    }

    public static BasicDomainVO BasicDomain(BasicDomainDTO basicDomainDTO) throws CdnetworksException {
        String uri = "/cdnw/api/domain/%s";
        Config config = Config.builder()
                .uri(String.format(uri, basicDomainDTO.getDomainName()))
                .method(HttpGet.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, basicDomainDTO);
        return BasicDomainVO.convert(response);
    }

    public static UpdateDomainVO UpdateDomain(UpdateDomainDTO updateDomainDTO) throws CdnetworksException {
        String uri = "/cdnw/api/domain/%s";
        Config config = Config.builder()
                .uri(String.format(uri, updateDomainDTO.getDomain()))
                .method(HttpPut.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, updateDomainDTO);
        return UpdateDomainVO.convert(response);
    }

    public static QueryHttpHeaderVO QueryHttpHeader(QueryHttpHeaderDTO queryHttpHeaderDTO) throws CdnetworksException {
        String uri = "/api/config/headermodify/%s";
        Config config = Config.builder()
                .uri(String.format(uri, queryHttpHeaderDTO.getDomainName()))
                .method(HttpGet.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, queryHttpHeaderDTO);
        return QueryHttpHeaderVO.convert(response);
    }

    public static QueryCacheTimeVO QueryCacheTime(QueryCacheTimeDTO queryCacheTimeDTO) throws CdnetworksException {
        String uri = "/api/config/cachetime/%s";
        Config config = Config.builder()
                .uri(String.format(uri, queryCacheTimeDTO.getDomainName()))
                .method(HttpGet.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, queryCacheTimeDTO);
        return QueryCacheTimeVO.convert(response);
    }

    public static QueryHttpCodeCacheVO QueryHttpCodeCacheConfig(QueryHttpCodeCacheDTO queryHttpCodeCacheDTO) throws CdnetworksException {
        String uri = "/api/config/httpcodecache/%s";
        Config config = Config.builder()
                .uri(String.format(uri, queryHttpCodeCacheDTO.getDomainName()))
                .method(HttpGet.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, queryHttpCodeCacheDTO);
        return QueryHttpCodeCacheVO.convert(response);
    }

    public static QueryAntiHotlinkingVO QueryAntiHotlinking(QueryAntiHotlinkingDTO queryAntiHotlinkingDTO) throws CdnetworksException {
        String uri = "/api/config/visitcontrol/%s";
        Config config = Config.builder()
                .uri(String.format(uri, queryAntiHotlinkingDTO.getDomainName()))
                .method(HttpGet.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, queryAntiHotlinkingDTO);
        return QueryAntiHotlinkingVO.convert(response);
    }

    public static QueryDomainCertVO QueryDomainCert(QueryDomainCertDTO queryDomainCertDTO) throws CdnetworksException {
        String uri = "/api/config/certificate/%s";
        Config config = Config.builder()
                .uri(String.format(uri, queryDomainCertDTO.getDomain()))
                .method(HttpGet.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, queryDomainCertDTO);
        return QueryDomainCertVO.convert(response);
    }

    public static QueryCompressionVO QueryCompression(QueryCompressionDTO queryCompressionDTO) throws CdnetworksException {
        // todo 未完成
        String uri = "/api/config/compresssetting/%s";
        Config config = Config.builder()
                .uri(String.format(uri, queryCompressionDTO.getDomainName()))
                .method(HttpGet.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, queryCompressionDTO);
        return QueryCompressionVO.convert(response);
    }

    public static QueryHttp2SettingsVO QueryHttp2Settings(QueryHttp2SettingsDTO queryHttp2SettingsDTO) throws CdnetworksException {
        String uri = "/api/config/http2/%s";
        Config config = Config.builder()
                .uri(String.format(uri, queryHttp2SettingsDTO.getDomain()))
                .method(HttpGet.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, queryHttp2SettingsDTO);
        return QueryHttp2SettingsVO.convert(response);
    }

    public static UpdateHttp2SettingsVO UpdateHttp2Settings(UpdateHttp2SettingsDTO updateHttp2SettingsDTO) throws CdnetworksException {
        String uri = "/api/config/http2/%s";
        Config config = Config.builder()
                .uri(String.format(uri, updateHttp2SettingsDTO.getDomainName()))
                .method(HttpPut.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, updateHttp2SettingsDTO);
        return UpdateHttp2SettingsVO.convert(response);
    }

    public static UpdateOriginVO UpdateOrigin(UpdateOriginDTO updateOriginDTO) throws CdnetworksException {
        String uri = "/api/domain/property/%s";
        Config config = Config.builder()
                .uri(String.format(uri, updateOriginDTO.getDomain()))
                .method(HttpPut.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, updateOriginDTO);
        return UpdateOriginVO.convert(response);
    }

    public static QueryOriginProtocolVO QueryOriginProtocol(QueryOriginProtocolDTO queryOriginProtocolDTO) throws CdnetworksException {
        String uri = "/api/config/back2originrewrite/%s";
        Config config = Config.builder()
                .uri(String.format(uri, queryOriginProtocolDTO.getDomain()))
                .method(HttpGet.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, queryOriginProtocolDTO);
        return QueryOriginProtocolVO.convert(response);
    }

    public static UpdateOriginProtocolVO UpdateOriginProtocol(UpdateOriginProtocolDTO updateOriginProtocolDTO) throws CdnetworksException {
        String uri = "/api/config/back2originrewrite/%s";
        Config config = Config.builder()
                .uri(String.format(uri, updateOriginProtocolDTO.getDomain()))
                .method(HttpPut.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, updateOriginProtocolDTO);
        return UpdateOriginProtocolVO.convert(response);
    }

    public static UpdateCacheTimeVO UpdateCacheTime(UpdateCacheTimeDTO updateCacheTimeDTO) throws CdnetworksException {
        String uri = "/api/config/cachetime/%s";
        Config config = Config.builder()
                .uri(String.format(uri, updateCacheTimeDTO.getDomainName()))
                .method(HttpPut.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, updateCacheTimeDTO);
        return UpdateCacheTimeVO.convert(response);
    }

    public static UpdateHttpCodeCacheVO UpdateHttpCodeCache(UpdateHttpCodeCacheDTO updateHttpCodeCacheDTO) throws CdnetworksException {
        String uri = "/api/config/httpcodecache/%s";
        Config config = Config.builder()
                .uri(String.format(uri, updateHttpCodeCacheDTO.getDomainName()))
                .method(HttpPut.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, updateHttpCodeCacheDTO);
        return UpdateHttpCodeCacheVO.convert(response);
    }

    public static UpdateAntiHotlinkingVO UpdateAntiHotlinking(UpdateAntiHotlinkingDTO updateAntiHotlinkingDTO) throws CdnetworksException {
        String uri = "/api/config/visitcontrol/%s";
        Config config = Config.builder()
                .uri(String.format(uri, updateAntiHotlinkingDTO.getDomainName()))
                .method(HttpPut.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, updateAntiHotlinkingDTO);
        return UpdateAntiHotlinkingVO.convert(response);
    }

    public static UpdateHttpHeaderVO UpdateHttpHeader(UpdateHttpHeaderDTO updateHttpHeaderDTO) throws CdnetworksException {
        String uri = "/api/config/headermodify/%s";
        Config config = Config.builder()
                .uri(String.format(uri, updateHttpHeaderDTO.getDomainName()))
                .method(HttpPut.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, updateHttpHeaderDTO);
        return UpdateHttpHeaderVO.convert(response);
    }

    public static UpdateDomainCertVO UpdateDomainCert(UpdateDomainCertDTO updateDomainCertDTO) throws CdnetworksException {
        String uri = "/api/config/certificate/%s";
        Config config = Config.builder()
                .uri(String.format(uri, updateDomainCertDTO.getDomain()))
                .method(HttpPut.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, updateDomainCertDTO);
        return UpdateDomainCertVO.convert(response);
    }

    public static AddCertVO AddCert(AddCertDTO addCertDTO) throws CdnetworksException {
        String uri = "/api/certificate";
        Config config = Config.builder()
                .uri(uri)
                .method(HttpPost.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, addCertDTO);
        return AddCertVO.convert(response);
    }

    public static QueryInnerRedirectVO QueryInnerRedirect(QueryInnerRedirectDTO queryInnerRedirectDTO) throws CdnetworksException {
        String uri = "/api/config/InnerRedirect/%s";
        Config config = Config.builder()
                .uri(String.format(uri, queryInnerRedirectDTO.getDomainName()))
                .method(HttpGet.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, queryInnerRedirectDTO);
        return QueryInnerRedirectVO.convert(response);
    }

    public static UpdateInnerRedirectVO UpdateInnerRedirect(UpdateInnerRedirectDTO updateInnerRedirectDTO) throws CdnetworksException {
        String uri = "/api/config/InnerRedirect/%s";
        Config config = Config.builder()
                .uri(String.format(uri, updateInnerRedirectDTO.getDomainName()))
                .method(HttpPut.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, updateInnerRedirectDTO);
        return UpdateInnerRedirectVO.convert(response);
    }

    public static QueryTotalTrafficVO QueryTotalTraffic(QueryTotalTrafficDTO queryTotalTrafficDTO) throws CdnetworksException {
        String uri = "/api/report/domainflow";
        String query = "?datefrom=%s&dateto=%s&type=%s";

        try {
            query = String.format(query, URLEncoder.encode(queryTotalTrafficDTO.getDatefrom(), StandardCharsets.UTF_8.toString()), URLEncoder.encode(queryTotalTrafficDTO.getDateto(), StandardCharsets.UTF_8.toString()), URLEncoder.encode(queryTotalTrafficDTO.getType(), StandardCharsets.UTF_8.toString()));
        } catch (UnsupportedEncodingException e) {
            throw new CdnetworksException("QueryTotalTraffic: encode query params fail.", e);
        }

        uri = uri + query;

        Config config = Config.builder()
                .uri(uri)
                .method(HttpPost.METHOD_NAME)
                .build();
        log.info("QueryTotalTraffic: domainList={}", queryTotalTrafficDTO.getDomainList().toJson());
        CdnetworksHttp.Response response = invoke(config, queryTotalTrafficDTO);
        return QueryTotalTrafficVO.convert(response);
    }

    public static QueryBandwidthVO QueryBandwidth(QueryBandwidthDTO queryBandwidthDTO) throws CdnetworksException {
        String uri = "/api/report/domainbandwidth";
        String query = "?datefrom=%s&dateto=%s&type=%s";

        try {
            query = String.format(query, URLEncoder.encode(queryBandwidthDTO.getDatefrom(), StandardCharsets.UTF_8.toString()), URLEncoder.encode(queryBandwidthDTO.getDateto(), StandardCharsets.UTF_8.toString()), URLEncoder.encode(queryBandwidthDTO.getType(), StandardCharsets.UTF_8.toString()));
        } catch (UnsupportedEncodingException e) {
            throw new CdnetworksException("QueryBandwidth: encode query params fail.", e);
        }

        uri = uri + query;

        Config config = Config.builder()
                .uri(uri)
                .method(HttpPost.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, queryBandwidthDTO);
        return QueryBandwidthVO.convert(response);
    }

    public static CreatePurgeVO CreatePurge(CreatePurgeDTO createPurgeDTO) throws CdnetworksException {
        String uri = "/ccm/purge/ItemIdReceiver";
        Config config = Config.builder()
                .uri(uri)
                .method(HttpPost.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, createPurgeDTO);
        return CreatePurgeVO.convert(response);
    }

    public static CreatePrefetchVO CreatePrefetch(CreatePrefetchDTO createPrefetchDTO) throws CdnetworksException {
        String uri = "/cdn/prefetches";
        Config config = Config.builder()
                .uri(uri)
                .method(HttpPost.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, createPrefetchDTO);
        return CreatePrefetchVO.convert(response);
    }

    public static QueryPrefetchVO QueryPrefetch(QueryPrefetchDTO queryPrefetchDTO) throws CdnetworksException {
        String uri = "/cdn/prefetches/%s";
        Config config = Config.builder()
                .uri(String.format(uri, queryPrefetchDTO.getId()))
                .method(HttpGet.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, queryPrefetchDTO);
        return QueryPrefetchVO.convert(response);
    }

    public static QueryPurgeVO QueryPurge(QueryPurgeDTO queryPurgeDTO) throws CdnetworksException {
        String uri = "/ccm/purge/ItemIdQuery";
        Config config = Config.builder()
                .uri(uri)
                .method(HttpPost.METHOD_NAME)
                .build();
        CdnetworksHttp.Response response = invoke(config, queryPurgeDTO);
        return QueryPurgeVO.convert(response);
    }

    public static void main(String[] args) {
//        CdnetworksCdn.AccessKey = "bzqv7XjPMSYwLJywSfMTpMcXmPYm8H1WJBmr";
//        CdnetworksCdn.SecretKey = "mxQ3h0QVouUEYNtij0oU3mgZv36CGySDbfu5fpQKEwAGFtiZGS17svWcfSPhtTrs";

//        AddCdnDomainDTO addCdnDomainDTO = new AddCdnDomainDTO();
//        addCdnDomainDTO.setDomainName("abc.20mo.cn");
//        AddCdnDomainDTO.AddCdnDomainRequestOriginConfig originConfig = new AddCdnDomainDTO.AddCdnDomainRequestOriginConfig();
//        originConfig.setDefaultOriginHostHeader("cdn.jsdelivr.net");
//        originConfig.setOriginIps("cdn.jsdelivr.net");
//        originConfig.setOriginPort("443");
//        addCdnDomainDTO.setOriginConfig(originConfig);
//        AddCdnDomainVO addCdnDomainVO = AddCdnDomain(addCdnDomainDTO);
//        System.out.println(addCdnDomainVO);

//        DisableDomainDTO dto = new DisableDomainDTO();
////        dto.setDomainName("img.20mo.cn");
//        dto.setDomainId("4890170");
//        DisableDomainVO vo = DisableDomain(dto);
//        System.out.println(vo);

//        DeleteDomainDTO dto = new DeleteDomainDTO();
////        dto.setDomainName("img.20mo.cn");
//        dto.setDomainId("4890169");
//        DeleteDomainVO vo = DeleteDomain(dto);
//        System.out.println(vo);

//        BasicDomainDTO dto = new BasicDomainDTO();
//        dto.setDomainName("abc.20mo.cn");
//        BasicDomainVO vo = BasicDomain(dto);
//        System.out.println(vo);

//        UpdateDomainDTO dto = new UpdateDomainDTO();
//        dto.setDomain("fastly.20mo.cn");
//        System.out.println(dto.toJson());
//        UpdateDomainVO vo = UpdateDomain(dto);
//        System.out.println(vo);

//        QueryHttpHeaderConfigDTO dto = new QueryHttpHeaderConfigDTO();
//        dto.setDomainName("abc.20mo.cn");
//        QueryHttpHeaderConfigVO vo = QueryHttpHeaderConfig(dto);
//        System.out.println(vo);

//        QueryCacheTimeDTO dto = new QueryCacheTimeDTO();
//        dto.setDomainName("abc.20mo.cn");
//        QueryCacheTimeVO vo = QueryCacheTime(dto);
//        System.out.println(vo);

//        QueryHttpCodeCacheDTO dto = new QueryHttpCodeCacheDTO();
//        dto.setDomainName("abc.20mo.cn");
//        QueryHttpCodeCacheVO vo = QueryHttpCodeCacheConfig(dto);
//        System.out.println(vo);

//        QueryAntiHotlinkingDTO dto = new QueryAntiHotlinkingDTO();
//        dto.setDomainName("abc.20mo.cn");
//        QueryAntiHotlinkingVO vo = QueryAntiHotlinking(dto);
//        System.out.println(vo);

//        QueryDomainCertDTO dto = new QueryDomainCertDTO();
//        dto.setDomain("fastly.20mo.cn");
//        QueryDomainCertVO vo = QueryDomainCert(dto);
//        System.out.println(vo);

//        QueryCompressionDTO dto = new QueryCompressionDTO();
//        dto.setDomainName("fastly.20mo.cn");
//        QueryCompressionVO vo = QueryCompression(dto);
//        System.out.println(vo);

//        QueryHttp2SettingsDTO dto = new QueryHttp2SettingsDTO();
//        dto.setDomain("abc.20mo.cn");
//        QueryHttp2SettingsVO vo = QueryHttp2Settings(dto);
//        System.out.println(vo);

//        UpdateHttp2SettingsDTO dto = new UpdateHttp2SettingsDTO();
//        dto.setDomainName("fastly.20mo.cn");
//        dto.setHttp2Settings(UpdateHttp2SettingsDTO.Http2Settings.EnableHttp2());
//        UpdateHttp2SettingsVO vo = UpdateHttp2Settings(dto);
//        System.out.println(vo);

//        QueryOriginProtocolDTO dto = new QueryOriginProtocolDTO();
//        dto.setDomain("abc.20mo.cn");
//        QueryOriginProtocolVO vo = QueryOriginProtocol(dto);
//        System.out.println(vo);

//        QueryTotalTrafficDTO dto = new QueryTotalTrafficDTO();
//        // yyyy-MM-ddTHH:mm:ss+08:00
//        dto.setDatefrom("2024-03-10T00:00:00%2B08:00");
//        dto.setDateto("2024-03-18T00:00:00%2B08:00");
//        dto.setType("daily");
//        QueryTotalTrafficDTO.DomainList domainList = new QueryTotalTrafficDTO.DomainList();
//        List<String> domainName = Collections.singletonList("api.iowen.cn");
//        domainList.setDomainName(domainName);
//        dto.setDomainList(domainList);
//        QueryTotalTrafficVO vo = QueryTotalTraffic(dto);
//        System.out.println(vo);
    }
}
