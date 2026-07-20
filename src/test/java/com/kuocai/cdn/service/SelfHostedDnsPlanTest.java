package com.kuocai.cdn.service;

import com.kuocai.cdn.entity.SelfHostedNodeGroup;
import com.kuocai.cdn.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelfHostedDnsPlanTest {
    @Test
    void oneHundredAddressesAreSplitIntoTenGroupsOfTen() throws Exception {
        SelfHostedNodeGroup group = SelfHostedNodeGroup.builder()
                .id(35L).cnameLabel("edge").build();
        Set<String> addresses = addresses(100);

        SelfHostedDnsPlan.Plan plan = SelfHostedDnsPlan.build(group, addresses,
                new SelfHostedDnsPlan.State(), "dns.example.com");

        assertEquals(10, plan.shardAddresses.size());
        assertEquals(10, plan.parentRecords.size());
        for (Map.Entry<String, LinkedHashSet<String>> shard : plan.shardAddresses.entrySet()) {
            assertEquals(10, shard.getValue().size());
            assertTrue(shard.getKey().length() <= 63);
        }
    }

    @Test
    void addressOneHundredAndOneIsRejected() {
        SelfHostedNodeGroup group = SelfHostedNodeGroup.builder()
                .id(1L).cnameLabel("edge").build();

        BusinessException error = assertThrows(BusinessException.class,
                () -> SelfHostedDnsPlan.build(group, addresses(101),
                        new SelfHostedDnsPlan.State(), "dns.example.com"));

        assertTrue(error.getMessage().contains("最多支持100个 IP 地址"));
    }

    @Test
    void partialFinalShardIsBalancedInsteadOfOverloadingOneNode() throws Exception {
        SelfHostedDnsPlan.Plan plan = SelfHostedDnsPlan.build(
                SelfHostedNodeGroup.builder().id(1L).cnameLabel("edge").build(),
                addresses(11), new SelfHostedDnsPlan.State(), "dns.example.com");

        List<Integer> sizes = new java.util.ArrayList<>();
        for (LinkedHashSet<String> shard : plan.shardAddresses.values()) {
            sizes.add(shard.size());
        }
        assertEquals(java.util.Arrays.asList(6, 5), sizes);
    }

    @Test
    void legacyFlatRecordMapIsKeptForMigration() {
        SelfHostedDnsPlan.State state = SelfHostedDnsPlan.State.parse(
                "{\"A|192.0.2.1\":101,\"A|192.0.2.2\":102}");

        assertEquals(2, state.legacyDirectRecords.size());
        assertTrue(state.parentRecords.isEmpty());
        assertTrue(state.shardRecords.isEmpty());
    }

    private Set<String> addresses(int count) {
        Set<String> result = new LinkedHashSet<>();
        for (int index = 1; index <= count; index++) {
            int third = (index - 1) / 254;
            int fourth = (index - 1) % 254 + 1;
            result.add(SelfHostedDnsPlan.recordKey("A", "192.0." + third + "." + fourth));
        }
        return result;
    }
}
