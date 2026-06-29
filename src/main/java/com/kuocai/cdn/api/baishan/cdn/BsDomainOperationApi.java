package com.kuocai.cdn.api.baishan.cdn;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.baishan.cdn.properties.BsCdn;
import com.kuocai.cdn.api.baishan.cdn.vo.*;
import com.kuocai.cdn.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class BsDomainOperationApi {

    /**
     * 创建域名
     */
    public static JSONObject create(BsDomainVo domainVo) throws BusinessException {
        String url = BsCdn.API + "/v2/domain?token=" + BsCdn.TOKEN;
        try {
            String s = BsRequest.sendPostRequest(url, domainVo);
            //返回结果
            return JSONObject.parseObject(s).getJSONObject("data");
        } catch (Exception e) {
            throw new BusinessException("bs域名创建失败" + e.getMessage()).setCause(e).log();
        }
    }


    /**
     * 启用域名, 接口中不需要使用密码
     */
    public static void enable(String domainName) throws BusinessException {
        String url = BsCdn.API + "/v2/domain/enable?token=" + BsCdn.TOKEN + "&domains=" + domainName;
        try {
            String s = BsRequest.sendGetRequest(url);
        } catch (Exception e) {
            throw new BusinessException("bs域名启用失败:" + e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 停用域名，接口中不需要使用密码
     */
    public static void disable(String domainName) throws BusinessException {
        String url = BsCdn.API + "/v2/domain/disable?token=" + BsCdn.TOKEN + "&domains=" + domainName;
        try {
            String s = BsRequest.sendGetRequest(url);
        } catch (Exception e) {
            throw new BusinessException("bs域名停用失败:" + e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 删除域名
     */
    public static void delete(String domainName) throws BusinessException {
        String url = BsCdn.API + "/v2/domain?token=" + BsCdn.TOKEN + "&domains=" + domainName;
        try {
            String s = BsRequest.sendDeleteRequest(url);
        } catch (Exception e) {
            throw new BusinessException("bs域名删除失败:" + e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 修改ipv6
     */
    public static void ipv6(String domainName, Integer ipv6Type) throws BusinessException {

    }

    /**
     * 修改源站配置
     *
     * @param sourceVo
     */
    public static void saveSourceInfo(BsBackSourceVo sourceVo) throws BusinessException {
        String url = BsCdn.API + "/v2/domain/config";
        try {
            sourceVo.setToken(BsCdn.TOKEN);
            String s = BsRequest.sendPostRequest(url, sourceVo);
        } catch (Exception e) {
            throw new BusinessException("bs保存回源请求头信息失败:" + e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 修改回源请求头
     *
     * @param requestHeadVo
     */
    public static void saveRequestHeadInfo(BsRequestHeadVo requestHeadVo) throws BusinessException {
        String url = BsCdn.API + "/v2/domain/config";
        try {
            requestHeadVo.setToken(BsCdn.TOKEN);
            String s = BsRequest.sendPostRequest(url, requestHeadVo);
        } catch (Exception e) {
            throw new BusinessException("bs保存域名源站信息失败:" + e.getMessage()).setCause(e).log();
        }
    }


    /**
     * 修改缓存配置
     *
     * @param cacheVo
     */
    public static void saveCacheInfo(BsCacheVo cacheVo) throws BusinessException {
        String url = BsCdn.API + "/v2/domain/config";
        try {
            cacheVo.setToken(BsCdn.TOKEN);
            BsRequest.sendPostRequest(url, cacheVo);
        } catch (Exception e) {
            throw new BusinessException("bs保存域名缓存信息失败:" + e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 新增证书
     *
     * @param bsCertificateVo
     * @throws BusinessException
     */
    public static int certificate(BsCertificateVo bsCertificateVo) throws BusinessException {
        String url = BsCdn.API + "/v2/domain/certificate?token=" + BsCdn.TOKEN;
        try {
            String s = BsRequest.sendPostRequest(url, bsCertificateVo);
            JSONObject certJsonObj = JSONObject.parseObject(s);
            return certJsonObj.getJSONObject("data").getInteger("cert_id");
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 删除证书
     *
     * @param id
     * @throws BusinessException
     */
    public static void delCertificate(Integer id) throws BusinessException {
        String url = BsCdn.API + "/v2/domain/certificate?token=" + BsCdn.TOKEN;
        try {
            Map<String, Object> req = new HashMap<>();
            req.put("cert_id", id);
            String s = BsRequest.sendDeleteRequest(url, req);
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }



    public static JSONObject queryCertInfo(Integer certId) throws BusinessException {
        String url = BsCdn.API + "/v2/domain/certificate?token=" + BsCdn.TOKEN + "&cert_id=" + certId;
        try {
            String s = BsRequest.sendGetRequest(url);
            return JSONObject.parseObject(s).getJSONObject("data");
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 修改证书信息
     *
     * @param httpsConfigVo
     * @throws BusinessException
     */
    public static void httpsConfig(BsHttpsConfigVo httpsConfigVo) throws BusinessException {
        String url = BsCdn.API + "/v2/domain/config";
        try {
            httpsConfigVo.setToken(BsCdn.TOKEN);
            BsRequest.sendPostRequest(url, httpsConfigVo);
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }


    /**
     * 修改HTTP2
     *
     * @param http2Status on/off
     * @throws BusinessException
     */
    public static void http2Info(String domainName, String http2Status) throws BusinessException {
        String url = BsCdn.API + "/v2/domain/config";
        try {
            JSONObject query = query(domainName);
            String forceHttps = query.getJSONObject("config").getJSONObject("https").getString("force_https");
            Integer certId = query.getJSONObject("config").getJSONObject("https").getInteger("cert_id");

            BsHttpsConfigVo httpsConfigVo = new BsHttpsConfigVo();
            httpsConfigVo.setDomains(domainName);
            httpsConfigVo.setToken(BsCdn.TOKEN);
            httpsConfigVo.setConfig(new BsHttpsConfigVo.BsHttpsConfigInner(BsHttpsConfigVo.BsHttpsVo.builder()
                    .cert_id(certId)
                    .http2(http2Status)
                    .force_https(forceHttps)
                    .build()));
            BsRequest.sendPostRequest(url, httpsConfigVo);
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 修改跳转方式
     *
     * @param forceCode
     * @throws BusinessException
     */
    public static void forceRedirect(String domainName, String forceCode) throws BusinessException {
        String url = BsCdn.API + "/v2/domain/config";
        try {
            JSONObject query = query(domainName);
            String http2 = query.getJSONObject("config").getJSONObject("https").getString("http2");
            Integer certId = query.getJSONObject("config").getJSONObject("https").getInteger("cert_id");

            BsHttpsConfigVo httpsConfigVo = new BsHttpsConfigVo();
            httpsConfigVo.setDomains(domainName);
            httpsConfigVo.setToken(BsCdn.TOKEN);
            httpsConfigVo.setConfig(new BsHttpsConfigVo.BsHttpsConfigInner(BsHttpsConfigVo.BsHttpsVo.builder()
                    .cert_id(certId)
                    .http2(http2)
                    .force_https(forceCode)
                    .build()));
            String s = BsRequest.sendPostRequest(url, httpsConfigVo);
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 修改referer防盗链
     *
     * @param refererVo
     * @throws BusinessException
     */
    public static void saveRefererInfo(BsRefererVo refererVo) throws BusinessException {
        String url = BsCdn.API + "/v2/domain/config";
        try {
            refererVo.setToken(BsCdn.TOKEN);
            String s = BsRequest.sendPostRequest(url, refererVo);
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    /**
     * 修改referer防盗链
     *
     * @param ipList
     * @throws BusinessException
     */
    public static void saveIpList(BsIpList ipList) throws BusinessException {
        String url = BsCdn.API + "/v2/domain/config";
        try {
            ipList.setToken(BsCdn.TOKEN);
            String s = BsRequest.sendPostRequest(url, ipList);
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }

    public static void deleteConfig(String domainName, List<String> configs) throws BusinessException {
        String url = BsCdn.API + "/v2/domain/config";
        try {
            Map<String, Object> req = new HashMap<>();
            req.put("token", BsCdn.TOKEN);
            req.put("domains", domainName);
            req.put("config", configs);
            String s = BsRequest.sendDeleteRequest(url, req);
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }



    /**
     * 设置压缩
     *
     * {
     *     "config": {
     *         "compress_response": {
     *             "content_type": [
     *                 "text/plain"
     *             ],
     *             "min_size": "100",
     *             "min_size_unit": "KB"
     *         }
     *     },
     *     "domains": "bs.kedaya.site",
     *     "token": "72f922a47725dbeba9b521bb3ffd184a"
     * }
     * @throws BusinessException
     */
    public static void compressResponse(String domainName) throws BusinessException {
        String url = BsCdn.API + "/v2/domain/config";
        try {
            Map<String, Object> compressResponse = new HashMap<>();
            compressResponse.put("content_type", Arrays.asList("text/plain", "text/html", "text/css", "text/xml", "application/javascript", "application/x-javascript", "application/json", "application/rss+xml", "text/javascript", "image/tiff", "image/svg+xml", "application/xml"));
            compressResponse.put("min_size", "5");
            compressResponse.put("min_size_unit", "MB");
            Map<String, Object> config = new HashMap<>();
            config.put("compress_response", compressResponse);
            Map<String, Object> req = new HashMap<>();
            req.put("config", config);
            req.put("domains", domainName);
            req.put("token", BsCdn.TOKEN);
            String s = BsRequest.sendPostRequest(url, req);
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }


    /**
     * Range回源
     *
     * @throws BusinessException
     */
    public static void rangeBackSource(String domainName, String isOpen) throws BusinessException {
        String url = BsCdn.API + "/v2/domain/config";
        try {
            Map<String, Object> req = new HashMap<>();
            req.put("token", BsCdn.TOKEN);
            req.put("domains", domainName);
            Map<String, Object> rangeIsOpen = new HashMap<>();
            rangeIsOpen.put("range_back_source", isOpen);
            req.put("config", rangeIsOpen);
            BsRequest.sendPostRequest(url, req);
        } catch (Exception e) {
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
    }


    /**
     * 查询域名详细信息
     */
    public static JSONObject query(String domainName) throws BusinessException {
        String url = BsCdn.API + "/v2/domain?token=" + BsCdn.TOKEN + "&domains=" + domainName;
        try {
            String s = BsRequest.sendGetRequest(url);
            com.alibaba.fastjson.JSONArray data = JSONObject.parseObject(s).getJSONArray("data");
            if (data == null || data.isEmpty()) {
                throw new BusinessException("账号下无此域名：" + domainName);
            }
            return data.getJSONObject(0);
        } catch (Exception e) {
            throw new BusinessException("bs查询域名详细信息失败:" + e.getMessage()).setCause(e).log();
        }
    }
}
