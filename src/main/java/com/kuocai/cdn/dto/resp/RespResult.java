package com.kuocai.cdn.dto.resp;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.kuocai.cdn.util.Assert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 接口相应结果
 *
 * @author XUEW
 * @date 下午9:01 2023/2/12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespResult {

    /**
     * 响应编码
     */
    protected String code;

    /**
     * 响应状态
     */
    protected Boolean flag;

    /**
     * 响应信息
     */
    protected String message;

    /**
     * 响应数据
     */
    protected Object data;

    /**
     * 请求成功
     */
    public static RespResult success() {
        return RespResult.builder()
                .code("SUCCESS")
                .message("请求成功")
                .build();
    }

    /**
     * 请求成功
     */
    public static RespResult success(String message) {
        return RespResult.builder()
                .code("SUCCESS")
                .message(message)
                .build();
    }

    /**
     * 请求成功
     */
    public static RespResult success(String message, Object data) {
        return RespResult.builder()
                .code("SUCCESS")
                .message(message)
                .data(data)
                .build();
    }


    /**
     * 未查询到数据
     */
    public static RespResult notFound() {
        return RespResult.builder()
                .code("NOT_FOUND")
                .message("请求资源不存在")
                .build();
    }

    /**
     * 未查询到数据
     */
    public static RespResult notFound(String message) {
        return RespResult.builder()
                .code("NOT_FOUND")
                .message(String.format("%s不存在", message))
                .build();
    }

    /**
     * 未查询到数据
     */
    public static RespResult notFound(String message, Object data) {
        return RespResult.builder()
                .code("NOT_FOUND")
                .message(String.format("%s不存在", message))
                .data(data)
                .build();
    }

    /**
     * 请求参数不能为空
     */
    public static RespResult paramEmpty() {
        return RespResult.builder()
                .code("PARAM_EMPTY")
                .message("请求参数为空")
                .build();
    }

    /**
     * 请求参数不能为空
     */
    public static RespResult paramEmpty(String message) {
        return RespResult.builder()
                .code("PARAM_EMPTY")
                .message(String.format("%s参数为空", message))
                .build();
    }

    /**
     * 请求参数不能为空
     */
    public static RespResult paramEmpty(String message, Object data) {
        return RespResult.builder()
                .code("PARAM_EMPTY")
                .message(String.format("%s参数为空", message))
                .data(data)
                .build();
    }

    /**
     * 请求失败
     */
    public static RespResult fail() {
        return RespResult.builder()
                .code("FAIL")
                .message("请求失败")
                .build();
    }

    /**
     * 请求失败
     */
    public static RespResult fail(String message) {
        return RespResult.builder()
                .code("FAIL")
                .message(message)
                .build();
    }

    /**
     * 请求失败
     */
    public static RespResult fail(String message, Object data) {
        return RespResult.builder()
                .code("FAIL")
                .message(message)
                .data(data)
                .build();
    }

    /**
     * 请求是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(code);
    }

    /**
     * 请求是否成功并有数据返回
     */
    public boolean isSuccessWithDateResp() {
        return "SUCCESS".equals(code) && Assert.notEmpty(data);
    }

    /**
     * 请求是否成功
     */
    public boolean notSuccess() {
        return !isSuccess();
    }

    /**
     * 获取响应的数据集合
     */
    public <T> List<T> getDataList(Class<T> clazz) {
        String jsonString = JSONObject.toJSONString(data);
        return JSONObject.parseArray(jsonString, clazz);
    }

    /**
     * 获取响应的数据
     */
    public <T> T getDataObj(Class<T> clazz) {
        String jsonString = JSONObject.toJSONString(data);
        return JSONObject.parseObject(jsonString, clazz);
    }

    /**
     * 获取响应的数据
     *
     * @return JSON 字符串
     */
    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", code);
        jsonObject.put("flag", flag);
        jsonObject.put("message", message);
        jsonObject.put("data", data);
        jsonObject.put("success", isSuccess());
        jsonObject.put("successWithDateResp", isSuccessWithDateResp());
        return JSONObject.toJSONString(jsonObject, SerializerFeature.WriteMapNullValue);
    }
}
