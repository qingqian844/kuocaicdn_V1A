package com.kuocai.cdn.integration.scdn;

import com.kuocai.cdn.util.JedisUtil;
import org.springframework.stereotype.Component;

@Component
public class ScdnOneTimeCodeStore {

    public void put(String key, String value, int ttlSeconds) {
        JedisUtil.setStr(key, value, ttlSeconds);
    }

    public String take(String key) {
        return JedisUtil.getAndDeleteStr(key);
    }
}

