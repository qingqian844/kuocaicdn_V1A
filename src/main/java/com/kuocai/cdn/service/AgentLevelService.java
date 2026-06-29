package com.kuocai.cdn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.dao.AgentLevelDao;
import com.kuocai.cdn.entity.AgentLevel;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.Assert;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


/**
 * (AgentLevel)服务
 *
 * @author todoitbo
 * @since 2023-05-22 21:02:30
 */
@Service
public class AgentLevelService extends BaseService<AgentLevel> {

    @Resource
    protected AgentLevelDao dao;

    public boolean saveData(AgentLevel agentLevel) {
        Long id = agentLevel.getId();
        if (Assert.isEmpty(id)) {
            AgentLevel level = dao.selectOne(new QueryWrapper<AgentLevel>().eq("name", agentLevel.getName()));
            // 如果没有重名,则插入,否则不插入
            if (Assert.isEmpty(level)) {
                save(agentLevel);
                return true;
            } else {
                return false;
            }
        } else {
            save(agentLevel);
            return true;
        }


    }
}
