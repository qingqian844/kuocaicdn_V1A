package com.kuocai.cdn.exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CdnetworksException extends BusinessException {
    private static final long serialVersionUID = 8798273456912941642L;

    private int code;

    private String message;

    public CdnetworksException(String message) {
        super(message);
        this.message = message;
    }

    public CdnetworksException(int code, String message) {
        super(code, message);
        this.code = code;
        this.message = message;
    }


    public CdnetworksException(String message, Object... objects) {
        super("");
        for (Object o : objects) {
            message = message.replaceFirst("\\{}", o.toString());
        }
        super.setMessage(message);
        this.message = message;
    }

    public CdnetworksException setCause(Throwable cause) {
        this.initCause(cause);
        return this;
    }

    public CdnetworksException log() {
        log.error(message);
        return this;
    }
}
