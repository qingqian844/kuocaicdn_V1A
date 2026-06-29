package com.kuocai.cdn.controller.system;

import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.entity.SysUserBanned;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Slf4j
@Controller
@Scope(value = "session")
public class FrontPageController extends BaseController {

    @GetMapping("/")
    public String root(Map<String, Object> map) {
        return "index";
    }

    @GetMapping("/index")
    public String index(Map<String, Object> map) {
        return "redirect:/";
    }

    @GetMapping("/index.html")
    public String indexHtml(Map<String, Object> map) {
        return "redirect:/";
    }

    @GetMapping("/product_price")
    public String productPrice(Map<String, Object> map) {
        return "product_price";
    }

    @GetMapping("/contact")
    public String contact(Map<String, Object> map) {
        return "contact";
    }

    @GetMapping("/dcdn")
    public String dcdn(Map<String, Object> map) {
        return "user/dcdn";
    }

    @GetMapping("/banned")
    public String banned(Map<String, Object> map) {
        SysUserBanned banned = null;
        if (loginUser != null) {
            banned = sysUserBannedService.queryByUserId(loginUser.getId());
        }
        if (banned == null) {
            return "redirect:/";
        }
        map.put("banned", banned);
        return "error/banned";
    }

    @GetMapping("/example")
    public String example(Map<String, Object> map) {
        java.util.List<SysUser> sysUsers = sysUserService.queryAll();
        map.put("sysUsers", sysUsers);
        return "example";
    }
}
