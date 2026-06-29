package com.kuocai.cdn.api.tencent.dns.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.tencentcloudapi.common.AbstractModel;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.HashMap;

/**
 * @author xiaobo
 * @date 2023/3/6
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class DeleteRecordDTO extends AbstractModel {

    /**
     * 域名
     */
    @SerializedName("Domain")
    @Expose
    private String Domain;

    /**
     * 记录 ID 。可以通过接口DescribeRecordList查到所有的解析记录列表以及对应的RecordId
     */
    @SerializedName("RecordId")
    @Expose
    private Long RecordId;

    /**
     * 域名 ID 。参数 DomainId 优先级比参数 Domain 高，如果传递参数 DomainId 将忽略参数 Domain 。可以通过接口DescribeDomainList查到所有的Domain以及DomainId
     */
    @SerializedName("DomainId")
    @Expose
    private Long DomainId;

    /**
     * Internal implementation, normal users should not use it.
     */
    @Override
    public void toMap(HashMap<String, String> map, String prefix) {
        this.setParamSimple(map, prefix + "Domain", this.Domain);
        this.setParamSimple(map, prefix + "RecordId", this.RecordId);
        this.setParamSimple(map, prefix + "DomainId", this.DomainId);

    }
}
