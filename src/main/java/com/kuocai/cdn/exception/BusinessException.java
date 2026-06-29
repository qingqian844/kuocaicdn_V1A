package com.kuocai.cdn.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 业务异常类
 */
@Slf4j
public class BusinessException extends BaseException {

    private static final long serialVersionUID = -4879677283847539655L;

    private int code;

    private String message;

    public BusinessException(String message) {
        super(message);
        this.message = message;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(String message, Object... objects) {
        super(message);
        for (Object o : objects) {
            message = message.replaceFirst("\\{\\}", o.toString());
        }
        super.setMessage(message);
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 设置原因
     *
     * @param cause 原因
     */
    public BusinessException setCause(Throwable cause) {
        this.initCause(cause);
        return this;
    }

    /**
     * 打印错误日志
     */
    public BusinessException log() {
        log.error(message);
        return this;
    }
}