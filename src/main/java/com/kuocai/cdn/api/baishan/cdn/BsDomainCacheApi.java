package com.kuocai.cdn.api.baishan.cdn;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.baishan.cdn.properties.BsCdn;
import com.kuocai.cdn.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 白山云域名缓存操作API
 */
@Slf4j
public class BsDomainCacheApi {

    /**
     * 缓存刷新
     *
     * @param urls 刷新的url
     * @param type dir, url
     */
    public static String refresh(List<String> urls, String type) throws BusinessException {
        log.info("白山云域名缓存刷新");
        String url = BsCdn.API + "/v2/cache/refresh?token=" + BsCdn.TOKEN;
        try {
            Map<String, Object> req = new HashMap<>();
            req.put("urls", urls);
            req.put("type", type);
            String s = BsRequest.sendPostRequest(url, req);
            JSONObject resultJsonObj = JSONObject.parseObject(s);
            return resultJsonObj.getJSONObject("data").getString("task_id");
        } catch (Exception e) {
            log.error("白山云域名缓存刷新失败:[params:[{},{}];error:{}]", urls, type, e.getMessage());
            throw new BusinessException("bs域名缓存刷新失败");
        }
    }

    /**
     * 缓存刷新
     *
     * @param urls 刷新的url
     */
    public static String prefetch(List<String> urls) throws BusinessException {
        log.info("白山云域名缓存刷新");
        String url = BsCdn.API + "/v2/cache/prefetch?token=" + BsCdn.TOKEN;
        try {
            Map<String, Object> req = new HashMap<>();
            req.put("urls", urls);
            String s = BsRequest.sendPostRequest(url, req);
            JSONObject resultJsonObj = JSONObject.parseObject(s);
            return resultJsonObj.getJSONObject("data").getString("task_id");
        } catch (Exception e) {
            log.error("白山云域名文件预热失败:[params:[{}],error:{}]", urls, e.getMessage());
            throw new BusinessException("bs域名文件预热失败");
        }
    }

    /**
     * 查询缓存刷新历史
     *
     * @param taskId
     * @return
     * @throws BusinessException
     */
    public static JSONArray queryRefresh(String taskId) throws BusinessException {
        try {
            String url = BsCdn.API + "/v2/cache/refresh?token=" + BsCdn.TOKEN + "&task_id=" + taskId;
            String s = BsRequest.sendGetRequest(url);
            JSONObject resultJsonObj = JSONObject.parseObject(s);
            return resultJsonObj.getJSONObject("data").getJSONArray("list");
        } catch (Exception e) {
            log.error("白山云域名缓存刷新查询失败:[param:[{}];error:{}]", taskId, e.getMessage());
            throw new BusinessException("bs域名缓存刷新查询失败");
        }
    }

    /**
     * 查询缓存预存历史
     *
     * @param taskId
     * @return
     * @throws BusinessException
     */
    public static JSONArray queryPrefetch(String taskId) throws BusinessException {
        try {
            String url = BsCdn.API + "/v2/cache/prefetch?token=" + BsCdn.TOKEN + "&task_id=" + taskId;
            String s = BsRequest.sendGetRequest(url);
            JSONObject resultJsonObj = JSONObject.parseObject(s);
            return resultJsonObj.getJSONObject("data").getJSONArray("list");
        } catch (Exception e) {
            log.error("白山云域名缓存预存查询失败:[param:[{}];error:{}]", taskId, e.getMessage());
            throw new BusinessException("bs域名缓存预存查询失败");
        }
    }

}
