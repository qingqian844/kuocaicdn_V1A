package com.kuocai.cdn.api.cdnetworks.cdn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryOriginProtocolDTO implements IRequestDTO {
    private String domain = "";

    @Override
    public Map<String, Object> toMap() {
        return null;
    }

    @Override
    public String toJson() {
        return null;
    }
}
