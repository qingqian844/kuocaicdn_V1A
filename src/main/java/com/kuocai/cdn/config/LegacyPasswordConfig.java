package com.kuocai.cdn.config;

import com.kuocai.cdn.util.AesUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LegacyPasswordConfig {

    public LegacyPasswordConfig(@Value("${security.password.legacy-aes-key:}") String legacyAesKey) {
        AesUtils.configureLegacyKey(legacyAesKey);
    }
}
