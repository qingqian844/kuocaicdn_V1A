package com.kuocai.cdn.vo.cacheset;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CacheTaskVo {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String taskType;

    private String url;

    private String fileType;

    private String createTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long createTimeLong;

    /**
     * 状态：处理中，完成，失败，等待，刷新中，预热中
     */
    private String status;

    private String progress;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String userName;

    private String img;
}
