package com.kuocai.cdn.service;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.async.SmsAsync;
import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.constant.PurchasedFlowConstants;
import com.kuocai.cdn.dao.PurchasedFlowDao;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.entity.PurchasedFlow;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.service.base.VoData;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.KuocaiBaseUtil;
import com.kuocai.cdn.vo.PurchasedFlowVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;


/**
 * (PurchasedFlow)服务
 *
 * @author makejava
 * @since 2023-04-01 17:01:41
 */
@Slf4j
@Service
public class PurchasedFlowService extends BaseService<PurchasedFlow> implements VoData<PurchasedFlow, PurchasedFlowVo> {

    @Autowired
    protected PurchasedFlowDao dao;

    @Lazy
    @Resource
    private SysUserService sysUserService;

    @Resource
    private FlowPackageService flowPackageService;

    @Resource
    private SmsAsync smsAsync;

    @Resource
    private SysUserBannedService sysUserBannedService;


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
        jsonObject.put("data", convert2Vo(jsonArray.toJavaList(PurchasedFlow.class)));
        return jsonObject;
    }

    /**
     * 查询用户在用状态流量包
     *
     * @param userId 用户ID
     * @param limit  查询条数
     * @return 结果
     */
    public List<PurchasedFlowVo> queryUserOnUsedPurchasedFlow(Long userId, Integer limit) {
        QueryWrapper<PurchasedFlow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("status", PurchasedFlowConstants.ON_USED);
        queryWrapper.orderByAsc("deadline");
        if (ObjectUtil.notEqual(limit, 0)) {
            queryWrapper.last("limit " + limit);
        }
        return convert2Vo(queryByWrapper(queryWrapper));
    }

    /**
     * 存在额外的流量包可用
     *
     * @param userId 用户ID
     * @return 是否存在
     */
    public boolean hasAdditionalFlow(Long userId) {
        List<PurchasedFlowVo> purchasedFlowVos = queryUserOnUsedPurchasedFlow(userId, 10);
        return purchasedFlowVos.size() > 1;
    }

    /**
     * 转为Vo对象
     *
     * @param purchasedFlows 已购流量包列表
     * @return VO列表
     */
    @Override
    public List<PurchasedFlowVo> convert2Vo(List<PurchasedFlow> purchasedFlows) {
        if (Assert.isEmpty(purchasedFlows)) {
            return new ArrayList<>();
        }
        // 解析所有用户ID
        List<Long> userIds = purchasedFlows.stream().map(PurchasedFlow::getUserId).distinct().collect(Collectors.toList());
        List<SysUser> sysUsers = sysUserService.queryByIds(userIds);
        Map<Long, SysUser> sysUserMap = sysUsers.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        ArrayList<PurchasedFlowVo> purchasedFlowVos = new ArrayList<>();
        for (PurchasedFlow purchasedFlow : purchasedFlows) {
            String jsonString = JSONObject.toJSONString(purchasedFlow);
            PurchasedFlowVo purchasedFlowVo = JSONObject.parseObject(jsonString, PurchasedFlowVo.class);
            SysUser sysUser = sysUserMap.get(purchasedFlow.getUserId());
            purchasedFlowVo.setFlowPackageSizeString(KuocaiBaseUtil.autoReducedFlowUnit(purchasedFlow.getFlowPackageSize()));
            purchasedFlowVo.setUsedFlowSizeString(KuocaiBaseUtil.autoReducedFlowUnit(purchasedFlow.getUsedFlow()));
            purchasedFlowVo.setPercentage(KuocaiBaseUtil.percentage(purchasedFlow.getUsedFlow(), purchasedFlow.getFlowPackageSize()));
            String[] split = purchasedFlowVo.getPercentage().split("%");
            purchasedFlowVo.setPercentageValue(Double.valueOf(split[0]));
            purchasedFlowVo.setUserName(sysUser.getUserName());
            purchasedFlowVo.setImg(sysUser.getImg());
            // 过滤出用户信
            purchasedFlowVos.add(purchasedFlowVo);
        }
        return purchasedFlowVos;
    }

    /**
     * 禁用已购流量包
     *
     * @param purchasedFlowId
     * @param remark
     */
    public void bandPurchasedFlow(Long purchasedFlowId, String remark) {
        log.info("禁用已购流量包，流量包ID：{}，备注：{}", purchasedFlowId, remark);
        PurchasedFlow purchasedFlow = PurchasedFlow.builder().id(purchasedFlowId).status(PurchasedFlowConstants.ON_BANED).banedReason(remark).build();
        save(purchasedFlow);
    }

    /**
     * 恢复已购流量包
     *
     * @param purchasedFlowId 购买流程id
     */
    public void unBandPurchasedFlow(Long purchasedFlowId) {
        log.info("恢复已购流量包，流量包ID：{}", purchasedFlowId);
        PurchasedFlow purchasedFlow = PurchasedFlow.builder().id(purchasedFlowId).status(PurchasedFlowConstants.ON_USED).banedReason("").build();
        save(purchasedFlow);
    }

    /**
     * 将流量包设置为已过期状态
     *
     * @param flowPackageIds
     */
    public void updateOnOverByIds(List<Long> flowPackageIds) {
        log.info("将流量包设置为已过期状态，流量包IDS：{}", flowPackageIds);
        updateObjByIds(flowPackageIds, PurchasedFlow.builder().status(PurchasedFlowConstants.ON_EXPIRED).build());
    }


    /**
     * description: 获取第二天是否到期(这里只查询两个字段，方便后期加索引不需要重写方法)
     *
     * @param inTime 时间
     * @return List
     * @author bo
     */
    public List<Map<String, Object>> getNextDayDeadline(String inTime) {
        return dao.getNextDayDeadline(inTime);
    }

    /**
     * 获取已过期但是未更新状态的流量包
     */
    public List<Long> getOveredNotUpdatePackages() {
        List<PurchasedFlow> overedPackage = dao.getOveredPackage();
        return overedPackage.stream().map(PurchasedFlow::getId).collect(Collectors.toList());
    }

    /**
     * description: 根据redis的监听更新用户购买的流量包的状态
     *
     * @author bo
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchasedFlowStatusByRedis(Long purchasedFlowId) {
        PurchasedFlow purchasedFlow = dao.selectById(purchasedFlowId);
        // 这里理论上不需要进行判空的，防止业务问题
        if (!Assert.isEmpty(purchasedFlow)) {
            purchasedFlow.setStatus(PurchasedFlowConstants.ON_EXPIRED);
            log.info("更新流量包过期状态，用户过期流量包ID：[{}]", purchasedFlow.getId());
            dao.updateById(purchasedFlow);
            Long userId = purchasedFlow.getUserId();
            if (sysUserBannedService.queryByUserId(userId) != null) {
                log.info("用户[{}]已注销/封禁，不再发送通知", userId);
                return;
            }
            String flowPackageName = purchasedFlow.getFlowPackageName();
            // 这里不能再抛出异常了，否则会引起事务回滚
            try {
                // 异步发送短信或邮箱通知
                smsAsync.notifyPacketExpiration(userId, flowPackageName);
            } catch (Exception e) {
                log.error("流量包已过期短信或邮箱通知失败：用户id：{},流量包名称：{},异常信息：{}", userId, flowPackageName, e.getMessage());
            }
        }
    }

    /**
     * description: 检测是否可以进行退款
     *
     * @author bo
     */
    public Long checkRefund(Long id) {
        String timeString = KuocaiBaseUtil.accessTimeString(-1);
        return dao.checkRefund(timeString, id);
    }

    /**
     * 赠送月度免费流量包
     *
     * @param sysUsers 用户列表
     */
    public void giftMonthPackage(List<SysUser> sysUsers) {
        /*
        // 获取当前月份
        int month = new Date().getMonth() + 1;
        Long size = SystemConfig.websiteBaseConfig.getMonthGiftGb() * 1073741824L;
        // 获取当前日期和时间
        Calendar calendar = Calendar.getInstance();
        // 设置为下个月
        calendar.add(Calendar.MONTH, 1);
        // 将日期设置为1号
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        // 将时间设置为0点0分0秒
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        // 获取Date对象
        Date nextMonthFirstDay = calendar.getTime();
        for (SysUser sysUser : sysUsers) {
            PurchasedFlow record = PurchasedFlow.builder().userId(sysUser.getId()).flowPackageName(month + "月赠送流量包").flowPackageSize(size).deadline(nextMonthFirstDay).status(PurchasedFlowConstants.ON_USED).usedFlow(0L).build();
            PurchasedFlow purchasedFlow = save(record);
            log.info("成功赠送用户[{}][{}赠送流量包]，赠送记录：[{}]", sysUser.getId(), nextMonthFirstDay, purchasedFlow.getId());
        }
        */
    }

    @Transactional
    public PurchasedFlow claimMonthGiftPackage(Long userId) {
        Integer monthGiftGb = SystemConfig.websiteBaseConfig == null ? null : SystemConfig.websiteBaseConfig.getMonthGiftGb();
        if (Assert.isEmpty(userId) || Assert.isEmpty(monthGiftGb) || monthGiftGb <= 0) {
            return null;
        }

        Calendar monthStart = Calendar.getInstance();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);
        monthStart.set(Calendar.HOUR_OF_DAY, 0);
        monthStart.set(Calendar.MINUTE, 0);
        monthStart.set(Calendar.SECOND, 0);
        monthStart.set(Calendar.MILLISECOND, 0);

        Calendar nextMonthStart = (Calendar) monthStart.clone();
        nextMonthStart.add(Calendar.MONTH, 1);

        QueryWrapper<PurchasedFlow> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .like("flow_package_name", "每月福利")
                .ge("create_time", monthStart.getTime())
                .lt("create_time", nextMonthStart.getTime());
        if (dao.selectCount(wrapper) > 0) {
            return null;
        }

        long size = monthGiftGb * 1073741824L;
        String packageName = (monthStart.get(Calendar.MONTH) + 1) + "月每月福利流量包";
        PurchasedFlow record = PurchasedFlow.builder()
                .userId(userId)
                .flowPackageName(packageName)
                .flowPackageSize(size)
                .deadline(nextMonthStart.getTime())
                .status(PurchasedFlowConstants.ON_USED)
                .usedFlow(0L)
                .build();
        PurchasedFlow purchasedFlow = save(record);
        log.info("成功赠送用户[{}]每月福利流量包，赠送记录：[{}]", userId, purchasedFlow.getId());
        return purchasedFlow;
    }
}
