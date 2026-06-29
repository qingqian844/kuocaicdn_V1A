package com.kuocai.cdn.common.mongo.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "invite_reward")
public class InviteReward {
    @Id
    private String id;
    // 用户id
    private Long userId;
    // 邀请人id
    private Long inviteUserId;
    // 二进制 分别 用户和邀请人是否领取奖励
    // 0 0 未领取 0 1 用户领取 1 0 邀请人领取 1 1 都领取
    private Integer status;
    private String timestamp;

    // 是否用户领取
    public boolean isUserReceived() {
        return (status & 1) == 1;
    }

    // 是否邀请人领取
    public boolean isInviteUserReceived() {
        return (status & 2) == 2;
    }

    // 设置用户领取
    public void setUserReceived() {
        status |= 1;
    }

    // 设置邀请人领取
    public void setInviteUserReceived() {
        status |= 2;
    }

    // 是否都领取
    public boolean isAllReceived() {
        return status == 3;
    }

    // 设置都领取
    public void setAllReceived() {
        status = 3;
    }
}
