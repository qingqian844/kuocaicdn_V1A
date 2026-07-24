package com.kuocai.cdn.api.kingsoft.cdn;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.api.kingsoft.cdn.utils.KingsoftAwsSignatureUtil;
import com.kuocai.cdn.api.kingsoft.cdn.properties.KingsoftCdn;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
public class KingsoftApiService {

    private static final String CDN_API_SCHEME = "https";
    private static final String CDN_API_HOST = "cdn.api.ksyun.com";

    private static final Set<String> JSON_ACTIONS = new HashSet<>(Arrays.asList(
            "GetDomainAuthContent", "AuthDomainOwner", "PreloadCaches", "RefreshCaches",
            "GetRefreshOrPreloadTask", "SetCacheRuleConfig", "DeleteHttpHeadersConfig",
            "SetHttpHeadersConfig", "SetPageCompressConfig", "SetBrCompressConfig",
            "ConfigCertificate", "SetCertificate", "SetHttp2OptionConfig", "SetForceRedirectConfig",
            "SetRequestAuthConfig", "SetOriginAdvancedConfig", "SetTlsVersionConfig", "SetOcspStaplingConfig",
            "SetErrorPageConfig", "GetErrorPageConfig"
    ));

    public JSONObject callKingsoftApi(String action, String version, Map<String, String> params) throws BusinessException {
        String path = findPathForAction(action);
        try {
            String timestamp = KingsoftAwsSignatureUtil.generateTimestamp();
            String date = timestamp.substring(0, 8);

            Map<String, String> allParamsForSign = new TreeMap<>();
            allParamsForSign.put("Action", action);
            allParamsForSign.put("Version", version);
            if(params != null) {
                allParamsForSign.putAll(params);
            }
            allParamsForSign.put("X-Amz-Algorithm", "AWS4-HMAC-SHA256");
            allParamsForSign.put("X-Amz-Credential", KingsoftCdn.AccessKey + "/" + date + "/" + KingsoftCdn.Region + "/cdn/aws4_request");
            allParamsForSign.put("X-Amz-Date", timestamp);
            allParamsForSign.put("X-Amz-Expires", "300");
            allParamsForSign.put("X-Amz-SignedHeaders", "host;x-amz-date");

            Map<String, String> signHeaders = new TreeMap<>();
            signHeaders.put("host", CDN_API_HOST);
            signHeaders.put("x-amz-date", timestamp);

            String canonicalQueryString = buildEncodedQueryString(allParamsForSign);

            String canonicalHeaders = signHeaders.entrySet().stream()
                    .map(e -> e.getKey() + ":" + e.getValue().trim())
                    .collect(Collectors.joining("\n")) + "\n";

            String canonicalRequest = "GET\n" + urlEncodePath(path) + "\n" + canonicalQueryString + "\n" + canonicalHeaders + "\n" + "host;x-amz-date\n" + "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

            String stringToSign = "AWS4-HMAC-SHA256\n" + timestamp + "\n" + date + "/" + KingsoftCdn.Region + "/cdn/aws4_request\n" + KingsoftAwsSignatureUtil.bytesToHex(java.security.MessageDigest.getInstance("SHA-256").digest(canonicalRequest.getBytes(StandardCharsets.UTF_8)));

            byte[] signingKey = KingsoftAwsSignatureUtil.getSigningKey(KingsoftCdn.SecretKey, date, KingsoftCdn.Region, "cdn");
            String signature = KingsoftAwsSignatureUtil.sign(signingKey, stringToSign);

            allParamsForSign.put("X-Amz-Signature", signature);
            URI uri = buildEncodedGetUri(CDN_API_SCHEME, CDN_API_HOST, -1, path, allParamsForSign);

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet httpGet = new HttpGet(uri);
                httpGet.setHeader("Host", CDN_API_HOST);
                httpGet.setHeader("X-Amz-Date", timestamp);
                httpGet.setHeader("X-Action", action);
                httpGet.setHeader("X-Version", version);

                String requestTarget = CDN_API_SCHEME + "://" + CDN_API_HOST + path;
                log.info("Kingsoft CDN API GET Request: action={}, target={}", action, requestTarget);
                return executeRequest(httpClient, httpGet, requestTarget, "");
            }
        } catch (Exception e) {
            log.error("An unknown error occurred while calling Kingsoft CDN GET API", e);
            throw new BusinessException("Internal error while calling Kingsoft GET API: " + e.getMessage());
        }
    }

    public JSONObject postKingsoftApi(String action, String version, String path, Map<String, Object> body) throws BusinessException {
        boolean useJson = JSON_ACTIONS.contains(action);

        validateCredentials();
        try {
            String timestamp = KingsoftAwsSignatureUtil.generateTimestamp();
            URI uri = new URIBuilder().setScheme(CDN_API_SCHEME).setHost(CDN_API_HOST).setPath(path).build();

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(uri);
                String requestBody = "";
                Map<String, String> signHeaders = new TreeMap<>();

                signHeaders.put("host", CDN_API_HOST);
                signHeaders.put("x-amz-date", timestamp);

                if (useJson) {
                    requestBody = (body == null) ? "{}" : JSON.toJSONString(body);
                    httpPost.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
                    signHeaders.put("content-type", "application/json");
                } else {
                    Map<String, String> allParams = new TreeMap<>();
                    if (body != null) {
                        for (Map.Entry<String, Object> entry : body.entrySet()) {
                            if (entry.getValue() != null) {
                                allParams.put(entry.getKey(), String.valueOf(entry.getValue()));
                            }
                        }
                    }

                    List<org.apache.http.NameValuePair> formParams = new ArrayList<>();
                    allParams.forEach((k, v) -> formParams.add(new BasicNameValuePair(k, v)));

                    UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(formParams, "UTF-8");
                    requestBody = EntityUtils.toString(formEntity, "UTF-8");
                    httpPost.setEntity(formEntity);
                    signHeaders.put("content-type", "application/x-www-form-urlencoded");
                }

                String authorizationHeaderValue = KingsoftAwsSignatureUtil.generateAuthorizationHeader(
                        "POST", path, KingsoftCdn.Region, Collections.emptyMap(),
                        signHeaders, requestBody, KingsoftCdn.AccessKey, KingsoftCdn.SecretKey
                );

                httpPost.setHeader("Authorization", authorizationHeaderValue);
                signHeaders.forEach((k, v) -> {
                    if (k.equals("content-type")) httpPost.setHeader("Content-Type", v);
                    else if (k.equals("host")) httpPost.setHeader("Host", v);
                    else if (k.equals("x-amz-date")) httpPost.setHeader("X-Amz-Date", v);
                });
                httpPost.setHeader("X-Action", action);
                httpPost.setHeader("X-Version", version);

                log.info("Kingsoft CDN API POST Request URL: {}", uri);
                log.debug("Kingsoft CDN API POST Request Headers: {}", Arrays.toString(httpPost.getAllHeaders()));
                log.debug("Kingsoft CDN API POST Request Body: {}", requestBody);

                return executeRequest(httpClient, httpPost, uri.toString(), requestBody);
            }
        } catch (Exception e) {
            log.error("An unknown error occurred while calling Kingsoft CDN API (POST)", e);
            throw new BusinessException("Internal error while calling Kingsoft POST API: " + e.getMessage());
        }
    }

    private void validateCredentials() throws BusinessException {
        if (Assert.isEmpty(KingsoftCdn.AccessKey) || Assert.isEmpty(KingsoftCdn.SecretKey)) {
            throw new BusinessException("金山云 AccessKey 或 SecretKey 未加载，请先在后台系统设置中保存金山云 CDN 配置");
        }
    }


    private JSONObject executeRequest(CloseableHttpClient client, HttpUriRequest request, String url, String body) throws Exception {
        try (CloseableHttpResponse response = client.execute(request)) {
            HttpEntity responseEntity = response.getEntity();
            String responseBody = responseEntity != null ? EntityUtils.toString(responseEntity) : null;
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode >= 200 && statusCode < 300) {
                log.info("Kingsoft CDN API Response [{}]: {}", statusCode, responseBody);
                if (Assert.isEmpty(responseBody)) {
                    return new JSONObject();
                }
                return JSON.parseObject(responseBody);
            } else {
                log.error("Kingsoft CDN API HTTP Error: {} - {}, Request URL: {}, Request Body: {}",
                        statusCode, responseBody, url, body);
                if (!Assert.isEmpty(responseBody)) {
                    String apiErrorMessage = null;
                    try {
                        JSONObject errorJson = JSON.parseObject(responseBody);
                        JSONObject error = null;
                        if (errorJson.containsKey("Error")) {
                            error = errorJson.getJSONObject("Error");
                        } else if (errorJson.containsKey("error")) {
                            error = errorJson.getJSONObject("error");
                        }
                        if (error != null) {
                            String errorCode = Optional.ofNullable(error.getString("Code")).orElse(error.getString("code"));
                            String errorMessage = Optional.ofNullable(error.getString("Message")).orElse(error.getString("message"));
                            if (!Assert.isEmpty(errorCode) || !Assert.isEmpty(errorMessage)) {
                                apiErrorMessage = (Assert.isEmpty(errorCode) ? "" : errorCode + ": ") +
                                        (Assert.isEmpty(errorMessage) ? "" : errorMessage);
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    if (!Assert.isEmpty(apiErrorMessage)) {
                        throw new BusinessException(apiErrorMessage);
                    }
                }
                throw new BusinessException("Kingsoft CDN API call failed: HTTP " + statusCode + " - " + responseBody);
            }
        }
    }

    private String findPathForAction(String action) {
        switch (action) {
            case "GetCdnDomains":
                return "/2019-06-01/domain/GetCdnDomains";
            case "GetCdnDomainBasicInfo":
                return "/2016-09-01/domain/GetCdnDomainBasicInfo";
            case "GetDomainConfigs":
                return "/2016-09-01/domain/GetDomainConfigs";
            case "ModifyCdnDomainBasicInfo":
                return "/2016-09-01/domain/ModifyCdnDomainBasicInfo";
            case "StartStopCdnDomain":
                return "/2016-09-01/domain/StartStopCdnDomain";
            case "DeleteCdnDomain":
                return "/2016-09-01/domain/DeleteCdnDomain";
            case "AddCdnDomain":
                return "/V3/AddCdnDomain";
            case "ConfigCertificate":
                return "/2016-09-01/cert/ConfigCertificate";
            case "SetCertificate":
                return "/2016-09-01/cert/SetCertificate";
            case "GetCertificates":
                return "/2016-09-01/cert/GetCertificates";
            case "SetHttp2OptionConfig":
                return "/2016-09-01/domain/SetHttp2OptionConfig";
            case "SetForceRedirectConfig":
                return "/2016-09-01/domain/SetForceRedirectConfig";
            case "SetCacheRuleConfig":
                return "/2016-09-01/domain/SetCacheRuleConfig";
            case "SetReferProtectionConfig":
                return "/2016-09-01/domain/SetReferProtectionConfig";
            case "SetIpProtectionConfig":
                return "/2016-09-01/domain/SetIpProtectionConfig";
            case "DeleteHttpHeadersConfig":
                return "/2016-09-01/domain/DeleteHttpHeadersConfig";
            case "SetHttpHeadersConfig":
                return "/2016-09-01/domain/SetHttpHeadersConfig";
            case "SetPageCompressConfig":
                return "/2016-09-01/domain/SetPageCompressConfig";
            case "SetBrCompressConfig":
                return "/2021-12-01/domain/SetBrCompressConfig";
            case "SetVideoSeekConfig":
                return "/2016-09-01/domain/SetVideoSeekConfig";
            case "SetTlsVersionConfig":
                return "/2016-09-01/domain/SetTlsVersionConfig";
            case "SetOcspStaplingConfig":
                return "/2016-09-01/domain/SetOcspStaplingConfig";
            case "SetLogConfig":
                return "/2016-09-01/domain/SetLogConfig";
            case "GetLogConfig":
                return "/2016-09-01/domain/GetLogConfig";
            case "SetDomainLogService":
                return "/2016-09-01/log/SetDomainLogService";
            case "GetDomainLogService":
                return "/2016-09-01/log/GetDomainLogService";
            case "GetDomainAuthContent":
                return "/2020-06-30/domain/GetDomainAuthContent";
            case "AuthDomainOwner":
                return "/2020-06-30/domain/AuthDomainOwner";
            case "GetBandwidthData":
                return "/2016-09-01/statistics/GetBandwidthData";
            case "GetFlowData":
                return "/2016-09-01/statistics/GetFlowData";
            case "GetServerData":
                return "/2020-06-30/statistics/GetServerData";
            case "GetPvData":
                return "/2016-09-01/statistics/GetPvData";
            case "GetHttpCodeData":
                return "/2016-09-01/statistics/GetHttpCodeData";
            case "GetSrcHttpCodeData":
                return "/2016-09-01/statistics/GetSrcHttpCodeData";
            case "GetTopUrlData":
                return "/2016-09-01/statistics/GetTopUrlData";
            case "GetHitRateData":
                return "/2016-09-01/statistics/GetHitRateData";
            case "PreloadCaches":
                return "/2016-09-01/content/PreloadCaches";
            case "RefreshCaches":
                return "/2016-09-01/content/RefreshCaches";
            case "GetRefreshOrPreloadTask":
                return "/2016-09-01/content/GetRefreshOrPreloadTask";
            case "SetErrorPageConfig":
                return "/2016-09-01/domain/SetErrorPageConfig";
            case "GetErrorPageConfig":
                return "/2016-09-01/domain/GetErrorPageConfig";
            default:
                throw new IllegalArgumentException("Unsupported API action: " + action);
        }
    }

    static URI buildEncodedGetUri(String scheme, String host, int port, String path, Map<String, String> params) {
        String authority = host;
        if (port > 0 && !("https".equalsIgnoreCase(scheme) && port == 443)
                && !("http".equalsIgnoreCase(scheme) && port == 80)) {
            authority += ":" + port;
        }
        String query = buildEncodedQueryString(params);
        return URI.create(scheme + "://" + authority + urlEncodePath(path)
                + (query.isEmpty() ? "" : "?" + query));
    }

    static String buildEncodedQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        return params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> urlEncodeValue(e.getKey()) + "=" + urlEncodeValue(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String urlEncodePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return Arrays.stream(path.split("/", -1))
                .map(KingsoftApiService::urlEncodeValue)
                .collect(Collectors.joining("/"));
    }

    private static String urlEncodeValue(String value) {
        if (value == null) {
            return "";
        }
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20")
                    .replace("%7E", "~")
                    .replace("*", "%2A");
        } catch (Exception e) {
            throw new RuntimeException("Failed to URL encode value: " + value, e);
        }
    }
}
