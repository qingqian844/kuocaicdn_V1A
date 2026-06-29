package com.kuocai.cdn.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderMessageDTO {

    /**
     * 聊天内容
     */
    private String msg;

    /**
     * 聊天内容类型
     * img｜text
     */
    private String type;

    /**
     * 聊天时间
     * yyyy-MM-dd HH:mm:ss
     */
    private String time;

    /**
     * 聊天发送人
     */
    private String from;

}
