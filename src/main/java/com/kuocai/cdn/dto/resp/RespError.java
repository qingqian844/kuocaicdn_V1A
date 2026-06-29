package com.kuocai.cdn.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 错误信息
 *
 * @author XUEW
 * @date 下午9:01 2023/2/12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespError extends RespResult {

    /**
     * 请求地址（发生异常时返回）
     */
    private String requestUrl;

    /**
     * 异常类（发生异常时返回）
     */
    private String exception;
}
