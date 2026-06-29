package com.kuocai.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TencentCloudApiConfigVo {

    /**
     * 项目名
     */
    private String tencentCloudProjectName;

    /**
     * SecretId
     */
    private String tencentCloudSecretId;

    /**
     * SecretKey
     */
    private String tencentCloudSecretKey;

}
