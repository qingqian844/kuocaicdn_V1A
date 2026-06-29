package com.kuocai.cdn.api.kingsoft.cdn.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class PageCompressConfig {

    @JSONField(name = "Enable")
    private String enable;

    @JSONField(name = "CompressType")
    private String compressType;

    @JSONField(name = "FileType")
    private String fileType;
    
    @JSONField(name = "FileSize")
    private String fileSize;
} 