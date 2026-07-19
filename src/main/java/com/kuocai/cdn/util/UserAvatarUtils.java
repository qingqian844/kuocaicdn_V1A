package com.kuocai.cdn.util;

import com.kuocai.cdn.constant.UserConstants;
import com.kuocai.cdn.vo.WebsiteBaseConfigVo;

public final class UserAvatarUtils {
    private UserAvatarUtils() {
    }

    public static String defaultAvatar(WebsiteBaseConfigVo config) {
        if (config == null || Assert.isEmpty(config.getDefaultAvatarImg())) {
            return UserConstants.IMG;
        }
        return config.getDefaultAvatarImg().trim();
    }
}
