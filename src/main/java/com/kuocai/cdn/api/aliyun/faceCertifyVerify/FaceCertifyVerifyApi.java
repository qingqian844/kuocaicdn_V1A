package com.kuocai.cdn.api.aliyun.faceCertifyVerify;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.DatadigitalFincloudGeneralsaasFaceCertifyInitializeRequest;
import com.alipay.api.request.DatadigitalFincloudGeneralsaasFaceCertifyQueryRequest;
import com.alipay.api.request.DatadigitalFincloudGeneralsaasFaceCertifyVerifyRequest;
import com.alipay.api.response.DatadigitalFincloudGeneralsaasFaceCertifyInitializeResponse;
import com.alipay.api.response.DatadigitalFincloudGeneralsaasFaceCertifyQueryResponse;
import com.alipay.api.response.DatadigitalFincloudGeneralsaasFaceCertifyVerifyResponse;
import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.entity.FaceCertifyVerify;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.vo.AlipayAuthenticationConfigVo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FaceCertifyVerifyApi {

    public static final String SIGN_TYPE = "RSA2";

    public static final String FORMAT = "json";

    public static final String CHARSET = "UTF8";

    private static final String DEFAULT_SERVER_URL = "https://openapi.alipay.com/gateway.do";

    private static volatile String configuredServerUrl;

    private static volatile String configuredAppId;

    private static volatile String configuredPrivateKey;

    private static volatile String configuredAlipayPublicKey;

    public static void configure(String serverUrl, String appId, String privateKey, String alipayPublicKey) {
        configuredServerUrl = serverUrl;
        configuredAppId = appId;
        configuredPrivateKey = privateKey;
        configuredAlipayPublicKey = alipayPublicKey;
    }

    private static AlipayClient alipayClient() throws BusinessException {
        return alipayClient(SystemConfig.alipayAuthenticationConfig);
    }

    private static AlipayClient alipayClient(AlipayAuthenticationConfigVo adminConfig) throws BusinessException {
        return new DefaultAlipayClient(
                firstNonBlank(
                        adminConfig == null ? null : adminConfig.getGatewayUrlAlipay(),
                        valueOrDefault(configuredServerUrl, "alipay.face.server-url", "ALIPAY_FACE_SERVER_URL", DEFAULT_SERVER_URL)
                ),
                requireConfig(
                        adminConfig == null ? null : adminConfig.getAppIdAlipay(),
                        configuredAppId,
                        "alipay.face.app-id",
                        "ALIPAY_FACE_APP_ID"
                ),
                requireConfig(
                        adminConfig == null ? null : adminConfig.getPrivateKeyAlipay(),
                        configuredPrivateKey,
                        "alipay.face.private-key",
                        "ALIPAY_FACE_PRIVATE_KEY"
                ),
                FORMAT,
                CHARSET,
                requireConfig(
                        adminConfig == null ? null : adminConfig.getPublicKeyAlipay(),
                        configuredAlipayPublicKey,
                        "alipay.face.public-key",
                        "ALIPAY_FACE_PUBLIC_KEY"
                ),
                SIGN_TYPE
        );
    }

    private static String firstNonBlank(String preferredValue, String fallbackValue) {
        return hasText(preferredValue) ? preferredValue.trim() : fallbackValue;
    }

    private static String requireConfig(String adminValue, String configuredValue, String propertyName, String envName)
            throws BusinessException {
        if (hasText(adminValue)) {
            return adminValue.trim();
        }
        String value = valueOrDefault(configuredValue, propertyName, envName, "");
        if (hasText(value)) {
            return value.trim();
        }
        throw new BusinessException("Alipay face certification is not configured: " + propertyName);
    }

    private static String valueOrDefault(String configuredValue, String propertyName, String envName, String defaultValue) {
        if (hasText(configuredValue)) {
            return configuredValue.trim();
        }
        String propertyValue = System.getProperty(propertyName);
        if (hasText(propertyValue)) {
            return propertyValue.trim();
        }
        String envValue = System.getenv(envName);
        if (hasText(envValue)) {
            return envValue.trim();
        }
        return defaultValue;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static FaceCertifyVerify initialize(FaceCertifyVerify faceCertifyVerify) throws BusinessException {
        return initialize(faceCertifyVerify, SystemConfig.alipayAuthenticationConfig);
    }

    public static FaceCertifyVerify initialize(FaceCertifyVerify faceCertifyVerify,
                                               AlipayAuthenticationConfigVo adminConfig) throws BusinessException {
        AlipayClient alipayClient = alipayClient(adminConfig);
        DatadigitalFincloudGeneralsaasFaceCertifyInitializeRequest request = new DatadigitalFincloudGeneralsaasFaceCertifyInitializeRequest();
        JSONObject model = new JSONObject();
        model.put("outer_order_no", faceCertifyVerify.getOrderNo());
        model.put("biz_code", "FUTURE_TECH_BIZ_FACE_SDK");
        JSONObject identityParam = new JSONObject();
        identityParam.put("identity_type", "CERT_INFO");
        identityParam.put("cert_type", "IDENTITY_CARD");
        identityParam.put("cert_name", faceCertifyVerify.getName());
        identityParam.put("cert_no", faceCertifyVerify.getNo());
        identityParam.put("phone_no", faceCertifyVerify.getPhone());
        model.put("identity_param", identityParam);
        JSONObject merchantConfig = new JSONObject();
        merchantConfig.put("return_url", "");
        merchantConfig.put("face_reserve_strategy", "reserve");
        model.put("merchant_config", merchantConfig);
        request.setBizContent(model.toJSONString());
        log.info("人脸核身初始化请求参数：{}, {}", model.toJSONString(), faceCertifyVerify.getOrderNo());
        try {
            DatadigitalFincloudGeneralsaasFaceCertifyInitializeResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                faceCertifyVerify.setCertifyId(response.getCertifyId());
            } else {
                throw new BusinessException(response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            throw new BusinessException("人脸核身初始化失败：{}", e.getMessage()).setCause(e);
        }
        return faceCertifyVerify;
    }

    public static String startVerify(String certify_id) throws BusinessException {
        return startVerify(certify_id, SystemConfig.alipayAuthenticationConfig);
    }

    public static String startVerify(String certify_id, AlipayAuthenticationConfigVo adminConfig)
            throws BusinessException {
        AlipayClient alipayClient = alipayClient(adminConfig);
        DatadigitalFincloudGeneralsaasFaceCertifyVerifyRequest request = new DatadigitalFincloudGeneralsaasFaceCertifyVerifyRequest();
        JSONObject model = new JSONObject();
        model.put("certify_id", certify_id);
        request.setBizContent(model.toJSONString());
        try {
            DatadigitalFincloudGeneralsaasFaceCertifyVerifyResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                return response.getCertifyUrl();
            } else {
                throw new BusinessException(response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            throw new BusinessException("人脸核身失败：{}", e.getMessage()).setCause(e);
        }
    }

    public static String query(String certify_id) throws BusinessException {
        return query(certify_id, SystemConfig.alipayAuthenticationConfig);
    }

    public static String query(String certify_id, AlipayAuthenticationConfigVo adminConfig)
            throws BusinessException {
        AlipayClient alipayClient = alipayClient(adminConfig);
        DatadigitalFincloudGeneralsaasFaceCertifyQueryRequest request = new DatadigitalFincloudGeneralsaasFaceCertifyQueryRequest();
        JSONObject model = new JSONObject();
        model.put("certify_id", certify_id);
        request.setBizContent(model.toJSONString());
        try {
            DatadigitalFincloudGeneralsaasFaceCertifyQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                if ("T".equals(response.getPassed())) {
                    return "TRUE";
                } else {
                    return response.getFailReason();
                }
            } else {
                throw new BusinessException(response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            throw new BusinessException("人脸核身验证失败：{}", e.getMessage()).setCause(e);
        }
    }
}
