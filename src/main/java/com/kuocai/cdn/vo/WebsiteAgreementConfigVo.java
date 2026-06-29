package com.kuocai.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xiaobo
 * @date 2023/4/1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebsiteAgreementConfigVo {

    /**
     * 协议内容
     */
    private String agreementInfo;
}
