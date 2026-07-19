package com.kuocai.cdn.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ResolvedAreaRouteVo {
    private String serviceArea;
    private String mode;
    private List<AreaRouteTargetVo> targets;

    public boolean isMultiCdn() {
        return "multi_cdn".equals(mode) && targets != null && targets.size() > 1;
    }

    public AreaRouteTargetVo getPrimaryTarget() {
        return targets == null || targets.isEmpty() ? null : targets.get(0);
    }
}
