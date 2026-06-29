package com.kuocai.cdn.service.domain.statistics;

import cn.hutool.core.date.DateTime;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.service.base.BaseService;


/**
 * 易凡加速域名统计(CdnDomain)服务
 */
public class YiFanDomainStatisticsServiceImpl extends BaseService<CdnDomain> implements ICdnStatisticsPlatformService {

    /**
     * 查询网络资源消耗统计信息
     *
     * @param domainName 域名
     * @param start      开始时间
     * @param end        结束时间
     * @return 响应
     */
    @Override
    public Object queryResourceStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        return null;
    }

    /**
     * 查询访问情况统计信息
     *
     * @param domainName 域名
     * @param start      开始时间
     * @param end        结束时间
     * @return 响应
     */
    @Override
    public Object queryVisitsStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        return null;
    }

    /**
     * 查询HTTP状态码统计信息
     *
     * @param domainName 域名
     * @param start      开始时间
     * @param end        结束时间
     * @return 响应
     */
    @Override
    public Object queryHttpCodeStatusStatistics(String domainName, DateTime start, DateTime end) throws BusinessException {
        return null;
    }

    /**
     * 查询 TOP URI
     *
     * @param domainName 域名
     * @param start      开始时间
     * @param end        结束时间
     * @return 结果
     * @throws CdnHuaweiException 异常
     */
    @Override
    public Object queryTopUri(String domainName, DateTime start, DateTime end) throws BusinessException {
        return null;
    }
}
