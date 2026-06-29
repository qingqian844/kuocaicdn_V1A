package com.kuocai.cdn.api.wechat.scancode.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.LoginDeviceService;
import com.kuocai.cdn.service.SysRoleService;
import com.kuocai.cdn.service.SysUserService;
import com.kuocai.cdn.util.Assert;
import me.chanjar.weixin.mp.api.WxMpService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author xiaobo
 * @date 2023/4/10
 */
@Service
public class WeChatCodeService {

    @Resource
    private WxMpService wxMpService;

    @Resource
    private SysUserService sysUserService;

    @Resource
    private LoginDeviceService loginDeviceService;

    @Resource
    private SysRoleService roleService;

    /**
     * description: 微信扫码登录
     *
     * @author bo
     * @version 1.0
     * @date 2023/4/10 14:56
     */
    public RespResult wechatLogin(String openid, HttpServletRequest request) throws BusinessException {
        // 看是否已经绑定过
        List<SysUser> sysUserList = sysUserService.queryByWrapper(new QueryWrapper<SysUser>().eq("wechat_open_id", openid));
        if (sysUserList.isEmpty()) {
            return RespResult.success("请先登录平台进行微信绑定", "fail");
        } else {
            SysUser sysUser = sysUserList.get(0);
            Long userId = sysUser.getId();
            String token = sysUserService.getUserSignToken(sysUser, false, request);
            // 保存登录设备记录
            loginDeviceService.saveLoginDevice(userId, request);
            // 更新登录信息
            sysUserService.updateLoginInfo(userId, request);
            return RespResult.success("登录成功", token);
        }
    }

    /**
     * description: 微信绑定
     *
     * @author bo
     * @version 1.0
     * @date 2023/4/10 20:43
     */
    public RespResult wechatBinding(String openid, SysUser sysUser) {
        // 看是否已经绑定过(这里前端可以直接做状态)
        if (Assert.isEmpty(sysUser.getWechatOpenId())) {
            sysUser.setWechatOpenId(openid);
            sysUserService.save(sysUser);
            sysUserService.rmCacheUser(sysUser.getId());
            return RespResult.success("绑定成功！", "success");
        } else {
            return RespResult.success("该账户已绑定微信,若想绑定新的，请先解绑", "fail");
        }
    }

    /**
     * description: 微信绑定
     *
     * @author bo
     * @version 1.0
     * @date 2023/4/10 20:43
     */
    public RespResult wechatUnBinding(SysUser sysUser) {
        // 看是否已经绑定过(这里前端可以直接做状态)
        if (!Assert.isEmpty(sysUser.getWechatOpenId())) {
            sysUser.setWechatOpenId("");
            sysUserService.save(sysUser);
            sysUserService.rmCacheUser(sysUser.getId());
            return RespResult.success("解绑成功！");
        } else {
            return RespResult.fail("该账户还未进行微信绑定！");
        }
    }

}
