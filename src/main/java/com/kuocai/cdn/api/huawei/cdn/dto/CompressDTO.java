package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 智能压缩
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CompressDTO {

    /**
     * 智能压缩开关（on：开启，off：关闭）。
     */
    private String status;

    /**
     * 智能压缩类型（gzip：gzip压缩，br：br压缩）。
     */
    private String type;
}
