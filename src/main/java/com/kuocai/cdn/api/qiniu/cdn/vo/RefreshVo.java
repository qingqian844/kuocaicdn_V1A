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
public class RefreshVo {

    /**
     * 缓存刷新的文件
     * 要刷新的单个url列表，总数不超过60条；
     */
    private List<String> urls;

    /**
     * 缓存刷新目录
     * 	要刷新的目录url列表，总数不超过10条；
     */
    private List<String> dirs;
}
