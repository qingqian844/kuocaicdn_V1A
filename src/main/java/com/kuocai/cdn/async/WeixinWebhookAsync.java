package com.kuocai.cdn.async;

import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.entity.WorkOrder;
import com.kuocai.cdn.service.SysUserService;
import com.kuocai.cdn.util.RuntimeConfigUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Slf4j
@Component
public class WeixinWebhookAsync {
    private final SysUserService sysUserService;
    private final Executor executorService;
    private final String webhookUrl;

    WeixinWebhookAsync(@Lazy SysUserService sysUserService,
                       @Qualifier("taskExecutor") Executor executorService,
                       @Value("${weixin.webhook.url:}") String webhookUrl) {
        this.sysUserService = sysUserService;
        this.executorService = executorService;
        this.webhookUrl = webhookUrl;
    }

    public void sendNewWorkOrderMessage(WorkOrder workOrder, String workOrderContent) {
        if (!RuntimeConfigUtils.hasText(webhookUrl)) {
            log.warn("Weixin work-order webhook is not configured");
            return;
        }
        executorService.execute(() -> sendMessage(workOrder, workOrderContent));
    }

    private void sendMessage(WorkOrder workOrder, String workOrderContent) {
        JSONObject message = new JSONObject();
        message.put("msgtype", "markdown");
        JSONObject markdown = new JSONObject();
        SysUser user = sysUserService.queryById(workOrder.getUserId());
        String userName = user == null ? String.valueOf(workOrder.getUserId()) : user.getUserName();
        String content;
        if (workOrderContent == null) {
            content = String.format("新工单提醒：\n> 编号：%d\n> 用户：%s\n> 标题：%s\n> 域名：%s\n> 内容：%s\n\n[查看工单](https://www.kuocaicdn.com/order-detail?workOrderId=%d)", workOrder.getId(), userName, workOrder.getTitle(), workOrder.getDomain(), workOrder.getRemark(), workOrder.getId());
        } else {
            content = String.format("工单回复提醒：\n> 编号：%d\n> 用户：%s\n> 标题：%s\n> 域名：%s\n> 内容：%s\n\n[查看工单](https://www.kuocaicdn.com/order-detail?workOrderId=%d)", workOrder.getId(), userName, workOrder.getTitle(), workOrder.getDomain(), workOrderContent, workOrder.getId());
        }
        markdown.put("content", content);
        message.put("markdown", markdown);
        String result = sendPost(webhookUrl, message.toJSONString());
        log.info("Send work-order webhook result: {}", result);
    }

    public static String sendPost(String url, String body) {
        HttpPost post = new HttpPost(url);
        post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        post.setEntity(new StringEntity(body, "UTF-8"));
        try (CloseableHttpClient client = HttpClientBuilder.create().build();
             CloseableHttpResponse response = client.execute(post)) {
            return response.toString();
        } catch (Exception e) {
            log.error("send post fail.", e);
            return null;
        }
    }
}
