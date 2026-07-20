package com.kuocai.cdn.integration.scdn;

import com.kuocai.cdn.util.JwtUtil;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ScdnAccessTokenIssuer {

    public String issue(Map<String, String> claims, int expiresInSeconds) {
        return JwtUtil.getToken(claims, "kuocai-cdn", "kuocai-scdn", expiresInSeconds);
    }
}

