package com.kuocai.cdn.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuocai.cdn.entity.WithdrawRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * (WithdrawRecord)数据库访问层
 *
 * @author todoitbo
 * @since 2023-06-17 14:54:09
 */
@Repository
public interface WithdrawRecordDao extends BaseMapper<WithdrawRecord> {

    @Select("select count(1) from withdraw_record where create_time > #{withdrawTime} and status = 'waiting' and user_id = #{userId}")
    Integer toTestWhetherThisWeek(@Param("withdrawTime") String withdrawTime, @Param("userId") Long userId);

}

