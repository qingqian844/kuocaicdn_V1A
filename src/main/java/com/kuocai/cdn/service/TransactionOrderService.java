package com.kuocai.cdn.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.constant.*;
import com.kuocai.cdn.dao.TransactionOrderDao;
import com.kuocai.cdn.dto.GeneralResultDto;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.entity.*;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.service.base.VoData;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.KuocaiBaseUtil;
import com.kuocai.cdn.util.PayUtils;
import com.kuocai.cdn.vo.OrderCollectVo;
import com.kuocai.cdn.vo.TransactionOrderVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


/**
 * (TransactionOrder)服务
 *
 * @author makejava
 * @since 2023-03-10 10:10:55
 */
@Slf4j
@Service
public class TransactionOrderService extends BaseService<TransactionOrder> implements VoData<TransactionOrder, TransactionOrderVo> {

    @Autowired
    protected TransactionOrderDao dao;

    @Resource
    private PurchasedFlowService purchasedFlowService;

    @Resource
    private FlowPackageService flowPackageService;

    @Lazy
    @Resource
    private SysUserAccountService userAccountService;

    @Resource
    @Lazy
    private SysUserService sysUserService;

    @Resource
    @Lazy
    private EdgeOneDomainQuotaService edgeOneDomainQuotaService;

    @Transactional(rollbackFor = Exception.class)
    public synchronized TransactionOrder completeExternalPaidTransaction(TransactionOrder transactionOrder, BigDecimal paidAmount, Date payTime) {
        if (Assert.isEmpty(transactionOrder)) {
            return null;
        }
        if (TransactionOrderStatus.TRADE_SUCCESS.equals(transactionOrder.getStatus())) {
            return transactionOrder;
        }
        transactionOrder.setStatus(TransactionOrderStatus.TRADE_SUCCESS);
        if (Assert.notEmpty(paidAmount)) {
            transactionOrder.setAmount(paidAmount);
        }
        transactionOrder.setPayTime(Assert.isEmpty(payTime) ? new Date() : payTime);
        transactionOrder = save(transactionOrder);

        if (TransactionOrderType.BALANCE_RECHARGE.equals(transactionOrder.getOrderType())) {
            SysUserAccount account = userAccountService.getSysUserAccountByUserId(transactionOrder.getUserId());
            account = Assert.isEmpty(account) ? new SysUserAccount() : account;
            BigDecimal amount = Assert.isEmpty(paidAmount) ? transactionOrder.getAmount() : paidAmount;
            account.addAccountBalance(amount);
            account.addAmassRecharge(amount);
            account.setUserName(transactionOrder.getUserName());
            account.setUserId(transactionOrder.getUserId());
            userAccountService.save(account);
            return transactionOrder;
        }

        if (TransactionOrderType.EDGEONE_DOMAIN_QUOTA.equals(transactionOrder.getOrderType())) {
            edgeOneDomainQuotaService.grantPaidQuota(transactionOrder);
        }
        return transactionOrder;
    }

    private int resolvePaidFlowPackageMonths(TransactionOrder transactionOrder, FlowPackage flowPackage) {
        String detail = transactionOrder.getDetail();
        if (Assert.notEmpty(detail) && detail.contains("months=")) {
            String months = detail.substring(detail.indexOf("months=") + "months=".length()).trim();
            int end = months.indexOf(";");
            if (end >= 0) {
                months = months.substring(0, end);
            }
            try {
                return Math.max(Integer.parseInt(months), 1);
            } catch (NumberFormatException ignored) {
                return resolveFlowPackageMonths(flowPackage, null);
            }
        }
        return resolveFlowPackageMonths(flowPackage, null);
    }

    @Override
    public JSONObject queryForDatatables(Long userId, DataTableQuery query) {
        JSONObject jsonObject = null;
        if (Assert.isEmpty(userId)) {
            jsonObject = super.queryForDatatables(query);
        } else {
            jsonObject = super.queryForDatatables(userId, query);
        }
        JSONArray jsonArray = jsonObject.getJSONArray("data");
        jsonObject.put("data", convert2Vo(jsonArray.toJavaList(TransactionOrder.class)));
        return jsonObject;
    }

    /**
     * 使用余额创建一个订单
     *
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public void createBalanceTransactionOrder(SysUser loginUser, FlowPackage flowPackage, Integer buyTime, SysUserAccount account) {
        BigDecimal money = BigDecimal.ZERO;
        if (buyTime == 3) {
            money = flowPackage.getPrice3();
        } else if (buyTime == 12) {
            money = flowPackage.getPrice12();
        } else {
            money = flowPackage.getPrice();
            buyTime = 1;
        }
        // 1.当前余额进行扣款
        account.reduceAccountBalance(money);
        userAccountService.save(account);
        // 2.新增一个订单
        TransactionOrder transactionOrder = TransactionOrder.builder().payType(TransactionOrderPayType.BALANCE_PAY).orderType(TransactionOrderType.FLOW_PACKAGE).orderNum(PayUtils.getOutTradeNo()).userId(loginUser.getId()).userName(loginUser.getUserName()).createTime(new Date()).amount(money).status(TransactionOrderStatus.TRADE_SUCCESS).detail(flowPackage.detail()).title(flowPackage.getPackageName()).payTime(new Date()).productId(flowPackage.getId()).build();
        TransactionOrder transactionOrderInfo = save(transactionOrder);
        // 代理用户获得提成
        // 流量包计费方式
        String chargeType = flowPackage.getChargeType();
        int chargeValue = KuocaiBaseUtil.getChargeValue(chargeType) * buyTime;
        Date deadLineDate = KuocaiBaseUtil.getAfterMonthDate(chargeValue);
        // 3.创建一个已购买流量包
        PurchasedFlow purchasedFlow = PurchasedFlow.builder().userId(loginUser.getId()).flowPackageId(flowPackage.getId()).flowPackageName(flowPackage.getPackageName()).flowPackageSize(flowPackage.getSize()).edgeoneDomainQuota(flowPackage.getEdgeoneDomainQuota() == null ? 0 : flowPackage.getEdgeoneDomainQuota()).transactionOrderId(transactionOrderInfo.getId()).usedFlow(new Long("0")).deadline(deadLineDate).status(PurchasedFlowConstants.ON_USED).build();
        purchasedFlowService.save(purchasedFlow);
        // 流量包购买次数增加
        Integer buyCount = flowPackage.getBuyCount();
        flowPackage.setBuyCount(buyCount == null ? 1 : buyCount + 1);
        flowPackageService.save(flowPackage);
        log.info("购买流量包成功，当前用户：[{}]，账户余额：[{}]，交易订单：[{}]", loginUser.getId(), account.getAccountBalance(), transactionOrderInfo.getId());
    }

    /**
     * 使用其他支付创建一个订单
     *
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public int resolveFlowPackageMonths(FlowPackage flowPackage, String buyTime) {
        if (Assert.notEmpty(buyTime)) {
            if (FlowPackageChargeType.QUARTER.equals(buyTime)) {
                return 3;
            }
            if (FlowPackageChargeType.YEAR.equals(buyTime)) {
                return 12;
            }
            if (FlowPackageChargeType.MONTH.equals(buyTime)) {
                return 1;
            }
            try {
                return Math.max(Integer.parseInt(buyTime), 1);
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        if (flowPackage != null && FlowPackageChargeType.QUARTER.equals(flowPackage.getChargeType())) {
            return 3;
        }
        if (flowPackage != null && FlowPackageChargeType.YEAR.equals(flowPackage.getChargeType())) {
            return 12;
        }
        return 1;
    }

    public BigDecimal resolveFlowPackageAmount(FlowPackage flowPackage, int months) {
        if (Assert.isEmpty(flowPackage)) {
            return null;
        }
        if (months == 12 && Assert.notEmpty(flowPackage.getPrice12())) {
            return flowPackage.getPrice12();
        }
        if (months == 3 && Assert.notEmpty(flowPackage.getPrice3())) {
            return flowPackage.getPrice3();
        }
        BigDecimal price = flowPackage.getPrice();
        if (Assert.isEmpty(price)) {
            return null;
        }
        if (FlowPackageChargeType.MONTH.equals(flowPackage.getChargeType()) && months > 1 && months != 3 && months != 12) {
            return price.multiply(BigDecimal.valueOf(months));
        }
        return price;
    }

    public synchronized void createOtherTransactionOrder(TransactionOrder transactionOrder, BigDecimal totalAmount, Integer buyTime) {
        TransactionOrder nowTransactionOrder = queryById(transactionOrder.getId());
        if (Assert.isEmpty(nowTransactionOrder)) {
            log.error("没有流量包订单");
            return;
        }
        // 接口幂等性
        if (ObjectUtil.equal(nowTransactionOrder.getStatus(), TransactionOrderStatus.TRADE_SUCCESS)) {
            log.info("当前流量包订单已支付");
            return;
        }
        // 验签成功后
        // 1. 计算用户账户充值金额
        if (TransactionOrderType.EDGEONE_DOMAIN_QUOTA.equals(transactionOrder.getOrderType())) {
            transactionOrder.setStatus(TransactionOrderStatus.TRADE_SUCCESS);
            transactionOrder.setAmount(totalAmount);
            transactionOrder.setPayTime(new Date());
            save(transactionOrder);
            edgeOneDomainQuotaService.grantPaidQuota(transactionOrder);
            return;
        }
        SysUserAccount sysUserAccount = userAccountService.queryByUserId(transactionOrder.getUserId());
        sysUserAccount = Assert.isEmpty(sysUserAccount) ? new SysUserAccount() : sysUserAccount;
        sysUserAccount.addAmassRecharge(totalAmount);
        userAccountService.save(sysUserAccount);
        // 代理用户获得提成
        // 2.更新订单信息
        transactionOrder.setStatus(TransactionOrderStatus.TRADE_SUCCESS);
        transactionOrder.setAmount(totalAmount);
        transactionOrder.setPayTime(new Date());
        save(transactionOrder);
        // 2.查询流量包信息 为空捕获回滚
        if (TransactionOrderType.EDGEONE_DOMAIN_QUOTA.equals(transactionOrder.getOrderType())) {
            edgeOneDomainQuotaService.grantPaidQuota(transactionOrder);
            return;
        }
        FlowPackage flowPackage = flowPackageService.queryById(transactionOrder.getProductId());
        // 流量包计费方式
        String chargeType = flowPackage.getChargeType();
        // 通用流量包购买可以选择多少个月
        int chargeValue = KuocaiBaseUtil.getChargeValue(chargeType) * buyTime;
        Date deadLineDate = KuocaiBaseUtil.getAfterMonthDate(chargeValue);
        // 3.创建一个已购买流量包
        PurchasedFlow purchasedFlow = PurchasedFlow.builder().userId(transactionOrder.getUserId()).flowPackageId(flowPackage.getId()).flowPackageName(flowPackage.getPackageName()).flowPackageSize(flowPackage.getSize()).edgeoneDomainQuota(flowPackage.getEdgeoneDomainQuota() == null ? 0 : flowPackage.getEdgeoneDomainQuota()).transactionOrderId(transactionOrder.getId()).usedFlow(new Long("0")).deadline(deadLineDate).status(PurchasedFlowConstants.ON_USED).build();
        purchasedFlowService.save(purchasedFlow);
        // 流量包购买次数增加
        Integer buyCount = flowPackage.getBuyCount();
        flowPackage.setBuyCount(buyCount == null ? 1 : buyCount + 1);
        flowPackageService.save(flowPackage);
    }

    /**
     * 根据商户编号查询订单信息
     *
     * @param outTradeNo
     * @return
     */
    public TransactionOrder queryTransactionOrderByOrderNo(String outTradeNo) {
        List<TransactionOrder> transactionOrders = queryByObj(TransactionOrder.builder().orderNum(outTradeNo).build());
        if (Assert.isEmpty(transactionOrders)) {
            return null;
        }
        return transactionOrders.get(0);
    }

    /**
     * 更新订单状态(已过期的但是数据库还是未过期的)
     *
     * @param userId 用户id
     * @return boolean true->表示当月已经提现
     */
    public boolean toTestWhetherThisMonth(Long userId) {
        Integer testWhetherThisMonth = dao.toTestWhetherThisMonth(userId);
        return testWhetherThisMonth > 0;
    }

    /**
     * 更新订单状态(已过期的但是数据库还是未过期的)
     *
     * @param user
     * @param expireTime
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTransactionOrderStatus(SysUser user, String expireTime) {
        List<TransactionOrder> noPayAndExpireOrder = new ArrayList<>();
        if (!Assert.isEmpty(noPayAndExpireOrder)) {
            noPayAndExpireOrder = dao.getNoPayAndExpireOrder(user.getRoleId() != 1 ? user.getId() : null, expireTime);
        }
        if (noPayAndExpireOrder.size() > 0) {
            noPayAndExpireOrder.forEach(transactionOrder -> {
                transactionOrder.setStatus(TransactionOrderStatus.EXPIRED);
                log.info("更新订单超时状态，订单ID：[{}]", transactionOrder.getId());
                dao.updateById(transactionOrder);
            });
        }
    }

    /**
     * description: 更新订单状态到退款
     *
     * @param user 用户
     * @author bo
     * @date 2023/7/4 19:40
     */
        @Transactional(rollbackFor = Exception.class)
    public void updateOrderStatusToRefund(SysUser user) {
        // External refund synchronization is disabled in the open source edition.
    }

    /**
     * description: 根据redis的监听更新订单状态
     *
     * @author bo
     * @version 1.0
     * @date 2023/4/9 20:14
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTransactionOrderStatusByRedis(Long transactionOrderId) {
        TransactionOrder transactionOrder = dao.selectById(transactionOrderId);
        // 这里理论上不需要进行判空的，防止业务问题
        if (!Assert.isEmpty(transactionOrder) && TransactionOrderStatus.WAIT_BUYER_PAY.equals(transactionOrder.getStatus())) {
            transactionOrder.setStatus(TransactionOrderStatus.EXPIRED);
            log.info("更新订单超时状态，订单ID：[{}]", transactionOrder.getId());
            dao.updateById(transactionOrder);
        }
    }


    /**
     * 查询流量扣除订单类型为(flow_deduction)
     *
     * @param userId 用户id
     * @return {@code List<TransactionOrder>}
     */
    public List<TransactionOrder> queryFlowDeductionOrderType(Long userId) {
        QueryWrapper<TransactionOrder> transactionOrderQueryWrapper = new QueryWrapper<>();
        transactionOrderQueryWrapper.eq("user_id", userId);
        transactionOrderQueryWrapper.eq("order_type", TransactionOrderType.FLOW_DEDUCTION);
        transactionOrderQueryWrapper.ne("status", TransactionOrderStatus.TRADE_SUCCESS);
        List<TransactionOrder> transactionOrders = queryByWrapper(transactionOrderQueryWrapper);
        return transactionOrders;
    }


    /**
     * 使用余额支付订单
     * 扣除用户余额，将订单支付
     *
     * @param sysUserAccount
     * @param transactionOrder
     */
    @Transactional(rollbackFor = {Exception.class})
    public void useBalance2PayTransactionOrder(SysUserAccount sysUserAccount, TransactionOrder transactionOrder) {
        // 1.将订单修改为已支付
        transactionOrder.setStatus(TransactionOrderStatus.TRADE_SUCCESS);
        transactionOrder.setPayTime(new DateTime());
        save(transactionOrder);
        // 2. 扣费
        if (TransactionOrderType.EDGEONE_DOMAIN_QUOTA.equals(transactionOrder.getOrderType())) {
            edgeOneDomainQuotaService.grantPaidQuota(transactionOrder);
            BigDecimal accountBalance = sysUserAccount.getAccountBalance();
            sysUserAccount.reduceAccountBalance(transactionOrder.getAmount());
            log.info("使用余额支付EdgeOne根域名额度订单，订单ID：[{}]，当前余额金额：[{}]，使用后余额金额：[{}]，使用余额金额：[{}]",
                    transactionOrder.getId(), accountBalance, sysUserAccount.getAccountBalance(), transactionOrder.getAmount());
            userAccountService.save(sysUserAccount);
            return;
        }
        BigDecimal accountBalance = sysUserAccount.getAccountBalance();
        sysUserAccount.reduceAccountBalance(transactionOrder.getAmount());
        // 代理用户获得提成
        log.info("使用余额支付订单，订单ID：[{}]，当前余额金额：[{}]，使用后余额金额：[{}]，使用余额金额：[{}]", transactionOrder.getId(), accountBalance, sysUserAccount.getAccountBalance(), transactionOrder.getAmount());
        userAccountService.save(sysUserAccount);
    }


    /**
     * 创建一个注册奖励订单
     *
     * @param type 类型
     */
    public void createAwardTransactionOrder(String type, SysUser referrer, SysUser sysUser, double money) {
        // 1.查询用户信息
        TransactionOrder transactionOrder = TransactionOrder.builder()
                .payType(TransactionOrderPayType.ARTIFICIAL_PAY)
                .orderType(TransactionOrderType.RECOMMENDATION_REWARD)
                .orderNum(PayUtils.getOutTradeNo())
                .createTime(new Date())
                .amount(BigDecimal.valueOf(money))
                .status(TransactionOrderStatus.TRADE_SUCCESS)
                .payTime(new DateTime())
                .title("推荐用户奖励")
                .build();
        // 推荐人
        if (ObjectUtil.equal(type, "referrer")) {
            transactionOrder.setUserId(referrer.getId());
            transactionOrder.setUserName(referrer.getUserName());
            transactionOrder.setDetail(String.format("推荐注册获取奖励%s元", money));
        } else if (ObjectUtil.equal(type, "self")) {
            // 自己注册的奖励
            transactionOrder.setUserId(sysUser.getId());
            transactionOrder.setUserName(sysUser.getUserName());
            transactionOrder.setDetail(String.format("注册获取奖励%s元", money));
        } else {
            return;
        }
        save(transactionOrder);
    }

    @Override
    public List<TransactionOrderVo> convert2Vo(List<TransactionOrder> source) {
        if (Assert.isEmpty(source)) {
            return new ArrayList<>();
        }
        List<Long> userIds = source.stream().map(TransactionOrder::getUserId).distinct().collect(Collectors.toList());
        List<SysUser> sysUserList = sysUserService.queryByIds(userIds);
        Map<Long, String> imgList = sysUserList.stream().collect(Collectors.toMap(SysUser::getId, u -> u.getImg()));
        List<TransactionOrderVo> transactionOrderVoList = new LinkedList<>();
        source.forEach(transactionOrder -> {
            TransactionOrderVo transactionOrderVo = BeanUtil.fillBeanWithMap(BeanUtil.beanToMap(transactionOrder), new TransactionOrderVo(), false);
            transactionOrderVo.setImgUrl(imgList.get(transactionOrderVo.getUserId()));
            transactionOrderVoList.add(transactionOrderVo);
        });
        return transactionOrderVoList;
    }

    /**
     * description: 订单退款->(包括代理用户)
     *
     * @param transactionOrder 订单
     * @author bo
     * @date 2023/6/16 13:48
     */
        @Transactional(rollbackFor = Exception.class)
    public boolean orderRefund(TransactionOrder transactionOrder) {
        if (Assert.isEmpty(transactionOrder)) {
            return false;
        }
        try {
            transactionOrder.setStatus(TransactionOrderStatus.REFUND);
            save(transactionOrder);
            if (TransactionOrderPayType.BALANCE_PAY.equals(transactionOrder.getPayType())) {
                userAccountService.addBalance(transactionOrder.getUserId(), transactionOrder.getAmount());
            }
            return true;
        } catch (Exception e) {
            log.error("order refund failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * description: 订单退款（类型）
     *
     * @param orderId 订单Id
     * @author bo
     * @date 2023/6/16 13:48
     */
        @Transactional(rollbackFor = Exception.class)
    public GeneralResultDto toOrderRefund(Long orderId) {
        GeneralResultDto generalResultDto = new GeneralResultDto();
        TransactionOrder transactionOrder = queryById(orderId);
        if (Assert.isEmpty(transactionOrder)) {
            generalResultDto.setSuccess(false);
            generalResultDto.setMessage("order not found");
            return generalResultDto;
        }
        if (!TransactionOrderPayType.BALANCE_PAY.equals(transactionOrder.getPayType())) {
            generalResultDto.setSuccess(false);
            generalResultDto.setMessage("external payment refund is disabled");
            return generalResultDto;
        }
        boolean refunded = orderRefund(transactionOrder);
        generalResultDto.setSuccess(refunded);
        generalResultDto.setMessage(refunded ? "refund success" : "refund failed");
        return generalResultDto;
    }

    /**
     * description: 更新订单状态为退款中
     *
     * @param orderNo 订单号
     * @return com.kuocai.cdn.dto.GeneralResultDto
     * @author bo
     * @date 2023/7/1 15:09
     */
    @Transactional(rollbackFor = Exception.class)
    public GeneralResultDto updateOrderToRefunding(String orderNo) {
        GeneralResultDto generalResultDto = new GeneralResultDto();
        TransactionOrder transactionOrder = queryTransactionOrderByOrderNo(orderNo);
        // 对于已支付的订单进行状态修改
        if (!Assert.isEmpty(transactionOrder) && TransactionOrderStatus.TRADE_SUCCESS.equals(transactionOrder.getStatus())) {
            transactionOrder.setStatus(TransactionOrderStatus.REFUNDING);
            generalResultDto.setSuccess(true);
            generalResultDto.setMessage("退款中，资金将原路返回，请留意资金变化~~");
            save(transactionOrder);
        }
        return generalResultDto;
    }

    /**
     * description: 获取汇总消费
     *
     * @param userId 用户ID
     * @return com.kuocai.cdn.vo.OrderCollectVo
     */
    public OrderCollectVo getAmount(Long userId) {
        return dao.getAmount(userId, DateUtil.now());
    }

    /**
     * 定时对订单中的过期用户名进行重置
     */
    public void rename() {
        List<TransactionOrder> transactionOrders = dao.getRename();
        Map<Long, List<TransactionOrder>> transactionOrderMap = transactionOrders.stream().collect(Collectors.groupingBy(TransactionOrder::getUserId));
        for (Long id : transactionOrderMap.keySet()) {
            SysUser sysUser = sysUserService.queryById(id);
            List<TransactionOrder> orderList = transactionOrderMap.get(id);
            for (TransactionOrder transactionOrder : orderList) {
                transactionOrder.setUserName(sysUser.getUserName());
                save(transactionOrder);
            }
        }
    }
}
