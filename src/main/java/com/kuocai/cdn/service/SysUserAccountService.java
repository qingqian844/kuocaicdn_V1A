package com.kuocai.cdn.service;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.constant.BonusRecordStatus;
import com.kuocai.cdn.constant.TransactionOrderPayType;
import com.kuocai.cdn.constant.TransactionOrderStatus;
import com.kuocai.cdn.constant.TransactionOrderType;
import com.kuocai.cdn.dao.SysUserAccountDao;
import com.kuocai.cdn.dao.SysUserDao;
import com.kuocai.cdn.entity.*;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.PayUtils;
import com.kuocai.cdn.vo.SysUserAccountVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * 用户账户(SysUserAccount)服务
 *
 * @author makejava
 * @since 2023-02-28 15:52:18
 */
@Service
@Slf4j
public class SysUserAccountService extends BaseService<SysUserAccount> {

    @Resource
    protected SysUserAccountDao dao;

    @Resource
    private SysUserDao sysUserDao;

    @Resource
    private TransactionOrderService transactionOrderService;

    @Resource
    private AgentLevelService agentLevelService;

    @Resource
    @Lazy
    private BonusRecordService bonusRecordService;


    /**
     * description: 根据用户id获取用户账户
     */
    public SysUserAccount getSysUserAccountByUserId(Long userId) {
        return dao.selectOne(new QueryWrapper<SysUserAccount>().eq("user_id", userId));
    }

    /**
     * description: 根据用户id的list获取用户list
     */
    public List<SysUserAccount> getSysUserAccountsByUserIds(List<Long> userIds) {
        return dao.selectList(new QueryWrapper<SysUserAccount>().in("user_id", userIds));
    }

    /**
     * description: 用户充值
     */
    @Transactional(rollbackFor = Exception.class)
    public void userRecharge(Map params) {
        // 验签成功后,根据订单id查询到TransactionOrder
        TransactionOrder transactionOrder = transactionOrderService.queryTransactionOrderByOrderNo(MapUtil.getStr(params, "out_trade_no"));
        if (Assert.isEmpty(transactionOrder)) {
            log.error("Alipay recharge notify order not found, outTradeNo: {}", params.get("out_trade_no"));
            return;
        }
        // 订单不为空且订单状态不为支付成功
        if (!Assert.isEmpty(transactionOrder) && !TransactionOrderStatus.TRADE_SUCCESS.equals(transactionOrder.getStatus())) {
            transactionOrder.setStatus(MapUtil.getStr(params, "trade_status"));
            // 这里去支付宝返回的用户支付金额(非实付),防止在创建订单的时候前端窜改金额
            BigDecimal totalAmount = BigDecimal.valueOf(MapUtil.getDouble(params, "total_amount"));
            transactionOrder.setAmount(totalAmount);
            transactionOrder.setPayTime(MapUtil.getDate(params, "gmt_payment"));
            transactionOrderService.save(transactionOrder);
            SysUserAccount sysUserAccount = getSysUserAccountByUserId(transactionOrder.getUserId());
            sysUserAccount = Assert.isEmpty(sysUserAccount) ? new SysUserAccount() : sysUserAccount;
            sysUserAccount.addAccountBalance(totalAmount);
            sysUserAccount.addAmassRecharge(totalAmount);
            sysUserAccount.setUserName(transactionOrder.getUserName());
            // 这里没可以直接不判定上面的sysUserAccount是否为null，是与不是都set
            sysUserAccount.setUserId(transactionOrder.getUserId());
            save(sysUserAccount);
        }
    }

    /**
     * description: 获取所用用户账户信息
     */
    public Map<String, BigDecimal> getAllAccountInfo() {
        return dao.getAllAccountInfo();
    }

    /**
     * description: 查询用户排名
     */
    public List<Map<String, Object>> queryRankingList(Integer limitNum) {
        return dao.queryRankingList(limitNum);
    }

    /**
     * description: 根据用户id查询用户
     */
    public SysUserAccount queryByUserId(Long userId) {
        List<SysUserAccount> userAccounts = queryByObj(SysUserAccount.builder().userId(userId).build());
        if (Assert.isEmpty(userAccounts)) {
            return null;
        }
        return userAccounts.get(0);
    }

    /**
     * 新建一个账户
     */
    public void createNewAccount(Long userId, String userName) {
        BigDecimal money = new BigDecimal("0");
        SysUserAccount sysUserAccount = SysUserAccount.builder().userName(userName).userId(userId).accountBalance(money).amassRecharge(money).build();
        save(sysUserAccount);
    }

    /**
     * description: 管理员操作进行充值或扣除
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean manualOrReduceAccount(SysUserAccountVo sysUserAccountVo, SysUser sysUser) {
        try {
            BigDecimal rechargeAmount = sysUserAccountVo.getRechargeAmount();
            String payType = sysUserAccountVo.getPayType();
            String userName = StrUtil.cleanBlank(sysUserAccountVo.getUserName());
            SysUser user = sysUserDao.selectById(sysUserAccountVo.getUserId());
            TransactionOrder transactionOrder = TransactionOrder.builder()
                    // 此处不进行判null
                    .orderType(payType)
                    .orderNum(PayUtils.getOutTradeNo())
                    .amount(rechargeAmount)
                    .title("账户变动")
                    .detail(TransactionOrderType.ADMIN_BALANCE_DEDUCTION.equals(payType) ? "人工扣款" : "人工充值")
                    .createBy(sysUser.getId())
                    .payType(TransactionOrderPayType.ARTIFICIAL_PAY)
                    .userId(sysUserAccountVo.getUserId())
                    .payTime(new Date())
                    .status(TransactionOrderStatus.TRADE_SUCCESS)
                    .userName(user.getUserName())
                    .build();
            transactionOrderService.save(transactionOrder);
            SysUserAccount sysUserAccount = getSysUserAccountByUserId(transactionOrder.getUserId());
            sysUserAccount = Assert.isEmpty(sysUserAccount) ? new SysUserAccount() : sysUserAccount;
            // 扣款逻辑 TODO 注意，这里事务不具有强一致性，前端已经进行了扣款不能大于余额的判定(这里可以优化->抛出异常即可满足一致性)
            if (TransactionOrderType.ADMIN_BALANCE_DEDUCTION.equals(payType) && sysUserAccount.getAccountBalance().compareTo(rechargeAmount) >= 0) {
                sysUserAccount.reduceAccountBalance(rechargeAmount);
            }
            // 充值逻辑
            if (TransactionOrderType.ADMIN_BALANCE_RECHARGE.equals(payType)) {
                sysUserAccount.addAccountBalance(rechargeAmount);
                sysUserAccount.addAmassRecharge(rechargeAmount);
            }
            sysUserAccount.setUserName(userName);
            sysUserAccount.setUserId(transactionOrder.getUserId());
            save(sysUserAccount);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * description: 推荐人获取订单抽成，定义事务为有则加无则增
     *
     * @param userId    这里指下单用户id
     * @param money     下单金额
     * @param orderType 下单类型
     * @author bo
     * @date 2023/5/30 22:03
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void userAgentCommission(Long userId, BigDecimal money, String orderType, Long transactionOrderId) {
        /* if (orderType.equals(TransactionOrderType.FLOW_PACKAGE)) {
            log.info("该订单编号{}，流量包跳过分润", transactionOrderId);
            return;
        } */
        // 订单金额少于 15 不分润
        BigDecimal noMoney = new BigDecimal("15");
        if (noMoney.compareTo(money) > 0) {
            log.info("该订单编号{}，金额少于 15 跳过分润", transactionOrderId);
            return;
        }
        // 这里对所有对象都进行非null校验，防止出现业务之外的异常
        SysUser sysUser = sysUserDao.selectById(userId);
        Long agentUserId = sysUser.getAgentUserId();
        // 判断此用户是否有代理人
        if (!Assert.isEmpty(agentUserId)) {
            SysUser agentUser = sysUserDao.selectById(agentUserId);
            // 判断推荐人是否是代理用户
            if (!Assert.isEmpty(agentUser) && !Assert.isEmpty(agentUser.getAgentLevelId())) {
                AgentLevel agentLevel = agentLevelService.queryById(agentUser.getAgentLevelId());
                // 判断代理等级是否存在
                if (!Assert.isEmpty(agentLevel)) {
                    SysUserAccount sysUserAccount = getSysUserAccountByUserId(agentUserId);
                    //  判断推荐人是否存在余额账户
                    if (!Assert.isEmpty(sysUserAccount)) {
                        // 判断是流量订单还是流量包订单
                        boolean orderTypeOld = orderType.equals(TransactionOrderType.FLOW);
                        BigDecimal orderMoney = orderTypeOld
                                ? money.multiply(agentLevel.getFlowOrderRate())
                                : money.multiply(agentLevel.getPackageRate());
                        // sysUserAccount.addBonus(orderMoney);
                        // save(sysUserAccount);
                        // 分润订单真正的类型
                        String orderTypeRel = orderTypeOld ? TransactionOrderType.FLOW_ORDER_PROFIT : TransactionOrderType.FLOW_PACKAGE_PROFIT;
                        // 分润订单标题
                        String title = orderTypeOld ? "流量订单分润" : "流量包分润";
                        // 分润订单明细
                        String detail = orderTypeOld ? (sysUser.getUserName() + "支付流量订单" + money + "元" + "分润" + orderMoney + "元") : (sysUser.getUserName() + "购买流量包" + money + "元" + "分润" + orderMoney.setScale(2, RoundingMode.HALF_UP) + "元");
                        TransactionOrder transactionOrder = TransactionOrder.builder().orderType(orderTypeRel).orderNum(PayUtils.getOutTradeNo()).amount(orderMoney).title(title).detail(detail).createBy(agentUserId).payType(TransactionOrderPayType.BALANCE_PAY).userId(agentUserId).payTime(new Date()).status(TransactionOrderStatus.TRADE_SUCCESS).userName(agentUser.getUserName()).build();
                        // 保存分润订单
                        transactionOrderService.save(transactionOrder);
                        // 分润记录新增
                        BonusRecord bonusRecord = BonusRecord.builder().transactionOrderId(transactionOrderId).userId(sysUser.getId()).agentUserId(agentUserId).title(title).amount(money).bonus(orderMoney).bonusType(orderTypeRel).status(BonusRecordStatus.WAITING).createTime(new Date()).build();
                        bonusRecordService.save(bonusRecord);
                        log.info("推荐人{}因{}下单{}，获得佣金{}", agentUser.getUserName(), sysUser.getUserName(), orderType, orderMoney);
                    }
                }
            }

        }
    }

    /**
     * 余额扣除
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void addBalance(Long userId, BigDecimal rechargeAmount) {
        SysUserAccount userAccount = getSysUserAccountByUserId(userId);
        userAccount.addAccountBalance(rechargeAmount);
        save(userAccount);
    }

    /**
     * 分润扣除
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void shareProfitDeduct(Long userId, BigDecimal rechargeAmount) {
        SysUserAccount userAccount = getSysUserAccountByUserId(userId);
        userAccount.reduceBonus(rechargeAmount);
        save(userAccount);
    }
}
