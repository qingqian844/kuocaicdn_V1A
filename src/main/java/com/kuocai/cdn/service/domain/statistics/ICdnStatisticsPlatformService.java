package com.kuocai.cdn.service.domain.statistics;

import cn.hutool.core.date.DateTime;
import com.kuocai.cdn.exception.BusinessException;

public interface ICdnStatisticsPlatformService {


    Object queryResourceStatistics(String domainName, DateTime start, DateTime end) throws BusinessException;

    Object queryVisitsStatistics(String domainName, DateTime start, DateTime end) throws BusinessException;


    Object queryHttpCodeStatusStatistics(String domainName, DateTime start, DateTime end) throws BusinessException;

    Object queryTopUri(String domainName, DateTime start, DateTime end) throws BusinessException;
}
