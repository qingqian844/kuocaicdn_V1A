package com.kuocai.cdn.service;

import com.kuocai.cdn.api.tencent.dns.CreateRecordResponse;
import com.kuocai.cdn.api.tencent.dns.dto.CreateRecordDTO;
import com.kuocai.cdn.dao.CdnDomainDao;
import com.kuocai.cdn.dao.SelfHostedCacheJobDao;
import com.kuocai.cdn.dao.SelfHostedCacheJobNodeDao;
import com.kuocai.cdn.dao.SelfHostedDomainConfigDao;
import com.kuocai.cdn.dao.SelfHostedGroupNodeDao;
import com.kuocai.cdn.dao.SelfHostedNodeDao;
import com.kuocai.cdn.dao.SelfHostedNodeGroupDao;
import com.kuocai.cdn.dao.SelfHostedPortForwardDao;
import com.kuocai.cdn.entity.SelfHostedGroupNode;
import com.kuocai.cdn.entity.SelfHostedNode;
import com.kuocai.cdn.entity.SelfHostedNodeGroup;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SelfHostedCdnDnsSyncTest {
    @Test
    void migratesTenDirectRecordsThroughOneShardWithoutExceedingDnsLimit() throws Exception {
        Fixture fixture = fixture(ips(2, 11));
        LinkedHashMap<String, Long> legacy = new LinkedHashMap<>();
        for (int index = 1; index <= 10; index++) {
            String ip = "192.0.2." + index;
            long id = index;
            legacy.put(SelfHostedDnsPlan.recordKey("A", ip), id);
            fixture.service.preload(id, "edge", "A", ip);
        }
        fixture.group.setDnsRecordIds(com.alibaba.fastjson.JSON.toJSONString(legacy));

        fixture.service.syncGroupDns(1L);

        SelfHostedDnsPlan.State saved = SelfHostedDnsPlan.State.parse(fixture.group.getDnsRecordIds());
        String shard = SelfHostedDnsPlan.shardLabel(fixture.group, 1);
        assertEquals(1, saved.parentRecords.size());
        assertEquals(10, saved.shardRecords.get(shard).size());
        assertTrue(saved.legacyDirectRecords.isEmpty());
        assertEquals(1, fixture.service.countBySubDomain("edge"));
        assertEquals(10, fixture.service.countBySubDomain(shard));
        assertTrue(fixture.service.firstOperationFor("create:edge:CNAME")
                > fixture.service.lastOperationFor("delete:edge:A"));
    }

    @Test
    void fullShardDeletesStaleAddressBeforeAddingReplacement() throws Exception {
        Fixture fixture = fixture(ips(2, 11));
        String shard = SelfHostedDnsPlan.shardLabel(fixture.group, 1);
        SelfHostedDnsPlan.State state = new SelfHostedDnsPlan.State();
        state.parentRecords.put(SelfHostedDnsPlan.recordKey(
                "CNAME", shard + "." + com.kuocai.cdn.api.tencent.dns.properties.TencentDns.LOCAL_DOMAIN_NAME), 100L);
        fixture.service.preload(100L, "edge", "CNAME",
                shard + "." + com.kuocai.cdn.api.tencent.dns.properties.TencentDns.LOCAL_DOMAIN_NAME);
        LinkedHashMap<String, Long> shardRecords = new LinkedHashMap<>();
        for (int index = 1; index <= 10; index++) {
            String ip = "192.0.2." + index;
            long id = 100L + index;
            shardRecords.put(SelfHostedDnsPlan.recordKey("A", ip), id);
            fixture.service.preload(id, shard, "A", ip);
        }
        state.shardRecords.put(shard, shardRecords);
        fixture.group.setDnsRecordIds(state.toJson());

        fixture.service.syncGroupDns(1L);

        SelfHostedDnsPlan.State saved = SelfHostedDnsPlan.State.parse(fixture.group.getDnsRecordIds());
        assertFalse(saved.shardRecords.get(shard).containsKey(
                SelfHostedDnsPlan.recordKey("A", "192.0.2.1")));
        assertTrue(saved.shardRecords.get(shard).containsKey(
                SelfHostedDnsPlan.recordKey("A", "192.0.2.11")));
        assertTrue(fixture.service.firstOperationFor("delete:" + shard + ":A")
                < fixture.service.firstOperationFor("create:" + shard + ":A"));
        assertEquals(10, fixture.service.countBySubDomain(shard));
    }

    private Fixture fixture(List<String> desiredIps) {
        SelfHostedNodeDao nodeDao = mock(SelfHostedNodeDao.class);
        SelfHostedNodeGroupDao groupDao = mock(SelfHostedNodeGroupDao.class);
        SelfHostedGroupNodeDao groupNodeDao = mock(SelfHostedGroupNodeDao.class);
        SelfHostedDomainConfigDao domainConfigDao = mock(SelfHostedDomainConfigDao.class);
        SelfHostedNodeGroup group = SelfHostedNodeGroup.builder().id(1L).groupName("overseas")
                .cnameLabel("edge").coverage("overseas").status("enabled").build();
        when(groupDao.selectById(1L)).thenReturn(group);
        when(groupDao.updateById(any(SelfHostedNodeGroup.class))).thenReturn(1);
        when(domainConfigDao.selectCount(any())).thenReturn(0L);

        List<SelfHostedGroupNode> relations = new ArrayList<>();
        Map<Long, SelfHostedNode> nodes = new LinkedHashMap<>();
        long id = 1L;
        for (String ip : desiredIps) {
            relations.add(SelfHostedGroupNode.builder().groupId(1L).nodeId(id).build());
            nodes.put(id, SelfHostedNode.builder().id(id).host(ip).enabled(1).status("online")
                    .lastHeartbeat(new Date()).build());
            id++;
        }
        when(groupNodeDao.selectList(any())).thenReturn(relations);
        when(nodeDao.selectById(any())).thenAnswer(invocation -> nodes.get(invocation.getArgument(0)));

        FakeDnsService service = new FakeDnsService(nodeDao, groupDao, groupNodeDao, domainConfigDao);
        return new Fixture(group, service);
    }

    private List<String> ips(int start, int end) {
        List<String> result = new ArrayList<>();
        for (int value = start; value <= end; value++) {
            result.add("192.0.2." + value);
        }
        return result;
    }

    private static final class Fixture {
        private final SelfHostedNodeGroup group;
        private final FakeDnsService service;

        private Fixture(SelfHostedNodeGroup group, FakeDnsService service) {
            this.group = group;
            this.service = service;
        }
    }

    private static final class FakeDnsService extends SelfHostedCdnService {
        private final Map<Long, DnsRecord> records = new LinkedHashMap<>();
        private final List<String> operations = new ArrayList<>();
        private long nextId = 1000L;

        private FakeDnsService(SelfHostedNodeDao nodeDao, SelfHostedNodeGroupDao groupDao,
                               SelfHostedGroupNodeDao groupNodeDao,
                               SelfHostedDomainConfigDao domainConfigDao) {
            super(nodeDao, groupDao, groupNodeDao, domainConfigDao,
                    mock(SelfHostedCacheJobDao.class), mock(SelfHostedCacheJobNodeDao.class),
                    mock(CdnDomainDao.class), mock(SelfHostedPortForwardDao.class));
        }

        private void preload(long id, String subDomain, String type, String value) {
            records.put(id, new DnsRecord(subDomain, type, value));
        }

        @Override
        protected CreateRecordResponse createDnsRecord(CreateRecordDTO request) throws Exception {
            int count = countBySubDomain(request.getSubDomain());
            if (count >= SelfHostedDnsPlan.ADDRESSES_PER_SHARD) {
                throw new Exception("子域名负载均衡数量超出套餐限制");
            }
            for (DnsRecord record : records.values()) {
                if (!record.subDomain.equals(request.getSubDomain())) {
                    continue;
                }
                if (("CNAME".equals(record.type) && !"CNAME".equals(request.getRecordType()))
                        || (!"CNAME".equals(record.type) && "CNAME".equals(request.getRecordType()))) {
                    throw new Exception("CNAME 与其他记录类型冲突");
                }
            }
            long id = nextId++;
            records.put(id, new DnsRecord(request.getSubDomain(), request.getRecordType(), request.getValue()));
            operations.add("create:" + request.getSubDomain() + ":" + request.getRecordType()
                    + ":" + request.getValue());
            return new CreateRecordResponse(id, "test");
        }

        @Override
        protected void deleteDnsRecord(Long recordId) throws Exception {
            DnsRecord removed = records.remove(recordId);
            if (removed == null) {
                throw new Exception("record not exist");
            }
            operations.add("delete:" + removed.subDomain + ":" + removed.type + ":" + removed.value);
        }

        private int countBySubDomain(String subDomain) {
            int count = 0;
            for (DnsRecord record : records.values()) {
                if (record.subDomain.equals(subDomain)) {
                    count++;
                }
            }
            return count;
        }

        private int firstOperationFor(String prefix) {
            for (int index = 0; index < operations.size(); index++) {
                if (operations.get(index).startsWith(prefix)) {
                    return index;
                }
            }
            return -1;
        }

        private int lastOperationFor(String prefix) {
            int result = -1;
            for (int index = 0; index < operations.size(); index++) {
                if (operations.get(index).startsWith(prefix)) {
                    result = index;
                }
            }
            return result;
        }
    }

    private static final class DnsRecord {
        private final String subDomain;
        private final String type;
        private final String value;

        private DnsRecord(String subDomain, String type, String value) {
            this.subDomain = subDomain;
            this.type = type;
            this.value = value;
        }
    }
}
