package com.kuocai.cdn.api.huawei.cdn.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class LogDTO {

    /**
     * 只支持单个域名，如：www.test1.com。
     * 是否必选：是
     */
    private String domain_name;

    /**
     * 查询开始时间，查询开始时间到开始时间+1天内的日志数据，取值范围是距离当前30天内。
     * 是否必选：是
     */
    private Long query_date;

    /**
     * 单页最大数量，取值范围为1-10000。
     * 是否必选：否
     */
    private Integer page_size;

    /**
     * 当前查询第几页，取值范围为1-65535。
     * 是否必选：否
     */
    private Integer page_number;

    /**
     * 当用户开启企业项目功能时，该参数生效，表示查询资源所属项目，"all"表示所有项目。注意：当使用子帐号调用接口时，该参数必传。
     * 您可以通过调用企业项目管理服务（EPS）的查询企业项目列表接口（ListEnterpriseProject）查询企业项目id。
     * 是否必选：否
     */
    private String enterprise_project_id;
}
