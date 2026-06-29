package com.kuocai.cdn.vo;

import com.kuocai.cdn.api.huawei.cdn.dto.CacheRuleDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.ErrorCodeCacheDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * (SettingCacheVo)
 *
 * @author ItYoung
 * @since 2023-03-10 21:06:04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettingCacheVo {

    /**
     * 本地加速域名id
     */
    private String doMainId;

    /**
     * on 开启
     * off 关闭
     */
    private String cacheFollowOriginStatus;

    /**
     * 缓存规则参数
     */
    private List<CacheRuleDTO> cacheRules;

    /**
     * 状态码缓存时间
     */
    private List<ErrorCodeCacheDTO> errorCodeCache;
}
