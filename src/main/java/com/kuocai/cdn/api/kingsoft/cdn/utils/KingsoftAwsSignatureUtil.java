package com.kuocai.cdn.api.kingsoft.cdn.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KingsoftAwsSignatureUtil {

    private static final Logger log = LoggerFactory.getLogger(KingsoftAwsSignatureUtil.class);
    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final String SERVICE = "cdn";
    private static final String REQUEST_TYPE = "aws4_request";


    public static String generateAuthorizationHeader(String method, String path, String region, Map<String, String> params, Map<String, String> headers, String payload, String accessKey, String secretKey) {
        if (headers == null) {
            throw new IllegalArgumentException("Headers map cannot be null.");
        }

        String timestamp = headers.get("x-amz-date");

        if (timestamp == null) {
            throw new IllegalArgumentException("Header 'x-amz-date' is missing from the headers map provided for signing.");
        }

        String date = timestamp.substring(0, 8);

        if (params == null) {
            params = Collections.emptyMap();
        }

        String canonicalRequest = buildCanonicalRequest(method, path, params, headers, payload);
        String stringToSign = buildStringToSign(timestamp, date, region, canonicalRequest);
        byte[] signingKey = getSigningKey(secretKey, date, region, "cdn");

        String signedHeaders = headers.keySet().stream()
                .map(key -> key.toLowerCase(Locale.US))
                .sorted()
                .collect(Collectors.joining(";"));

        String signature = sign(signingKey, stringToSign);

        if (log.isDebugEnabled()) {
            log.debug("========== Kingsoft Signature Debug (Header) ==========");
            log.debug("CanonicalRequest:\n{}", canonicalRequest);
            log.debug("StringToSign:\n{}", stringToSign);
            log.debug("Signature: {}", signature);
            log.debug("======================================================");
        }

        return buildAuthorizationHeaderValue(accessKey, date, region, "cdn", signedHeaders, signature);
    }

    private static String buildCanonicalRequest(String method, String path, Map<String, String> params, Map<String, String> headers, String payload) {
        String canonicalURI = urlEncodePath(path);

        String canonicalQueryString = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> urlEncodeValue(entry.getKey()) + "=" + urlEncodeValue(entry.getValue()))
                .collect(Collectors.joining("&"));

        String canonicalHeaders = headers.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey().toLowerCase(Locale.US) + ":" + entry.getValue().trim().replaceAll("\\s+", " "))
                .collect(Collectors.joining("\n")) + "\n";

        String signedHeaders = headers.keySet().stream()
                .map(key -> key.toLowerCase(Locale.US))
                .sorted()
                .collect(Collectors.joining(";"));

        String payloadHash = sha256Hex(payload == null ? "" : payload);

        return method + "\n" +
                canonicalURI + "\n" +
                canonicalQueryString + "\n" +
                canonicalHeaders + "\n" +
                signedHeaders + "\n" +
                payloadHash;
    }

    private static String buildStringToSign(String timestamp, String date, String region, String canonicalRequest) {
        String scope = getCredentialScope(date, region, SERVICE);
        return ALGORITHM + "\n" +
                timestamp + "\n" +
                scope + "\n" +
                sha256Hex(canonicalRequest);
    }

    private static String buildAuthorizationHeaderValue(String accessKey, String date, String region, String service, String signedHeaders, String signature) {
        String scope = getCredentialScope(date, region, service);
        return ALGORITHM + " Credential=" + accessKey + "/" + scope + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;
    }

    private static String getCredentialScope(String date, String region, String service) {
        return date + "/" + region + "/" + service + "/" + REQUEST_TYPE;
    }

    public static Map<String, String> createStandardHeaders(String host, String timestamp) {
        Map<String, String> headers = new TreeMap<>();
        headers.put("Host", host);
        headers.put("X-Amz-Date", timestamp);
        return headers;
    }

    public static String generateTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    public static byte[] getSigningKey(String secretKey, String date, String region, String service) {
        try {
            byte[] kSecret = ("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8);
            byte[] kDate = hmacSha256(kSecret, date);
            byte[] kRegion = hmacSha256(kDate, region);
            byte[] kService = hmacSha256(kRegion, service);
            return hmacSha256(kService, REQUEST_TYPE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signing key", e);
        }
    }

    public static String sign(byte[] key, String msg) {
        try {
            return bytesToHex(hmacSha256(key, msg));
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign string", e);
        }
    }

    private static byte[] hmacSha256(byte[] key, String data) throws Exception {
        SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(signingKey);
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate SHA-256 hash", e);
        }
    }

    public static String bytesToHex(byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    private static String urlEncodePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return Arrays.stream(path.split("/", -1))
                .map(KingsoftAwsSignatureUtil::urlEncodeValue)
                .collect(Collectors.joining("/"));
    }

    private static String urlEncodeValue(String value) {
        if (value == null) {
            return "";
        }
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20");
        } catch (Exception e) {
            throw new RuntimeException("Failed to URL encode value: " + value, e);
        }
    }
}