/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2022-2022. All rights reserved.
 */

package com.kuocai.cdn.exception;

public class UnSupportProtocolException extends Exception {

    private static final long serialVersionUID = 4312820110480855928L;

    public UnSupportProtocolException(String message) {
        super(message);
    }
}
