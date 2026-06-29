package com.kuocai.cdn.controller.rest;

import com.kuocai.cdn.annotation.AuthorLimiter;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.service.SysUserAccountService;
import com.kuocai.cdn.vo.SysUserAccountVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户账户(SysUserAccount)控制器
 *
 * @author makejava
 * @since 2023-02-28 15:52:18
 */
@RestController
@RequestMapping(value = "SysUserAccount")
@Scope(value = "session")
public class SysUserAccountController extends BaseController {

    @Autowired
    protected SysUserAccountService service;

    /**
     * description: 用户充值
     *
     * @param sysUserAccountVo 用户账户id，支付类型，支付金额
     * @return com.kuocai.cdn.resp.RespResult
     * @author bo
     * @date 2023/3/2 1:19 PM
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("manualOrReduceAccount")
    public RespResult manualOrReduceAccount(SysUserAccountVo sysUserAccountVo) {
        return service.manualOrReduceAccount(sysUserAccountVo, loginUser) ? RespResult.success("操作成功") : RespResult.fail("操作失败");
    }
}
