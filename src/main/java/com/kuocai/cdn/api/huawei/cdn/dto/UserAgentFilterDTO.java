package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * User-Agent黑白名单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class UserAgentFilterDTO {

    /**
     * UA黑白名单类型 off：关闭UA黑白名单; black：UA黑名单; white：UA白名单;
     */
    private String type;

    /**
     * 配置UA黑白名单，当type=off时，非必传。最多配置10条规则，单条规则不超过100个字符，多条规则用“,”分割。
     */
    private String value;
}
