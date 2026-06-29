package com.kuocai.cdn.controller.redirect;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.entity.AgentConfig;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.UserStatus;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.SysUserService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 认证控制器
 *
 * @author XUEW
 * @date 下午9:00 2023/2/12
 */
@Slf4j
@Controller
@Scope(value = "session")
public class AlipayAuthenticationRedirect extends BaseController {

    AlipayAuthenticationRedirect(SysUserService userService) {
        this.userService = userService;
    }

    private final SysUserService userService;

    /**
     * 支付宝认证回调
     *
     * @param authCode 授权码
     * @param verifyId 验证ID
     * @return 跳转
     */
    @GetMapping("/alipay-authentication-redirect")
    public String redirect(@RequestParam("auth_code") String authCode,
                           @RequestParam("cert_verify_id") String verifyId, Map<String, Object> map) {
        // 获取到缓存的用户ID
        String userJson = JedisUtil.getStr("alipay-authentication:" + verifyId);
        if (Assert.isEmpty(userJson)) {
            // 认证超时
            log.error("实名认证超时，Redis中未查询到认证数据，当前登录用户：[{}]，authCode：[{}]，verifyId：[{}]", loginUserId, authCode, verifyId);
            map.put("msg", "实名认证超时，请在系统中重试");
            return "error/fail";
        }
        String url = "";
        // 解析取出用户信息
        JSONObject userObj = JSONObject.parseObject(userJson);
        // 获取代理用户信息
        SysUser sysUser = sysUserService.queryById(userObj.getLong("userId"));
        Long agentUserId = sysUser.getAgentUserId();
        if (Assert.notEmpty(agentUserId)) {
            SysUser agentUser = sysUserService.queryCacheUserById(agentUserId);
            if (Assert.notEmpty(agentUser.getAgentLevelId())) {
                AgentConfig agentConfig = agentConfigService.queryByUserId(agentUserId);
                String domain = agentConfig.getDomain();
                url = "http://" + domain;
            }
        }
        // 开始认证
        Boolean certificationResult = null;
        try {
            certificationResult = authenticationService.doCertification(authCode, verifyId);
        } catch (BusinessException e) {
            log.error("支付宝认证回调失败，当前登录用户：[{}]，错误信息：{}", loginUserId, e.getMessage());
            map.put("msg", "系统错误");
            return "error/fail";
        }
        if (Assert.isEmpty(certificationResult) || !certificationResult) {
            log.error("支付宝认证未通过，当前登录用户：[{}]", loginUserId);
            map.put("msg", "认证未通过");
            return "error/fail";
        }
        sysUser.setStatus(UserStatus.CERTIFIED.getCode());
        sysUser.setIdCardNum(userObj.getString("idCardNum"));
        sysUser.setRealName(userObj.getString("realName"));
        SysUser saveUser = userService.save(sysUser);
        session.setAttribute("loginUser", saveUser);
        return "error/success";
    }
}
