package com.kuocai.cdn.api.baishan.cdn;

import com.alibaba.fastjson.JSON;
import com.kuocai.cdn.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class BsRequest {

    private final static OkHttpClient client;

    static {
        client = new OkHttpClient.Builder()
                .build();
    }

    public static String sendGetRequest(String url) throws BusinessException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                throw new BusinessException(response.body().string());
            }
        } catch (IOException e) {
            throw new BusinessException(e.getMessage());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    public static String sendGetRequest(String url, Map<String, String> params) throws BusinessException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        for (String key : params.keySet()) {
            urlBuilder.addQueryParameter(key, params.get(key));
        }
        String requestUrl = urlBuilder.build().toString();
        Request request = new Request.Builder()
                .url(requestUrl)
                .build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                throw new BusinessException(response.body().string());
            }
        } catch (IOException e) {
            throw new BusinessException(e.getMessage());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }


    /**
     * DELETE请求
     *
     * @param url
     * @return
     * @throws BusinessException
     */
    public static String sendDeleteRequest(String url) throws BusinessException {
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                throw new BusinessException(response.body().string());
            }
        } catch (IOException e) {
            throw new BusinessException(e.getMessage());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }


    /**
     * DELETE请求
     *
     * @param url
     * @return
     * @throws BusinessException
     */
    public static String sendDeleteRequest(String url, Object json) throws BusinessException {
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(mediaType, JSON.toJSONString(json));
        Request request = new Request.Builder()
                .url(url)
                .delete(requestBody)
                .build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                throw new BusinessException(response.body().string());
            }
        } catch (IOException e) {
            throw new BusinessException(e.getMessage());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    /**
     * @param url
     * @param json
     * @return
     * @throws IOException
     */
    public static String sendPostRequest(String url, Object json) throws BusinessException {
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(mediaType, JSON.toJSONString(json));
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                throw new BusinessException(response.body().string());
            }
        } catch (IOException e) {
            throw new BusinessException(e.getMessage());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }
}
