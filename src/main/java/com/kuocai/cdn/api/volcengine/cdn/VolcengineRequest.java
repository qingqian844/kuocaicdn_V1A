package com.kuocai.cdn.api.volcengine.cdn;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.exception.CdnVolcengineException;
import com.kuocai.cdn.util.Assert;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.kuocai.cdn.api.volcengine.cdn.properties.VolcengineCdn.*;

/**
 * 通用火山云请求
 */
@Slf4j
public class VolcengineRequest {

    private static final Map<String, String> EMPTY_MAP = new HashMap<>();

    /**
     * 发送火山云请求
     *
     * @param action      API 名称
     * @param requestBody 请求体
     * @return 响应
     */
    public static JSONObject doRequest(String action, Map<String, Object> requestBody) throws URISyntaxException, CdnVolcengineException {
        return doRequest(action, EMPTY_MAP, EMPTY_MAP, requestBody);
    }

    /**
     * 发送火山云请求
     *
     * @param action      API 名称
     * @param requestBody 请求体
     * @return 响应
     */
    public static JSONObject doRequest(String action, JSONObject requestBody) throws URISyntaxException, CdnVolcengineException {
        return doRequest(action, EMPTY_MAP, EMPTY_MAP, requestBody);
    }

    /**
     * 发送火山云请求
     *
     * @param action      API 名称
     * @param query       查询参数
     * @param header      请求头
     * @param requestBody 请求体
     * @return 响应
     */
    public static JSONObject doRequest(String action, Map<String, String> query, Map<String, String> header, Map<String, Object> requestBody) throws CdnVolcengineException {
        byte[] body = JSONObject.toJSONBytes(requestBody);
        ArrayList<NameValuePair> nameValuePairs = new ArrayList<>();
        for (Map.Entry<String, String> entry : query.entrySet()) {
            nameValuePairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        nameValuePairs.add(new BasicNameValuePair("Action", action));
        nameValuePairs.add(new BasicNameValuePair("Version", Version));

        // 初始化签名结构
        RequestParam requestParam = new RequestParam(body, "POST", new Date(), "/", Host, "application/json", nameValuePairs);

        URI uri = null;
        try {
            uri = new URIBuilder().addParameters(requestParam.queryList).build();
        } catch (URISyntaxException e) {
            throw new CdnVolcengineException(e.getMessage()).setCause(e);
        }
        // 第五步：接下来开始计算签名
        // 初始化签名结果变量
        String xDate = getAppointFormatDate(new Date());
        String shortXDate = xDate.substring(0, 8);
        String xContentSha256 = hashSHA256(body);
        // 第六步：计算签名
        String[] headStr = {"content-type", "host", "x-content-sha256", "x-date"};
        String signedHeadersStr = String.join(";", headStr);
        String[] headStrSecond = {"content-type:" + requestParam.contentType, "host:" + requestParam.host, "x-content-sha256:" + xContentSha256, "x-date:" + xDate};
        String preRequestStr = String.join("\n", headStrSecond);
        String[] preCanonicalRequestStr = {requestParam.method, requestParam.path, uri.getRawQuery(), preRequestStr, "", signedHeadersStr, xContentSha256};
        String canonicalRequestStr = String.join("\n", preCanonicalRequestStr);
        String hashedCanonicalRequest = hashSHA256(canonicalRequestStr.getBytes());
        String[] credentialStr = {shortXDate, Region, Service, "request"};
        String credentialScope = String.join("/", credentialStr);

        String[] preStringToSign = {"HMAC-SHA256", xDate, credentialScope, hashedCanonicalRequest};
        String stringToSign = String.join("\n", preStringToSign);
        byte[] kDate = hmacSHA256(SK.getBytes(), shortXDate);
        byte[] kRegion = hmacSHA256(kDate, Region);
        byte[] kService = hmacSHA256(kRegion, Service);
        byte[] kSigning = hmacSHA256(kService, "request");
        String signature = Hex.encodeHexString(hmacSHA256(kSigning, stringToSign));
        String authorization = String.format("HMAC-SHA256 Credential=%s, SignedHeaders=%s, Signature=%s", AK + "/" + credentialScope, signedHeadersStr, signature);

        // 第七步，在request中，创建一个 HTTP 请求实例。
        HttpPost request = new HttpPost("https://" + requestParam.host + requestParam.path + "?" + uri.getRawQuery());
        // 第八步：将 Signature 签名写入HTTP Header 中，并发送 HTTP 请求。
        // 设置经过签名的5个HTTP Header
        request.setEntity(new ByteArrayEntity(body));
        for (Map.Entry<String, String> entry : header.entrySet()) {
            request.setHeader(entry.getKey(), entry.getValue());
        }
        request.setHeader("Host", requestParam.host);
        request.setHeader("Content-Type", requestParam.contentType);
        request.setHeader("X-Date", xDate);
        request.setHeader("X-Content-Sha256", xContentSha256);
        request.setHeader("Authorization", authorization);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        byte[] responseBody = null;
        // 发送 HTTP 请求。
        try {
            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            responseBody = EntityUtils.toByteArray(entity);
            EntityUtils.consume(entity);
        } catch (IOException e) {
            throw new CdnVolcengineException(e.getMessage());
        }
        return dealResponse(responseBody);
    }

    /**
     * 处理响应对象
     *
     * @param responseBody 响应
     * @return 响应的JSONObject
     */
    public static JSONObject dealResponse(byte[] responseBody) throws CdnVolcengineException {
        JSONObject responseObject = JSONObject.parseObject(new String(responseBody, StandardCharsets.UTF_8));
        if (Assert.isEmpty(responseObject)) {
            throw new CdnVolcengineException("调用火山云接口失败！响应内容为空！");
        }
        JSONObject responseMetadata = responseObject.getJSONObject("ResponseMetadata");
        JSONObject error = responseMetadata.getJSONObject("Error");
        if (Assert.notEmpty(error)) {
            String errorCode = error.getString("Code");
            String errorMsg = error.getString("Message");
            String msg = VolcengineErrorCodeMap.getMsg(errorCode);
            log.error("【{}】【{}】【{}】", errorCode, msg, errorMsg);
            errorMsg = Assert.isEmpty(msg) ? errorMsg : msg;
            throw new CdnVolcengineException(errorMsg);
        }
        return responseObject.getJSONObject("Result");
    }

    /**
     * sha256非对称加密
     */
    public static byte[] hmacSHA256(byte[] key, String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(content.getBytes());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * sha256 hash算法
     */
    public static String hashSHA256(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Hex.encodeHexString(md.digest(content));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取请求日期格式
     */
    private static String getAppointFormatDate(Date date) {
        DateFormat df = new SimpleDateFormat(TIME_FORMAT_V4);
        df.setTimeZone(tz);
        return df.format(date);
    }

    /**
     * 签算请求结构类
     */
    public static class RequestParam {
        public final byte[] body;
        public final String method;
        public final Date date;
        public final String path;
        public final String host;
        public final String contentType;
        public final ArrayList<NameValuePair> queryList;

        public RequestParam(byte[] body, String method, Date date, String path, String host, String contentType, ArrayList<NameValuePair> queryList) {
            this.body = body;
            this.method = method;
            this.date = date;
            this.path = path;
            this.host = host;
            this.contentType = contentType;
            this.queryList = queryList;
        }
    }

    public static void main(String[] args) throws CdnVolcengineException {
        // 获取 example.com 最近 5 分钟的带宽数据
        String j = "{\"Domain\":\"test1.xuewei.world\",\"ServiceType\":\"web\",\"OriginProtocolEnum\":\"http\",\"Origin\":[{\"Condition\":null,\"OriginAction\":{\"OriginLines\":[{\"Address\":\"1.1.1.1\",\"HttpPort\":\"80\",\"HttpsPort\":\"443\",\"InstanceType\":\"ip\",\"OriginType\":\"primary\",\"PrivateBucketAccess\":false,\"Weight\":\"1\"}]}}],\"Project\":\"ProjectE\"}";
        JSONObject requestBody = JSONObject.parseObject(j);
        JSONObject responseBody = doRequest("AddCdnDomain", new HashMap<>(), new HashMap<>(), requestBody);
        // 打印输出的结果
    }
}
