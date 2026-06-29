package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.dao.AnnouncementDao;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.entity.Announcement;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.service.base.VoData;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.AnnouncementVo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.kuocai.cdn.constant.AnnouncementStatus.HISTORY;
import static com.kuocai.cdn.constant.AnnouncementStatus.PUBLISHED;

/**
 * (Announcement)服务
 *
 * @author todoitbo
 * @since 2023-05-10 20:39:05
 */
@Service
public class AnnouncementService extends BaseService<Announcement> implements VoData<Announcement, AnnouncementVo> {

    @Resource
    protected AnnouncementDao dao;

    @Resource
    private SysUserService sysUserService;

    /**
     * 重写Datatable查询接口
     *
     * @param query 查询参数
     * @return 响应
     */
    @Override
    public JSONObject queryForDatatables(Long userId, DataTableQuery query) {
        JSONObject jsonObject = null;
        if (Assert.isEmpty(userId)) {
            jsonObject = super.queryForDatatables(query);
        } else {
            jsonObject = super.queryForDatatables(userId, query);
        }
        JSONArray jsonArray = jsonObject.getJSONArray("data");
        jsonObject.put("data", convert2Vo(jsonArray.toJavaList(Announcement.class)));
        return jsonObject;
    }

    /**
     * 查询方法，补齐信息
     * 通过vo方式返回前端
     */
    @Override
    public List<AnnouncementVo> convert2Vo(List<Announcement> announcements) {
        if (Assert.isEmpty(announcements)) {
            return new ArrayList<>();
        }
        List<Long> userIds = announcements.stream().map(Announcement::getUserId).distinct().collect(Collectors.toList());
        List<SysUser> sysUsers = sysUserService.queryByIds(userIds);
        Map<Long, SysUser> sysUserMap = sysUsers.stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        ArrayList<AnnouncementVo> announcementArrayList = new ArrayList<>();
        for (Announcement announcement : announcements) {
            String jsonString = JSONObject.toJSONString(announcement);
            AnnouncementVo announcementVo = JSONObject.parseObject(jsonString, AnnouncementVo.class);
            SysUser sysUser = sysUserMap.get(announcement.getUserId());
            announcementVo.setImg(sysUser.getImg());
            announcementVo.setUserName(sysUser.getUserName());
            announcementArrayList.add(announcementVo);
        }
        return announcementArrayList;
    }

    /**
     * 获取已发布状态的公告
     */
    public Announcement getPublished() {
        List<Announcement> announcements = queryByWrapper(new QueryWrapper<Announcement>().eq("status", PUBLISHED));
        if (Assert.notEmpty(announcements)) {
            return announcements.get(0);
        }
        return null;
    }

    /**
     * 保存公告
     *
     * @param announcement 保存目标
     */
    @Override
    public Announcement save(Announcement announcement) {
        boolean newPublish = Assert.isEmpty(announcement.getId());
        announcement.setStatus(PUBLISHED);
        Announcement save = super.save(announcement);
        if (newPublish) {
            publish(save.getId());
        }
        return save;
    }

    /**
     * 发布公告
     */
    public void publish(Long id) {
        Announcement published = getPublished();
        if (Assert.notEmpty(published)) {
            published.setStatus(HISTORY);
            super.save(published);
        }
        super.save(Announcement.builder().id(id).status(PUBLISHED).build());
    }
}
