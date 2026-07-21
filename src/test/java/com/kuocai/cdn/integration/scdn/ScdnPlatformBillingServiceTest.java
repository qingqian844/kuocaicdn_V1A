package com.kuocai.cdn.integration.scdn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.entity.TransactionOrder;
import com.kuocai.cdn.enumeration.UserStatus;
import com.kuocai.cdn.service.SysUserService;
import com.kuocai.cdn.service.TransactionOrderService;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScdnPlatformBillingServiceTest {
    private JdbcTemplate jdbc;
    private ScdnPlatformBillingService service;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:billing-" + System.nanoTime() +
                ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(dataSource);
        createSchema();
        jdbc.update("INSERT INTO sys_user_account (id,user_id,account_balance) VALUES (1,7,100.000000)");

        SysUserService users = mock(SysUserService.class);
        when(users.queryCacheUserById(7L)).thenReturn(SysUser.builder().id(7L).userName("billing-user")
                .status(UserStatus.CERTIFIED.getCode()).roleId(2L).build());
        TransactionOrderService orders = mock(TransactionOrderService.class);
        AtomicLong ids = new AtomicLong(100);
        when(orders.save(any(TransactionOrder.class))).thenAnswer(invocation -> {
            TransactionOrder order = invocation.getArgument(0);
            order.setId(ids.incrementAndGet());
            return order;
        });
        ObjectMapper objectMapper = new ObjectMapper();
        service = new ScdnPlatformBillingService(jdbc, objectMapper, users, orders,
                new ScdnPlatformEventService(jdbc, objectMapper), mock(ScdnStateEventReconciler.class));
    }

    @Test
    void duplicateDebitIsReplayedWithoutChargingTwice() {
        ScdnContracts.WalletOperationRequest request = wallet("usage:20260721:001", "30.00", null);

        ScdnContracts.WalletOperationResponse first = service.debit("idem-debit-00000001", request);
        ScdnContracts.WalletOperationResponse replay = service.debit("idem-debit-00000001", request);

        assertEquals(first.getPlatformOrderId(), replay.getPlatformOrderId());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM scdn_wallet_ledger", Integer.class));
        assertEquals(new BigDecimal("70.000000"), jdbc.queryForObject(
                "SELECT account_balance FROM sys_user_account WHERE user_id=7", BigDecimal.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM scdn_outbox_event", Integer.class));
        assertFalse(jdbc.queryForObject("SELECT payload_json FROM scdn_outbox_event", String.class)
                .contains("balanceAfter"));
        assertFalse(jdbc.queryForObject("SELECT response_json FROM scdn_idempotency_record", String.class)
                .contains("balanceAfter"));
    }

    @Test
    void rejectsIdempotencyKeyReuseWithDifferentAmount() {
        service.debit("idem-debit-00000002", wallet("usage:20260721:002", "10.00", null));

        ScdnIntegrationException exception = assertThrows(ScdnIntegrationException.class,
                () -> service.debit("idem-debit-00000002", wallet("usage:20260721:002", "11.00", null)));

        assertEquals("IDEMPOTENCY_KEY_CONFLICT", exception.getCode());
        assertEquals(new BigDecimal("90.000000"), jdbc.queryForObject(
                "SELECT account_balance FROM sys_user_account WHERE user_id=7", BigDecimal.class));
    }

    @Test
    void refundCannotExceedOriginalDebit() {
        service.debit("idem-debit-00000003", wallet("usage:20260721:003", "25.00", null));
        service.refund("idem-refund-0000001", wallet("refund:20260721:003a", "10.00", "usage:20260721:003"));

        ScdnIntegrationException exception = assertThrows(ScdnIntegrationException.class,
                () -> service.refund("idem-refund-0000002", wallet("refund:20260721:003b", "16.00", "usage:20260721:003")));

        assertEquals("REFUND_EXCEEDS_DEBIT", exception.getCode());
        assertEquals(new BigDecimal("85.000000"), jdbc.queryForObject(
                "SELECT account_balance FROM sys_user_account WHERE user_id=7", BigDecimal.class));
    }

    private ScdnContracts.WalletOperationRequest wallet(String reference, String amount, String original) {
        ScdnContracts.WalletOperationRequest request = new ScdnContracts.WalletOperationRequest();
        request.setBusinessReference(reference);
        request.setUserId(7L);
        request.setAmount(new BigDecimal(amount));
        request.setDescription("test");
        request.setOriginalBusinessReference(original);
        return request;
    }

    private void createSchema() {
        jdbc.execute("CREATE TABLE sys_user_account (id BIGINT PRIMARY KEY,user_id BIGINT UNIQUE,account_balance DECIMAL(18,6),update_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE scdn_wallet_ledger (id BIGINT AUTO_INCREMENT PRIMARY KEY,business_reference VARCHAR(128) UNIQUE,original_business_reference VARCHAR(128),operation VARCHAR(16),transaction_order_id BIGINT,user_id BIGINT,amount DECIMAL(18,6),balance_after DECIMAL(18,6),create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE scdn_idempotency_record (id BIGINT AUTO_INCREMENT PRIMARY KEY,idempotency_key VARCHAR(128) UNIQUE,operation_type VARCHAR(32),request_hash VARCHAR(64),response_json CLOB,status VARCHAR(16),create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE scdn_outbox_event (id BIGINT AUTO_INCREMENT PRIMARY KEY,event_id VARCHAR(64) UNIQUE,event_type VARCHAR(64),aggregate_type VARCHAR(32),aggregate_id VARCHAR(128),payload_json CLOB,status VARCHAR(16) DEFAULT 'pending',attempt_count INT DEFAULT 0,available_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,published_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE scdn_order_link (id BIGINT AUTO_INCREMENT PRIMARY KEY,external_order_id VARCHAR(96) UNIQUE,transaction_order_id BIGINT UNIQUE,user_id BIGINT,amount DECIMAL(18,6),last_event_status VARCHAR(32),create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    }
}
