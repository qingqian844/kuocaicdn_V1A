package com.kuocai.cdn.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuocai.cdn.entity.CdnDomain;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * 加速域名(CdnDomain)数据库访问层
 *
 * @author XUEW
 * @since 2023-02-26 23:30:24
 */
@Repository
public interface CdnDomainDao extends BaseMapper<CdnDomain> {

    /**
     * 根据用户ID和ID查询
     * @param userId 用户ID
     * @param id     ID
     * @return ID
     */
    @Select("select `id` from `cdn_domain` where `user_id` = #{userId} and `id` = #{id} limit 1")
    Long selectOneIdByUserIdAndId(Long userId, Long id);
}
