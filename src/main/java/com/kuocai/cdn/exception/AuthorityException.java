package com.kuocai.cdn.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 权限异常类
 */
@Slf4j
public class AuthorityException extends BusinessException {

    private static final long serialVersionUID = -4879677283847539655L;

    private int code;

    private String message;


    public AuthorityException(String message) {
        super(message);
        this.message = message;
    }

    public AuthorityException(int code, String message) {
        super(code, message);
        this.code = code;
        this.message = message;
    }

    public AuthorityException(String message, Object... objects) {
        super("");
        for (Object o : objects) {
            message = message.replaceFirst("\\{\\}", o.toString());
        }
        super.setMessage(message);
        this.message = message;
    }

    /**
     * 设置原因
     *
     * @param cause 原因
     */
    public AuthorityException setCause(Throwable cause) {
        this.initCause(cause);
        return this;
    }

    /**
     * 打印错误日志
     */
    public AuthorityException log() {
        log.error(message);
        return this;
    }
}