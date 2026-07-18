package com.kuocai.cdn.vo;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.kuocai.cdn.api.huawei.cdn.dto.FlexibleOriginDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.OriginRequestHeaderDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.OriginRequestUrlRewriteDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class DomainOriginSettingVo {

    /**
     * 加速域名ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long doMainId;


    /**
     * 回源协议（follow：协议跟随回源，http：HTTP回源(默认)，https：https回源）。
     */
    private String originProtocol;

    /**
     * 状态（"off"/"on"）
     * Range回源
     * 回源跟随
     * 回源是否校验ETag
     */
    private String status;

    /**
     * 回源URL改写参数
     */
    private List<OriginRequestUrlRewriteDTO> originRequestUrlRewriteDTOS;

    /**
     * 修改回源超时时间
     */
    private Integer originReceiveTimeOut;

    /**
     * 修改高级回源配置
     */
    private List<FlexibleOriginDTO> flexibleOrigins;

    /**
     * 回源请求头改写 该功能将覆盖原有配置（清空之前的配置），在使用此接口时，请上传全量头部信息。
     */
    private List<OriginRequestHeaderDTO> originRequestHeader;

    /**
     * HTTP回源端口
     */
    private Integer httpPort;

    /**
     * HTTPS回源端口
     */
    private Integer httpsPort;

    /**
     * 回源跟随重定向最大次数
     */
    private Integer maxTimes;

}
