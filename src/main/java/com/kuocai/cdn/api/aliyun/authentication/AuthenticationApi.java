package com.kuocai.cdn.api.aliyun.authentication;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.request.AlipayUserCertdocCertverifyConsultRequest;
import com.alipay.api.request.AlipayUserCertdocCertverifyPreconsultRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.alipay.api.response.AlipayUserCertdocCertverifyConsultResponse;
import com.alipay.api.response.AlipayUserCertdocCertverifyPreconsultResponse;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import static com.kuocai.cdn.api.aliyun.authentication.AuthenticationConst.*;

/**
 * 支付宝实名认证API
 */
@Slf4j
public class AuthenticationApi {

    /**
     * 实名证件信息比对验证预咨询
     * 获取verify_id，申请验证ID
     * 有效期为 2 小时，过期后在校验接口使用会报错 "校验信息已过期"
     *
     * @param realName  姓名
     * @param idCardNum 身份证号
     * @return 响应
     * @link https://opendocs.alipay.com/open/02qq4q
     */
    public static RespResult preConsult(String realName, String idCardNum) throws BusinessException {
        AlipayClient alipayClient = new DefaultAlipayClient(SERVER_URL, APP_ID, PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);
        AlipayUserCertdocCertverifyPreconsultRequest request = new AlipayUserCertdocCertverifyPreconsultRequest();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user_name", realName);
        jsonObject.put("cert_type", "IDENTITY_CARD");
        jsonObject.put("cert_no", idCardNum);
        request.setBizContent(jsonObject.toJSONString());
        try {
            AlipayUserCertdocCertverifyPreconsultResponse response = alipayClient.execute(request);
            // 响应成功返回verify_id,响应失败返回失败信息
            return response.isSuccess() ? RespResult.success(response.getVerifyId()) : RespResult.fail(response.getSubMsg());
        } catch (AlipayApiException e) {
            throw new BusinessException("实名证件信息比对验证预咨询接口调用失败，{}", e.getMessage()).setCause(e);
        }
    }

    /**
     * 换取授权访问令牌
     *
     * @return token
     * @link https://opendocs.alipay.com/open/02qq4q
     */
    public static RespResult getAccessToken(String code) throws BusinessException {
        AlipayClient alipayClient = new DefaultAlipayClient(SERVER_URL, APP_ID, PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);
        AlipaySystemOauthTokenRequest request = new AlipaySystemOauthTokenRequest();
        request.setGrantType("authorization_code");
        request.setCode(code);
        AlipaySystemOauthTokenResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            throw new BusinessException("换取授权访问令牌接口调用失败，{}", e.getMessage()).setCause(e);
        }
        return response.isSuccess() ? RespResult.success(response.getAccessToken()) : RespResult.fail(response.getSubMsg());
    }

    /**
     * 实名证件信息比对验证咨询
     *
     * @param verifyId 信息校验验证ID
     * @return 是否通过
     * @link https://opendocs.alipay.com/open/02qq4q
     */
    public static RespResult passAuthentication(String verifyId, String accessToken) throws BusinessException {
        AlipayClient alipayClient = new DefaultAlipayClient(SERVER_URL, APP_ID, PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);
        AlipayUserCertdocCertverifyConsultRequest request = new AlipayUserCertdocCertverifyConsultRequest();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("verify_id", verifyId);
        request.setBizContent(jsonObject.toJSONString());
        AlipayUserCertdocCertverifyConsultResponse response = null;
        try {
            response = alipayClient.execute(request, accessToken);
        } catch (AlipayApiException e) {
            throw new BusinessException("实名证件信息比对验证咨询接口调用失败，{}", e.getMessage()).setCause(e);
        }
        return "T".equals(response.getPassed()) ? RespResult.success() : RespResult.fail(response.getSubMsg());
    }
}