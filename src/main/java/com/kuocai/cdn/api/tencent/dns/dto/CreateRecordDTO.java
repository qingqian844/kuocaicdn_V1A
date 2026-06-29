package com.kuocai.cdn.api.tencent.dns.dto;

/**
 * @author xiaobo
 * @date 2023/3/6
 */

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.tencentcloudapi.common.AbstractModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.HashMap;

@Data
@Builder
@Accessors(chain = true)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CreateRecordDTO extends AbstractModel {

    /**
     * 域名
     */
    @SerializedName("Domain")
    @Expose
    private String Domain;

    /**
     * 记录类型，通过 API 记录类型获得，大写英文，比如：A 。
     */
    @SerializedName("RecordType")
    @Expose
    private String RecordType;

    /**
     * 记录线路，通过 API 记录线路获得，中文，比如：默认。
     */
    @SerializedName("RecordLine")
    @Expose
    private String RecordLine;

    /**
     * 记录值，如 IP : 200.200.200.200， CNAME : cname.dnspod.com.， MX : mail.dnspod.com.。
     */
    @SerializedName("Value")
    @Expose
    private String Value;

    /**
     * 域名 ID 。参数 DomainId 优先级比参数 Domain 高，如果传递参数 DomainId 将忽略参数 Domain 。
     */
    @SerializedName("DomainId")
    @Expose
    private Long DomainId;

    /**
     * 主机记录，如 www，如果不传，默认为 @。
     */
    @SerializedName("SubDomain")
    @Expose
    private String SubDomain;

    /**
     * 线路的 ID，通过 API 记录线路获得，英文字符串，比如：10=1。参数RecordLineId优先级高于RecordLine，如果同时传递二者，优先使用RecordLineId参数。
     */
    @SerializedName("RecordLineId")
    @Expose
    private String RecordLineId;

    /**
     * MX 优先级，当记录类型是 MX 时有效，范围1-20，MX 记录时必选。
     */
    @SerializedName("MX")
    @Expose
    private Long MX;

    /**
     * TTL，范围1-604800，不同等级域名最小值不同。
     */
    @SerializedName("TTL")
    @Expose
    private Long TTL;

    /**
     * 权重信息，0到100的整数。仅企业 VIP 域名可用，0 表示关闭，不传该参数，表示不设置权重信息。
     */
    @SerializedName("Weight")
    @Expose
    private Long Weight;

    /**
     * 记录初始状态，取值范围为 ENABLE 和 DISABLE 。默认为 ENABLE ，如果传入 DISABLE，解析不会生效，也不会验证负载均衡的限制。
     */
    @SerializedName("Status")
    @Expose
    private String Status;

    public CreateRecordDTO() {
        RecordType = "CNAME";
        RecordLine = "默认";
    }

    @Override
    protected void toMap(HashMap<String, String> map, String prefix) {
        this.setParamSimple(map, prefix + "Domain", this.Domain);
        this.setParamSimple(map, prefix + "RecordType", this.RecordType);
        this.setParamSimple(map, prefix + "RecordLine", this.RecordLine);
        this.setParamSimple(map, prefix + "Value", this.Value);
        this.setParamSimple(map, prefix + "DomainId", this.DomainId);
        this.setParamSimple(map, prefix + "SubDomain", this.SubDomain);
        this.setParamSimple(map, prefix + "RecordLineId", this.RecordLineId);
        this.setParamSimple(map, prefix + "MX", this.MX);
        this.setParamSimple(map, prefix + "TTL", this.TTL);
        this.setParamSimple(map, prefix + "Weight", this.Weight);
        this.setParamSimple(map, prefix + "Status", this.Status);
    }


}
