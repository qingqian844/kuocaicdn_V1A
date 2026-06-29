package com.kuocai.cdn.vo;

import lombok.Builder;
import lombok.Data;

/**
 * @author xiaobo
 * @date 2023/7/23
 */
@Data
@Builder
public class WorkOrderMessageRemindVo {

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 工单标题
     */
    private String workOrderTitle;

    /**
     * 消息内容
     */
    private String workOrderContent;


}
