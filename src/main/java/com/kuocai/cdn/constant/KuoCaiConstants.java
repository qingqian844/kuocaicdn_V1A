package com.kuocai.cdn.constant;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;

/**
 * @author xiaobo
 * @date 2023/2/16
 */
public class KuoCaiConstants {

    /**
     * format-json
     */
    public static final String FORMAT_JSON = "json";

    /**
     * Encoding-utf-8
     */
    public static final String ENCODING_UTF = "UTF-8";

    /**
     * return error
     */
    public static final String RETURN_ERROR = "error";

    /**
     * 工单消息Redis键
     */
    public static final String WORK_ORDER_MESSAGE = "WorkOrderMsg";

    /**
     * Redis存储上周注册key(list<String>类型)
     */
    public static final String LAST_WEEK_REGISTER = "last_week_register";

    /**
     * redis存储本周注册数key(list<String>类型)
     */
    public static final String THIS_WEEK_REGISTER = "this_week_register";

    /**
     * Redis存储上周登录key(list<String>类型)
     */
    public static final String LAST_WEEK_LOGIN = "last_week_login";

    /**
     * redis存储本周登录数key(list<String>类型)
     */
    public static final String THIS_WEEK_LOGIN = "this_week_login";

    /**
     * redis存储昨日活跃用户数(set类型)
     */
    public static final String ACTIVE_USERS_YESTERDAY = "active_users_yesterday";

    /**
     * redis存储的前天活跃用户数(set类型)
     */
    public static final String DAY_BEFORE_YESTERDAY_ACTIVE_USERS = "day_before_yesterday_active_users";

    /**
     * 支付二维码前缀
     */
    public static final String ALIPAY_QR_USER = "alipay_qr_user";

    /**
     * 支付二维码过期时间
     */
    public static final Long QR_EXPIRATION_TIME = 60L;

    /**
     * 累计充值排行榜限制数
     */
    public static final Integer ACCUMULATIVE_RECHARGE_LIMITS = 5;

    /**
     * 支付宝或微信默认过期时间15分钟
     */
    public static final Integer DEFAULT_EXPIRE_TIME = 15;

    /**
     * 统计数据缓存时间
     */
    public static final int STATISTICAL_DATA_CACHE_SECOND = 60 * 30;

    /**
     * redis订单过期前缀
     */
    public static final String ORDER_REDIS_PREFIX = "order_expire:";

    /**
     * 流量包过期redis前缀
     */
    public static final String FLOW_PACKAGE_REDIS_PREFIX = "flow_package_expire:";

    /**
     * 流量包过期补偿时间，这里多2s补偿
     */
    public static final int FLOW_PACKAGE_COMPENSATE_TIME = 2;

    /**
     * 统计数据缓存时间
     */
    public static final int STATISTICAL_DATA_CACHE_SECOND_lONG = 60 * 60 * 6;

    /**
     * 推荐奖励金额
     */
    public static final Double defaultAward = 10d;

    /**
     * 统计数据缓存时间
     */
    public static final int STATISTICAL_DATA_CACHE_SECOND_SHORT = 60 * 60;

    /**
     * 正在产生数据的统计缓存时间
     */
    public static final int STATISTICAL_DATA_CACHE_SECOND_REALTIME = 60;

    /**
     * 智能缓存时间
     */
    public static int smartCacheTime(DateTime start, DateTime end) {
        DateTime now = DateUtil.date();
        if (!start.after(now) && end.after(now)) {
            return STATISTICAL_DATA_CACHE_SECOND_REALTIME;
        }
        // 计算日期差
        long between = DateUtil.between(start, end, DateUnit.DAY);
        return between > 1 ? STATISTICAL_DATA_CACHE_SECOND_lONG : STATISTICAL_DATA_CACHE_SECOND_SHORT;
    }

    /**
     * 缓存预热redis_key
     */
    public static final String CACHE_PREHEATING_KEY = "cache_preheating";

    /**
     * 缓存刷新url redis_key
     */
    public static final String CACHE_REFRESH_URL_KEY = "cache_refresh_url";

    /**
     * 缓存刷新folder redis_key
     */
    public static final String CACHE_REFRESH_FOLDER_KEY = "cache_refresh_folder";

    /**
     * 工单消息信息提醒redis过期键
     */
    public static final String WORK_ORDER_MESSAGE_REMIND_PREFIX = "wId:fd:tId:";

    /**
     * 工单消息信息提醒redis过期键(过渡，防止频繁进行交互)
     */
    public static final String WORK_ORDER_MESSAGE_REMIND_TRANSITION_PREFIX = "wId:fId:tId:t:";

    /**
     * 工单消息信息提醒redis过期时间(过渡)->秒，下同
     */
    public static final int WORK_ORDER_MESSAGE_REMIND_TRANSITION_EXPIRE = 300;

    /**
     * 工单消息信息提醒redis过期时间
     */
    public static final int WORK_ORDER_MESSAGE_REMIND_EXPIRE = 600;

}
