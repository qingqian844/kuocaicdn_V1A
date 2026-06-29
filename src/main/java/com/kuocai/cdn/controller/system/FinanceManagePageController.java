package com.kuocai.cdn.controller.system;

import com.kuocai.cdn.annotation.AuthorLimiter;
import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.entity.SysUserAccount;
import com.kuocai.cdn.entity.TransactionOrder;
import com.kuocai.cdn.util.Assert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 财务管理页面跳转控制器
 *
 * @author XUEW
 * @date 下午9:00 2023/2/12
 */
@Slf4j
@Controller
@Scope(value = "session")
public class FinanceManagePageController extends BaseController {

//    @Value("${alipay.limit.money}")
    private double aliPayLimitMoney;

    /**
     * 人工充值
     */
    @AuthorLimiter
    @GetMapping("/manual-recharge")
    public String manualRecharge(Map<String, Object> map) {
        List<Map<String, Object>> queryRankingList = sysUserAccountService.queryRankingList(18737204);
        map.put("sysUsers", queryRankingList);
        return "admin/account/manual-recharge";
    }

    /**
     * 人工扣款
     */
    @AuthorLimiter
    @GetMapping("/manual-deduction")
    public String manualDeduction(Map<String, Object> map) {
        List<Map<String, Object>> queryRankingList = sysUserAccountService.queryRankingList(18737204);
        map.put("sysUsers", queryRankingList);
        return "admin/account/manual-deduction";
    }

    /**
     * 账户充值
     */
    @GetMapping("/recharge")
    public String recharge(Map<String, Object> map) {
        SysUserAccount sysUserAccount = sysUserAccountService.getSysUserAccountByUserId(loginUserId);
        map.put("userAccount", sysUserAccount);
        map.put("wechatConfig", SystemConfig.weChatConfig);
        map.put("aliPayConfig", SystemConfig.aliPayConfig);
        map.put("limitMoney", aliPayLimitMoney);
        return "admin/account/recharge";
    }

    /**
     * 订单列表
     */
    @GetMapping("/transaction-order-list")
    public String transactionOrderList(Map<String, Object> map) {
        // 添加日志验证用户权限

        // 先检查用户权限，只有管理员才能查看所有用户数据
        boolean isAdmin = loginUser.getRoleId() == 1;
        map.put("isAdmin", isAdmin);
        
        List<SysUser> sysUsers = new ArrayList<>();
        
        if (isAdmin) {
            // 管理员可以查看所有下单用户
            List<TransactionOrder> transactionOrderList = transactionOrderService.queryAll();
            List<Long> userIds = transactionOrderList.stream()
                .map(TransactionOrder::getUserId)
                .distinct()
                .collect(Collectors.toList());
            
            if (Assert.notEmpty(userIds)) {
                sysUsers = sysUserService.queryByIds(userIds);
            }
        } else {
            sysUsers.add(loginUser);
        }
        
        map.put("users", sysUsers);
        
        // 查询用户的余额
        SysUserAccount sysUserAccount = sysUserAccountService.queryByUserId(loginUserId);
        map.put("balance", sysUserAccount.getAccountBalance());
        
        return "admin/account/transaction-order-list";
    }

    @GetMapping("/withdraw_records")
    public String withdrawRecordsList(Map<String, Object> map) {
        if (loginUser.getRoleId() != 1) {
            map.put("isAdmin", false);
        } else {
            map.put("isAdmin", true);
        }
        return "admin/account/withdraw_records";
    }
}