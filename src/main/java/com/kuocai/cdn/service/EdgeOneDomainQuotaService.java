package com.kuocai.cdn.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.api.tencent.edgeone.TencentEdgeOneClient;
import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.constant.PurchasedFlowConstants;
import com.kuocai.cdn.constant.TransactionOrderPayType;
import com.kuocai.cdn.constant.TransactionOrderStatus;
import com.kuocai.cdn.constant.TransactionOrderType;
import com.kuocai.cdn.dao.EdgeOneDomainQuotaOrderDao;
import com.kuocai.cdn.dao.EdgeOneRootDomainRecordDao;
import com.kuocai.cdn.entity.*;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.PayUtils;
import com.kuocai.cdn.vo.EdgeOneDomainQuotaSummaryVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class EdgeOneDomainQuotaService extends BaseService<EdgeOneDomainQuotaOrder> {

    private static final String RECORD_ACTIVE = "active";
    public static final String QUOTA_REQUIRED = "EDGEONE_DOMAIN_QUOTA_REQUIRED";

    @Resource
    private EdgeOneRootDomainRecordDao rootDomainRecordDao;

    @Resource
    private EdgeOneDomainQuotaOrderDao quotaOrderDao;

    @Resource
    private PurchasedFlowService purchasedFlowService;

    @Resource
    private TransactionOrderService transactionOrderService;

    @Resource
    private SysUserAccountService sysUserAccountService;

    public boolean isEnabled() {
        return SystemConfig.websiteBaseConfig == null
                || !Boolean.FALSE.equals(SystemConfig.websiteBaseConfig.getEdgeoneDomainQuotaEnabled());
    }

    public int freeQuota() {
        Integer value = SystemConfig.websiteBaseConfig == null ? null : SystemConfig.websiteBaseConfig.getEdgeoneFreeDomainQuota();
        return value == null ? 1 : Math.max(value, 0);
    }

    public BigDecimal unitPrice() {
        BigDecimal value = SystemConfig.websiteBaseConfig == null ? null : SystemConfig.websiteBaseConfig.getEdgeoneDomainQuotaPrice();
        return value == null ? new BigDecimal("30") : value.max(BigDecimal.ZERO);
    }

    public int quotaValidDays() {
        Integer value = SystemConfig.websiteBaseConfig == null ? null : SystemConfig.websiteBaseConfig.getEdgeoneDomainQuotaValidDays();
        return value == null || value <= 0 ? 30 : value;
    }

    public EdgeOneDomainQuotaSummaryVo summary(Long userId) {
        int free = freeQuota();
        int paid = paidQuota(userId);
        int packageQuota = packageQuota(userId);
        int used = usedQuota(userId);
        int total = free + paid + packageQuota;
        return EdgeOneDomainQuotaSummaryVo.builder()
                .enabled(isEnabled())
                .freeQuota(free)
                .paidQuota(paid)
                .packageQuota(packageQuota)
                .totalQuota(total)
                .usedQuota(used)
                .remainingQuota(Math.max(total - used, 0))
                .unitPrice(unitPrice())
                .quotaValidDays(quotaValidDays())
                .overQuota(used > total)
                .build();
    }

    public boolean hasRootDomain(Long userId, String rootDomain) {
        QueryWrapper<EdgeOneRootDomainRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("root_domain", rootDomain).eq("status", RECORD_ACTIVE);
        return rootDomainRecordDao.selectCount(wrapper) > 0;
    }

    public String resolveRootDomain(String domainName) throws BusinessException {
        return TencentEdgeOneClient.getRootDomain(domainName);
    }

    public EdgeOneDomainQuotaSummaryVo checkCreateQuota(Long userId, String domainName) throws BusinessException {
        if (!isEnabled()) {
            return summary(userId);
        }
        String rootDomain = resolveRootDomain(domainName);
        if (hasRootDomain(userId, rootDomain)) {
            return summary(userId);
        }
        EdgeOneDomainQuotaSummaryVo summary = summary(userId);
        if (summary.getRemainingQuota() <= 0) {
            throw new BusinessException(QUOTA_REQUIRED);
        }
        return summary;
    }

    public void recordRootDomain(Long userId, String domainName, Long cdnDomainId) {
        try {
            String rootDomain = resolveRootDomain(domainName);
            if (hasRootDomain(userId, rootDomain)) {
                return;
            }
            EdgeOneRootDomainRecord record = EdgeOneRootDomainRecord.builder()
                    .userId(userId)
                    .rootDomain(rootDomain)
                    .firstDomainName(domainName)
                    .cdnDomainId(cdnDomainId)
                    .status(RECORD_ACTIVE)
                    .createTime(new Date())
                    .build();
            rootDomainRecordDao.insert(record);
        } catch (Exception e) {
            log.warn("Record EdgeOne root domain failed, userId: {}, domainName: {}, reason: {}",
                    userId, domainName, e.getMessage());
        }
    }

    public List<EdgeOneRootDomainRecord> listRootDomainRecords(Long userId) {
        QueryWrapper<EdgeOneRootDomainRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("status", RECORD_ACTIVE)
                .orderByDesc("create_time");
        return rootDomainRecordDao.selectList(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public TransactionOrder createQuotaOrder(SysUser user, String payType) throws BusinessException {
        if (Assert.isEmpty(user)) {
            throw new BusinessException("login required");
        }
        TransactionOrder order = TransactionOrder.builder()
                .payType(payType)
                .orderType(TransactionOrderType.EDGEONE_DOMAIN_QUOTA)
                .orderNum(PayUtils.getOutTradeNo())
                .userId(user.getId())
                .userName(user.getUserName())
                .createBy(user.getId())
                .createTime(new Date())
                .amount(unitPrice())
                .status(TransactionOrderStatus.WAIT_BUYER_PAY)
                .title("EdgeOne root domain quota")
                .detail("quota=1; validDays=" + quotaValidDays())
                .productId(1L)
                .build();
        return transactionOrderService.save(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createBalanceQuotaOrder(SysUser user, SysUserAccount account) throws BusinessException {
        BigDecimal price = unitPrice();
        if (Assert.isEmpty(account)) {
            throw new BusinessException("account not found");
        }
        if (Assert.isEmpty(account.getAccountBalance()) || account.getAccountBalance().compareTo(price) < 0) {
            throw new BusinessException("余额不足，请先充值");
        }
        account.reduceAccountBalance(price);
        sysUserAccountService.save(account);
        TransactionOrder order = createQuotaOrder(user, TransactionOrderPayType.BALANCE_PAY);
        order.setStatus(TransactionOrderStatus.TRADE_SUCCESS);
        order.setPayTime(new Date());
        order = transactionOrderService.save(order);
        grantPaidQuota(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void grantPaidQuota(TransactionOrder order) {
        if (Assert.isEmpty(order) || !TransactionOrderType.EDGEONE_DOMAIN_QUOTA.equals(order.getOrderType())) {
            return;
        }
        QueryWrapper<EdgeOneDomainQuotaOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("transaction_order_id", order.getId());
        if (quotaOrderDao.selectCount(wrapper) > 0) {
            return;
        }
        EdgeOneDomainQuotaOrder quotaOrder = EdgeOneDomainQuotaOrder.builder()
                .userId(order.getUserId())
                .transactionOrderId(order.getId())
                .quotaCount(1)
                .deadline(DateUtil.offsetDay(new Date(), quotaValidDays()))
                .status(RECORD_ACTIVE)
                .createTime(new Date())
                .build();
        quotaOrderDao.insert(quotaOrder);
    }

    private int usedQuota(Long userId) {
        QueryWrapper<EdgeOneRootDomainRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("status", RECORD_ACTIVE);
        return Math.toIntExact(rootDomainRecordDao.selectCount(wrapper));
    }

    private int paidQuota(Long userId) {
        QueryWrapper<EdgeOneDomainQuotaOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("status", RECORD_ACTIVE)
                .gt("deadline", new Date());
        List<EdgeOneDomainQuotaOrder> orders = quotaOrderDao.selectList(wrapper);
        return orders.stream().map(EdgeOneDomainQuotaOrder::getQuotaCount).filter(v -> v != null && v > 0).mapToInt(Integer::intValue).sum();
    }

    private int packageQuota(Long userId) {
        QueryWrapper<PurchasedFlow> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("status", PurchasedFlowConstants.ON_USED)
                .gt("deadline", new Date())
                .gt("edgeone_domain_quota", 0);
        List<PurchasedFlow> purchasedFlows = purchasedFlowService.queryByWrapper(wrapper);
        return purchasedFlows.stream().map(PurchasedFlow::getEdgeoneDomainQuota).filter(v -> v != null && v > 0).mapToInt(Integer::intValue).sum();
    }
}

