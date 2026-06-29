package com.kuocai.cdn.service;

import cn.hutool.core.util.ObjectUtil;
import com.kuocai.cdn.api.aliyun.faceCertifyVerify.FaceCertifyVerifyApi;
import com.kuocai.cdn.constant.ConfigBizTypeConstants;
import com.kuocai.cdn.dao.FaceCertifyVerifyDao;
import com.kuocai.cdn.entity.FaceCertifyVerify;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import com.kuocai.cdn.vo.AlipayAuthenticationConfigVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
public class FaceCertifyVerifyService extends BaseService<FaceCertifyVerify> {

    @Resource
    protected FaceCertifyVerifyDao dao;

    @Resource
    protected SysConfigService sysConfigService;

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    private String randomOrderNo() {
        String no = "KCY" + LocalDateTime.now().format(dateFormatter);
        String suffix = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 32 - no.length());
        return no + suffix;
    }

    private FaceCertifyVerify queryByUserId(Long userId) {
        // 先查缓存
        String orderNo = JedisUtil.getStr("alipay-faceVerify:" + userId);
        FaceCertifyVerify verify;
        if (Assert.notEmpty(orderNo)) {
            verify = dao.selectByOrderNo(orderNo);
        } else {
            verify = dao.selectTodayVerify(userId);
        }
        return verify;
    }

    public String doVerify(Long userId, String name, String no, String phone) throws BusinessException {
        FaceCertifyVerify verify = queryByUserId(userId);
        boolean needInit = false;
        if (null == verify) {
            needInit = true;
        } else {
            needInit = ObjectUtil.notEqual(name, verify.getName()) || ObjectUtil.notEqual(no, verify.getNo()) || ObjectUtil.notEqual(phone, verify.getPhone());
        }
        if (needInit) {
            int count = dao.countTodayVerify(userId);
            if (5 <= count) {
                throw new BusinessException("24小时内申请认证次数超限，请联系客服！");
            }
            verify = initializeVerify(userId, name, no, phone);
        }
        String url = FaceCertifyVerifyApi.startVerify(verify.getCertifyId(), currentAlipayConfig());
        log.info("用户 {}，姓名 {}，身份证号 {}，手机号 {}，开始人脸核身认证，认证ID {}，认证URL {}", userId, name, no, phone, verify.getCertifyId(), url);
        // 由于 URL 太长了，先缓存，再跳转
        JedisUtil.setStr("alipay-faceJump:" + verify.getOrderNo(), url, 10 * 60);
        return "/FaceCertifyVerify/i/" + verify.getOrderNo();
    }

    public FaceCertifyVerify initializeVerify(Long userId, String name, String no, String phone) throws BusinessException {
        FaceCertifyVerify faceCertifyVerify = FaceCertifyVerify.builder()
                .userId(userId).status("wait")
                .orderNo(randomOrderNo())
                .name(name).no(no).phone(phone)
                .build();
        faceCertifyVerify = save(faceCertifyVerify);
        log.info("用户 {}，姓名 {}，身份证号 {}，手机号 {}，开始人脸核身认证", userId, name, no, phone);
        try {
            FaceCertifyVerifyApi.initialize(faceCertifyVerify, currentAlipayConfig());
            JedisUtil.setStr("alipay-faceVerify:" + userId, faceCertifyVerify.getOrderNo(), 24 * 60 * 60);
        } catch (BusinessException e) {
            deleteById(faceCertifyVerify.getId());
            throw new BusinessException(e.getMessage());
        }
        return save(faceCertifyVerify);
    }

    public FaceCertifyVerify query(Long userId) throws BusinessException {
        FaceCertifyVerify faceCertifyVerify = queryByUserId(userId);
        if (Assert.isEmpty(faceCertifyVerify)) {
            throw new BusinessException("没有找到认证申请记录");
        }
        try {
            String query = FaceCertifyVerifyApi.query(faceCertifyVerify.getCertifyId(), currentAlipayConfig());
            log.info("用户 {}，查询人脸核身认证，认证ID {}，查询结果 {}", userId, faceCertifyVerify.getCertifyId(), query);
            // 这种情况是没有结果
            if (null == query) {
                throw new BusinessException("请在支付宝完成认证后继续！");
            }
            // 有结果
            if ("TRUE".equals(query)) {
                faceCertifyVerify.setRemark("认证成功");
                faceCertifyVerify.setStatus("success");
            } else {
                faceCertifyVerify.setRemark(query);
                faceCertifyVerify.setStatus("fail");
            }
            // 此次认证结束
            JedisUtil.delKeys(new String[]{"alipay-faceVerify:" + userId, "alipay-faceJump:" + faceCertifyVerify.getOrderNo()});
            return save(faceCertifyVerify);
        } catch (BusinessException e) {
            throw new BusinessException(e.getMessage());
        }
    }

    public String jump(String code) {
        String url = JedisUtil.getStr("alipay-faceJump:" + code);
        if (Assert.isEmpty(url)) {
            return "/";
        }
        return url;
    }

    private AlipayAuthenticationConfigVo currentAlipayConfig() {
        return sysConfigService.getConfigContentVo(
                AlipayAuthenticationConfigVo.class,
                ConfigBizTypeConstants.ALIPAY_AUTHENTICATION_CONFIG
        );
    }
}
