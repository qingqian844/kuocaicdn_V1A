package com.kuocai.cdn.controller.system;

import com.kuocai.cdn.controller.base.BaseController;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.CdnDomainVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * 域名统计页面跳转控制器
 *
 * @author XUEW
 * @date 下午9:00 2023/2/12
 */
@Slf4j
@Controller
@Scope(value = "session")
public class DomainStatisticsPageController extends BaseController {

    /**
     * 数据统计
     */
    @GetMapping("/data-board")
    public String dataBoard(Long userId, Map<String, Object> map) throws CdnHuaweiException {
        if (isAdmin()) {
            // 查询所有域名
            List<CdnDomainVo> cdnDomainVo = Assert.isEmpty(userId) ? cdnDomainService.getAllCdnDomainVo() : cdnDomainService.getMyCdnDomainVo(userId);
            map.put("cdnDomains", cdnDomainVo);
            // 查询所有用户
            List<SysUser> sysUsers = sysUserService.queryAll();
            map.put("users", sysUsers);
        } else {
            // 查询当前用户所有域名
            List<CdnDomainVo> cdnDomainVo = cdnDomainService.getMyCdnDomainVo(loginUserId);
            map.put("cdnDomains", cdnDomainVo);
        }
        map.put("userId", Assert.isEmpty(userId) ? loginUserId : userId);
        return "admin/data-board";
    }

    /**
     * 网络资源消耗
     */
    @GetMapping("statistics-resource")
    public String statisticsResource(Long id, Map<String, Object> map) {
        if (Assert.isEmpty(id)) {
            return "redirect:/404";
        }
        CdnDomain cdnDomain = cdnDomainService.queryById(id);
        if (Assert.isEmpty(cdnDomain)) {
            return "redirect:/404";
        }
        map.put("cdnDomain", cdnDomain);
        return "admin/domain/statistics-resource";
    }

    /**
     * 访问情况
     */
    @GetMapping("statistics-visits")
    public String statisticsVisits(Long id, Map<String, Object> map) {
        if (Assert.isEmpty(id)) {
            return "redirect:/404";
        }
        CdnDomain cdnDomain = cdnDomainService.queryById(id);
        if (Assert.isEmpty(cdnDomain)) {
            return "redirect:/404";
        }
        map.put("cdnDomain", cdnDomain);
        return "admin/domain/statistics-visits";
    }

    /**
     * 状态码
     */
    @GetMapping("statistics-status")
    public String statisticsStatus(Long id, Map<String, Object> map) {
        if (Assert.isEmpty(id)) {
            return "redirect:/404";
        }
        CdnDomain cdnDomain = cdnDomainService.queryById(id);
        if (Assert.isEmpty(cdnDomain)) {
            return "redirect:/404";
        }
        map.put("cdnDomain", cdnDomain);
        return "admin/domain/statistics-status";
    }

    /**
     * 新的统一统计查询页面
     *
     * @param id  域名ID
     * @param map Model
     * @return 模板路径
     */
    @GetMapping("statistics-query")
    public String queryStatistics(Long id, Map<String, Object> map) {
        if (Assert.isEmpty(id)) {
            return "redirect:/404";
        }
        CdnDomain cdnDomain = cdnDomainService.queryById(id);
        if (Assert.isEmpty(cdnDomain)) {
            return "redirect:/404";
        }

        map.put("cdnDomain", cdnDomain);
        // 返回新创建的模板页面
        return "CdnDomainStatistics/queryStatistics";
    }

}
