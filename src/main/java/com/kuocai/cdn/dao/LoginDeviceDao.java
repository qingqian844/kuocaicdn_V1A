package com.kuocai.cdn.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuocai.cdn.entity.LoginDevice;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * 登录设备(SysLoginDevice)数据库访问层
 *
 * @author XUEW
 * @since 2023-02-15 20:09:23
 */
@Repository
public interface LoginDeviceDao extends BaseMapper<LoginDevice> {

    /**
     * description: getLatWeekRegisterCount 获取上周7天的登录数
     *
     * @param lastSunTime 上周末时间
     * @return java.util.Map<java.lang.String, java.lang.String>
     * @author bo
     * @date 2023/3/28 16:27
     */
    @Select("select concat_ws(',',(select count(distinct user_id) from login_device where DATE(login_time) = DATE(SUBDATE(#{lastSunTime},6))) ,\n" +
            " (select count(distinct user_id) from login_device where DATE(login_time) = DATE(SUBDATE(#{lastSunTime},5))) ,\n" +
            " (select count(distinct user_id) from login_device where DATE(login_time) = DATE(SUBDATE(#{lastSunTime},4))) ,\n" +
            " (select count(distinct user_id) from login_device where DATE(login_time) = DATE(SUBDATE(#{lastSunTime},3))) ,\n" +
            " (select count(distinct user_id) from login_device where DATE(login_time) = DATE(SUBDATE(#{lastSunTime},2))) ,\n" +
            " (select count(distinct user_id) from login_device where DATE(login_time) = DATE(SUBDATE(#{lastSunTime},1))) ,\n" +
            " (select count(distinct user_id) from login_device where DATE(login_time) = DATE(#{lastSunTime}))) ")
    String getLatWeekLoginCount(String lastSunTime);

    /**
     * description: 根据传入时间获取登录数(可以与获取注册数归为一个)
     *
     * @param inTime String
     * @return java.lang.String
     * @author bo
     * @date 2023/3/28 16:43
     */
    @Select("select count(distinct user_id) from login_device where DATE(login_time) = DATE(#{inTime})")
    String getLoginCountByTime(String inTime);
}

