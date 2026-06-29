package com.kuocai.cdn.api.baidu.cdn.helper;

import com.baidubce.AbstractBceClient;
import com.baidubce.http.HttpMethodName;
import com.baidubce.internal.InternalRequest;
import com.baidubce.model.AbstractBceRequest;
import com.baidubce.services.cdn.CdnClient;
import com.baidubce.services.cdn.model.CdnRequest;
import com.kuocai.cdn.api.baidu.cdn.vo.GetHowToVerifyResponse;
import com.kuocai.cdn.exception.BusinessException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GetHowToVerifyHelper {
    public static GetHowToVerifyResponse invoke(CdnClient client, String domain) throws BusinessException {
        try {
            Class<? extends CdnClient> aClass = client.getClass();
            Method createRequest = aClass.getDeclaredMethod("createRequest", AbstractBceRequest.class, HttpMethodName.class, String[].class);
            createRequest.setAccessible(true);
            String[] path = new String[]{"domain", domain, "how-to-verify"};
            InternalRequest internalRequest = (InternalRequest) createRequest.invoke(client, new CdnRequest(), HttpMethodName.GET, path);
            Class<?> superclass = aClass.getSuperclass();
            Method invokeHttpClient = superclass.getDeclaredMethod("invokeHttpClient", InternalRequest.class, Class.class);
            invokeHttpClient.setAccessible(true);
            return (GetHowToVerifyResponse) invokeHttpClient.invoke(client, internalRequest, GetHowToVerifyResponse.class);
        } catch (Exception e) {
            throw new BusinessException("获取域名验证信息失败");
        }
    }
}
