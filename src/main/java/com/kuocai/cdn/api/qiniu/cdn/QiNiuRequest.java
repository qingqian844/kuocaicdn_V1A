package com.kuocai.cdn.api.qiniu.cdn;

import com.kuocai.cdn.api.qiniu.cdn.properties.QiNiuCdn;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Client;
import com.qiniu.http.Response;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import lombok.extern.slf4j.Slf4j;

/**
 * 七牛云通用请求方式
 */
@Slf4j
public class QiNiuRequest {

    private final static String server = "http://api.qiniu.com";

    private final static Auth auth;

    private final static Client client;

    static {
        auth = Auth.create(QiNiuCdn.AK, QiNiuCdn.SK);
        client = new Client();
    }

    public static void get(String url) throws QiniuException {
        StringMap headers = auth.authorization(server + url, null, Client.JsonMime);
        Response response = client.get(server + url, headers);
        if (!response.isOK()) {
            throw new QiniuException(response);
        }
    }

    public static <T> T get(String url, byte[] body, Class<T> cls) throws QiniuException {
        StringMap headers = auth.authorization(server + url, body, Client.JsonMime);
        Response response = client.get(server + url, headers);
        if (response.isOK()) {
            return response.jsonToObject(cls);
        } else {
            throw new QiniuException(response);
        }
    }

    public static void put(String url, byte[] body) throws QiniuException {
        StringMap headers = auth.authorization(server + url, body, Client.JsonMime);
        Response response = client.put(server + url, body, headers, Client.JsonMime);
        if (!response.isOK()) {
            throw new QiniuException(response);
        }
    }

    public static <T> T put(String url, byte[] body, Class<T> cls) throws QiniuException {
        StringMap headers = auth.authorization(server + url, body, Client.JsonMime);
        Response response = client.put(server + url, body, headers, Client.JsonMime);
        if (response.isOK()) {
            return response.jsonToObject(cls);
        } else {
            throw new QiniuException(response);
        }
    }


    public static void post(String url, byte[] body) throws QiniuException {
        StringMap headers = auth.authorization(server + url, body, Client.JsonMime);
        Response response = client.post(server + url, body, headers, Client.JsonMime);
        if (!response.isOK()) {
            throw new QiniuException(response);
        }
    }

    public static <T> T post(String url, byte[] body, Class<T> cls) throws QiniuException {
        StringMap headers = auth.authorization(server + url, body, Client.JsonMime);
        Response response = client.post(server + url, body, headers, Client.JsonMime);
        if (response.isOK()) {
            return response.jsonToObject(cls);
        } else {
            throw new QiniuException(response);
        }
    }

    public static <T> T postStatis(String url, byte[] body, Class<T> cls) throws QiniuException {
        StringMap headers = auth.authorization("http://fusion.qiniuapi.com" + url, body, Client.JsonMime);
        Response response = client.post("http://fusion.qiniuapi.com" + url, body, headers, Client.JsonMime);
        if (response.isOK()) {
            return response.jsonToObject(cls);
        } else {
            throw new QiniuException(response);
        }
    }

    public static void delete(String url, byte[] body) throws QiniuException {
        StringMap headers = auth.authorization(server + url, body, Client.JsonMime);
        Response response = client.delete(server + url, body, headers, Client.JsonMime);
        if (!response.isOK()) {
            throw new QiniuException(response);
        }
    }

    public static <T> T delete(String url, byte[] body, Class<T> cls) throws QiniuException {
        StringMap headers = auth.authorization(server + url, body, Client.JsonMime);
        Response response = client.delete(server + url, body, headers, Client.JsonMime);
        if (response.isOK()) {
            return response.jsonToObject(cls);
        } else {
            throw new QiniuException(response);
        }
    }
}
