package com.kuocai.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xiaobo
 * @date 2023/3/22
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MergeCdnApiConfigVo {

    /**
     * 华为生效时间
     */
    private String huaweiWorkHours;

    /**
     * 火山生效时间
     */
    private String volcanicWorkHours;

}
