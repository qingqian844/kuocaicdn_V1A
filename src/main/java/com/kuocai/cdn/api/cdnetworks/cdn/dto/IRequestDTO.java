package com.kuocai.cdn.api.cdnetworks.cdn.dto;

import java.util.Map;

public interface IRequestDTO {
    Map<String, Object> toMap();

    String toJson();
}
