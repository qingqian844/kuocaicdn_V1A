package com.kuocai.cdn.config;

import com.kuocai.cdn.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtKeyConfig {

    public JwtKeyConfig(@Value("${security.jwt.public-key:}") String publicKey,
                        @Value("${security.jwt.private-key:}") String privateKey) {
        JwtUtil.configure(publicKey, privateKey);
    }
}
