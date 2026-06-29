package com.kuocai.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xiaobo
 * @date 2023/3/27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DnsConfigVo {

    /**
     * 主域名
     */
    private String primaryDomain;

    /**
     * DNS生存时间
     */
    private String domainTtl;

    /**
     * 选择的DNS
     */
    private String selectDns;

    /**
     * secretId
     */
    private String secretId;

    /**
     * secretKey
     */
    private String secretKey;

}
