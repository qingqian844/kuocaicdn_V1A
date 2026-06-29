package com.kuocai.cdn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xiaobo
 * @date 2023/4/1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebsiteContactConfigVo {

    /**
     * 联系电话
     */
    private String websiteTel;

    /**
     * 联系邮箱
     */
    private String websiteEmail;

    /**
     * 联系QQ
     */
    private String websiteQq;

    /**
     * 公司名称
     */
    private String company;

}
