package com.kuocai.cdn.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuocai.cdn.entity.SysUserAccount;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 用户账户(SysUserAccount)数据库访问层
 *
 * @author makejava
 * @since 2023-02-28 15:52:18
 */
@Repository
public interface SysUserAccountDao extends BaseMapper<SysUserAccount> {

    /**
     * description: 查询用户总充值，差额
     *
     * @return java.util.Map<java.lang.String, java.math.BigDecimal>
     * @author bo
     * @date 2023/3/20 11:23 AM
     */
    @Select("select sum(amass_recharge) totalRecharge, sum(account_balance) difference\n" +
            "from sys_user_account")
    Map<String, BigDecimal> getAllAccountInfo();

    /**
     * description: 查询排行数
     *
     * @param limitNum 排行数限制
     * @return java.util.List<java.util.Map < java.lang.String, java.lang.Object>>
     * @author bo
     * @date 2023/3/20 3:06 PM
     */
    @Select("select \n" +
            "       sua.id,\n" +
            "       sua.user_id userId,\n" +
            "       su.user_name userName,\n" +
            "       sua.account_balance accountBalance,\n" +
            "       sua.amass_recharge amassRecharge,\n" +
            "       sua.create_time createTime,\n" +
            "       sua.update_time updateTime,\n" +
            "       su.email email,\n" +
            "       su.phone phone,\n" +
            "       su.img\n" +
            "from sys_user_account sua\n" +
            "left join sys_user su on sua.user_id = su.id\n" +
            "order by sua.amass_recharge DESC\n" +
            "limit #{limitNum}")
    List<Map<String, Object>> queryRankingList(Integer limitNum);

}

