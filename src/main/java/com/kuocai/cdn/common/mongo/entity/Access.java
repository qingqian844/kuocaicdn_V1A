package com.kuocai.cdn.common.mongo.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "access")
public class Access {
    @Id
    private String id;
    private Long userId;
    private String trackId;
    private String ip;
    private String method;
    private String url;
    private String params;
    private String ua;
    private String referer;
    private String time;
}
