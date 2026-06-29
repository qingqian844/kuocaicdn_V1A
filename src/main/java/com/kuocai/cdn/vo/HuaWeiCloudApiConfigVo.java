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
public class HuaWeiCloudApiConfigVo {

    /**
     * 项目名
     */
    private String huaWeiCloudProjectName;

    /**
     * AK
     */
    private String huaWeiCloudAk;

    /**
     * SK
     */
    private String huaWeiCloudSk;

}
