package com.kuocai.cdn.api.kingsoft.cdn;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KingsoftApiServiceTest {

    @Test
    void wildcardDomainUsesTheSameRfc3986EncodingForSigningAndTransport() {
        Map<String, String> params = new HashMap<>();
        params.put("Action", "GetCdnDomains");
        params.put("DomainName", "*.example.com");
        params.put("X-Amz-SignedHeaders", "host;x-amz-date");

        String encodedQuery = KingsoftApiService.buildEncodedQueryString(params);
        URI uri = KingsoftApiService.buildEncodedGetUri(
                "https", "cdn.api.ksyun.com", -1,
                "/2019-06-01/domain/GetCdnDomains", params);

        assertTrue(encodedQuery.contains("DomainName=%2A.example.com"));
        assertTrue(encodedQuery.contains("X-Amz-SignedHeaders=host%3Bx-amz-date"));
        assertFalse(encodedQuery.contains("DomainName=*.example.com"));
        assertEquals(encodedQuery, uri.getRawQuery());
    }
}
