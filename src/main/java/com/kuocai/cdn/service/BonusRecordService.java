package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.constant.BonusRecordStatus;
import com.kuocai.cdn.dao.BonusRecordDao;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.entity.BonusRecord;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.entity.SysUserAccount;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.service.base.VoData;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.KuocaiBaseUtil;
import com.kuocai.cdn.vo.BonusRecordVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * (BonusRecord)服务
 *
 * @author todoitbo
 * @since 2023-06-14 20:03:23
 */
@Service
public class BonusRecordService extends BaseService<BonusRecord> implements VoData<BonusRecord, BonusRecordVo> {

    @Autowired
    protected BonusRecordDao dao;

    @Resource
    private SysUserService sysUserService;

    @Resource
    private SysUserAccountService sysUserAccountService;

    /**
     * 重写Datatable查询接口
     *
     * @param query 查询参数
     * @return 响应
     */
    @Override
    public JSONObject queryForDatatables(DataTableQuery query) {
        JSONObject jsonObject = super.queryForDatatables(query);
        JSONArray jsonArray = jsonObject.getJSONArray("data");
        jsonObject.put("data", convert2Vo(jsonArray.toJavaList(BonusRecord.class)));
        return jsonObject;
    }

    /**
     * 查询方法，补齐信息
     * 通过vo方式返回前端
     */
    @Override
    public List<BonusRecordVo> convert2Vo(List<BonusRecord> ronusRecords) {
        if (Assert.isEmpty(ronusRecords)) {
            return new ArrayList<>();
        }
        List<Long> userIds = ronusRecords.stream().map(BonusRecord::getUserId).distinct().collect(Collectors.toList());
        List<Long> userIdAgents = ronusRecords.stream().map(BonusRecord::getAgentUserId).distinct().collect(Collectors.toList());
        userIds.addAll(userIdAgents);
        List<SysUser> sysUsers = sysUserService.queryByIds(userIds);
        Map<Long, SysUser> sysUserMap = sysUsers.stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        ArrayList<BonusRecordVo> bonusRecordArrayList = new ArrayList<>();
        for (BonusRecord bonusRecord : ronusRecords) {
            String jsonString = JSONObject.toJSONString(bonusRecord);
            BonusRecordVo bonusRecordVo = JSONObject.parseObject(jsonString, BonusRecordVo.class);
            SysUser sysUser = sysUserMap.get(bonusRecordVo.getUserId());
            SysUser agentSysUser = sysUserMap.get(bonusRecordVo.getAgentUserId());
            bonusRecordVo.setImg(sysUser.getImg());
            bonusRecordVo.setUserName(sysUser.getUserName());
            bonusRecordVo.setAgentUserName(agentSysUser.getUserName());
            bonusRecordVo.setAgentImg(agentSysUser.getImg());
            bonusRecordArrayList.add(bonusRecordVo);
        }
        return bonusRecordArrayList;
    }

    /**
     * 根据订单id获取分润明细
     */
    public BonusRecord queryByTransactionOrderId(Long transactionOrderId) {
        return dao.selectOne(new QueryWrapper<BonusRecord>().eq("transaction_order_id", transactionOrderId));
    }

    /**
     * 获取确认的分润
     */
    public List<BonusRecord> getConfirmBonusRecords() {
        return dao.getConfirmBonusRecords(KuocaiBaseUtil.accessTimeString(-1));
    }

    /**
     * 修改分润明细状态并给代理人增加分润金额
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmBonu(BonusRecord bonusRecord) {
        bonusRecord.setStatus(BonusRecordStatus.CONFIRM);
        save(bonusRecord);
        // 增加分润金额
        SysUserAccount sysUserAccount = sysUserAccountService.queryByUserId(bonusRecord.getAgentUserId());
        sysUserAccount.addBonus(bonusRecord.getBonus());
        sysUserAccountService.save(sysUserAccount);
    }

    /**
     * 管理员取消分润明细
     */
    public void cancelBonu(BonusRecord bonusRecord) {
        bonusRecord.setStatus(BonusRecordStatus.CANCEL);
        save(bonusRecord);
    }
}
