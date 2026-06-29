package com.kuocai.cdn.service;

import com.kuocai.cdn.dao.SysRoleDao;
import com.kuocai.cdn.entity.SysRole;
import com.kuocai.cdn.service.base.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 角色服务
 *
 * @author XUEW
 * @date 下午9:03 2023/2/12
 */
@Service
public class SysRoleService extends BaseService<SysRole> {

    @Autowired
    protected SysRoleDao dao;
}