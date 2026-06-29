package com.kuocai.cdn.vo;

import com.kuocai.cdn.api.huawei.cdn.dto.HttpInfoRequestBodyDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 证书管理查询页面VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CertificationListVo {

    private HttpInfoRequestBodyDTO https;

    private String domainId;
}
