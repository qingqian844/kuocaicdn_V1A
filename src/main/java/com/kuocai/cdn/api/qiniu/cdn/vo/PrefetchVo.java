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
public class PrefetchVo {

    /**
     * 文件预取
     * 要刷新的单个url列表，总数不超过60条；
     */
    private List<String> urls;
}
