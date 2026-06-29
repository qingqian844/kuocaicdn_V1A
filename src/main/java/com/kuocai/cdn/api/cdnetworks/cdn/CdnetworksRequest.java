package com.kuocai.cdn.api.cdnetworks.cdn;

import com.alibaba.fastjson.JSON;
import com.kuocai.cdn.api.cdnetworks.cdn.properties.CdnetworksCdn;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpPost;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class CdnetworksRequest {
    private String uri;
    private String url;
    private String host;
    private String method;
    private String protocol;
    private Map<String, String> params;
    private Map<String, String> headers;
    private String body;
    private String signedHeaders;
    private Object msg;

    public CdnetworksRequest() {
        params = new HashMap<>(4);
        headers = new HashMap<>(8);
        putHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        putHeader(HttpHeaders.ACCEPT, "application/json");
        putHeader(CdnetworksCdn.X_CNC_AUTH_METHOD, CdnetworksCdn.AUTH_METHOD);
    }

    public void putParam(String name, String value) {
        params.put(name, value);
    }

    public String getParam(String name) {
        String values = params.get(name);
        return StringUtils.isNotBlank(values) ? values : null;
    }

    public void putHeader(String name, String value) {
        headers.put(name, value);
    }

    public String getHeader(String name) {
        String value = null;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (StringUtils.equalsIgnoreCase(entry.getKey(), name)) {
                value = entry.getValue();
                break;
            }
        }
        return value;
    }

    public String getQueryString() {
        int index = uri.indexOf("?");
        if (HttpPost.METHOD_NAME.equals(method) || index == -1) {
            return "";
        }
        return uri.substring(index + 1);
    }

    public void setJsonBody(Object object) {
        body = JSON.toJSONString(object);
    }
}
