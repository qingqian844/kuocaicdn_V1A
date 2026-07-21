package com.kuocai.cdn.integration.scdn;

import com.kuocai.cdn.config.ScdnIntegrationProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ScdnViewModelAdvice {
    private final ScdnIntegrationProperties properties;

    public ScdnViewModelAdvice(ScdnIntegrationProperties properties) {
        this.properties = properties;
    }

    @ModelAttribute("scdnIntegrationEnabled")
    public boolean enabled() {
        return properties.isEnabled();
    }
}

