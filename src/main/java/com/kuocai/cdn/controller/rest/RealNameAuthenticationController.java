package com.kuocai.cdn.controller.rest;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.annotation.AuthorLimiter;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.annotation.SysLog;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.RealNameAuthentication;
import com.kuocai.cdn.enumeration.RealNameAuthenticationStatus;
import com.kuocai.cdn.service.RealNameAuthenticationService;
import com.kuocai.cdn.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * (RealNameAuthentication)控制器
 *
 * @author XUEW
 * @since 2023-05-05 14:12:18
 */
@RestController
@RequestMapping(value = "RealNameAuthentication")
@Scope(value = "session")
public class RealNameAuthenticationController extends BaseController {

    @Autowired
    protected RealNameAuthenticationService service;

    /**
     * Datatables查询接口
     *
     * @param query 查询条件
     * @return 查询结果
     */
    @RateLimiter
    @PostMapping("queryForDatatables")
    public RespResult queryForDatatables(@RequestBody DataTableQuery query) {
        JSONObject datatables = null;
        if (isAdmin()) {
            datatables = service.queryForDatatables(null, query);
        } else {
            datatables = service.queryForDatatables(loginUserId, query);
        }
        return RespResult.success("查询成功", datatables);
    }

    /**
     * 新增一条实名认证申请记录
     *
     * @param authenticationType 认证申请类型
     * @param name               身份证名称
     * @param idCardNum          身份证号码
     * @param frontImg           正面身份证照片
     * @param backImg            反面身份证照片
     * @return
     */
    @RateLimiter
    @PostMapping("insertAuthenticationRecord")
    @SysLog(module = "个人中心", describe = "创建一个实名认证记录")
    public RespResult insertAuthenticationRecord(String authenticationType, String name, String idCardNum, MultipartFile frontImg, MultipartFile backImg) {
        if (Assert.isEmpty(loginUserId) || Assert.isEmpty(authenticationType) || Assert.isEmpty(name) || Assert.isEmpty(idCardNum) || Assert.isEmpty(frontImg)) {
            return RespResult.fail("参数异常");
        }
        // 人工审核暂不支持个人实名认证
        if ("person".equals(authenticationType)) {
            return RespResult.fail("人工审核暂不支持个人实名认证");
        }
        if (Assert.isEmpty(backImg)) {
            if ("person".equals(authenticationType)) {
                return RespResult.fail("请上传身份证反面照片！");
            } else {
                return RespResult.fail("请上传企业授权书！");
            }
        }
        String frontPath = null;
        String backPath = null;
        try {
            frontPath = ossClient.upload(frontImg);
            backPath = ossClient.upload(backImg);
        } catch (Exception e) {
            return RespResult.fail(e.getMessage());
        }
        if (Assert.isEmpty(frontPath) || Assert.isEmpty(backPath)) {
            return RespResult.fail("文件上传失败");
        }
        RealNameAuthentication recordInfo = RealNameAuthentication.builder().name(name).authenticationType(authenticationType).userId(loginUserId).idCardNum(idCardNum).frontImg(frontPath).backImg(backPath).build();
        recordInfo.setStatus(RealNameAuthenticationStatus.WAIT.getCode());
        service.save(recordInfo);
        return RespResult.success("提交认证成功");
    }


    /**
     * 审核认证申请记录
     *
     * @param id                      记录ID
     * @param authenticationOperation 操作
     * @param authenticationRemark    备注
     * @return
     */
    @AuthorLimiter
    @RateLimiter
    @PostMapping("auditAuthenticationRecord")
    @SysLog(module = "个人中心", describe = "审核认证申请记录")
    @Transactional(rollbackFor = Exception.class)
    public RespResult auditAuthenticationRecord(Long id, String authenticationOperation, String authenticationRemark, String realName, String idCardNum) {
        if (Assert.isEmpty(id) || Assert.isEmpty(authenticationOperation) || Assert.isEmpty(realName) || Assert.isEmpty(idCardNum)) {
            return RespResult.fail("参数异常");
        }
        RealNameAuthentication realNameAuthentication = service.queryById(id);
        if (ObjectUtil.equal(authenticationOperation, RealNameAuthenticationStatus.SUCCESS.getCode())) {
            realNameAuthentication.setStatus(RealNameAuthenticationStatus.SUCCESS.getCode());
            Long userId = realNameAuthentication.getUserId();
            if (Assert.notEmpty(userId)) {
                // 将用户修改为已经实名的用户
                sysUserService.user2RealName(userId, realName, idCardNum);
            }
        } else if (ObjectUtil.equal(authenticationOperation, RealNameAuthenticationStatus.FAIL.getCode())) {
            realNameAuthentication.setStatus(RealNameAuthenticationStatus.FAIL.getCode());
        } else {
            return RespResult.fail("操作异常");
        }
        // 将审批记录给修改
        realNameAuthentication.setRemark(authenticationRemark);
        service.save(realNameAuthentication);
        return RespResult.success("审核成功");
    }

}
