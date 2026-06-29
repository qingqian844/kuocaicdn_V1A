package com.kuocai.cdn.common.mongo.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "logs")
public class Logs {
    @Id
    private String id;
    private String level;
    private String logger;
    private String traceId;
    private String thread;
    private String message;
    private String callerData;
    private String throwableProxy;
    private String timestamp;
}
