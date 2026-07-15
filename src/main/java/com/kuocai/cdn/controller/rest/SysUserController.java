package com.kuocai.cdn.controller.rest;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.annotation.AuthorLimiter;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.annotation.SysLog;
import com.kuocai.cdn.common.mongo.entity.InviteReward;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.datatable.DataTableColumn;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.dto.datatable.DataTableSearch;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.SysUserService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.KuocaiDateUtil;
import com.kuocai.cdn.util.RecommendCodeUtils;
import com.kuocai.cdn.util.ValidatorUtils;
import com.kuocai.cdn.vo.SysUserVo;
import com.kuocai.cdn.vo.UserVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户控制器
 *
 * @author XUEW
 * @date 下午9:00 2023/2/12
 */
@Slf4j
@RestController
@RequestMapping(value = "SysUser")
@Scope(value = "session")
public class SysUserController extends BaseController {

    protected final SysUserService userService;
    protected final MongoTemplate mongoTemplate;

    SysUserController(SysUserService sysUserService, MongoTemplate mongoTemplate) {
        this.userService = sysUserService;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Datatables查询用户接口
     *
     * @param query 查询条件
     * @return 查询结果
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("queryUserForDatatables")
    public RespResult queryUserForDatatables(@RequestBody DataTableQuery query) {
        List<DataTableColumn> columns = query.getColumns();
        columns.add(DataTableColumn.builder().data("role_id").search(DataTableSearch.builder().value("2").build()).build());
        JSONObject datatables = userService.queryUserForDatatables(query);
        return RespResult.success("查询成功", datatables);
    }

    /**
     * Datatables查询管理员接口
     *
     * @param query 查询条件
     * @return 查询结果
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("queryAdminForDatatables")
    public RespResult queryAdminForDatatables(@RequestBody DataTableQuery query) {
        List<DataTableColumn> columns = query.getColumns();
        columns.add(DataTableColumn.builder().data("role_id").search(DataTableSearch.builder().value("1").build()).build());
        JSONObject datatables = userService.queryUserForDatatables(query);
        return RespResult.success("查询成功", datatables);
    }

    /**
     * 用户修改头像
     */
    @RateLimiter
    @PostMapping("updateImg")
    @SysLog(module = "用户管理", describe = "用户修改头像")
    public RespResult updateImg(MultipartFile file) {
        if (Assert.isEmpty(file)) {
            return RespResult.fail("未上传文件");
        }
        try {
            if (userService.updateImg(file, loginUserId)) {
                userService.rmCacheUser(loginUserId);
            }
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
        return RespResult.success("修改成功");
    }

    /**
     * 用户修改密码
     */
    @RateLimiter
    @PostMapping("updatePwd")
    @SysLog(module = "账户管理", describe = "用户修改密码")
    public RespResult updatePwd(SysUserVo sysUser) {
        Long roleId = Assert.notEmpty(loginUser) ? loginUser.getRoleId() : null;
        if (Assert.isEmpty(sysUser) || Assert.isEmpty(sysUser.getUserPwd()) || Assert.isEmpty(sysUser.getOldPwd())) {
            return RespResult.fail("输入参数错误");
        }
        if (!ValidatorUtils.isPassword(sysUser.getUserPwd())) {
            return RespResult.fail("密码格式不正确");
        }
        if (ObjectUtil.equal(sysUser.getOldPwd(), sysUser.getUserPwd())) {
            return RespResult.fail("密码不能和原来一致");
        }
        try {
            if (userService.updatePwd(sysUser, loginUserId)) {
                userService.rmCacheUser(loginUserId);
            }
            revokeAuthToken(request);
            session.invalidate();
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
        return RespResult.success("修改成功", roleId);
    }

    /**
     * 更新用户邮箱信息
     *
     * @param email 邮箱
     * @param code  验证码
     * @return 响应
     */
    @RateLimiter
    @PostMapping("updateEmail")
    @SysLog(module = "账户管理", describe = "用户更新邮箱信息")
    public RespResult updateEmailVerify(String email, String code) {
        if (Assert.isEmpty(email) || Assert.isEmpty(code)) {
            return RespResult.fail("参数错误");
        }
        if (!ValidatorUtils.isEmail(email)) {
            return RespResult.fail("非法参数[邮箱]");
        }
        if (Assert.notEmpty(email) && Assert.notEmpty(userService.queryUserByEmail(email))) {
            return RespResult.fail("邮箱已经注册");
        }
        ;
        try {
            if (userService.updateEmailVerify(loginUserId, email, code)) {
                userService.rmCacheUser(loginUserId);
            }
        } catch (BusinessException e) {
            throw new RuntimeException(e);
        }
        return RespResult.success("修改成功");
    }

    /**
     * 更新用户手机信息
     *
     * @param phone 手机
     * @param code  验证码
     * @return 响应
     */
    @RateLimiter
    @PostMapping("updatePhone")
    @SysLog(module = "账户管理", describe = "用户更新手机信息")
    public RespResult updatePhoneVerify(String phone, String code) {
        if (Assert.isEmpty(phone) || Assert.isEmpty(code)) {
            return RespResult.fail("参数错误");
        }
        if (!ValidatorUtils.isPhone(phone)) {
            return RespResult.fail("非法参数[手机]");
        }
        if (Assert.notEmpty(phone) && Assert.notEmpty(userService.queryUserByPhone(phone))) {
            return RespResult.fail("手机号已经注册");
        }
        try {
            if (userService.updatePhoneVerify(loginUserId, phone, code)) {
                userService.rmCacheUser(loginUserId);
            }
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
        return RespResult.success("绑定成功");
    }

    /**
     * 更新用户基本信息
     *
     * @param userName  用户名
     * @param myWebsite 网站
     * @return 响应
     */
    @RateLimiter
    @PostMapping("updateBaseInfo")
    @SysLog(module = "账户管理", describe = "用户更新基本信息")
    public RespResult updateBaseInfo(String userName, String myWebsite, Integer autoBalance) {
        if (Assert.isEmpty(userName)) {
            return RespResult.fail("用户名不可为空");
        }
        SysUser updateUser = SysUser.builder().id(loginUserId).userName(userName).myWebsite(myWebsite).autoBalance(autoBalance).build();
        if (Assert.notEmpty(userService.save(updateUser))) {
            userService.rmCacheUser(loginUserId);
        }
        return RespResult.success("更新成功");
    }

    /**
     * 修改用户流量单价
     *
     * @param userId    用户ID
     * @param flowPrice 流量单价
     * @return 响应
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("updateUserFlowPrice")
    @SysLog(module = "账户管理", describe = "修改用户流量单价")
    public RespResult updateUserFlowPrice(Long userId, BigDecimal flowPrice, Integer maxDomainCount, String route, Integer enableOverseas, Integer enableGlobal) {
        SysUser sysUser = userService.updateUserFlowPrice(userId, flowPrice, maxDomainCount, route, enableOverseas, enableGlobal);
        if (Assert.isEmpty(sysUser)) {
            return RespResult.fail("修改失败");
        }
        // The route belongs to the edited user. Clear that user's cache so the
        // next newly-created domain immediately uses the newly selected route.
        userService.rmCacheUser(userId);
        return RespResult.success("修改成功");
    }

    /**
     * 用户注册
     *
     * @return 响应
     */
    @RateLimiter
    @PostMapping("registerUser")
    @Transactional(rollbackFor = Exception.class)
    @SysLog(module = "账户管理", describe = "用户手机注册")
    public RespResult registerUser(@RequestBody UserVo userVo) {
        String userName = userVo.getUserName();
        String userPwd = userVo.getUserPwd();
        // 验证码
        String smsCode = userVo.getCode();
        String phone = userVo.getPhone();
        if (Assert.isEmpty(userName) || Assert.isEmpty(userPwd) || Assert.isEmpty(smsCode) || Assert.isEmpty(phone)) {
            return RespResult.fail("参数异常");
        }
        if (!ValidatorUtils.isPassword(userPwd)) {
            return RespResult.fail("密码格式不正确");
        }
        if (Assert.notEmpty(userService.queryUserByPhone(phone))) {
            return RespResult.fail("手机号已经注册");
        }
        if (Assert.notEmpty(userService.queryUserByUserName(userName))) {
            return RespResult.fail("用户名已占用");
        }
        SysUser sysUser = null;
        try {
            sysUser = userService.registerUser(userName, userPwd, smsCode, phone, agentId);
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }

        String code = userVo.getRecommend();
        if (Assert.notEmpty(code)) {
            // 获取推荐人ID
            long id = RecommendCodeUtils.codeToId(code);
            if (Assert.notEmpty(userService.queryById(id))) {
                // 注册用户设置推荐人
                sysUser.setReferrerId(id);
                userService.save(sysUser);
                // 推荐奖励
                sendInviteReward(sysUser.getId(), id);
                // 推荐人奖励
                // flowDonateService.sendFlowGift("推荐注册奖励", id, SystemConfig.websiteBaseConfig.getInviteRewardGb(), 365);
                // 自己奖励
                // flowDonateService.sendFlowGift("受邀注册奖励", sysUser.getId(), SystemConfig.websiteBaseConfig.getInvitedRewardGb(), 365);
            }
        }
        SysUserVo sysUserVo = SysUserVo.builder().userAccount(phone).userPwd(userPwd).RoleId(2L).remember(false).build();
        try {
            String token = userService.loginUser(sysUserVo, request);
            addAuthCookie(token, false, request);
            return RespResult.success("登录成功");
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    private void sendInviteReward(Long userId, Long inviteUserId) {
        InviteReward inviteReward = new InviteReward();
        inviteReward.setUserId(userId);
        inviteReward.setInviteUserId(inviteUserId);
        inviteReward.setStatus(0B00);
        inviteReward.setTimestamp(KuocaiDateUtil.getCurrentTime());
        mongoTemplate.save(inviteReward);
    }

    /**
     * 用户邮箱注册
     *
     * @return 响应
     */
    @RateLimiter
    @PostMapping("registerUserByEmail")
    @Transactional(rollbackFor = Exception.class)
    @SysLog(module = "账户管理", describe = "用户邮箱注册")
    public RespResult registerUserByEmail(@RequestBody UserVo userVo) {
        String userName = userVo.getUserName();
        String userPwd = userVo.getUserPwd();
        // 验证码
        String smsCode = userVo.getCode();
        String email = userVo.getEmail();
        if (Assert.isEmpty(userName) || Assert.isEmpty(userPwd) || Assert.isEmpty(smsCode) || Assert.isEmpty(email)) {
            return RespResult.fail("参数异常");
        }
        if (!ValidatorUtils.isPassword(userPwd)) {
            return RespResult.fail("密码格式不正确");
        }
        if (Assert.notEmpty(userService.queryUserByEmail(email))) {
            return RespResult.fail("邮箱已经注册");
        }
        if (Assert.notEmpty(userService.queryUserByUserName(userName))) {
            return RespResult.fail("用户名已占用");
        }
        SysUser sysUser = null;
        try {
            sysUser = userService.registerUserByEmail(userName, userPwd, smsCode, email, agentId);
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
        String code = userVo.getRecommend();
        if (Assert.notEmpty(code)) {
            // 推荐人奖励
            long id = RecommendCodeUtils.codeToId(code);
            if (Assert.notEmpty(userService.queryById(id))) {
                // 注册用户设置推荐人
                sysUser.setReferrerId(id);
                userService.save(sysUser);
                // 推荐奖励
                sendInviteReward(sysUser.getId(), id);
                // 推荐人奖励
//                flowDonateService.sendFlowGift("推荐注册奖励", id, SystemConfig.websiteBaseConfig.getInviteRewardGb(), 30);
                // 自己奖励
//                flowDonateService.sendFlowGift("受邀注册奖励", sysUser.getId(), SystemConfig.websiteBaseConfig.getInvitedRewardGb(), 30);
            }
        }
        SysUserVo sysUserVo = SysUserVo.builder().userAccount(email).userPwd(userPwd).RoleId(2L).remember(false).build();
        try {
            String token = userService.loginUser(sysUserVo, request);
            addAuthCookie(token, false, request);
            return RespResult.success("登录成功");
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    /**
     * 添加用户
     *
     * @return 响应
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("addUser")
    @SysLog(module = "账户管理", describe = "管理员添加用户")
    public RespResult addUser(String userName, String userPwd, String email, String phone, String idCardNum, String myWebSite, String realName, Long roleId, BigDecimal flowPrice, Integer maxDomainCount, Long agentLevelId, Integer autoBalance, MultipartFile file) {
        if (Assert.isEmpty(userName) || Assert.isEmpty(userPwd)) {
            return RespResult.fail("用户名和密码不能为空");
        }
        if (Assert.notEmpty(email) && !ValidatorUtils.isEmail(email)) {
            return RespResult.fail("邮箱格式不正确");
        }
        if (Assert.notEmpty(phone) && !ValidatorUtils.isPhone(phone)) {
            return RespResult.fail("手机格式不正确");
        }
        if (Assert.notEmpty(phone) && Assert.notEmpty(userService.queryUserByPhone(phone))) {
            return RespResult.fail("手机号已经注册");
        }
        if (Assert.notEmpty(email) && Assert.notEmpty(userService.queryUserByEmail(email))) {
            return RespResult.fail("邮箱已经注册");
        }
        if (Assert.notEmpty(userName) && Assert.notEmpty(userService.queryUserByUserName(userName))) {
            return RespResult.fail("用户名已经注册");
        }
        if (Assert.notEmpty(idCardNum) && Assert.notEmpty(userService.queryUserByIdCard(idCardNum))) {
            return RespResult.fail("身份证已经注册");
        }
        if (!ValidatorUtils.isPassword(userPwd)) {
            return RespResult.fail("密码格式不正确");
        }
        try {
            userService.addUser(userName, userPwd, email, phone, idCardNum, myWebSite, realName, roleId, flowPrice, maxDomainCount, file, agentLevelId, autoBalance);
            return RespResult.success("新增成功");
        } catch (BusinessException e) {
            return RespResult.fail(e.getMessage());
        }
    }

    /**
     * 修改用户
     *
     * @param type 密码是否被修改
     * @return 响应
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("updateUser")
    @SysLog(module = "账户管理", describe = "用户修改个人信息")
    public RespResult updateUser(Long id, String userName, String userPwd, String email, String phone, String idCardNum,
                                 String myWebSite, String realName, Long roleId, BigDecimal flowPrice, String type,
                                 Long agentLevelId, Integer autoBalance, MultipartFile file, Long agentUserId) {
        if (Assert.isEmpty(id) || Assert.isEmpty(userName) || Assert.isEmpty(type)) {
            return RespResult.fail("参数异常");
        }
        if (Assert.isEmpty(userService.queryById(id))) {
            return RespResult.notFound("user");
        }
        if (ObjectUtil.notEqual(type, "1") && Assert.isEmpty(userPwd)) {
            return RespResult.fail("密码不能为空");
        }
        if (Assert.notEmpty(email) && !ValidatorUtils.isEmail(email)) {
            return RespResult.fail("邮箱格式不正确");
        }
        if (Assert.notEmpty(phone) && !ValidatorUtils.isPhone(phone)) {
            return RespResult.fail("手机格式不正确");
        }
        if (ObjectUtil.notEqual(type, "1") && !ValidatorUtils.isPassword(userPwd)) {
            return RespResult.fail("密码格式不正确");
        }
        if (Assert.notEmpty(phone) && userService.queryUserByPhone(phone).stream().anyMatch(item -> ObjectUtil.notEqual(item.getId(), id))) {
            return RespResult.fail("手机号已经注册");
        }
        if (Assert.notEmpty(email) && userService.queryUserByEmail(email).stream().anyMatch(item -> ObjectUtil.notEqual(item.getId(), id))) {
            return RespResult.fail("邮箱已经注册");
        }
        if (Assert.notEmpty(userName) && userService.queryUserByUserName(userName).stream().anyMatch(item -> ObjectUtil.notEqual(item.getId(), id))) {
            return RespResult.fail("用户名已经注册");
        }
        if (Assert.notEmpty(idCardNum) && userService.queryUserByIdCard(idCardNum).stream().anyMatch(item -> ObjectUtil.notEqual(item.getId(), id))) {
            return RespResult.fail("身份证已经注册");
        }
        try {
            if (userService.updateUser(id, userName, userPwd, email, phone, idCardNum, myWebSite, realName, roleId, flowPrice, type, agentLevelId, file, autoBalance, agentUserId)) {
                userService.rmCacheUser(loginUserId);
            }
        } catch (BusinessException e) {
            return RespResult.fail("上传失败");
        }
        return RespResult.success("修改成功");
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 响应
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("forceDelete")
    @SysLog(module = "用户管理", describe = "删除用户")
    public RespResult forceDelete(Long id) {
        sysUserService.deleteById(id);
        sysUserAccountService.deleteByUserId(id);
        agentConfigService.deleteByUserId(id);
        cdnDomainService.deleteByUserId(id);
        cacheTaskService.deleteByUserId(id);
        flowDonateService.deleteByUserId(id);
        loginDeviceService.deleteByUserId(id);
        operationLogService.deleteByUserId(id);
        purchasedFlowService.deleteByUserId(id);
        realNameAuthenticationService.deleteByUserId(id);
        transactionOrderService.deleteByUserId(id);
        workOrderService.deleteByUserId(id);
        messageService.deleteByUserId(id);
        log.info("成功删除用户：{}", id);
        return RespResult.success("删除成功");
    }
}
