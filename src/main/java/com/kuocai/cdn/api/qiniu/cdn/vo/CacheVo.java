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
public class CacheVo {

    private List<CacheControls> cacheControls;

    /**
     * 是否开启去问号缓存，默认为false
     */
    private boolean ignoreParam;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class CacheControls {

        /**
         * 缓存时间，注意不论哪种时间单位，总时间都不能超过1年,type为follow，本字段配为 -1
         */
        private int time;

        /**
         * 缓存时间单位：0(秒)/1(分钟)/2(小时)/3(天)/4(周)/5(月)/6(年)，type为follow，本字段配为0
         */
        private int timeunit;

        /**
         * 缓存类型：all(默认全局规则)/path(路径匹配)/suffix(后缀匹配)/follow(遵循源站)
         */
        private String type;

        /**
         * 缓存路径规则：以分号;分割的字符串，每个里面类型一致，比如CCType为path的话，这里每个分号分割的都是以/开头，
         * suffix的话，以点号.开头，
         * 如果是all类型，或者follow，统一只要填一个星号*
         */
        private String rule;
    }

}
