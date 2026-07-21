package com.kuocai.cdn.integration.scdn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuocai.cdn.config.ScdnIntegrationProperties;
import com.kuocai.cdn.service.SysUserBannedService;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ScdnStateEventReconcilerTest {
    private JdbcTemplate jdbc;
    private ScdnStateEventReconciler reconciler;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:state-events-" + System.nanoTime()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(dataSource);
        createSchema();
        ScdnIntegrationProperties properties = new ScdnIntegrationProperties();
        properties.setEnabled(true);
        reconciler = new ScdnStateEventReconciler(jdbc, mock(SysUserBannedService.class),
                new ScdnPlatformEventService(jdbc, new ObjectMapper()), properties);
    }

    @Test
    void emitsOrderAndUserChangesFromCommittedPlatformState() {
        jdbc.update("INSERT INTO transaction_order (id,order_num,user_id,amount,status) VALUES (10,'P10',7,20,'TRADE_SUCCESS')");
        jdbc.update("INSERT INTO scdn_order_link " +
                        "(id,external_order_id,transaction_order_id,user_id,amount,last_event_status) " +
                        "VALUES (1,'S10',10,7,20,'WAIT_BUYER_PAY')");
        jdbc.update("INSERT INTO sys_user (id,status,role_id,agent_user_id) VALUES (7,'certified',2,NULL)");
        jdbc.update("INSERT INTO sys_user_banned (id,user_id) VALUES (1,7)");
        jdbc.update("INSERT INTO scdn_user_state_snapshot " +
                        "(user_id,account_status,real_name_verified,agent_user_id,banned) " +
                        "VALUES (7,'register_not_certified',0,NULL,0)");

        reconciler.reconcile();

        assertEquals("TRADE_SUCCESS", jdbc.queryForObject(
                "SELECT last_event_status FROM scdn_order_link WHERE external_order_id='S10'", String.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM scdn_outbox_event WHERE event_type='scdn.order.paid'", Integer.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM scdn_outbox_event WHERE event_type='scdn.user.frozen'", Integer.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM scdn_outbox_event WHERE event_type='scdn.user.real_name_changed'", Integer.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM scdn_outbox_event WHERE event_type='scdn.user.eligibility_changed'", Integer.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT banned FROM scdn_user_state_snapshot WHERE user_id=7", Integer.class));
    }

    private void createSchema() {
        jdbc.execute("CREATE TABLE transaction_order (id BIGINT PRIMARY KEY,order_num VARCHAR(64),user_id BIGINT,amount DECIMAL(18,6),status VARCHAR(32))");
        jdbc.execute("CREATE TABLE scdn_order_link (id BIGINT PRIMARY KEY,external_order_id VARCHAR(96) UNIQUE,transaction_order_id BIGINT UNIQUE,user_id BIGINT,amount DECIMAL(18,6),last_event_status VARCHAR(32))");
        jdbc.execute("CREATE TABLE sys_user (id BIGINT PRIMARY KEY,status VARCHAR(32),role_id BIGINT,agent_user_id BIGINT)");
        jdbc.execute("CREATE TABLE sys_user_banned (id BIGINT PRIMARY KEY,user_id BIGINT)");
        jdbc.execute("CREATE TABLE scdn_user_state_snapshot (user_id BIGINT PRIMARY KEY,account_status VARCHAR(32),real_name_verified TINYINT,agent_user_id BIGINT,banned TINYINT)");
        jdbc.execute("CREATE TABLE scdn_outbox_event (id BIGINT AUTO_INCREMENT PRIMARY KEY,event_id VARCHAR(64) UNIQUE,event_type VARCHAR(64),aggregate_type VARCHAR(32),aggregate_id VARCHAR(128),payload_json CLOB,status VARCHAR(16) DEFAULT 'pending',attempt_count INT DEFAULT 0,available_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,published_time TIMESTAMP)");
    }
}
