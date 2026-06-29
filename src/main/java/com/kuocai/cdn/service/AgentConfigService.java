package com.kuocai.cdn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.dao.AgentConfigDao;
import com.kuocai.cdn.entity.AgentConfig;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * (AgentConfig)服务
 *
 * @author XUEW
 * @since 2023-06-14 17:24:16
 */
@Service
public class AgentConfigService extends BaseService<AgentConfig> {

    @Autowired
    protected AgentConfigDao dao;

    @Value("${proxy.cname}")
    private String proxyCname;

    /**
     * 根据用户ID获取配置信息
     *
     * @param userId 用户ID
     */
    public synchronized AgentConfig queryByUserId(Long userId) {
        AgentConfig config = AgentConfig.builder().userId(userId).build();
        List<AgentConfig> agentConfigs = queryByObj(config);
        if (Assert.isEmpty(agentConfigs)) {
            String cname = proxyCname;
            config.setCname(cname);
            return save(config);
        }
        return agentConfigs.get(0);
    }

    /**
     * 根据域名获取配置信息
     */
    public AgentConfig queryByDomain(String domain) {
        String topLevelDomain = getTopLevelDomain(domain);
        QueryWrapper<AgentConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("domain", topLevelDomain);
        List<AgentConfig> agentConfigs = queryByWrapper(queryWrapper);
//        AgentConfig config = AgentConfig.builder().domain(domain).build();
//        List<AgentConfig> agentConfigs = queryByObj(config);
        if (Assert.isEmpty(agentConfigs)) {
            return null;
        }
        return agentConfigs.get(0);
    }

    public String getTopLevelDomain(String domain) {
        String[] parts = domain.split("\\.");
        int length = parts.length;

        if (length > 1) {
            return parts[length - 2] + "." + parts[length - 1];
        } else {
            return domain;
        }
    }
}
