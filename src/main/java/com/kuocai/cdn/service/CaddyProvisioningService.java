package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class CaddyProvisioningService {

    private final String adminUrl;
    private final String upstream;
    private final String minioEndpoint;
    private final String minioBucket;

    public CaddyProvisioningService(@Value("${caddy.admin-url:http://caddy:2019}") String adminUrl,
                                    @Value("${caddy.upstream:app:8000}") String upstream,
                                    @Value("${minio.endpoint}") String minioEndpoint,
                                    @Value("${minio.bucketName:uploads}") String minioBucket) {
        this.adminUrl = trimSlash(adminUrl);
        this.upstream = upstream;
        this.minioEndpoint = trimSlash(minioEndpoint);
        this.minioBucket = minioBucket;
    }

    public void provision(String domain) throws BusinessException {
        String caddyfile = buildCaddyfile(domain);
        String adapted = post(adminUrl + "/adapt?adapter=caddyfile", "text/caddyfile", caddyfile);
        JSONObject payload = JSON.parseObject(adapted);
        if (payload.containsKey("result") && payload.get("result") instanceof JSONObject) {
            payload = payload.getJSONObject("result");
        }
        post(adminUrl + "/load", "application/json", payload.toJSONString());
    }

    public boolean waitForHttps(String domain, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                HttpsURLConnection connection = (HttpsURLConnection) new URL("https://" + domain + "/health").openConnection();
                connection.setConnectTimeout(4000);
                connection.setReadTimeout(4000);
                connection.setInstanceFollowRedirects(false);
                int code = connection.getResponseCode();
                if (code > 0 && code < 500) {
                    return true;
                }
            } catch (Exception ignored) {
                // Certificate issuance and DNS propagation can take a short while.
            }
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    String buildCaddyfile(String domain) {
        return "{\n"
                + "  admin 0.0.0.0:2019\n"
                + "}\n\n"
                + domain + " {\n"
                + "  encode gzip zstd\n"
                + "  handle /" + minioBucket + "/* {\n"
                + "    reverse_proxy " + minioEndpoint + "\n"
                + "  }\n"
                + "  handle {\n"
                + "    reverse_proxy " + upstream + "\n"
                + "  }\n"
                + "}\n\n"
                + ":80 {\n"
                + "  handle /" + minioBucket + "/* {\n"
                + "    reverse_proxy " + minioEndpoint + "\n"
                + "  }\n"
                + "  handle {\n"
                + "    reverse_proxy " + upstream + "\n"
                + "  }\n"
                + "}\n";
    }

    private String post(String target, String contentType, String body) throws BusinessException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(target).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(15000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", contentType);
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(bytes);
            }
            int status = connection.getResponseCode();
            String response = read(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
            if (status < 200 || status >= 300) {
                throw new BusinessException("Caddy 配置失败，HTTP " + status + "：" + response);
            }
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("无法连接 Caddy 管理接口：" + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String read(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line);
            }
        }
        return text.toString();
    }

    private static String trimSlash(String value) {
        String result = value == null ? "" : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
