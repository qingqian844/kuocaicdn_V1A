package com.kuocai.cdn.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuocai.cdn.entity.SysUser;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户数据库访问层
 *
 * @author XUEW
 * @date 下午9:01 2023/2/12
 */
@Repository
public interface SysUserDao extends BaseMapper<SysUser> {

    /** <h1>以下方法返回类型为String是为了之后处理不需要再转</h1>*/

    /**
     * description: getLatWeekRegisterCount 获取上周7天的注册数
     *
     * @param lastSunTime
     * @return java.util.Map<java.lang.String, java.lang.String>
     * @author bo
     * @date 2023/3/28 16:27
     */
    @Select("select concat_ws(',',(select count(*) from sys_user where DATE(create_time) = DATE(SUBDATE(#{lastSunTime},6))) ,\n" +
            " (select count(*) from sys_user where DATE(create_time) = DATE(SUBDATE(#{lastSunTime},5))) ,\n" +
            " (select count(*) from sys_user where DATE(create_time) = DATE(SUBDATE(#{lastSunTime},4))) ,\n" +
            " (select count(*) from sys_user where DATE(create_time) = DATE(SUBDATE(#{lastSunTime},3))) ,\n" +
            " (select count(*) from sys_user where DATE(create_time) = DATE(SUBDATE(#{lastSunTime},2))) ,\n" +
            " (select count(*) from sys_user where DATE(create_time) = DATE(SUBDATE(#{lastSunTime},1))) ,\n" +
            " (select count(*) from sys_user where DATE(create_time) = DATE(#{lastSunTime}))) ")
    String getLatWeekRegisterCount(String lastSunTime);

    /**
     * description: 根据传入时间获取注册数
     *
     * @param inTime
     * @return java.lang.String
     * @throws
     * @author bo
     * @date 2023/3/28 16:38
     */
    @Select("select count(*) from sys_user where DATE(create_time) = DATE(#{inTime})")
    String getRegisterCountByTime(String inTime);

    /**
     * description: 更新用户等级为null
     *
     * @param id 用户id
     * @author bo
     * @date 2023/6/5 21:04
     */
    @Update("UPDATE sys_user SET agent_level_id = NULL, agent_user_id = NULL WHERE id = #{id}")
    void updateAgentLevelToNull(Long id);

    /**
     * description: 根据传入时间获取注册数
     *
     * @return java.lang.String
     * @throws
     * @author bo
     * @date 2023/3/28 16:38
     */
    @Select("SELECT * FROM sys_user WHERE id in (SELECT DISTINCT referrer_id FROM sys_user)")
    List<SysUser> queryAllReferrer();
}
