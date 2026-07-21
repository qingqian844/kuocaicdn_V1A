package com.kuocai.cdn.integration.scdn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuocai.cdn.constant.TransactionOrderPayType;
import com.kuocai.cdn.constant.TransactionOrderStatus;
import com.kuocai.cdn.constant.TransactionOrderType;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.entity.TransactionOrder;
import com.kuocai.cdn.service.SysUserService;
import com.kuocai.cdn.service.TransactionOrderService;
import com.kuocai.cdn.util.PayUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class ScdnPlatformBillingService {
    private static final int IDEMPOTENCY_KEY_MIN = 8;
    private static final int IDEMPOTENCY_KEY_MAX = 128;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final SysUserService sysUserService;
    private final TransactionOrderService transactionOrderService;
    private final ScdnPlatformEventService events;
    private final ScdnStateEventReconciler stateEvents;

    public ScdnPlatformBillingService(JdbcTemplate jdbcTemplate,
                                      ObjectMapper objectMapper,
                                      SysUserService sysUserService,
                                      TransactionOrderService transactionOrderService,
                                      ScdnPlatformEventService events,
                                      ScdnStateEventReconciler stateEvents) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.sysUserService = sysUserService;
        this.transactionOrderService = transactionOrderService;
        this.events = events;
        this.stateEvents = stateEvents;
    }

    @Transactional(rollbackFor = Exception.class)
    public ScdnContracts.OrderResponse createOrder(String idempotencyKey, ScdnContracts.CreateOrderRequest request) {
        return idempotent(idempotencyKey, "create_order", request, ScdnContracts.OrderResponse.class,
                () -> createOrderOnce(request));
    }

    public ScdnContracts.OrderResponse getOrder(String externalOrderId) {
        requireExternalOrderId(externalOrderId);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT l.external_order_id,l.transaction_order_id,l.user_id,l.amount,o.order_num,o.status " +
                        "FROM scdn_order_link l JOIN transaction_order o ON o.id=l.transaction_order_id " +
                        "WHERE l.external_order_id=?",
                externalOrderId);
        if (rows.isEmpty()) {
            throw new ScdnIntegrationException("ORDER_NOT_FOUND", "SCDN order does not exist", HttpStatus.NOT_FOUND);
        }
        return orderResponse(rows.get(0));
    }

    @Transactional(rollbackFor = Exception.class)
    public ScdnContracts.WalletOperationResponse debit(String idempotencyKey,
                                                        ScdnContracts.WalletOperationRequest request) {
        return idempotent(idempotencyKey, "wallet_debit", request, ScdnContracts.WalletOperationResponse.class,
                () -> debitOnce(request));
    }

    @Transactional(rollbackFor = Exception.class)
    public ScdnContracts.WalletOperationResponse refund(String idempotencyKey,
                                                         ScdnContracts.WalletOperationRequest request) {
        return idempotent(idempotencyKey, "wallet_refund", request, ScdnContracts.WalletOperationResponse.class,
                () -> refundOnce(request));
    }

    private ScdnContracts.OrderResponse createOrderOnce(ScdnContracts.CreateOrderRequest request) {
        requireExternalOrderId(request.getExternalOrderId());
        SysUser user = requireActiveUser(request.getUserId());
        BigDecimal amount = money(request.getAmount());
        if (amount.compareTo(new BigDecimal("0.01")) < 0) {
            throw badRequest("INVALID_AMOUNT", "Order amount must be at least 0.01");
        }
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT l.external_order_id,l.transaction_order_id,l.user_id,l.amount,o.order_num,o.status " +
                        "FROM scdn_order_link l JOIN transaction_order o ON o.id=l.transaction_order_id " +
                        "WHERE l.external_order_id=? FOR UPDATE",
                request.getExternalOrderId());
        if (!existing.isEmpty()) {
            ScdnContracts.OrderResponse response = orderResponse(existing.get(0));
            if (!response.getUserId().equals(request.getUserId()) || response.getAmount().compareTo(amount) != 0) {
                throw conflict("ORDER_REFERENCE_CONFLICT", "External order id is already used by another request");
            }
            return response;
        }

        TransactionOrder order = TransactionOrder.builder()
                .userId(user.getId())
                .userName(user.getUserName())
                .orderNum(PayUtils.getOutTradeNo())
                .orderType(TransactionOrderType.SCDN_PLAN)
                .title(limit(request.getTitle(), 255))
                .detail(limit(request.getDetail(), 1000))
                .amount(amount)
                .status(TransactionOrderStatus.WAIT_BUYER_PAY)
                .payType(TransactionOrderPayType.BALANCE_PAY)
                .createTime(new Date())
                .createBy(user.getId())
                .build();
        order = transactionOrderService.save(order);
        jdbcTemplate.update(
                "INSERT INTO scdn_order_link " +
                        "(external_order_id,transaction_order_id,user_id,amount,last_event_status) VALUES (?,?,?,?,?)",
                request.getExternalOrderId(), order.getId(), user.getId(), amount, order.getStatus());
        ScdnContracts.OrderResponse response = ScdnContracts.OrderResponse.builder()
                .externalOrderId(request.getExternalOrderId())
                .platformOrderId(order.getId())
                .platformOrderNumber(order.getOrderNum())
                .userId(user.getId())
                .amount(amount)
                .status(order.getStatus())
                .build();
        events.enqueue("scdn.order.created", "order", request.getExternalOrderId(), response);
        return response;
    }

    private ScdnContracts.WalletOperationResponse debitOnce(ScdnContracts.WalletOperationRequest request) {
        requireBusinessReference(request.getBusinessReference());
        requireActiveUser(request.getUserId());
        BigDecimal amount = money(request.getAmount());
        ScdnContracts.WalletOperationResponse existing = existingLedger(request.getBusinessReference());
        if (existing != null) {
            validateLedgerReplay(existing, request, "debit");
            return existing;
        }
        AccountRow account = lockAccount(request.getUserId());
        if (account.balance.compareTo(amount) < 0) {
            throw new ScdnIntegrationException("INSUFFICIENT_BALANCE", "Account balance is insufficient", HttpStatus.PAYMENT_REQUIRED);
        }
        BigDecimal balanceAfter = account.balance.subtract(amount).setScale(6, RoundingMode.HALF_UP);
        int updated = jdbcTemplate.update(
                "UPDATE sys_user_account SET account_balance=?,update_time=NOW() WHERE id=? AND account_balance>=?",
                balanceAfter, account.id, amount);
        if (updated != 1) {
            throw conflict("BALANCE_CHANGED", "Account balance changed, retry with a new idempotency key");
        }
        TransactionOrder order = paidOrder(request.getUserId(), amount,
                TransactionOrderType.SCDN_USAGE_DEDUCTION,
                "SCDN usage deduction",
                request.getDescription());
        ScdnContracts.WalletOperationResponse response = saveLedger(
                request, null, "debit", order.getId(), balanceAfter);
        events.enqueue("scdn.wallet.debited", "wallet", request.getBusinessReference(), response);
        return response;
    }

    private ScdnContracts.WalletOperationResponse refundOnce(ScdnContracts.WalletOperationRequest request) {
        requireBusinessReference(request.getBusinessReference());
        requireBusinessReference(request.getOriginalBusinessReference());
        requireActiveUser(request.getUserId());
        BigDecimal amount = money(request.getAmount());
        ScdnContracts.WalletOperationResponse existing = existingLedger(request.getBusinessReference());
        if (existing != null) {
            validateLedgerReplay(existing, request, "refund");
            return existing;
        }
        List<Map<String, Object>> originals = jdbcTemplate.queryForList(
                "SELECT user_id,amount FROM scdn_wallet_ledger WHERE business_reference=? AND operation='debit' FOR UPDATE",
                request.getOriginalBusinessReference());
        if (originals.isEmpty()) {
            throw new ScdnIntegrationException("DEBIT_NOT_FOUND", "Original debit does not exist", HttpStatus.NOT_FOUND);
        }
        Long originalUserId = number(originals.get(0).get("user_id")).longValue();
        BigDecimal originalAmount = decimal(originals.get(0).get("amount"));
        if (!originalUserId.equals(request.getUserId())) {
            throw conflict("REFUND_USER_MISMATCH", "Refund user does not match the original debit");
        }
        BigDecimal refunded = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount),0) FROM scdn_wallet_ledger WHERE original_business_reference=? AND operation='refund'",
                BigDecimal.class,
                request.getOriginalBusinessReference());
        if (refunded == null) {
            refunded = BigDecimal.ZERO;
        }
        if (refunded.add(amount).compareTo(originalAmount) > 0) {
            throw conflict("REFUND_EXCEEDS_DEBIT", "Refund total exceeds the original debit");
        }
        AccountRow account = lockAccount(request.getUserId());
        BigDecimal balanceAfter = account.balance.add(amount).setScale(6, RoundingMode.HALF_UP);
        jdbcTemplate.update("UPDATE sys_user_account SET account_balance=?,update_time=NOW() WHERE id=?",
                balanceAfter, account.id);
        TransactionOrder order = paidOrder(request.getUserId(), amount,
                TransactionOrderType.SCDN_REFUND,
                "SCDN refund",
                request.getDescription());
        ScdnContracts.WalletOperationResponse response = saveLedger(
                request, request.getOriginalBusinessReference(), "refund", order.getId(), balanceAfter);
        events.enqueue("scdn.wallet.refunded", "wallet", request.getBusinessReference(), response);
        return response;
    }

    private TransactionOrder paidOrder(Long userId, BigDecimal amount, String type, String title, String detail) {
        SysUser user = requireActiveUser(userId);
        TransactionOrder order = TransactionOrder.builder()
                .userId(userId)
                .userName(user.getUserName())
                .orderNum(PayUtils.getOutTradeNo())
                .orderType(type)
                .title(title)
                .detail(limit(detail, 1000))
                .amount(amount)
                .status(TransactionOrderStatus.TRADE_SUCCESS)
                .payType(TransactionOrderPayType.BALANCE_PAY)
                .createTime(new Date())
                .payTime(new Date())
                .createBy(userId)
                .build();
        return transactionOrderService.save(order);
    }

    private ScdnContracts.WalletOperationResponse saveLedger(ScdnContracts.WalletOperationRequest request,
                                                               String originalReference,
                                                               String operation,
                                                               Long orderId,
                                                               BigDecimal balanceAfter) {
        BigDecimal amount = money(request.getAmount());
        jdbcTemplate.update(
                "INSERT INTO scdn_wallet_ledger " +
                        "(business_reference,original_business_reference,operation,transaction_order_id,user_id,amount,balance_after) " +
                        "VALUES (?,?,?,?,?,?,?)",
                request.getBusinessReference(), originalReference, operation, orderId,
                request.getUserId(), amount, balanceAfter);
        return ScdnContracts.WalletOperationResponse.builder()
                .businessReference(request.getBusinessReference())
                .platformOrderId(orderId)
                .userId(request.getUserId())
                .amount(amount)
                .operation(operation)
                .status("completed")
                .build();
    }

    private ScdnContracts.WalletOperationResponse existingLedger(String businessReference) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT business_reference,operation,transaction_order_id,user_id,amount " +
                        "FROM scdn_wallet_ledger WHERE business_reference=? FOR UPDATE",
                businessReference);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = rows.get(0);
        return ScdnContracts.WalletOperationResponse.builder()
                .businessReference(string(row.get("business_reference")))
                .platformOrderId(number(row.get("transaction_order_id")).longValue())
                .userId(number(row.get("user_id")).longValue())
                .amount(decimal(row.get("amount")))
                .operation(string(row.get("operation")))
                .status("completed")
                .build();
    }

    private void validateLedgerReplay(ScdnContracts.WalletOperationResponse existing,
                                      ScdnContracts.WalletOperationRequest request,
                                      String operation) {
        if (!operation.equals(existing.getOperation())
                || !request.getUserId().equals(existing.getUserId())
                || money(request.getAmount()).compareTo(existing.getAmount()) != 0) {
            throw conflict("BUSINESS_REFERENCE_CONFLICT", "Business reference is already used by another operation");
        }
    }

    private AccountRow lockAccount(Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id,account_balance FROM sys_user_account WHERE user_id=? FOR UPDATE", userId);
        if (rows.isEmpty()) {
            throw new ScdnIntegrationException("ACCOUNT_NOT_FOUND", "Account does not exist", HttpStatus.NOT_FOUND);
        }
        return new AccountRow(number(rows.get(0).get("id")).longValue(), decimal(rows.get(0).get("account_balance")));
    }

    private SysUser requireActiveUser(Long userId) {
        SysUser user = userId == null ? null : sysUserService.queryCacheUserById(userId);
        if (user == null) {
            throw new ScdnIntegrationException("USER_NOT_FOUND", "User does not exist", HttpStatus.NOT_FOUND);
        }
        if (stateEvents.isBanned(userId)
                || "banned".equals(user.getStatus()) || "cancellation".equals(user.getStatus())) {
            throw new ScdnIntegrationException("USER_DISABLED", "User account is disabled", HttpStatus.FORBIDDEN);
        }
        return user;
    }

    private <T> T idempotent(String key, String operation, Object request, Class<T> responseType, Supplier<T> action) {
        validateIdempotencyKey(key);
        String requestHash = hash(request);
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT operation_type,request_hash,response_json,status FROM scdn_idempotency_record WHERE idempotency_key=? FOR UPDATE",
                key);
        if (!existing.isEmpty()) {
            return replay(existing.get(0), operation, requestHash, responseType);
        }
        try {
            jdbcTemplate.update(
                    "INSERT INTO scdn_idempotency_record (idempotency_key,operation_type,request_hash,status) VALUES (?,?,?,'processing')",
                    key, operation, requestHash);
        } catch (DuplicateKeyException e) {
            List<Map<String, Object>> raced = jdbcTemplate.queryForList(
                    "SELECT operation_type,request_hash,response_json,status FROM scdn_idempotency_record WHERE idempotency_key=? FOR UPDATE",
                    key);
            if (raced.isEmpty()) {
                throw conflict("IDEMPOTENCY_RACE", "Request is already being processed");
            }
            return replay(raced.get(0), operation, requestHash, responseType);
        }
        T response = action.get();
        try {
            jdbcTemplate.update(
                    "UPDATE scdn_idempotency_record SET response_json=?,status='completed',update_time=NOW() WHERE idempotency_key=?",
                    objectMapper.writeValueAsString(response), key);
            return response;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to persist idempotent response", e);
        }
    }

    private <T> T replay(Map<String, Object> row, String operation, String requestHash, Class<T> responseType) {
        if (!operation.equals(string(row.get("operation_type")))
                || !requestHash.equals(string(row.get("request_hash")))) {
            throw conflict("IDEMPOTENCY_KEY_CONFLICT", "Idempotency key was used with a different request");
        }
        if (!"completed".equals(string(row.get("status"))) || row.get("response_json") == null) {
            throw conflict("REQUEST_IN_PROGRESS", "Request is already being processed");
        }
        try {
            return objectMapper.readValue(string(row.get("response_json")), responseType);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to replay idempotent response", e);
        }
    }

    private ScdnContracts.OrderResponse orderResponse(Map<String, Object> row) {
        return ScdnContracts.OrderResponse.builder()
                .externalOrderId(string(row.get("external_order_id")))
                .platformOrderId(number(row.get("transaction_order_id")).longValue())
                .platformOrderNumber(string(row.get("order_num")))
                .userId(number(row.get("user_id")).longValue())
                .amount(decimal(row.get("amount")))
                .status(string(row.get("status")))
                .build();
    }

    private String hash(Object value) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(value);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash request", e);
        }
    }

    private BigDecimal money(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw badRequest("INVALID_AMOUNT", "Amount must be greater than zero");
        }
        BigDecimal rounded = amount.setScale(6, RoundingMode.HALF_UP);
        if (rounded.compareTo(BigDecimal.ZERO) <= 0) {
            throw badRequest("INVALID_AMOUNT", "Amount is below the minimum accounting precision");
        }
        return rounded;
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.length() < IDEMPOTENCY_KEY_MIN || key.length() > IDEMPOTENCY_KEY_MAX
                || !key.matches("[A-Za-z0-9._:-]+")) {
            throw badRequest("INVALID_IDEMPOTENCY_KEY", "Idempotency key must contain 8-128 safe characters");
        }
    }

    private void requireExternalOrderId(String externalOrderId) {
        if (externalOrderId == null || externalOrderId.trim().isEmpty() || externalOrderId.length() > 96) {
            throw badRequest("INVALID_ORDER_ID", "External order id is invalid");
        }
    }

    private void requireBusinessReference(String reference) {
        if (reference == null || reference.length() < 8 || reference.length() > 128
                || !reference.matches("[A-Za-z0-9._:-]+")) {
            throw badRequest("INVALID_BUSINESS_REFERENCE", "Business reference is invalid");
        }
    }

    private String limit(String value, int length) {
        if (value == null) {
            return "";
        }
        return value.length() <= length ? value : value.substring(0, length);
    }

    private Number number(Object value) {
        if (value instanceof Number) {
            return (Number) value;
        }
        return new BigDecimal(value.toString());
    }

    private BigDecimal decimal(Object value) {
        return value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(value.toString());
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }

    private ScdnIntegrationException badRequest(String code, String message) {
        return new ScdnIntegrationException(code, message, HttpStatus.BAD_REQUEST);
    }

    private ScdnIntegrationException conflict(String code, String message) {
        return new ScdnIntegrationException(code, message, HttpStatus.CONFLICT);
    }

    private static final class AccountRow {
        private final Long id;
        private final BigDecimal balance;

        private AccountRow(Long id, BigDecimal balance) {
            this.id = id;
            this.balance = balance;
        }
    }
}

