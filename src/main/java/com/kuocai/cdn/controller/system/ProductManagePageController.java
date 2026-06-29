package com.kuocai.cdn.controller.system;

import com.kuocai.cdn.annotation.AuthorLimiter;
import com.kuocai.cdn.controller.base.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Slf4j
@Controller
@Scope(value = "session")
public class ProductManagePageController extends BaseController {

    @AuthorLimiter
    @GetMapping("/user-price-list")
    public String userPriceList(Map<String, Object> map) {
        return "admin/product/user-price-list";
    }

    @GetMapping({
            "/flow-donate",
            "/flow-package-list",
            "/buy-flow-packages",
            "/buy-flow-packages-customize",
            "/flow-package-detail",
            "/purchased-flow-list",
            "/bought-flow-list",
            "/bought-flow-detail",
            "/gift",
            "/hidden-sales"
    })
    public String removedProductPages() {
        return "redirect:/product_price";
    }
}
