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

    /**
     * 附件原始文件名。
     */
    private String fileName;

    /**
     * 附件大小（字节）。
     */
    private Long fileSize;

    /**
     * 附件 MIME 类型。
     */
    private String contentType;

    /**
     * MinIO 对象名，仅用于生成受保护的附件地址。
     */
    private String storageKey;

    /**
     * 当前工单内可访问的附件预览地址。
     */
    private String attachmentUrl;

    /**
     * 当前工单内可访问的附件下载地址。
     */
    private String downloadUrl;

    /**
     * 前端展示用的附件大小。
     */
    private String fileSizeText;

}
