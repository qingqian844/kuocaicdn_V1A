package com.kuocai.cdn.api.tencent.dns;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.tencentcloudapi.common.AbstractModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashMap;

/**
 * @author xiaobo
 * @date 2023/3/6
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ModifyRecordResponse extends AbstractModel {

    /**
     * 记录ID
     */
    @SerializedName("RecordId")
    @Expose
    private Long RecordId;

    /**
     * 唯一请求 ID，每次请求都会返回。定位问题时需要提供该次请求的 RequestId。
     */
    @SerializedName("RequestId")
    @Expose
    private String RequestId;


    /**
     * Internal implementation, normal users should not use it.
     */
    @Override
    public void toMap(HashMap<String, String> map, String prefix) {
        this.setParamSimple(map, prefix + "RecordId", this.RecordId);
        this.setParamSimple(map, prefix + "RequestId", this.RequestId);

    }
}
