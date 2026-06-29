package com.kuocai.cdn.api.tencent.dns;

import com.kuocai.cdn.api.tencent.dns.dto.CreateRecordDTO;
import com.kuocai.cdn.api.tencent.dns.dto.DeleteRecordDTO;
import com.kuocai.cdn.api.tencent.dns.dto.ModifyRecordDTO;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import lombok.extern.slf4j.Slf4j;

/**
 * @author xiaobo
 * @date 2023/3/6
 */
@Slf4j
public class TencentApi {

    private TencentApi() {
    }

    /**
     * description: <h2>添加记录</h2>
     *
     * @param recordRequest 必传参数 domain ,SubDomain,value
     * @return com.kuocai.cdn.api.tencent.cdn.CreateRecordResponse
     * @author bo
     * @date 2023/3/6 2:25 PM
     * @link <a>https://cloud.tencent.com/document/product/1427/56180</a>
     */
    public static CreateRecordResponse createRecord(CreateRecordDTO recordRequest) throws TencentCloudSDKException {
        try {
            return DnspodClient.getDnsClient().createRecord(recordRequest);
        } catch (TencentCloudSDKException e) {
            String error = "添加记录失败：{}";
            log.error(error, e.getMessage());
            throw new TencentCloudSDKException(e.getMessage());
        }
    }

    /**
     * description: <h2>修改记录</h2>
     *
     * @return com.kuocai.cdn.api.tencent.cdn.ModifyRecordResponse
     * @author bo
     * @date 2023/3/6 2:25 PM
     * @link <a>https://cloud.tencent.com/document/product/1427/56157</a>
     */
    public static ModifyRecordResponse modifyRecord(ModifyRecordDTO modifyRecordDTO) throws TencentCloudSDKException {
        try {
            return DnspodClient.getDnsClient().modifyRecord(modifyRecordDTO);
        } catch (TencentCloudSDKException e) {
            String error = "修改记录失败：{}";
            log.error(error, e.getMessage());
            throw new TencentCloudSDKException(e.getMessage());
        }
    }

    /**
     * description: 删除记录
     *
     * @param deleteRecordRequest 必传参数RecordId(添加记录时候返回的RecordId),Domain(域名)
     * @return com.kuocai.cdn.api.tencent.cdn.DeleteRecordResponse
     * @throws TencentCloudSDKException e
     * @author bo
     * @date 2023/3/6 2:34 PM
     * @link <a>https://cloud.tencent.com/document/product/1427/56176</a>
     */
    public static DeleteRecordResponse deleteRecord(DeleteRecordDTO deleteRecordRequest) throws TencentCloudSDKException {
        try {
            return DnspodClient.getDnsClient().deleteRecord(deleteRecordRequest);
        } catch (TencentCloudSDKException e) {
            String error = "删除记录失败：{}";
            log.error(error, e.getMessage());
            throw new TencentCloudSDKException(e.getMessage());
        }
    }
}
