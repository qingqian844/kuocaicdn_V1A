package com.kuocai.cdn.api.huawei.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


/**
 * 创建预热任务。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class PreheatingTaskDTO {

    /**
     * 输入URL必须带有“http://”或“https://”，多个URL用逗号分隔
     * 目前不支持对目录的预热，单个url的长度限制为4096字符,单次最多输入1000个url。
     */
    private String[] urls;

    /**
     * 单页最大数量，取值范围为1-10000。
     * page_size和page_number必须同时传值。默认值30。
     */
    private Integer page_size;

    /**
     * 当前查询第几页，取值范围为1-65535。
     * 默认值1。
     */
    private Integer page_number;

    /**
     * 任务状态。
     * task_inprocess 表示任务处理中，task_done表示任务完成。
     */
    private String status;

    /**
     * 查询起始时间，相对于UTC 1970-01-01到当前时间相隔的毫秒数。
     */
    private Long start_date;

    /**
     * 查询结束时间，相对于UTC 1970-01-01到当前时间相隔的毫秒数。
     */
    private Long end_date;

    /**
     * 用来排序的字段，支持的字段有
     * “task_type”，“total”，“processing”， “succeed”，“failed”，“create_time”。
     * order_field和order_type必须同时传值，否则使用默认值"create_time" 和 "desc"。
     */
    private String order_field;

    /**
     * desc 或者asc。
     * 默认值desc。
     */
    private String order_type;

    /**
     * 默认是文件file。
     * file：文件,directory：目录。
     */
    private String file_type;

    /**
     * url的地址。
     */
    private String url;

    /**
     * 刷新预热任务的创建时间。不传参默认为查询7天内的任务。
     * 最长可查询15天内数据。
     */
    private Long create_time;
}
