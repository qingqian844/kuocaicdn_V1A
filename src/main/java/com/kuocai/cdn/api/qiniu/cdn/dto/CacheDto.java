package com.kuocai.cdn.api.qiniu.cdn.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 预热文件DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CacheDto {

    private long uid;
    private String requestId;
    private String taskId;
    private String isDir;
    private String url;
    private int progress;
    private String state;
    private String stateDetail;
    private String midState;
    private Date createAt;
    private Date beginAt;
    private Date endAt;
}