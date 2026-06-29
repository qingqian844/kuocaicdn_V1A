package com.kuocai.cdn.api.tencent.dns;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.kuocai.cdn.api.tencent.dns.dto.CreateRecordDTO;
import com.kuocai.cdn.api.tencent.dns.dto.DeleteRecordDTO;
import com.kuocai.cdn.api.tencent.dns.dto.ModifyRecordDTO;
import com.kuocai.cdn.api.tencent.dns.properties.TencentDns;
import com.tencentcloudapi.common.AbstractClient;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.JsonResponseModel;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;

import java.lang.reflect.Type;

/**
 * @author xiaobo
 * @date 2023/3/6
 */
public class DnspodClient extends AbstractClient {

    public DnspodClient(Credential credential, String region) {
        this(credential, region, new ClientProfile());
    }

    public DnspodClient(Credential credential, String region, ClientProfile profile) {
        super(TencentDns.END_POINT, TencentDns.API_VERSION, credential, region, profile);
    }

    /**
     * 添加记录
     *
     * @param req CreateRecordRequest
     * @return CreateRecordResponse
     * @throws TencentCloudSDKException
     */
    public CreateRecordResponse createRecord(CreateRecordDTO req) throws TencentCloudSDKException {
        JsonResponseModel<CreateRecordResponse> rsp = null;
        String rspStr = "";
        try {
            Type type = new TypeToken<JsonResponseModel<CreateRecordResponse>>() {
            }.getType();
            rspStr = this.internalRequest(req, "CreateRecord");
            rsp = gson.fromJson(rspStr, type);
        } catch (JsonSyntaxException e) {
            throw new TencentCloudSDKException("response message: " + rspStr + ".\n Error message: " + e.getMessage());
        }
        return rsp.response;
    }

    /**
     * 删除记录
     *
     * @param req DeleteRecordRequest
     * @return DeleteRecordResponse
     * @throws TencentCloudSDKException
     */
    public DeleteRecordResponse deleteRecord(DeleteRecordDTO req) throws TencentCloudSDKException {
        JsonResponseModel<DeleteRecordResponse> rsp = null;
        String rspStr = "";
        try {
            Type type = new TypeToken<JsonResponseModel<DeleteRecordResponse>>() {
            }.getType();
            rspStr = this.internalRequest(req, "DeleteRecord");
            rsp = gson.fromJson(rspStr, type);
        } catch (JsonSyntaxException e) {
            throw new TencentCloudSDKException("response message: " + rspStr + ".\n Error message: " + e.getMessage());
        }
        return rsp.response;
    }

    /**
     * description: 初始化客户端，为调用腾讯云其余接口做铺垫
     *
     * @return com.kuocai.cdn.api.tencent.dns.DnspodClient
     * @author bo
     * @date 2023/3/8 10:07 AM
     */
    public static DnspodClient getDnsClient() {
        Credential cred = new Credential(
                TencentDns.requiredSecretId(),
                TencentDns.requiredSecretKey()
        );
        // 实例化一个http选项，可选的，没有特殊需求可以跳过
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint(TencentDns.END_POINT);
        // 实例化一个client选项，可选的，没有特殊需求可以跳过
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        // 实例化要请求产品的client对象,clientProfile是可选的
        return new DnspodClient(cred, "");
    }

    /**
     * 修改记录
     *
     * @param req ModifyRecordRequest
     * @return ModifyRecordResponse
     */
    public ModifyRecordResponse modifyRecord(ModifyRecordDTO req) throws TencentCloudSDKException {
        JsonResponseModel<ModifyRecordResponse> rsp = null;
        String rspStr = "";
        try {
            Type type = new TypeToken<JsonResponseModel<ModifyRecordResponse>>() {
            }.getType();
            rspStr = this.internalRequest(req, "ModifyRecord");
            rsp = gson.fromJson(rspStr, type);
        } catch (JsonSyntaxException e) {
            throw new TencentCloudSDKException("response message: " + rspStr + ".\n Error message: " + e.getMessage());
        }
        return rsp.response;
    }
}
