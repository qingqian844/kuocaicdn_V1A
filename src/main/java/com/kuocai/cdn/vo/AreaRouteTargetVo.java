package com.kuocai.cdn.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AreaRouteTargetVo {
    private String targetKey;
    private String route;
    private String routeName;
}
