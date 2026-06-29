package com.kuocai.cdn.api.cdnetworks.cdn;

import com.kuocai.cdn.exception.CdnetworksException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class CdnetworksHttp {
    private CdnetworksHttp() {
    }

    public static class Response {

        private final StatusLine statusLine;

        private final Header[] headers;

        @Getter
        private final String body;

        public Response(CloseableHttpResponse response) throws CdnetworksException {
            HttpEntity entity = response.getEntity();
            if (null == entity) {
                this.body = null;
            } else {
                try {
                    this.body = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new CdnetworksException("get response body fail.", e);
                }
            }
            this.statusLine = response.getStatusLine();
            this.headers = response.getAllHeaders();
        }

        public String getHeader(String name) {
            return Arrays.stream(headers).filter(header -> StringUtils.equalsIgnoreCase(header.getName(), name)).map(Header::getValue).collect(Collectors.joining());
        }

        public String[] getHeaders(String name) {
            return Arrays.stream(headers).filter(header -> StringUtils.equalsIgnoreCase(header.getName(), name)).map(Header::getValue).toArray(String[]::new);
        }

        public Map<String, String> getAllHeaders() {
            return Arrays.stream(headers).collect(Collectors.toMap(Header::getName, Header::getValue));
        }

        public int getStatusCode() {
            return statusLine.getStatusCode();
        }

        public String getStatusMessage() {
            return statusLine.getReasonPhrase();
        }

        public String getResponseContent() {
            StringBuilder sb = new StringBuilder();
            sb.append("status code: ").append(statusLine.getStatusCode()).append(statusLine.getReasonPhrase()).append("\n");
            sb.append("headers: ").append("\n");
            for (Header header : headers) {
                sb.append(header.getName()).append(": ").append(header.getValue()).append("\n");
            }
            sb.append("body: ").append("\n");
            try {
                sb.append(getBody());
            } catch (Exception e) {
                log.error("ResponseContent body fail.", e);
            }
            return sb.toString();
        }

        public void print() {
            log.info(getResponseContent());
        }
    }

    public static class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {

        public static final String METHOD_NAME = "DELETE";

        @Override
        public String getMethod() {
            return METHOD_NAME;
        }

        public HttpDeleteWithBody(final String uri) {
            super();
            setURI(URI.create(uri));
        }

        public HttpDeleteWithBody(final URI uri) {
            super();
            setURI(uri);
        }

        public HttpDeleteWithBody() {
            super();
        }
    }

    private static void closeClient(CloseableHttpClient client) {
        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                log.error("client close fail.", e);
            }
        }
    }

    private static void closeResponse(CloseableHttpResponse response) {
        if (response != null) {
            try {
                response.close();
            } catch (IOException e) {
                log.error("response close fail.", e);
            }
        }
    }

    private static void setRequestHeaders(HttpRequestBase request, CdnetworksRequest requestDTO) {
        if (!requestDTO.getHeaders().isEmpty()) {
            for (Map.Entry<String, String> header : requestDTO.getHeaders().entrySet()) {
                request.setHeader(header.getKey(), header.getValue());
            }
        }
    }

    public static Response call(CdnetworksRequest requestDTO) throws CdnetworksException {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        CloseableHttpResponse response = null;
        try {
            switch (requestDTO.getMethod()) {
                case HttpPost.METHOD_NAME:
                    HttpPost post = new HttpPost(requestDTO.getUrl());
                    setRequestHeaders(post, requestDTO);
                    if (StringUtils.isNotBlank(requestDTO.getBody())) {
                        post.setEntity(new StringEntity(requestDTO.getBody(), StandardCharsets.UTF_8));
                    }
                    response = client.execute(post);
                    break;
                case HttpGet.METHOD_NAME:
                    HttpGet get = new HttpGet(requestDTO.getUrl());
                    setRequestHeaders(get, requestDTO);
                    response = client.execute(get);
                    break;
                case HttpPut.METHOD_NAME:
                    HttpPut put = new HttpPut(requestDTO.getUrl());
                    if (StringUtils.isNotBlank(requestDTO.getBody())) {
                        put.setEntity(new StringEntity(requestDTO.getBody(), StandardCharsets.UTF_8));
                    }
                    setRequestHeaders(put, requestDTO);
                    response = client.execute(put);
                    break;
                case HttpPatch.METHOD_NAME:
                    HttpPatch patch = new HttpPatch(requestDTO.getUrl());
                    if (StringUtils.isNotBlank(requestDTO.getBody())) {
                        patch.setEntity(new StringEntity(requestDTO.getBody(), StandardCharsets.UTF_8));
                    }
                    setRequestHeaders(patch, requestDTO);
                    response = client.execute(patch);
                    break;
                case HttpDelete.METHOD_NAME:
                    HttpDeleteWithBody deleteWithBody = new HttpDeleteWithBody(requestDTO.getUrl());
                    setRequestHeaders(deleteWithBody, requestDTO);
                    if (StringUtils.isNotBlank(requestDTO.getBody())) {
                        deleteWithBody.setEntity(new StringEntity(requestDTO.getBody(), StandardCharsets.UTF_8));
                    }
                    response = client.execute(deleteWithBody);
                    break;
                default:
                    log.error("not support this http method : {}", requestDTO.getMethod());
                    break;
            }
            if (response == null) {
                throw new CdnetworksException("api invoke fail. response is null.");
            }
            return new Response(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            closeClient(client);
            closeResponse(response);
        }
    }
}
