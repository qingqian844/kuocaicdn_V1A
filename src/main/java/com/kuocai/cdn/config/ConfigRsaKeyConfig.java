package com.kuocai.cdn.config;

import com.kuocai.cdn.util.ConfigureRsaUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConfigRsaKeyConfig {

    public ConfigRsaKeyConfig(@Value("${security.config-rsa.private-key:}") String privateKey,
                              @Value("${security.config-rsa.public-key:}") String publicKey) {
        ConfigureRsaUtils.configure(privateKey, publicKey);
    }
}
