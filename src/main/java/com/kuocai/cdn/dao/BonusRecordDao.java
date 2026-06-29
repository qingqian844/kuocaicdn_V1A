package com.kuocai.cdn.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuocai.cdn.entity.BonusRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * (BonusRecord)数据库访问层
 *
 * @author todoitbo
 * @since 2023-06-14 20:03:23
 */
@Repository
public interface BonusRecordDao extends BaseMapper<BonusRecord> {

    @Select("select * from bonus_record where status = 'waiting' and create_time < #{startTime}")
    List<BonusRecord> getConfirmBonusRecords(@Param("startTime") String startTime);

}

