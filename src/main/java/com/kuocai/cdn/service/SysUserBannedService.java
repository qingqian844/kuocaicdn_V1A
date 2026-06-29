package com.kuocai.cdn.service;

import cn.hutool.core.date.DateTime;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.dao.SysUserBannedDao;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.entity.SysUserBanned;
import com.kuocai.cdn.service.base.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service
public class SysUserBannedService extends BaseService<SysUserBanned> {

    @Resource
    protected SysUserBannedDao dao;

    public void banned(SysUser user, String reason) {
        SysUserBanned banned = SysUserBanned.builder()
                .userId(user.getId()).userName(user.getUserName()).bannedReason(reason).bannedTime(DateTime.now())
                .build();
        save(banned);
    }

    public void unbanned(SysUser user) {
        deleteByUserId(user.getId());
    }

    public SysUserBanned queryByUserId(Long id) {
        QueryWrapper<SysUserBanned> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", id);
        return dao.selectOne(wrapper);
    }

    public List<Long> queryBannedUserIdList(List<Long> userIdList) {
        return dao.selectBannedUserIdList(userIdList);
    }
}
