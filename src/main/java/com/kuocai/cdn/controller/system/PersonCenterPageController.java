package com.kuocai.cdn.controller.system;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.LoginDevice;
import com.kuocai.cdn.entity.RealNameAuthentication;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.RealNameAuthenticationStatus;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import com.kuocai.cdn.util.ValidatorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 个人中心页面跳转控制器
 *
 * @author XUEW
 * @date 下午9:00 2023/2/12
 */
@Slf4j
@Controller
@Scope(value = "session")
public class PersonCenterPageController extends BaseController {

    /**
     * 账号信息
     */
    @GetMapping("/user-info")
    public String userInfo(Map<String, Object> map) {
        List<LoginDevice> loginDevices = loginDeviceService.queryUserLastLoginDevice(loginUserId, 10);
        map.put("loginDevices", loginDevices);
        return "admin/user/user-info";
    }

    /**
     * Legacy phone verification path.
     */
    @GetMapping("/beta/verify/phone")
    public String legacyVerifyPhone() {
        return "redirect:/user-info";
    }

    /**
     * 实名认证
     */
    @GetMapping("/authentication-real-name")
    public String authenticationRealName(String error, Map<String, Object> map) {
        SysUser user = sysUserService.queryById(loginUserId);
        map.put("loginUser", user);
        map.put("error", error);
        sysUserService.rmCacheUser(loginUserId);
        List<RealNameAuthentication> authentications = realNameAuthenticationService.queryByObj(RealNameAuthentication.builder().userId(loginUserId).status(RealNameAuthenticationStatus.WAIT.getCode()).build());
        map.put("canSubmit", Assert.isEmpty(authentications));
        return "user/account/authentication-real-name";
    }

    /**
     * 实名认证申请记录
     */
    @GetMapping("/authentication-real-name-list")
    public String authenticationRealNameList(Map<String, Object> map) {
        if (isAdmin()) {
            return adminAuthenticationRealNameList(map);
        }
        return userAuthenticationRealNameList(map);
    }

    /**
     * 管理员-认证管理
     */
    public String adminAuthenticationRealNameList(Map<String, Object> map) {
        List<SysUser> sysUsers = sysUserService.queryAll();
        map.put("users", sysUsers);
        return "admin/user/authentication-real-name-list";
    }

    /**
     * 普通用户-认证管理
     */
    public String userAuthenticationRealNameList(Map<String, Object> map) {
        return "user/account/authentication-real-name-list";
    }

    /**
     * 实名认证人工审核
     */
    @GetMapping("/authentication-real-name-manual-check")
    public String authenticationRealNameManualCheck(Map<String, Object> map) {
        List<RealNameAuthentication> authentications = realNameAuthenticationService.queryByObj(RealNameAuthentication.builder().userId(loginUserId).status(RealNameAuthenticationStatus.WAIT.getCode()).build());
        map.put("canSubmit", Assert.isEmpty(authentications));
        return "user/account/authentication-real-name-manual-check";
    }

    /**
     * 请求支付宝实名认证
     */
    @GetMapping("/alipay-authentication")
    public void alipayAuthentication(String realName, String idCardNum, HttpServletResponse response) throws IOException {
        //这里前端校验比较好，不然很突兀(明天处理^_^)
        if (Assert.isEmpty(realName) || !ValidatorUtils.isIDCard(idCardNum)) {
            response.sendRedirect("authentication-real-name?error=ERROR");
            return;
        }
        // 查询此实名信息是否被占用
        List<SysUser> sysUsers = sysUserService.queryByObj(SysUser.builder().idCardNum(idCardNum).build());
        if (Assert.notEmpty(sysUsers)) {
            response.sendRedirect("authentication-real-name?error=EXIST");
            return;
        }
        Map<String, String> certificationMap = null;
        try {
            certificationMap = authenticationService.getAlipayCertificationMap(realName, idCardNum);
        } catch (BusinessException e) {
            log.error("请求支付宝实名认证失败：{}", e.getMessage());
            response.sendRedirect("500");
        }
        String certificationUrl = certificationMap.get("certificationUrl");
        String verifyId = certificationMap.get("verifyId");
        // 认证结果缓存
        JSONObject userObj = new JSONObject();
        userObj.put("realName", realName);
        userObj.put("idCardNum", idCardNum);
        userObj.put("userId", loginUserId);
        JedisUtil.setStr("alipay-authentication:" + verifyId, userObj.toJSONString(), 5 * 60);
        log.info("获取到支付宝实名认证链接：{}", certificationUrl);
        log.info("获取到支付宝实名认证ID：{}", verifyId);
        log.info("当前登录用户ID：{}", loginUserId);
        response.sendRedirect(certificationUrl);
    }

    @PostMapping("/alipay-authentication-qrcode")
    @ResponseBody
    public RespResult alipayAuthentication(String realName, String idCardNum) throws IOException {
        //这里前端校验比较好，不然很突兀(明天处理^_^)
        if (Assert.isEmpty(realName) || !ValidatorUtils.isIDCard(idCardNum)) {
            return RespResult.fail("实名参数不合法");
        }
        // 查询此实名信息是否被占用
        List<SysUser> sysUsers = sysUserService.queryByObj(SysUser.builder().idCardNum(idCardNum).build());
        if (Assert.notEmpty(sysUsers)) {
            return RespResult.fail("实名信息已被占用");
        }
        Map<String, String> certificationMap = null;
        try {
            certificationMap = authenticationService.getAlipayCertificationMap(realName, idCardNum);
        } catch (BusinessException e) {
            log.error("请求支付宝实名认证失败：{}", e.getMessage());
            return RespResult.fail("请求支付宝实名认证失败");
        }
        String certificationUrl = certificationMap.get("certificationUrl");
        String verifyId = certificationMap.get("verifyId");
        // 认证结果缓存
        JSONObject userObj = new JSONObject();
        userObj.put("realName", realName);
        userObj.put("idCardNum", idCardNum);
        userObj.put("userId", loginUserId);
        JedisUtil.setStr("alipay-authentication:" + verifyId, userObj.toJSONString(), 5 * 60);
        log.info("获取到支付宝实名认证链接：{}", certificationUrl);
        log.info("获取到支付宝实名认证ID：{}", verifyId);
        log.info("当前登录用户ID：{}", loginUserId);
        return RespResult.success("获取授权二维码成功", certificationUrl);
    }
}
