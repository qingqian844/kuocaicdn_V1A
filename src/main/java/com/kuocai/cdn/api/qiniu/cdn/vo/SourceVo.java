package com.kuocai.cdn.api.qiniu.cdn.vo;

/**
 * Copyright 2023 bejson.com
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Auto-generated: 2023-07-10 21:34:17
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SourceVo {

    /**
     * 源站类型: domain(域名)/ip(ip地址)/qiniuBucket(七牛云存储，备注：不支持平台是动态加速)/advanced(高级)
     */
    private String sourceType;

    /**
     * 回源Host, 普通域名默认SourceHost为域名本身，泛域名默认SourceHost为用户请求时的域名
     */
    private String sourceHost;

    /**
     * 回源ip, sourceType为ip时sourceIPs必填
     */
    private List<String> sourceIPs;

    /**
     * 回源域名, sourceType为domain时此字段必填
     */
    private String sourceDomain;

    /**
     * 回源的七牛云存储的bucket名称, sourceType为qiniuBucket时此字段必填
     */
    private String sourceQiniuBucket;

    /**
     * 回源协议, 仅用于https域名，可选值: http/https, 回源七牛bucket时本值无效,默认不填是follow请求协议
     */
    private String sourceURLScheme;

    private List<AdvancedSources> advancedSources;

    /**
     * 用于测试的URL Path, 检测源站是否可访问, 大小建议小于1KB，采用静态资源，并请不要删除, 后面域名任何配置更改都会测试该资源, 用以保证域名的访问性
     */
    private String testURLPath;

    /**
     * Auto-generated: 2023-07-10 21:34:17
     *
     * @author bejson.com (i@bejson.com)
     * @website http://www.bejson.com/java2pojo/
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AdvancedSources {


        /**
         * 高级回源的回源地址, 可以是IP或者域名, sourceType为advanced时advancedSources字段必填
         */
        private String addr;

        /**
         * 高级回源的回源addr权重, 0 ~ 100, 按照权重比例回源，sourceType为advanced时advancedSources字段必填
         */
        private int weight;

        /**
         * 高级回源的回源addr是否为备源地址，sourceType为advanced时advancedSources字段必填
         */
        private boolean backup;
    }

}
