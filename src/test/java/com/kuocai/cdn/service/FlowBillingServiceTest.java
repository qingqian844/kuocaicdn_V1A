package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlowBillingServiceTest {

    @Test
    void extractsFlowFromNestedResourceResponse() {
        JSONObject response = JSONObject.parseObject(
                "{\"Resource\":{\"resource_summary\":{\"access_flux_byte\":36132783104,\"bs_flux_byte\":1024}}}"
        );

        assertEquals(36132783104L, FlowBillingService.extractUsedFlow(response));
    }

    @Test
    void extractsFlowFromDirectResourceResponseWithLegacyField() {
        JSONObject response = JSONObject.parseObject(
                "{\"resource_summary\":{\"flux_byte\":1024}}"
        );

        assertEquals(1024L, FlowBillingService.extractUsedFlow(response));
    }

    @Test
    void doesNotBillOriginFlowBytes() {
        JSONObject response = JSONObject.parseObject(
                "{\"resource_summary\":{\"flux_byte\":1024,\"bs_flux_byte\":2048}}"
        );

        assertEquals(1024L, FlowBillingService.extractUsedFlow(response));
    }

    @Test
    void returnsZeroWhenFlowSummaryIsMissing() {
        assertEquals(0L, FlowBillingService.extractUsedFlow(new JSONObject()));
    }
}
