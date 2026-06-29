package com.kuocai.cdn.schedule;

import cn.hutool.core.collection.ListUtil;
import com.kuocai.cdn.api.tencent.dns.ModifyRecordResponse;
import com.kuocai.cdn.api.tencent.dns.TencentApi;
import com.kuocai.cdn.api.tencent.dns.dto.ModifyRecordDTO;
import com.kuocai.cdn.api.tencent.dns.properties.TencentDns;
import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.service.CdnDomainService;
import com.kuocai.cdn.util.Assert;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author XUEW
 * @apiNote 融合切换线路任务
 */
@Slf4j
@Component
@Profile("prod")
public class MergeRouteTask {

    @Resource
    private CdnDomainService cdnDomainService;

    /**
     * 每个小时更换线路
     */
    @Scheduled(cron = "0 0 * * * *")
    public void doChange() {
        String curRoute = "";
        String huaweiWorkHours = SystemConfig.mergeCdnApiConfig.getHuaweiWorkHours();
        String volcanicWorkHours = SystemConfig.mergeCdnApiConfig.getVolcanicWorkHours();

        String[] huaweiHours = huaweiWorkHours.split(";");
        String[] volcanicHours = volcanicWorkHours.split(";");

        if (ListUtil.toList(huaweiHours).contains(String.valueOf(LocalDateTime.now().getHour()))) {
            curRoute = CdnRoute.HUAWEI.getCode();
        }
        if (ListUtil.toList(volcanicHours).contains(String.valueOf(LocalDateTime.now().getHour()))) {
            curRoute = CdnRoute.VOLCENGINE.getCode();
        }
        // 查询所有融合线路的域名
        List<CdnDomain> cdnDomains = cdnDomainService.queryMergeDomains();
        for (CdnDomain cdnDomain : cdnDomains) {
            if (Assert.isEmpty(cdnDomain.getCnameHuawei())) {
                continue;
            }
            if (Assert.isEmpty(cdnDomain.getCnameVolcengine())) {
                continue;
            }
            String cname = "";
            if (CdnRoute.HUAWEI.getCode().equals(curRoute)) {
                cname = cdnDomain.getCnameHuawei();
            }
            if (CdnRoute.VOLCENGINE.getCode().equals(curRoute)) {
                cname = cdnDomain.getCnameVolcengine();
            }
            Long tencentDnsId = cdnDomain.getTencentDnsId();
            // TODO 根据ID更新腾讯DNS对应的值为cname
            ModifyRecordDTO modifyRecordDTO = new ModifyRecordDTO();
            //hv.kedaya.site.fn06qv3y.kuocaidns.com
            String oldCname = cdnDomain.getCname().replace("." + TencentDns.LOCAL_DOMAIN_NAME, "");
//            modifyRecordDTO.setDomain(TencentDns.LOCAL_DOMAIN_NAME).setSubDomain(cdnDomain.getDomainName() + "." + RandomUtil.randomString(8)).setValue(cname);
            modifyRecordDTO.setDomain(TencentDns.LOCAL_DOMAIN_NAME).setSubDomain(oldCname).setValue(cname);
            modifyRecordDTO.setRecordId(cdnDomain.getTencentDnsId());
            try {
                ModifyRecordResponse modifyRecordResponse = TencentApi.modifyRecord(modifyRecordDTO);
                // 如果返回有记录id，说明返回成功
                if (!Assert.isEmpty(modifyRecordResponse.getRecordId())) {
                    // 自己定义括彩云的
                    cdnDomain.setCname(modifyRecordDTO.getSubDomain() + "." + TencentDns.LOCAL_DOMAIN_NAME);
                    cdnDomain.setTencentDnsId(modifyRecordResponse.getRecordId());
                    cdnDomain = cdnDomainService.save(cdnDomain);
                }
            } catch (TencentCloudSDKException e) {
                // TODO 异常处理
            }

            log.info("更新域名【{}】使用的路线为【{}】", cdnDomain.getDomainName(), cname);
        }
    }
}
