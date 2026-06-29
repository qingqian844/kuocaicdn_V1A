package com.kuocai.cdn.common.lagback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.common.rabbitmq.RabbitFastStart;
import com.kuocai.cdn.util.TraceIdUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class RabbitAppender extends AppenderBase<ILoggingEvent> {
    @Getter
    @Setter
    private String profile = "dev";

    private Connection connection;
    private Channel channel;

    @Override
    public void start() {
        String vhost = "dev".equals(profile) ? "/kuocaicdn_dev" : "/kuocaicdn";
        ConnectionFactory connectionFactory = RabbitFastStart.getConnectionFactory(vhost);
        boolean isConnection = true;
        try {
            connection = connectionFactory.newConnection();
        } catch (IOException | TimeoutException ignored) {
            isConnection = false;
        }
        if (isConnection) {
            super.start();
        }
    }

    @Override
    public void stop() {
        try {
            channel.close();
            connection.close();
        } catch (IOException | TimeoutException ignored) {
        }
        super.stop();
    }

    private void send(String message) {
        try {
            if (channel == null || !channel.isOpen()) {
                channel = connection.createChannel();
            }
            channel.basicPublish("boot.direct", "direct.logs", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
        } catch (IOException ignored) {
        }
    }

    private JSONArray getCallerData(StackTraceElement[] callerData) {
        JSONArray callerDataArray = new JSONArray();
        if (callerData != null) {
            for (StackTraceElement element : callerData) {
                JSONObject callerDataObj = new JSONObject();
                callerDataObj.put("class", element.getClassName());
                callerDataObj.put("method", element.getMethodName());
                callerDataObj.put("file", element.getFileName());
                callerDataObj.put("line", element.getLineNumber());
                callerDataArray.add(callerDataObj);
            }
        }
        return callerDataArray;
    }

    private JSONObject getThrowableProxy(IThrowableProxy throwableProxy) {
        JSONObject throwableProxyObj = new JSONObject();
        if (throwableProxy != null) {
            throwableProxyObj.put("className", throwableProxy.getClassName());
            throwableProxyObj.put("message", throwableProxy.getMessage());
            IThrowableProxy[] suppressed = throwableProxy.getSuppressed();
            if (suppressed != null) {
                JSONObject[] suppressedObj = new JSONObject[suppressed.length];
                for (int i = 0; i < suppressed.length; i++) {
                    IThrowableProxy suppressedProxy = suppressed[i];
                    JSONObject suppressedProxyObj = new JSONObject();
                    suppressedProxyObj.put("className", suppressedProxy.getClassName());
                    suppressedProxyObj.put("message", suppressedProxy.getMessage());
                    suppressedObj[i] = suppressedProxyObj;
                }
                throwableProxyObj.put("suppressed", suppressedObj);
            }
        }
        return throwableProxyObj;
    }

    @Override
    protected void append(ILoggingEvent iLoggingEvent) {
        Map<String, String> mdcMap = iLoggingEvent.getMDCPropertyMap();
        String requestId = null;
        if (mdcMap != null) {
            requestId = mdcMap.get(TraceIdUtil.KEY);
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("level", iLoggingEvent.getLevel().toString());
        jsonObject.put("logger", iLoggingEvent.getLoggerName());
        jsonObject.put("traceId", requestId);
        jsonObject.put("thread", iLoggingEvent.getThreadName());
        jsonObject.put("message", iLoggingEvent.getFormattedMessage());
        jsonObject.put("timestamp", iLoggingEvent.getTimeStamp());
        jsonObject.put("callerData", getCallerData(iLoggingEvent.getCallerData()));
        jsonObject.put("throwableProxy", getThrowableProxy(iLoggingEvent.getThrowableProxy()));
        send(jsonObject.toJSONString());
    }
}
