package com.kuocai.cdn.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.kuocai.cdn.entity.CdnDomainSources;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 域名源站(CdnDomainSources)实体类
 *
 * @author XUEW
 * @since 2023-02-28 10:26:22
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CdnDomainSourcesVo {

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 主键ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long cdnDomainId;

    /**
     * 加速域名ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private String domainId;

    /**
     * 源站IP（非内网IP）或者域名
     */
    private String ipOrDomain;

    /**
     * 源站类型取值：ipaddr、 domain、obs_bucket，分别表示：源站IP、源站域名、OBS桶访问域名
     */
    private String originType;

    /**
     * 主备状态（1代表主站；0代表备站）,主源站必须存在，备源站可选，OBS桶不能有备源站
     */
    private Integer activeStandby;

    /**
     * 是否开启Obs静态网站托管(0表示关闭,1表示则为开启)，源站类型为obs_bucket时传递
     */
    private Integer enableObsWebHosting;

    /**
     * HTTP端口，默认80
     */
    private Integer httpPort;

    /**
     * HTTPS端口，默认443
     */
    private Integer httpsPort;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    private CdnDomainSources main;

    private CdnDomainSources back;

    /**
     * 白山云使用
     */
    private Integer port;

    /**
     * 白山云使用
     */
    private String originProtocol;
}
