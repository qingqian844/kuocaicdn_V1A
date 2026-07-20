package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.entity.SelfHostedNodeGroup;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class SelfHostedDnsPlan {
    static final int STATE_VERSION = 2;
    static final int ADDRESSES_PER_SHARD = 10;
    static final int MAX_SHARDS = 10;
    static final int MAX_ADDRESSES = ADDRESSES_PER_SHARD * MAX_SHARDS;

    private SelfHostedDnsPlan() {
    }

    static Plan build(SelfHostedNodeGroup group, Set<String> requestedAddresses,
                      State current, String dnsDomain) throws BusinessException {
        List<String> desiredAddresses = new ArrayList<>(new LinkedHashSet<>(requestedAddresses));
        Collections.sort(desiredAddresses);
        if (desiredAddresses.size() > MAX_ADDRESSES) {
            throw new BusinessException("自建 CDN 节点组最多支持" + MAX_ADDRESSES
                    + "个 IP 地址，当前需要" + desiredAddresses.size() + "个");
        }

        int shardCount = (desiredAddresses.size() + ADDRESSES_PER_SHARD - 1) / ADDRESSES_PER_SHARD;
        LinkedHashMap<String, LinkedHashSet<String>> shards = new LinkedHashMap<>();
        for (int index = 1; index <= shardCount; index++) {
            shards.put(shardLabel(group, index), new LinkedHashSet<>());
        }

        Set<String> desiredSet = new LinkedHashSet<>(desiredAddresses);
        Set<String> assigned = new LinkedHashSet<>();
        for (Map.Entry<String, LinkedHashSet<String>> entry : shards.entrySet()) {
            Map<String, Long> existing = current.shardRecords.get(entry.getKey());
            if (existing == null) {
                continue;
            }
            for (String key : existing.keySet()) {
                if (entry.getValue().size() >= ADDRESSES_PER_SHARD) {
                    break;
                }
                if (desiredSet.contains(key) && assigned.add(key)) {
                    entry.getValue().add(key);
                }
            }
        }

        for (String key : desiredAddresses) {
            if (assigned.contains(key)) {
                continue;
            }
            for (LinkedHashSet<String> shard : shards.values()) {
                if (shard.size() < ADDRESSES_PER_SHARD) {
                    shard.add(key);
                    assigned.add(key);
                    break;
                }
            }
        }

        LinkedHashSet<String> parentRecords = new LinkedHashSet<>();
        for (String shard : shards.keySet()) {
            parentRecords.add(recordKey("CNAME", fqdn(shard, dnsDomain)));
        }
        return new Plan(shards, parentRecords);
    }

    static String shardLabel(SelfHostedNodeGroup group, int index) {
        String idPart = group.getId() == null ? "new" : Long.toString(group.getId(), 36);
        String suffix = "-g" + idPart + "-" + String.format(Locale.ROOT, "%02d", index);
        String base = group.getCnameLabel();
        int maxBaseLength = 63 - suffix.length();
        if (base.length() > maxBaseLength) {
            base = base.substring(0, maxBaseLength);
        }
        while (base.endsWith("-")) {
            base = base.substring(0, base.length() - 1);
        }
        return (Assert.isEmpty(base) ? "edge" : base) + suffix;
    }

    static String recordKey(String recordType, String value) {
        return recordType + "|" + value;
    }

    static String recordType(String recordKey) throws BusinessException {
        int separator = recordKey.indexOf('|');
        if (separator <= 0) {
            throw new BusinessException("DNS 记录状态格式不正确");
        }
        return recordKey.substring(0, separator);
    }

    static String recordValue(String recordKey) throws BusinessException {
        int separator = recordKey.indexOf('|');
        if (separator <= 0 || separator == recordKey.length() - 1) {
            throw new BusinessException("DNS 记录状态格式不正确");
        }
        return recordKey.substring(separator + 1);
    }

    private static String fqdn(String label, String dnsDomain) {
        return label + "." + dnsDomain;
    }

    static final class Plan {
        final LinkedHashMap<String, LinkedHashSet<String>> shardAddresses;
        final LinkedHashSet<String> parentRecords;

        Plan(LinkedHashMap<String, LinkedHashSet<String>> shardAddresses,
             LinkedHashSet<String> parentRecords) {
            this.shardAddresses = shardAddresses;
            this.parentRecords = parentRecords;
        }
    }

    static final class State {
        final LinkedHashMap<String, Long> parentRecords = new LinkedHashMap<>();
        final LinkedHashMap<String, LinkedHashMap<String, Long>> shardRecords = new LinkedHashMap<>();
        final LinkedHashMap<String, Long> legacyDirectRecords = new LinkedHashMap<>();
        final List<Long> legacyRecordIds = new ArrayList<>();

        static State parse(String raw) {
            State state = new State();
            if (Assert.isEmpty(raw)) {
                return state;
            }
            try {
                JSONObject root = JSON.parseObject(raw);
                if (root.getIntValue("version") == STATE_VERSION) {
                    readRecordMap(root.getJSONObject("parent"), state.parentRecords);
                    JSONObject shards = root.getJSONObject("shards");
                    if (shards != null) {
                        for (String label : shards.keySet()) {
                            LinkedHashMap<String, Long> records = new LinkedHashMap<>();
                            readRecordMap(shards.getJSONObject(label), records);
                            state.shardRecords.put(label, records);
                        }
                    }
                    readRecordMap(root.getJSONObject("legacyDirect"), state.legacyDirectRecords);
                    JSONArray legacyIds = root.getJSONArray("legacyIds");
                    if (legacyIds != null) {
                        for (int index = 0; index < legacyIds.size(); index++) {
                            Long recordId = legacyIds.getLong(index);
                            if (recordId != null) {
                                state.legacyRecordIds.add(recordId);
                            }
                        }
                    }
                    return state;
                }
                readRecordMap(root, state.legacyDirectRecords);
                return state;
            } catch (Exception ignored) {
                try {
                    JSONArray legacyIds = JSON.parseArray(raw);
                    for (int index = 0; index < legacyIds.size(); index++) {
                        Long recordId = legacyIds.getLong(index);
                        if (recordId != null) {
                            state.legacyRecordIds.add(recordId);
                        }
                    }
                } catch (Exception malformed) {
                    // A malformed historical state is rebuilt by the next synchronization.
                }
                return state;
            }
        }

        String toJson() {
            JSONObject root = new JSONObject(true);
            root.put("version", STATE_VERSION);
            root.put("parent", recordMapJson(parentRecords));
            JSONObject shards = new JSONObject(true);
            for (Map.Entry<String, LinkedHashMap<String, Long>> entry : shardRecords.entrySet()) {
                shards.put(entry.getKey(), recordMapJson(entry.getValue()));
            }
            root.put("shards", shards);
            root.put("legacyDirect", recordMapJson(legacyDirectRecords));
            JSONArray legacyIds = new JSONArray();
            legacyIds.addAll(legacyRecordIds);
            root.put("legacyIds", legacyIds);
            return root.toJSONString();
        }

        private static void readRecordMap(JSONObject source, Map<String, Long> target) {
            if (source == null) {
                return;
            }
            for (String key : source.keySet()) {
                Long recordId = source.getLong(key);
                if (recordId != null) {
                    target.put(key, recordId);
                }
            }
        }

        private static JSONObject recordMapJson(Map<String, Long> records) {
            JSONObject result = new JSONObject(true);
            for (Map.Entry<String, Long> entry : records.entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
            return result;
        }
    }
}
