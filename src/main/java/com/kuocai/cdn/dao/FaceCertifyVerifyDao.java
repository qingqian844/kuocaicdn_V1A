package com.kuocai.cdn.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuocai.cdn.entity.FaceCertifyVerify;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Repository
public interface FaceCertifyVerifyDao extends BaseMapper<FaceCertifyVerify> {

    /**
     * 查询24小时内当前用户是否有认证记录
     */
    @Select("select * from face_certify_verify where user_id = #{userId} and status = 'wait' and create_time > date_sub(now(), interval 1 day) limit 1")
    FaceCertifyVerify selectTodayVerify(Long userId);

    /**
     * 通过 order_no 查询
     */
    @Select("select * from face_certify_verify where order_no = #{orderNo} limit 1")
    FaceCertifyVerify selectByOrderNo(String orderNo);

    /**
     * 查询 24小时内 用户有多少条认证
     */
    @Select("select count(1) from face_certify_verify where user_id = #{userId} and create_time > date_sub(now(), interval 1 day)")
    int countTodayVerify(Long userId);
}
