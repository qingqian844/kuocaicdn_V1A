package com.kuocai.cdn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.dao.SysMenuDao;
import com.kuocai.cdn.entity.SysMenu;
import com.kuocai.cdn.service.base.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * (SysMenu)服务
 *
 * @author XUEW
 * @since 2023-05-09 19:27:28
 */
@Service
public class SysMenuService extends BaseService<SysMenu> {

    @Autowired
    protected SysMenuDao dao;

    /**
     * 根据等级查询菜单
     */
    public List<SysMenu> queryByLevel(String level) {
        QueryWrapper<SysMenu> queryWrapper = new QueryWrapper<SysMenu>().eq("level", level).orderByDesc("priority");
        return queryByWrapper(queryWrapper);
    }

    /**
     * 获取主站二级菜单
     */
    public List<SysMenu> queryMainLevel2Menus() {
        QueryWrapper<SysMenu> queryWrapper = new QueryWrapper<SysMenu>().eq("level", "2").orderByDesc("priority");
        return queryByWrapper(queryWrapper);
    }

    /**
     * 获取主站一级菜单
     */
    public List<SysMenu> queryMainLevel1Menus() {
        QueryWrapper<SysMenu> queryWrapper = new QueryWrapper<SysMenu>()
                .eq("level", "1")
                .in("type", "only-main", "both")
                .orderByDesc("priority");
        return queryByWrapper(queryWrapper);
    }


    /**
     * 获取代理一级菜单
     */
    public List<SysMenu> queryProxyLevel1Menus() {
        QueryWrapper<SysMenu> queryWrapper = new QueryWrapper<SysMenu>()
                .eq("level", "1")
                .in("type", "only-proxy", "both")
                .orderByDesc("priority");
        return queryByWrapper(queryWrapper);
    }

}
