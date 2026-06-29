package com.kuocai.cdn.api.qiniu.cdn.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IpVo {

    /**
     * ip黑白名单控制控制类型, black/white/""；其中空字符串""代表关闭本功能，此时请注意ipACLValues需要为空
     */
    private String ipACLType;

    /**
     * ip黑白名单, ip格式为：127.0.0.1/24
     */
    private List<String> ipACLValues;
}
