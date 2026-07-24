package com.kuocai.cdn.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
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
        log.debug("开源版不执行代理分润，订单ID：{}", transactionOrderId);
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

}
