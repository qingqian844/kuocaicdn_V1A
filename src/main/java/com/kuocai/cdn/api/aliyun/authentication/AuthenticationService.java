package com.kuocai.cdn.api.aliyun.authentication;

import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AuthenticationService {

    /**
     * 获取支付宝认证Map.包含认证ID和认证链接
     *
     * @param realName  姓名
     * @param idCardNum 身份证
     * @return
     */
    public Map<String, String> getAlipayCertificationMap(String realName, String idCardNum) throws BusinessException {
        HashMap<String, String> hashMap = new HashMap<>(2);
        // 实名证件信息比对验证预咨询
        RespResult respResult = AuthenticationApi.preConsult(realName, idCardNum);
        if (!respResult.isSuccess()) {
            return hashMap;
        }
        // 返回认证链接
        String verifyId = respResult.getMessage();
        hashMap.put("verifyId", verifyId);
        hashMap.put("certificationUrl", String.format(AuthenticationConst.ALIPAY_CERTIFICATION_URL, verifyId));
        return hashMap;
    }

    /**
     * 实名认证
     *
     * @param authCode 授权码
     * @param verifyId 认证ID
     * @return 认证结果 true/false
     */
    public Boolean doCertification(String authCode, String verifyId) throws BusinessException {
        String token = AuthenticationApi.getAccessToken(authCode).getMessage();
        return AuthenticationApi.passAuthentication(verifyId, token).isSuccess();
    }

}
