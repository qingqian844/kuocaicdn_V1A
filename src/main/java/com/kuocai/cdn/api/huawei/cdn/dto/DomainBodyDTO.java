package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 域名操作
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class DomainBodyDTO {

    /**
     * 加速域名，采用模糊匹配的方式。（长度限制为1-255字符）
     */
    private String domain_name;

    /**
     * 加速域名的业务类型
     * 参考常量类 BusinessType
     */
    private String business_type;

    /**
     * 华为云CDN提供的加速服务范围
     * 参考常量类 ServiceArea
     */
    private String service_area;

    /**
     * 每页的数量，取值范围1-10000，不设值时默认值为30。
     */
    private Integer page_size;

    /**
     * 查询的页码。取值范围1-65535，不设值时默认值为1。
     */
    private Integer page_number;

    /**
     * 源站域名或源站IP，源站为IP类型时，仅支持IPv4，如需传入多个源站IP，以多个源站对象传入，除IP其他参数请保持一致
     * 主源站最多支持15个源站IP对象，备源站最多支持15个源站IP对象；源站为域名类型时仅支持1个源站对象。
     * 不支持IP源站和域名源站混用。
     */
    private List<DomainSourceDTO> sources;
}
