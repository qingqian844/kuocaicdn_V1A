package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.constant.FlowDonateStatus;
import com.kuocai.cdn.constant.PurchasedFlowConstants;
import com.kuocai.cdn.dao.FlowDonateDao;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.dto.resp.RespResult;
import com.kuocai.cdn.entity.FlowDonate;
import com.kuocai.cdn.entity.PurchasedFlow;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.service.base.VoData;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.KuocaiBaseUtil;
import com.kuocai.cdn.vo.FlowDonateVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * (FlowDonate)服务
 *
 * @author todoitbo
 * @since 2023-05-11 19:43:31
 */
@Slf4j
@Service
public class FlowDonateService extends BaseService<FlowDonate> implements VoData<FlowDonate, FlowDonateVo> {

    @Resource
    protected FlowDonateDao dao;


    @Resource
    private SysUserService sysUserService;

    @Resource
    private PurchasedFlowService purchasedFlowService;

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
        jsonObject.put("data", convert2Vo(jsonArray.toJavaList(FlowDonate.class)));
        return jsonObject;
    }

    /**
     * 查询方法，补齐信息
     * 通过vo方式返回前端
     */
    @Override
    public List<FlowDonateVo> convert2Vo(List<FlowDonate> flowDonates) {
        if (Assert.isEmpty(flowDonates)) {
            return new ArrayList<>();
        }
        List<Long> userIds = flowDonates.stream().map(FlowDonate::getUserId).distinct().collect(Collectors.toList());
        List<SysUser> sysUsers = sysUserService.queryByIds(userIds);
        Map<Long, SysUser> sysUserMap = sysUsers.stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        ArrayList<FlowDonateVo> flowDonateArrayList = new ArrayList<>();
        for (FlowDonate flowDonate : flowDonates) {
            String jsonString = JSONObject.toJSONString(flowDonate);
            FlowDonateVo flowDonateVo = JSONObject.parseObject(jsonString, FlowDonateVo.class);
            SysUser sysUser = sysUserMap.get(flowDonate.getUserId());
            if (Assert.isEmpty(sysUser)) {
                continue;
            }
            flowDonateVo.setImg(sysUser.getImg());
            flowDonateVo.setUserName(sysUser.getUserName());
            flowDonateVo.setFlowPackageSizeString(KuocaiBaseUtil.autoReducedFlowUnit(flowDonateVo.getFlowPackageSize()));
            flowDonateArrayList.add(flowDonateVo);
        }
        return flowDonateArrayList;
    }

    /**
     * 撤回赠送流量包
     *
     * @param id flowDonateId
     * @return 赠送流量实体
     */
    @Transactional(rollbackFor = Exception.class)
    public FlowDonate toWithdraw(Long id) {
        FlowDonate flowDonate = queryById(id);
        if (!Assert.isEmpty(flowDonate)) {
            Long purchasedFlowId = flowDonate.getPurchasedFlowId();
            if (Assert.notEmpty(purchasedFlowId)) {
                purchasedFlowService.deleteById(purchasedFlowId);
            }
            flowDonate.setStatus(FlowDonateStatus.WITHDRAW);
            flowDonate = save(flowDonate);
        }
        return flowDonate;
    }

    /**
     * 赠送流量包
     *
     * @param flowDonate flowDonate
     * @return 赠送流量实体
     */
    @Transactional(rollbackFor = Exception.class)
    public FlowDonate saveFlowDonate(FlowDonate flowDonate) {
        PurchasedFlow record = PurchasedFlow.builder().userId(flowDonate.getUserId()).flowPackageName(flowDonate.getFlowPackageName()).flowPackageSize(flowDonate.getFlowPackageSize().longValue()).deadline(flowDonate.getDeadline()).status(PurchasedFlowConstants.ON_USED).usedFlow(0L).build();
        PurchasedFlow purchasedFlow = purchasedFlowService.save(record);
        flowDonate.setPurchasedFlowId(purchasedFlow.getId());
        return save(flowDonate);
    }

    /**
     * 流量赠送
     */
    @Async
    public void donateFlow(String donateType, List<Long> targetIds, String flowPackageName, Double flowPackageSize, Date deadline) {
        List<Long> targetUserIds = new ArrayList<>();
        switch (donateType) {
            case "user":
                targetUserIds.addAll(targetIds);
                break;
            case "agent":
                for (Long agentId : targetIds) {
                    targetUserIds.addAll(sysUserService.queryUserByAgentId(agentId).stream().map(SysUser::getId).collect(Collectors.toList()));
                }
                break;
        }
        for (Long userId : targetUserIds) {
            FlowDonate flowDonate = FlowDonate.builder().userId(userId).flowPackageName(flowPackageName).flowPackageSize(flowPackageSize).deadline(deadline).status(FlowDonateStatus.SUCCESS).build();
            saveFlowDonate(flowDonate);
        }
    }

    public void sendFlowGift(String giftName, Long userId, Integer gb, Integer days) {
        // 获取当前日期
        LocalDate now = LocalDate.now();
        // 添加8天
        LocalDate plus8Days = now.plusDays(days);
        // 将LocalDate转换为Date
        Date deadline = Date.from(plus8Days.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Long size = gb * 1073741824L;
        PurchasedFlow record = PurchasedFlow.builder().userId(userId).flowPackageName(giftName).flowPackageSize(size).deadline(deadline).status(PurchasedFlowConstants.ON_USED).usedFlow(0L).build();
        PurchasedFlow purchasedFlow = purchasedFlowService.save(record);
        log.info("成功赠送流量：{}", purchasedFlow);
    }
}

