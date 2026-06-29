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
public class VolcanicCloudApiConfigVo {

    /**
     * 项目名
     */
    private String volcanicCloudProjectName;

    /**
     * AK
     */
    private String volcanicCloudAk;

    /**
     * SK
     */
    private String volcanicCloudSk;

}
