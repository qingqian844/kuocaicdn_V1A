package com.kuocai.cdn.api.yifan.cdn;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.util.RuntimeConfigUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class YiFanRequest {
    public static final String TIME_FORMAT = "yyyyMMdd'T'HHmmss'Z'";
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");
    private static final String ALGORITHM = "HMAC-SHA256";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String SERVICE = "CDN";
    private static final String REGION = "cn-north-1";
    private static final String HOST = "cdn.api.yifancloud.cn";
    private static final String CONTENT_TYPE = "application/json";
    private static final String AK = RuntimeConfigUtils.required("yifan.cdn.ak", "YIFAN_CDN_AK");
    private static final String SK = RuntimeConfigUtils.required("yifan.cdn.sk", "YIFAN_CDN_SK");
    private static final CloseableHttpClient httpClient = HttpClients.createDefault();

    private static final OkHttpClient okHttpClient = new OkHttpClient().newBuilder().build();

    private static String getSignature(String stringToSign, String requestDate, String region, String service) throws Exception {
        byte[] kSecret = SK.getBytes("UTF-8");
        byte[] kDate = hmacSha256(kSecret, requestDate.substring(0, 8));
        byte[] kRegion = hmacSha256(kDate, region);
        byte[] kService = hmacSha256(kRegion, service);
        byte[] kSigning = hmacSha256(kService, "request");
        byte[] signature = hmacSha256(kSigning, stringToSign);
        return bytesToHex(signature);
    }

    private static byte[] hmacSha256(byte[] key, String value) throws Exception {
        String algorithm = "HmacSHA256";
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(value.getBytes("UTF8"));
    }

    private static String getCredentialScope(String requestDate) {
        return requestDate.substring(0, 8) + "/" + REGION + "/" + SERVICE + "/request";
    }

    private static String getHexEncodedHash(String value) throws NoSuchAlgorithmException {
        return getHexEncodedHash(value.getBytes());
    }

    private static String getHexEncodedHash(byte[] value) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
        md.update(value);
        byte[] digest = md.digest();
        return bytesToHex(digest);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String getFormattedDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(TIME_FORMAT);
        dateFormat.setTimeZone(UTC_TIME_ZONE);
        return dateFormat.format(date);
    }

    public static JSONObject request(String path, String method, TreeMap<String, String> query, byte[] body) throws Exception {
        // 构建查询参数
        ArrayList<NameValuePair> queryList = new ArrayList<>();
        for (Map.Entry<String, String> entry : query.entrySet()) {
            queryList.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        // 初始化签名结构
        URI uri = new URIBuilder().addParameters(queryList).build();
        // 初始化签名结果变量
        String xDate = getFormattedDate(new Date());
        String shortXDate = xDate.substring(0, 8);
        String xContentSha256 = getHexEncodedHash(body);
        // 构建验签头
        String[] headStr = {"content-type", "host", "x-content-sha256", "x-date"};
        String signedHeadersStr = String.join(";", headStr);
        String[] headStrSecond = {"content-type:" + CONTENT_TYPE, "host:" + HOST, "x-content-sha256:" + xContentSha256, "x-date:" + xDate};
        String preRequestStr = String.join("\n", headStrSecond);
        // HTTPRequestMethod + '\n' + CanonicalURI + '\n' + CanonicalQueryString + '\n' + CanonicalHeaders + '\n' + SignedHeaders + '\n' + HexEncode(Hash(RequestPayload))
        String[] preCanonicalRequestStr = {method, path, uri.getRawQuery(), preRequestStr, "", signedHeadersStr, xContentSha256};
        String canonicalRequestStr = String.join("\n", preCanonicalRequestStr);
        String hashedCanonicalRequest = getHexEncodedHash(canonicalRequestStr.getBytes());
        String credentialScope = getCredentialScope(shortXDate);
        // StringToSign = Algorithm + '\n' + RequestDate + '\n' + CredentialScope + '\n' + HexEncode(Hash(CanonicalRequest))
        String[] preStringToSign = {ALGORITHM, xDate, credentialScope, hashedCanonicalRequest};
        String stringToSign = String.join("\n", preStringToSign);
        String signature = getSignature(stringToSign, shortXDate, REGION, SERVICE);
        log.debug("signature:{}", signature);
        // HMAC-SHA256 Credential = {AccessKey}/{ShortDate}/{Region}/{Service}/{Request}, SignedHeaders={SignedHeaders}, Signature={Signature}
        String authorization = String.format("HMAC-SHA256 Credential=%s, SignedHeaders=%s, Signature=%s", AK + "/" + credentialScope, signedHeadersStr, signature);
        // 处理具体请求。
        String url = "http://" + HOST + path + (StringUtils.isNotBlank(uri.getRawQuery()) ? "?" + uri.getRawQuery() : "");
        return doRequest(url, method, body, xDate, xContentSha256, authorization);
    }

    public static JSONObject doRequest1(String url, String method, byte[] body, String xDate, String xContentSha256, String authorization) {
        HttpRequestBase request = new HttpGet(url);
        if ("POST".equals(method)) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new ByteArrayEntity(body));
            request = httpPost;
        } else if ("DELETE".equals(method)) {
            HttpDelete httpDelete = new HttpDelete(url);
            request = httpDelete;
        } else {
            HttpGet httpGet = new HttpGet(url);
            request = httpGet;
        }
        // 设置经过签名的5个HTTP Header
        request.setHeader("Host", HOST);
        request.setHeader("Content-Type", CONTENT_TYPE);
        request.setHeader("X-Date", xDate);
        request.setHeader("X-Content-Sha256", xContentSha256);
        request.setHeader("Authorization", authorization);
        JSONObject responseBody = null;
        // 发送 HTTP 请求。
        try {
            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            String responseBodyBytes = EntityUtils.toString(entity);
            EntityUtils.consume(entity);
            return JSONObject.parseObject(responseBodyBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONObject doRequest(String url, String method, byte[] body, String xDate, String xContentSha256, String authorization) throws BusinessException, UnsupportedEncodingException {
        OkHttpClient client = new OkHttpClient();
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .header("Host", HOST)
                .header("Content-Type", CONTENT_TYPE)
                .header("X-Date", xDate)
                .header("X-Content-Sha256", xContentSha256)
                .header("Authorization", authorization);
        if ("POST".equals(method)) {
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), body);
            requestBuilder.post(requestBody);
        } else if ("DELETE".equals(method)) {
            requestBuilder.delete();
        } else {
            requestBuilder.get();
        }
        Request request = requestBuilder.build();
        JSONObject responseBody = null;
        try {
            Response response = client.newCall(request).execute();
            ResponseBody responseEntity = response.body();
            if (responseEntity == null) {
                return responseBody;
            }
            responseBody = JSON.parseObject(responseEntity.string());
            if (response.isSuccessful()) {
                return responseBody;
            } else {
                throw new BusinessException("请求失败，失败原因：{}", responseBody.getString("errorMsg"));
            }
        } catch (IOException e) {
            log.error("易凡路线接口请求失败--->[url:{}, method:{}, body:{}, error:{}]", url, method, body, e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        List<String> domainIds = Arrays.asList("64a3d79d53422345319cbb8d");
        request("/api/v1.0/domain/disable", "POST", new TreeMap<>(), JSON.toJSONBytes(domainIds));
    }
}
