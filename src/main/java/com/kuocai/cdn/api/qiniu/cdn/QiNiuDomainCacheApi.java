package com.kuocai.cdn.api.qiniu.cdn;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.api.qiniu.cdn.dto.CacheDto;
import com.kuocai.cdn.api.qiniu.cdn.properties.QiNiuCdn;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.util.Assert;
import com.qiniu.cdn.CdnManager;
import com.qiniu.cdn.CdnResult;
import com.qiniu.common.Constants;
import com.qiniu.common.QiniuException;
import com.qiniu.util.Auth;
import com.qiniu.util.Json;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 七牛域名缓存配置相关
 */
@Slf4j
public class QiNiuDomainCacheApi {

    /**
     * 缓存刷新
     *
     * @param urls
     * @throws BusinessException
     */
    public static CdnResult.RefreshResult refreshUrls(String[] urls) throws BusinessException {

        try {
            Auth auth = Auth.create(QiNiuCdn.AK, QiNiuCdn.SK);
            CdnManager cdnManager = new CdnManager(auth);
            CdnResult.RefreshResult refreshResult = cdnManager.refreshUrls(urls);
            return refreshResult;
        } catch (QiniuException e) {
            throw new BusinessException(e.error());
        }
    }

    /**
     * 缓存刷新
     *
     * @param dirs
     * @throws BusinessException
     */
    public static CdnResult.RefreshResult refreshDirs(String[] dirs) throws BusinessException {

        try {
            Auth auth = Auth.create(QiNiuCdn.AK, QiNiuCdn.SK);
            CdnManager cdnManager = new CdnManager(auth);
            CdnResult.RefreshResult refreshResult = cdnManager.refreshDirs(dirs);
            return refreshResult;
        } catch (QiniuException e) {
            throw new BusinessException(e.error());
        }
    }

    /**
     * 文件预取
     *
     * @param urls
     * @throws BusinessException
     */
    public static CdnResult.PrefetchResult prefetch(String[] urls) throws BusinessException {
        try {
            Auth auth = Auth.create(QiNiuCdn.AK, QiNiuCdn.SK);
            CdnManager cdnManager = new CdnManager(auth);
            CdnResult.PrefetchResult prefetchResult = cdnManager.prefetchUrls(urls);
            return prefetchResult;
        } catch (QiniuException e) {
            throw new BusinessException(e.error());
        }
    }

    public static List<CacheDto> queryPrefetch(String requestId) throws BusinessException {
        String url = "/v2/tune/prefetch/list";
        try{
            Map<String, Object> req = new HashMap<>();
            req.put("requestId", requestId);
            req.put("pageSize", Integer.MAX_VALUE);
            req.put("startTime", DateUtil.formatDate(DateUtil.offsetDay(new Date(), -6)));
            req.put("endTime", DateUtil.formatDate(new Date()));
            byte[] body = Json.encode(req).getBytes(Constants.UTF_8);
            JSONObject jsonObject = QiNiuRequest.postStatis(url, body, JSONObject.class);
            if(Assert.notEmpty(jsonObject.getJSONArray("items"))){
                return jsonObject.getJSONArray("items").toJavaList(CacheDto.class);
            }else{
                return new ArrayList<>();
            }
        }catch (QiniuException e){
            throw new BusinessException(e.error());
        }
    }


    public static List<CacheDto> queryRefresh(String requestId) throws BusinessException {
        String url = "/v2/tune/refresh/list";
        try{
            Map<String, Object> req = new HashMap<>();
            req.put("requestId", requestId);
            req.put("pageSize", Integer.MAX_VALUE);
            req.put("startTime", DateUtil.formatDate(DateUtil.offsetDay(new Date(), -6)));
            req.put("endTime", DateUtil.formatDate(new Date()));
            byte[] body = Json.encode(req).getBytes(Constants.UTF_8);
            JSONObject jsonObject = QiNiuRequest.postStatis(url, body, JSONObject.class);
            if(Assert.notEmpty(jsonObject.getJSONArray("items"))){
                return jsonObject.getJSONArray("items").toJavaList(CacheDto.class);
            }else{
                return new ArrayList<>();
            }
        }catch (QiniuException e){
            throw new BusinessException(e.error());
        }
    }

    public static void main(String[] args) throws BusinessException {
        QiNiuDomainCacheApi.queryRefresh("64b4e87e43d7233c5f1d58f8");
    }
}
