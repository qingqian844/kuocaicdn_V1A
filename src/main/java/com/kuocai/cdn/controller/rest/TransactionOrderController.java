package com.kuocai.cdn.controller.rest;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.annotation.SysLog;
import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.constant.TransactionOrderStatus;
import com.kuocai.cdn.constant.TransactionOrderType;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.SysUserAccount;
import com.kuocai.cdn.entity.TransactionOrder;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.EdgeOneDomainQuotaService;
import com.kuocai.cdn.service.SysUserAccountService;
import com.kuocai.cdn.service.TransactionOrderService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.KuocaiBaseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "TransactionOrder")
@Scope(value = "session")
public class TransactionOrderController extends BaseController {

    @Resource
    private TransactionOrderService service;

    @Resource
    private SysUserAccountService userAccountService;

    @Resource
    private EdgeOneDomainQuotaService edgeOneDomainQuotaService;

    @RateLimiter
    @PostMapping("getCollectOrder")
    public RespResult getCollectOrder(Long userId) {
        Long realUserId = loginUserId;
        if (isAdmin() && !Assert.isEmpty(userId)) {
            realUserId = userId;
        }
        return RespResult.success("查询成功", service.getAmount(realUserId));
    }

    @RateLimiter
    @PostMapping("queryForDatatables")
    public RespResult queryForDatatables(@RequestBody DataTableQuery query) {
        String nowToExpireTime = KuocaiBaseUtil.getNowToExpireTime(SystemConfig.websiteBaseConfig.getExpireTime());
        service.updateTransactionOrderStatus(loginUser, nowToExpireTime);
        JSONObject datatables = isAdmin()
                ? service.queryForDatatables(null, query)
                : service.queryForDatatables(loginUserId, query);
        return RespResult.success("查询成功", datatables);
    }

    @RateLimiter
    @PostMapping("queryEdgeOneDomainQuota")
    public RespResult queryEdgeOneDomainQuota() {
        return RespResult.success("success", edgeOneDomainQuotaService.summary(loginUserId));
    }

    @RateLimiter
    @PostMapping("queryEdgeOneDomainQuotaPayOptions")
    public RespResult queryEdgeOneDomainQuotaPayOptions() {
        JSONObject data = new JSONObject();
        data.put("alipayEnabled", false);
        data.put("wechatEnabled", false);
        return RespResult.success("success", data);
    }

    @RateLimiter
    @PostMapping("createEdgeOneDomainQuotaBalanceOrder")
    public RespResult createEdgeOneDomainQuotaBalanceOrder() {
        try {
            SysUserAccount sysUserAccount = userAccountService.queryByUserId(loginUserId);
            edgeOneDomainQuotaService.createBalanceQuotaOrder(loginUser, sysUserAccount);
            return RespResult.success("购买成功", edgeOneDomainQuotaService.summary(loginUserId));
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage(), edgeOneDomainQuotaService.summary(loginUserId));
        } catch (Exception e) {
            log.error("Create EdgeOne quota balance order failed", e);
            return RespResult.fail("购买失败，请稍后再试");
        }
    }

    @RateLimiter
    @PostMapping("useBalance2PayTransactionOrder")
    @SysLog(module = "财务管理", describe = "使用余额支付订单")
    public RespResult useBalance2PayTransactionOrder(Long transactionId) {
        if (Assert.isEmpty(transactionId)) {
            return RespResult.paramEmpty("transactionId");
        }
        TransactionOrder transactionOrder = service.queryById(transactionId);
        RespResult access = checkTransactionOrderAccess(transactionOrder);
        if (access != null) {
            return access;
        }
        if (!TransactionOrderStatus.WAIT_BUYER_PAY.equals(transactionOrder.getStatus())) {
            return RespResult.fail("order status invalid");
        }
        if (!TransactionOrderType.FLOW_DEDUCTION.equals(transactionOrder.getOrderType())
                && !TransactionOrderType.EDGEONE_DOMAIN_QUOTA.equals(transactionOrder.getOrderType())) {
            return RespResult.fail("order type invalid");
        }
        SysUserAccount sysUserAccount = userAccountService.queryByUserId(loginUserId);
        if (Assert.isEmpty(sysUserAccount)) {
            return RespResult.notFound("account");
        }
        BigDecimal accountBalance = sysUserAccount.getAccountBalance();
        BigDecimal price = transactionOrder.getAmount();
        if (Assert.isEmpty(accountBalance) || Assert.isEmpty(price)) {
            return RespResult.fail("账户异常");
        }
        if (accountBalance.compareTo(price) < 0) {
            return RespResult.fail("余额不足");
        }
        service.useBalance2PayTransactionOrder(sysUserAccount, transactionOrder);
        return RespResult.success("支付成功");
    }

    @RateLimiter
    @PostMapping("useBalanceOneButtonPay")
    @SysLog(module = "财务管理", describe = "使用余额支付未支付订单")
    public RespResult useBalanceOneButtonPay() {
        QueryWrapper<TransactionOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("order_type", TransactionOrderType.FLOW_DEDUCTION);
        wrapper.eq("status", TransactionOrderStatus.WAIT_BUYER_PAY);
        wrapper.eq("user_id", loginUserId);
        wrapper.orderByDesc("create_time");
        wrapper.last("limit 10");
        BigDecimal total = BigDecimal.ZERO;
        List<TransactionOrder> orderList = service.queryByWrapper(wrapper);
        if (orderList == null || orderList.isEmpty()) {
            return RespResult.fail("没有待支付的流量账单");
        }
        for (TransactionOrder order : orderList) {
            if (Assert.isEmpty(order.getAmount())) {
                return RespResult.fail("order amount invalid");
            }
            total = total.add(order.getAmount());
        }
        SysUserAccount sysUserAccount = userAccountService.queryByUserId(loginUserId);
        if (Assert.isEmpty(sysUserAccount)) {
            return RespResult.notFound("account");
        }
        if (Assert.isEmpty(sysUserAccount.getAccountBalance()) || sysUserAccount.getAccountBalance().compareTo(total) < 0) {
            return RespResult.fail("余额不足");
        }
        for (TransactionOrder order : orderList) {
            SysUserAccount userAccount = userAccountService.queryByUserId(loginUserId);
            if (Assert.isEmpty(userAccount) || userAccount.getAccountBalance().compareTo(order.getAmount()) < 0) {
                return RespResult.fail("余额不足");
            }
            service.useBalance2PayTransactionOrder(userAccount, order);
        }
        return RespResult.success("支付成功");
    }
}
