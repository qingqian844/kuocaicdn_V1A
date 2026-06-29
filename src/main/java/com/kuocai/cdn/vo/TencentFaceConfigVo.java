package com.kuocai.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author xiaobo
 * @date 2023/3/27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TencentFaceConfigVo {

    /**
     * 用于表示API调用者身份，可以简单类比为用户
     */
    private String secretId;

    /**
     * 用于验证API调用者的身份，可以简单类比为密码
     */
    private String secretKey;

    /**
     * ruleId为自动接入人脸核身的业务RuleId
     */
    private String ruleId;

    /**
     * 认证费用
     */
    private BigDecimal certificationFee;


}
