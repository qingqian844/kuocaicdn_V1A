package com.kuocai.cdn.controller.rest;

import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.FaceCertifyVerify;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.UserStatus;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.SysUserService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.ValidatorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "FaceCertifyVerify")
@Scope(value = "session")
public class FaceCertifyVerifyController extends BaseController {

    private final SysUserService userService;

    FaceCertifyVerifyController(SysUserService userService) {
        this.userService = userService;
    }

    @RateLimiter
    @PostMapping("/verify")
    public RespResult verify(String realName, String idCardNum, String phone) {
        if (Assert.isEmpty(realName) || !ValidatorUtils.isIDCard(idCardNum) || !ValidatorUtils.isPhone(phone)) {
            return RespResult.fail("实名参数不合法");
        }
        List<SysUser> sysUsers = sysUserService.queryByObj(SysUser.builder().idCardNum(idCardNum).build());
        if (Assert.notEmpty(sysUsers)) {
            return RespResult.fail("实名信息已被占用");
        }
        try {
            String url = faceCertifyVerifyService.doVerify(loginUserId, realName, idCardNum, phone);
            return RespResult.success("获取认证二维码成功", url);
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @RateLimiter
    @PostMapping("/query")
    public RespResult query() {
        try {
            FaceCertifyVerify verify = faceCertifyVerifyService.query(loginUserId);
            if ("success".equals(verify.getStatus())) {
                SysUser sysUser = userService.queryById(verify.getUserId());
                sysUser.setStatus(UserStatus.CERTIFIED.getCode());
                sysUser.setIdCardNum(verify.getNo());
                sysUser.setRealName(verify.getName());
                SysUser saveUser = userService.save(sysUser);
                session.setAttribute("loginUser", saveUser);
                return RespResult.success("实名认证成功");
            }
            return RespResult.fail(verify.getRemark());
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    @RateLimiter
    @GetMapping("/i/{orderNo}")
    public void jump(@PathVariable String orderNo, HttpServletResponse httpServletResponse) {
        String url = faceCertifyVerifyService.jump(orderNo);
        try {
            httpServletResponse.sendRedirect(url);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
