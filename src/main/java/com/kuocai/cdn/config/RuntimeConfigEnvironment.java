package com.kuocai.cdn.config;

import com.kuocai.cdn.util.RuntimeConfigUtils;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class RuntimeConfigEnvironment {

    public RuntimeConfigEnvironment(Environment environment) {
        RuntimeConfigUtils.configure(environment);
    }
}
