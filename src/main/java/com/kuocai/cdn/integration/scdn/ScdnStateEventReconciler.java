package com.kuocai.cdn.integration.scdn;

import com.kuocai.cdn.config.ScdnIntegrationProperties;
import com.kuocai.cdn.constant.TransactionOrderStatus;
import com.kuocai.cdn.service.SysUserBannedService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ScdnStateEventReconciler {
    private final JdbcTemplate jdbcTemplate;
    private final SysUserBannedService bannedService;
    private final ScdnPlatformEventService events;
    private final ScdnIntegrationProperties properties;

    public ScdnStateEventReconciler(JdbcTemplate jdbcTemplate,
                                    SysUserBannedService bannedService,
                                    ScdnPlatformEventService events,
                                    ScdnIntegrationProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.bannedService = bannedService;
        this.events = events;
        this.properties = properties;
    }

    public boolean isBanned(Long userId) {
        return userId != null && bannedService.queryByUserId(userId) != null;
    }

    public void trackUser(ScdnContracts.UserEligibilityResponse user, boolean banned) {
        if (!properties.isEnabled() || user == null || user.getUserId() == null) {
            return;
        }
        jdbcTemplate.update(
                "INSERT IGNORE INTO scdn_user_state_snapshot " +
                        "(user_id,account_status,real_name_verified,agent_user_id,banned) VALUES (?,?,?,?,?)",
                user.getUserId(), user.getAccountStatus(), user.isRealNameVerified(), user.getAgentUserId(), banned);
    }

    @Scheduled(fixedDelayString = "${scdn.integration.state-reconcile-delay-ms:5000}")
    @Transactional(rollbackFor = Exception.class)
    public void reconcile() {
        if (!properties.isEnabled()) {
            return;
        }
        reconcileOrderStates();
        reconcileUserStates();
    }

    private void reconcileOrderStates() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT l.external_order_id,l.last_event_status,o.status,o.order_num,o.user_id,o.amount " +
                        "FROM scdn_order_link l JOIN transaction_order o ON o.id=l.transaction_order_id " +
                        "WHERE COALESCE(l.last_event_status,'')<>COALESCE(o.status,'') ORDER BY l.id LIMIT 100 FOR UPDATE");
        for (Map<String, Object> row : rows) {
            String externalOrderId = string(row.get("external_order_id"));
            String status = string(row.get("status"));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("externalOrderId", externalOrderId);
            payload.put("platformOrderNumber", string(row.get("order_num")));
            payload.put("userId", row.get("user_id"));
            payload.put("amount", row.get("amount"));
            payload.put("status", status);
            events.enqueue(orderEventType(status), "order", externalOrderId, payload);
            jdbcTemplate.update("UPDATE scdn_order_link SET last_event_status=? WHERE external_order_id=?",
                    status, externalOrderId);
        }
    }

    private void reconcileUserStates() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT s.user_id,s.account_status previous_status,s.real_name_verified previous_verified," +
                        "s.agent_user_id previous_agent_user_id,s.banned previous_banned,u.status account_status," +
                        "CASE WHEN u.role_id=1 OR u.status='certified' THEN 1 ELSE 0 END real_name_verified," +
                        "u.agent_user_id,CASE WHEN EXISTS (SELECT 1 FROM sys_user_banned b WHERE b.user_id=u.id) " +
                        "THEN 1 ELSE 0 END banned,u.role_id " +
                        "FROM scdn_user_state_snapshot s JOIN sys_user u ON u.id=s.user_id " +
                        "WHERE s.account_status<>u.status " +
                        "OR s.real_name_verified<>(CASE WHEN u.role_id=1 OR u.status='certified' THEN 1 ELSE 0 END) " +
                        "OR COALESCE(s.agent_user_id,0)<>COALESCE(u.agent_user_id,0) " +
                        "OR s.banned<>(CASE WHEN EXISTS (SELECT 1 FROM sys_user_banned b WHERE b.user_id=u.id) " +
                        "THEN 1 ELSE 0 END) ORDER BY s.user_id LIMIT 100 FOR UPDATE");
        for (Map<String, Object> row : rows) {
            long userId = number(row.get("user_id"));
            String status = string(row.get("account_status"));
            boolean verified = bool(row.get("real_name_verified"));
            boolean banned = bool(row.get("banned"));
            Long agentUserId = nullableNumber(row.get("agent_user_id"));
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("userId", userId);
            payload.put("accountStatus", status);
            payload.put("realNameVerified", verified);
            payload.put("agentUserId", agentUserId);
            payload.put("banned", banned);
            payload.put("eligible", !banned && verified && !"cancellation".equals(status));
            if (bool(row.get("previous_banned")) != banned) {
                events.enqueue(banned ? "scdn.user.frozen" : "scdn.user.unfrozen", "user",
                        Long.toString(userId), payload);
            }
            if (bool(row.get("previous_verified")) != verified) {
                events.enqueue("scdn.user.real_name_changed", "user", Long.toString(userId), payload);
            }
            if (!Objects.equals(string(row.get("previous_status")), status)
                    || !Objects.equals(nullableNumber(row.get("previous_agent_user_id")), agentUserId)) {
                events.enqueue("scdn.user.eligibility_changed", "user", Long.toString(userId), payload);
            }
            jdbcTemplate.update(
                    "UPDATE scdn_user_state_snapshot SET account_status=?,real_name_verified=?,agent_user_id=?,banned=? " +
                            "WHERE user_id=?",
                    status, verified, agentUserId, banned, userId);
        }
    }

    private String orderEventType(String status) {
        if (TransactionOrderStatus.TRADE_SUCCESS.equals(status)) return "scdn.order.paid";
        if (TransactionOrderStatus.REFUND.equals(status)) return "scdn.order.refunded";
        if (TransactionOrderStatus.EXPIRED.equals(status)) return "scdn.order.expired";
        return "scdn.order.status_changed";
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }

    private long number(Object value) {
        return ((Number) value).longValue();
    }

    private Long nullableNumber(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private boolean bool(Object value) {
        return value instanceof Boolean ? (Boolean) value : ((Number) value).intValue() != 0;
    }
}
