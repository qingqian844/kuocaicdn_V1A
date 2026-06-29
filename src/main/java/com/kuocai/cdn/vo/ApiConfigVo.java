package com.kuocai.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xiaobo
 * @date 2023/3/27
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiConfigVo {


    /**
     *
     */
    private String accessKeyId;


    /**
     *
     */
    private String secretId;

}
