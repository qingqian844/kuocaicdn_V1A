package com.kuocai.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xiaobo
 * @date 2023/7/30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhiteMountainCloudApiConfigVo {

    /**
     * 项目名
     */
    private String whiteMountainCloudProjectName;

    /**
     * 基础api
     */
    private String whiteMountainCloudBaseApi;

    /**
     * token
     */
    private String whiteMountainCloudToken;
}
