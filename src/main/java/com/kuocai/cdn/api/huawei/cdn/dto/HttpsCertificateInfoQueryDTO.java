package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 查询所有绑定HTTPS证书的域名信息参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class HttpsCertificateInfoQueryDTO {

    /**
     * 每页的数量，取值范围1-10000，不设值时默认值为30。
     */
    private Integer page_size;

    /**
     * 查询的页码。取值范围1-65535，不设值时默认值为1。
     */
    private Integer page_number;

    /**
     * 加速域名
     */
    private String domain_name;

    /**
     * 域名所属用户的domain_id。
     */
    private String user_domain_id;
}
