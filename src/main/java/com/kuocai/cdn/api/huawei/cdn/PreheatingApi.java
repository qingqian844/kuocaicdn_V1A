package com.kuocai.cdn.api.huawei.cdn;

import com.alibaba.fastjson.JSONObject;
import com.cloud.apigateway.sdk.utils.Request;
import com.kuocai.cdn.api.huawei.cdn.dto.PreheatingTaskDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.PreheatingUrlTaskDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.RefreshTaskDTO;
import com.kuocai.cdn.api.huawei.cdn.properties.HuaWeiCdn;
import com.kuocai.cdn.exception.CdnHuaweiException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

/**
 * 华为云刷新预热API
 */
@Slf4j
public class PreheatingApi {

    /**
     * 创建刷新缓存任务
     *
     * @param refreshTask 刷新缓存任务
     * @return {@code JSONObject}
     * @link https://support.huaweicloud.com/intl/zh-cn/api-cdn/CreateRefreshTasks.html
     */
    public static JSONObject createRefreshCacheTask(RefreshTaskDTO refreshTask) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.POST_REFRESH_TASKS, "POST");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("refresh_task", refreshTask);
            request.setBody(jsonObject.toJSONString());
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "创建刷新缓存任务！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 创建预热任务
     *
     * @param preheatingTask 预热任务
     * @return {@code JSONObject}
     * @link https://support.huaweicloud.com/intl/zh-cn/api-cdn/CreatePreheatingTasks.html
     */
    public static JSONObject createPreheatingTask(PreheatingTaskDTO preheatingTask) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.POST_PREHEATING_TASKS, "POST");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("preheating_task", preheatingTask);
            request.setBody(jsonObject.toJSONString());
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "创建预热任务！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }


    /**
     * 查询刷新预热任务
     *
     * @param preheatingTask 预热任务
     * @return {@code JSONObject}
     * @link https://support.huaweicloud.com/intl/zh-cn/api-cdn/ShowHistoryTasks.html
     */
    public static JSONObject getHistoryTasks(PreheatingTaskDTO preheatingTask) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.GET_HISTORY_TASK, "GET");
            HuaweiRequest.addQueryStringParamDTO(request, preheatingTask);
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "查询刷新预热任务！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 查询刷新预热任务详情
     *
     * @param historyTasksId 预热任务Id
     * @param preheatingTask 预热任务
     * @return {@code JSONObject}
     * @link https://support.huaweicloud.com/intl/zh-cn/api-cdn/ShowHistoryTaskDetails.html
     */
    public static JSONObject getHistoryTaskDetail(String historyTasksId, PreheatingTaskDTO preheatingTask) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.GET_HISTORY_TASK_DETAIL.replace("{history_tasks_id}", historyTasksId), "GET");
            HuaweiRequest.addQueryStringParamDTO(request, preheatingTask);
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "查询刷新预热任务详情！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

    /**
     * 查询刷新预热URL记录。
     * 如需此接口，请提交工单开通
     *
     * @param preheatingUrlTaskDTO 预热任务
     * @return {@code JSONObject}
     * @link https://support.huaweicloud.com/intl/zh-cn/api-cdn/ShowUrlTaskInfo.html
     */
    public static JSONObject getPreheatingUrlTasks(PreheatingUrlTaskDTO preheatingUrlTaskDTO) throws CdnHuaweiException {
        try {
            Request request = HuaweiRequest.getRequest(HuaWeiCdn.GET_URL_TASKS, "GET");
            HuaweiRequest.addQueryStringParamDTO(request, preheatingUrlTaskDTO);
            Response response = HuaweiRequest.doRequest(request);
            return HuaweiRequest.dealResponse(response);
        } catch (Exception e) {
            String error = "查询刷新预热URL记录！错误原因：{}";
            log.error(error, e.getMessage());
            throw new CdnHuaweiException(error, e.getMessage()).setCause(e);
        }
    }

}
