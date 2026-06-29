package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kuocai.cdn.constant.PurchasedFlowConstants;
import com.kuocai.cdn.dao.GiftDao;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.entity.Gift;
import com.kuocai.cdn.entity.PurchasedFlow;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.service.base.VoData;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.KuocaiBaseUtil;
import com.kuocai.cdn.vo.GiftVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;


/**
 * (Gift)服务
 *
 * @author makejava
 * @since 2023-05-15 14:43:53
 */
@Service
public class GiftService extends BaseService<Gift> implements VoData<Gift, GiftVo> {

    @Autowired
    protected GiftDao dao;

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
    public JSONObject queryForDatatables(DataTableQuery query) {
        JSONObject jsonObject = super.queryForDatatables(query);
        JSONArray jsonArray = jsonObject.getJSONArray("data");
        List<Gift> gifts = jsonArray.toJavaList(Gift.class);
        List<GiftVo> giftVos = convert2Vo(gifts);
        jsonObject.put("data", giftVos);
        return jsonObject;
    }

    /**
     * 转为Vo对象
     *
     * @param gifts 流量包列表
     * @return VO列表
     */
    @Override
    public List<GiftVo> convert2Vo(List<Gift> gifts) {
        if (Assert.isEmpty(gifts)) {
            return new ArrayList<>();
        }
        ArrayList<GiftVo> giftVos = new ArrayList<>();
        for (Gift gift : gifts) {
            String jsonString = JSONObject.toJSONString(gift);
            GiftVo giftVo = JSONObject.parseObject(jsonString, GiftVo.class);
            giftVo.setFlowPackageSizeString(KuocaiBaseUtil.autoReducedFlowUnit(gift.getFlowPackageSize()));
            // 领取的记录专程userIds集合
            String purchasedRecord = gift.getPurchasedRecord();
            List<SysUser> sysUsers = new ArrayList<>();
            if (Assert.notEmpty(purchasedRecord)) {
                List<Long> ids = JSON.parseObject(purchasedRecord).getJSONArray("ids").toJavaList(Long.class);
                sysUsers = sysUserService.queryByIds(ids);
            }
            giftVo.setUsers(sysUsers);
            // 前段json解析不了
            giftVo.setPurchasedRecord(null);
            giftVos.add(giftVo);
        }
        return giftVos;
    }

    /**
     * 根据兑换码查询礼品
     */
    public Gift queryInfoByCode(String code) {
        Gift gift = Gift.builder().code(code).build();
        List<Gift> gifts = queryByObj(gift);
        if (Assert.isEmpty(gifts)) {
            return null;
        } else {
            return gifts.get(0);
        }
    }

    /**
     * 礼品兑换
     *
     * @param gift 礼品信息
     */
    @Transactional(rollbackFor = Exception.class)
    public Gift giftExchange(Gift gift, Long userId, List<Long> ids) {
        // 给用户添加一个已购流量包
        PurchasedFlow record = PurchasedFlow.builder().userId(userId).flowPackageName(gift.getFlowPackageName()).flowPackageSize(gift.getFlowPackageSize().longValue()).deadline(gift.getDeadline()).status(PurchasedFlowConstants.ON_USED).usedFlow(0L).build();
        PurchasedFlow purchasedFlow = purchasedFlowService.save(record);
        // 修改Gift信息
        ids.add(userId);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("ids", ids);
        String purchasedRecordFinal = jsonObject.toJSONString();
        gift.setPurchasedRecord(purchasedRecordFinal);
        return save(gift);

    }

}
