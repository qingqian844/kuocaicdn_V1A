package com.kuocai.cdn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.api.tencent.edgeone.TencentEdgeOneClient;
import com.kuocai.cdn.dao.EdgeOneRootDomainRecordDao;
import com.kuocai.cdn.entity.*;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.vo.EdgeOneDomainQuotaSummaryVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class EdgeOneDomainQuotaService extends BaseService<EdgeOneDomainQuotaOrder> {

    private static final String RECORD_ACTIVE = "active";

    @Resource
    private EdgeOneRootDomainRecordDao rootDomainRecordDao;

    public boolean isEnabled() {
        return false;
    }

    public EdgeOneDomainQuotaSummaryVo summary(Long userId) {
        int used = usedQuota(userId);
        return EdgeOneDomainQuotaSummaryVo.builder()
                .enabled(false)
                .freeQuota(0)
                .paidQuota(0)
                .packageQuota(0)
                .totalQuota(0)
                .usedQuota(used)
                .remainingQuota(Integer.MAX_VALUE)
                .overQuota(false)
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
        return summary(userId);
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
    public void grantPaidQuota(TransactionOrder order) {
        // Paid quota is intentionally unavailable in the open source edition.
    }

    private int usedQuota(Long userId) {
        QueryWrapper<EdgeOneRootDomainRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("status", RECORD_ACTIVE);
        return Math.toIntExact(rootDomainRecordDao.selectCount(wrapper));
    }

}

