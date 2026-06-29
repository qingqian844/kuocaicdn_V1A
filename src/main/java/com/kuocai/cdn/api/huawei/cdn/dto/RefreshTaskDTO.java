package com.kuocai.cdn.api.huawei.cdn.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 刷新缓存任务
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class RefreshTaskDTO {

    /**
     * 刷新的类型，其值可以为file 或directory，默认为file
     */
    private String type;


    /**
     * 输入URL必须带有“http://”或“https://”，多个URL用逗号分隔
     * 单个url的长度限制为4096字符，单次最多输入1000个url。
     */
    private String[] urls;

}
