package com.kuocai.cdn.api.qiniu.cdn;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.qiniu.cdn.vo.*;
import com.kuocai.cdn.exception.BusinessException;
import com.qiniu.common.Constants;
import com.qiniu.common.QiniuException;
import com.qiniu.util.Json;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

/**
 * 七牛域名操作相关
 */
@Slf4j
public class QiNiuDomainOperationApi {


    /**
     * 创建域名
     */
    public static JSONObject create(String domainName, QiniuDomainVo domainVo) throws BusinessException {
        String url = "/domain/{name}";
        String replace = url.replace("{name}", domainName);
        try {
            byte[] body = Json.encode(domainVo).getBytes(Constants.UTF_8);
            return QiNiuRequest.post(replace, body, JSONObject.class);
        } catch (QiniuException e) {
            throw new BusinessException(e.error());
        }
    }


    /**
     * 启用域名, 接口中不需要使用密码
     */
    public static void enable(String domainName) throws BusinessException {
        String url = "/domain/{name}/online";
        String replace = url.replace("{name}", domainName);
        try {
            HashMap<String, String> req = new HashMap<>();
            byte[] body = Json.encode(req).getBytes(Constants.UTF_8);
            QiNiuRequest.post(replace, body);
        } catch (QiniuException e) {
            throw new BusinessException(e.error());
        }
    }

    /**
     * 停用域名，接口中不需要使用密码
     */
    public static void disable(String domainName) throws BusinessException {
        String url = "/domain/{name}/offline";
        String replace = url.replace("{name}", domainName);
        try {
            HashMap<String, String> req = new HashMap<>();
            byte[] body = Json.encode(req).getBytes(Constants.UTF_8);
            QiNiuRequest.post(replace, body);
        } catch (QiniuException e) {
            throw new BusinessException(e.error());
        }
    }

    /**
     * 删除域名
     */
    public static void delete(String domainName) throws BusinessException {
        String url = "/domain/{name}";
        String replace = url.replace("{name}", domainName);
        try {
            HashMap<String, String> req = new HashMap<>();
            byte[] body = Json.encode(req).getBytes(Constants.UTF_8);
            QiNiuRequest.delete(replace, body);
        } catch (QiniuException e) {
            throw new BusinessException(e.error());
        }
    }

    /**
     * 修改ipv6
     */
    public static void ipv6(String domainName, Integer ipv6Type) throws BusinessException {
        String url = "/domain/{name}/ipv6";
        String replace = url.replace("{name}", domainName);
        try {
            HashMap<String, Object> req = new HashMap<>();
            req.put("ipTypes", ipv6Type);
            byte[] body = Json.encode(req).getBytes(Constants.UTF_8);
            QiNiuRequest.put(replace, body);
        } catch (QiniuException e) {
            throw new BusinessException(e.error());
        }
    }

    /**
     * 修改源站配置
     * @param domainName
     * @param sourceVo
     */
    public static void saveSourceInfo(String domainName, SourceVo sourceVo) throws BusinessException {
        String url = "/domain/{name}/source";
        String replace = url.replace("{name}", domainName);
        try {

            byte[] body = Json.encode(sourceVo).getBytes(Constants.UTF_8);
            QiNiuRequest.put(replace, body);
        } catch (QiniuException e) {
            throw new BusinessException(e.error());
        }
    }


    /**
     * 修改缓存配置
     * @param domainName
     * @param cacheVo
     */
    public static void saveCacheInfo(String domainName, CacheVo cacheVo) throws BusinessException {
        String url = "/domain/{name}/cache";
        String replace = url.replace("{name}", domainName);
        try {

            byte[] body = Json.encode(cacheVo).getBytes(Constants.UTF_8);
            QiNiuRequest.put(replace, body);
        } catch (QiniuException e) {
            throw new BusinessException(e.error());
        }
    }

    /**
     * 修改证书信息
     * @param domainName
     * @param httpsConfigVo
     * @throws BusinessException
     */
    public static void httpsConfig(String domainName, HttpsConfigVo httpsConfigVo) throws BusinessException {
        String url = "/domain/{name}/httpsconf";
        String replace = url.replace("{name}", domainName);
        try {
            byte[] body = Json.encode(httpsConfigVo).getBytes(Constants.UTF_8);
            QiNiuRequest.put(replace, body);
        } catch (QiniuException e) {
            
            throw new BusinessException(e.error());
        }
    }

    /**
     * 修改referer防盗链
     * @param domainName
     * @param refererVo
     * @throws BusinessException
     */
    public static void saveRefererInfo(String domainName, RefererVo refererVo) throws BusinessException {
        String url = "/domain/{name}/referer";
        String replace = url.replace("{name}", domainName);
        try {
            byte[] body = Json.encode(refererVo).getBytes(Constants.UTF_8);
            QiNiuRequest.put(replace, body);
        } catch (QiniuException e) {
            
            throw new BusinessException(e.error());
        }
    }

    /**
     * 修改IP黑白名单
     * @param domainName
     * @param ipVo
     * @throws BusinessException
     */
    public static void saveIpInfo(String domainName, IpVo ipVo) throws BusinessException {
        String url = "/domain/{name}/ipacl";
        String replace = url.replace("{name}", domainName);
        try {
            byte[] body = Json.encode(ipVo).getBytes(Constants.UTF_8);
            QiNiuRequest.put(replace, body);
        } catch (QiniuException e) {
            
            throw new BusinessException(e.error());
        }
    }

    /**
     * 修改响应头头
     * @param domainName
     * @param respHeaderVo
     * @throws BusinessException
     */
    public static void respHeader(String domainName, RespHeaderVo respHeaderVo) throws BusinessException {
        String url = "/domain/{name}/respheader";
        String replace = url.replace("{name}", domainName);
        try {
            byte[] body = Json.encode(respHeaderVo).getBytes(Constants.UTF_8);
            QiNiuRequest.put(replace, body);
        } catch (QiniuException e) {
            
            throw new BusinessException(e.error());
        }
    }

    /**
     * HTTP升级为HTTPS
     * @param domainName
     * @param httpsConfigVo
     * @throws BusinessException
     */
    public static void sslize(String domainName, HttpsConfigVo httpsConfigVo) throws BusinessException {
        String url = "/domain/{name}/sslize";
        String replace = url.replace("{name}", domainName);
        try {
            byte[] body = Json.encode(httpsConfigVo).getBytes(Constants.UTF_8);
            QiNiuRequest.put(replace, body);
        } catch (QiniuException e) {
            
            throw new BusinessException(e.error());
        }
    }

    /**
     * HTTPS降级为HTTP
     */
    public static void unsslize(String domainName) throws BusinessException {
        String url = "/domain/{name}/unsslize";
        String replace = url.replace("{name}", domainName);
        try {
            HashMap<String, String> req = new HashMap<>();
            byte[] body = Json.encode(req).getBytes(Constants.UTF_8);
            QiNiuRequest.put(replace, body);
        } catch (QiniuException e) {
            
            throw new BusinessException(e.error());
        }
    }

    /**
     * 查询域名详细信息
     */
    public static JSONObject query(String domainName){
        String url = "/domain/{name}";
        String replace = url.replace("{name}", domainName);
        try {
            JSONObject qiNiu = QiNiuRequest.get(replace, null, JSONObject.class);
            return qiNiu;
        } catch (QiniuException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws BusinessException {
//        unsslize("qiniu.kedaya.site");
//        disable("qiniu.kedaya.site");
//        delete("qiniu.kedaya.site");


    }
}
