package com.kuocai.cdn.controller.rest;

import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.entity.SysMenu;
import com.kuocai.cdn.service.SysMenuService;
import com.kuocai.cdn.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

/**
 * (SysMenu)控制器
 *
 * @author XUEW
 * @since 2023-05-09 19:27:28
 */
@Controller
@Scope(value = "session")
public class SysMenuController extends BaseController {

    @Autowired
    protected SysMenuService service;

    /**
     * 菜单配置
     */
    @GetMapping("/menu")
    public String menu(Map<String, Object> map) {
        map.put("menus", sysMenuService.queryAll());
        return "admin/settings/menu";
    }

    /**
     * 保存菜单
     */
    @PostMapping("/saveMenu")
    public String saveMenu(SysMenu menu, Map<String, Object> map) {
        String url = menu.getUrl();
        String name = menu.getName();
        if (Assert.isEmpty(url) && Assert.isEmpty(name)) {
            return "redirect:/menu";
        }
        if (!url.startsWith("/") && !url.startsWith("http")) {
            menu.setUrl("http://" + url);
        }
        if ("2".equals(menu.getLevel())) {
            menu.setType("only-main");
        }
        Integer priority = menu.getPriority();
        menu.setPriority(Assert.isEmpty(priority) ? 0 : priority);
        service.save(menu);
        return "redirect:/menu";
    }

    /**
     * 根据ID删除
     */
    @GetMapping("deleteMenu")
    public String deleteMenu(Long id) {
        service.deleteById(id);
        return "redirect:/menu";
    }
}
