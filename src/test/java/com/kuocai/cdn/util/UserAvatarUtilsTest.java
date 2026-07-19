package com.kuocai.cdn.util;

import com.kuocai.cdn.constant.UserConstants;
import com.kuocai.cdn.vo.WebsiteBaseConfigVo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserAvatarUtilsTest {
    @Test
    void fallsBackToBundledAvatar() {
        assertEquals(UserConstants.IMG, UserAvatarUtils.defaultAvatar(null));
        assertEquals(UserConstants.IMG,
                UserAvatarUtils.defaultAvatar(new WebsiteBaseConfigVo()));
    }

    @Test
    void returnsConfiguredAvatar() {
        assertEquals("https://cdn.example.com/avatar.png",
                UserAvatarUtils.defaultAvatar(WebsiteBaseConfigVo.builder()
                        .defaultAvatarImg(" https://cdn.example.com/avatar.png ").build()));
    }
}
