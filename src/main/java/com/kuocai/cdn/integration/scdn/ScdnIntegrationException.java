package com.kuocai.cdn.integration.scdn;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ScdnIntegrationException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public ScdnIntegrationException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }
}

