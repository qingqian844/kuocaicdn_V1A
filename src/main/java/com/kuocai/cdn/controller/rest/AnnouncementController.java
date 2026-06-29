package com.kuocai.cdn.controller.rest;

import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.annotation.AuthorLimiter;
import com.kuocai.cdn.annotation.RateLimiter;
import com.kuocai.cdn.annotation.SysLog;
import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.Announcement;
import com.kuocai.cdn.service.AnnouncementService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.AnnouncementVo;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * (Announcement)控制器
 *
 * @author todoitbo
 * @since 2023-05-10 20:39:05
 */
@RestController
@RequestMapping(value = "Announcement")
@Scope(value = "session")
public class AnnouncementController extends BaseController {

    @Resource
    protected AnnouncementService service;


    /**
     * Datatables查询接口
     *
     * @param query 查询条件
     * @return 查询结果
     */
    @RateLimiter
    @PostMapping("queryForDatatables")
    public RespResult queryForDatatables(@RequestBody DataTableQuery query) {
        JSONObject datatables = null;
        if (isAdmin()) {
            datatables = service.queryForDatatables(null, query);
        } else {
            datatables = service.queryForDatatables(loginUserId, query);
        }
        return RespResult.success("查询成功", datatables);
    }

    /**
     * 根据ID删除
     *
     * @param id ID
     * @return 响应
     */
    @RateLimiter
    @AuthorLimiter
    @PostMapping("delete")
    @SysLog(module = "系统公告", describe = "根据ID删除公告")
    public RespResult delete(Long id) {
        if (service.deleteById(id) == 1) {
            return RespResult.success("删除成功");
        }
        return RespResult.fail("删除失败");
    }

    /**
     * 根据ID批量删除
     *
     * @param ids ID
     * @return 响应
     */
    @RateLimiter
    @AuthorLimiter
    @PostMapping("deleteBatch")
    @SysLog(module = "系统公告", describe = "根据ID批量删除公告")
    public RespResult deleteBatch(@RequestParam(value = "ids[]") Long[] ids) {
        if (Assert.isEmpty(ids)) {
            return RespResult.paramEmpty("ID数组");
        }
        if (service.deleteByIds(ListUtil.toList(ids)) >= 1) {
            return RespResult.success("删除成功");
        }
        return RespResult.fail("删除失败");
    }

    /**
     * 保存用户角色
     *
     * @param vo 数据
     * @return 响应
     */
    @RateLimiter
    @AuthorLimiter
    @PostMapping("save")
    @SysLog(module = "系统公告", describe = "保存或修改信息")
    public RespResult save(AnnouncementVo vo) {
        String title = vo.getTitle();
        String content = vo.getContent();
        Long id = vo.getId();
        if (Assert.isEmpty(title)) {
            return RespResult.paramEmpty("标题");
        }
        if (Assert.isEmpty(content)) {
            return RespResult.paramEmpty("内容");
        }
        Announcement announcement = service.save(Announcement.builder().id(id).userId(loginUserId).title(title.trim()).content(content.trim()).build());
        return RespResult.success("保存成功", announcement);
    }

    @RateLimiter
    @AuthorLimiter
    @PostMapping("publish")
    @SysLog(module = "系统公告", describe = "发布公告")
    public RespResult publish(Long id) {
        service.publish(id);
        return RespResult.success("发布成功");
    }
}
