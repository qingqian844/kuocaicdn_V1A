package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 查询刷新预热URL记录参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class PreheatingUrlTaskDTO {

    /**
     * 起始时间戳（毫秒），默认当天00:00
     * 必选: 否
     */
    private Long start_time;

    /**
     * 结束时间戳（毫秒），默认次日00:00
     * 必选: 否
     */
    private Long end_time;

    /**
     * 偏移量
     * 必选: 否
     */
    private Integer offset;

    /**
     * 单次查询数据条数，上限为100
     * 必选: 否
     */
    private Integer limit;

    /**
     * 刷新预热url
     * 必选: 否
     */
    private String url;

    /**
     * 任务类型，
     * REFRESH：刷新任务；PREHEATING：预热任务
     * 必选: 否
     */
    private String task_type;

    /**
     * url状态，
     * 状态类型：processing：处理中；succeed：完成；failed：失败；waiting：等待；refreshing：刷新中; preheating : 预热中
     * 必选: 否
     */
    private String status;

    /**
     * 文件类型，file:文件;directory:目录
     * 必选: 否
     */
    private String file_type;
}
