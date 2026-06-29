package com.kuocai.cdn.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuocai.cdn.entity.SysUserBanned;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysUserBannedDao extends BaseMapper<SysUserBanned> {
    /**
     * 查询用户是否被封禁
     *
     * @param userIdList 用户id列表
     * @return 被封禁的用户id列表
     */
    @Select("<script>" +
            "select user_id from sys_user_banned where user_id in " +
            "<foreach collection='userIdList' item='userId' open='(' separator=',' close=')'>#{userId}</foreach>" +
            "</script>")
    List<Long> selectBannedUserIdList(@Param("userIdList") List<Long> userIdList);
}
