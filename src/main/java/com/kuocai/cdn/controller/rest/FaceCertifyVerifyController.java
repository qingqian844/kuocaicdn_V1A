package com.kuocai.cdn.controller.rest;

import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.common.mongo.entity.InviteReward;
import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.FaceCertifyVerify;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.enumeration.UserStatus;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.FlowDonateService;
import com.kuocai.cdn.service.SysUserService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.ValidatorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;

@Slf4j
@RestController
@RequestMapping(value = "FaceCertifyVerify")
@Scope(value = "session")
public class FaceCertifyVerifyController extends BaseController {

    private final SysUserService userService;
    private final FlowDonateService flowDonateService;
    private final MongoTemplate mongoTemplate;
    private final Executor executorService;

    FaceCertifyVerifyController(SysUserService userService, FlowDonateService flowDonateService, MongoTemplate mongoTemplate,
                                @Qualifier("cdnDomainExecutor") Executor executorService) {
        this.userService = userService;
        this.flowDonateService = flowDonateService;
        this.mongoTemplate = mongoTemplate;
        this.executorService = executorService;
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
                // 邀请奖励
                executorService.execute(() -> {
                    // query one where userId = loginUserId
                    Query query = Query.query(Criteria.where("userId").is(saveUser.getId()));
                    InviteReward one = mongoTemplate.findOne(query, InviteReward.class);
                    if (Assert.notEmpty(one) && !one.isUserReceived()) {
                        flowDonateService.sendFlowGift("受邀注册奖励", sysUser.getId(), SystemConfig.websiteBaseConfig.getInvitedRewardGb(), 365);
                        one.setUserReceived();
                        mongoTemplate.save(one);
                    }
                });
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
