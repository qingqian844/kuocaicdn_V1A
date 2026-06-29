package com.kuocai.cdn.constant;


import com.kuocai.cdn.enumeration.UserStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 用户的一些默认参数
 */
@Component
public class UserConstants {

    public static final String IMG = "/common/default-avatar.png";

    public static final String STATUS = UserStatus.REGISTER_NOT_CERTIFIED.getCode();

    public static final Long ROLE_ID = 2L;

    public static final BigDecimal FLOW_PRICE = new BigDecimal("100");
}
