package com.kuocai.cdn.vo;

import com.kuocai.cdn.api.huawei.cdn.dto.RefererDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.UserAgentBlackAndWhiteListDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import com.kuocai.cdn.api.huawei.cdn.dto.UrlAuthDTO;

/**
 * (SettingAccessVo)
 *
 * @author ItYoung
 * @since 2023-03-24 21:06:04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettingAccessVo {

    /**
     * 本地加速域名id
     */
    private String doMainId;

    /**
     * 防盗链入参
     */
    private RefererDTO referer;

    /**
     * 设置Ipacl类型
     */
    private Integer type;

    /**
     * 设置Ipacl ip集合
     */
    private List<String> ips;

    /**
     * User-Agent黑白名单
     */
    private UserAgentBlackAndWhiteListDTO userAgentBlackAndWhiteListDTO;

    /**
     * URL鉴权
     */
    private UrlAuthDTO urlAuth;
}
