package com.kuocai.cdn.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.kuocai.cdn.api.huawei.cdn.dto.ForceRedirectConfigDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.HttpPutBodyDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class DomainHttpsSettingVo {

    /**
     * 加速域名ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long doMainId;

    /**
     * 证书设置
     */
    private HttpPutBodyDTO https;

    /**
     * 强制重定向
     */
    private ForceRedirectConfigDTO forceRedirect;
}
