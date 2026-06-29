package com.kuocai.cdn.exception;

/**
 * 业务异常类
 */
public class BaseException extends Exception {

    private static final long serialVersionUID = -4879677283847539655L;

    private String message;

    public BaseException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}