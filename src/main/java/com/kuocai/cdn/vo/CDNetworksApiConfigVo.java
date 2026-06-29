package com.kuocai.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CDNetworksApiConfigVo {

    /**
     * 项目名
     */
    private String cdnetworksProjectName;

    /**
     * AccessKey
     */
    private String cdnetworksAccessKey;

    /**
     * SecretKey
     */
    private String cdnetworksSecretKey;
}
