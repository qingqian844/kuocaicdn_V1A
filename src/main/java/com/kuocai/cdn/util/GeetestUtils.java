package com.kuocai.cdn.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Pattern;

@Slf4j
public class GeetestUtils {
    // 用于粗略校验 verify 字符串
    // public static final String r_verify = "^\\{\"captcha_id\":\"%s\",\"lot_number\":\"\\w+\",\"gen_time\":\"\\d+\",\"pass_token\":\"[\\w\\-\\+/=]+\",\"captcha_output\":\"[\\w\\-\\+/=]+\"\\}$";
    public static final String r_verify = "^\\{\"captcha_id\":\"%s\",\"lot_number\":\"[\\w\\-\\+/=]+\",\"gen_time\":\"\\d+\",\"pass_token\":\".+}$";
    private static final String captchaId = "949c66977e30cb2d8dd5643cf0bc6451";
    private static final String captchaKey = "46df46cf405be811cdab239a865f7997";
    private static final String api = "https://gcaptcha4.geetest.com";

    public static boolean validate(String verify) {
        if (!isVerifyStr(verify)) {
            log.info("验证 verify 字符串不合法，内容：{}", verify);
            return false;
        }
        JSONObject verifyJson;
        try {
            verifyJson = JSON.parseObject(verify);
        } catch (Exception e) {
            return false;
        }
        String lotNumber = verifyJson.getString("lot_number");
        String signToken = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, captchaKey).hmacHex(lotNumber);
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("lot_number", lotNumber);
        queryParams.add("captcha_output", verifyJson.getString("captcha_output"));
        queryParams.add("pass_token", verifyJson.getString("pass_token"));
        queryParams.add("gen_time", verifyJson.getString("gen_time"));
        queryParams.add("sign_token", signToken);
        String url = String.format(api + "/validate" + "?captcha_id=%s", captchaId);
        RestTemplate client = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpMethod method = HttpMethod.POST;
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        JSONObject resJson = new JSONObject();
        try {
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(queryParams, headers);
            ResponseEntity<String> response = client.exchange(url, method, requestEntity, String.class);
            resJson = JSON.parseObject(response.getBody());
        } catch (Exception e) {
            resJson.put("result", "error");
            resJson.put("reason", "验证码接口请求失败");
        }
        return "success".equals(resJson.getString("result"));
    }

    public static boolean isVerifyStr(String verify) {
        return !Assert.isEmpty(verify) && Pattern.matches(String.format(r_verify, captchaId), verify);
    }
}
