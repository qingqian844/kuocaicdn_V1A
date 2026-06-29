package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.dao.FlowPackageDao;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.entity.FlowPackage;
import com.kuocai.cdn.entity.TransactionOrder;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.service.base.VoData;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.KuocaiBaseUtil;
import com.kuocai.cdn.vo.FlowPackageVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


/**
 * (FlowPackage)服务
 *
 * @author makejava
 * @since 2023-03-17 15:36:38
 */
@Service
public class FlowPackageService extends BaseService<FlowPackage> implements VoData<FlowPackage, FlowPackageVo> {

    @Autowired
    protected FlowPackageDao dao;

    @Lazy
    @Autowired
    protected TransactionOrderService orderService;

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
        List<FlowPackage> flowPackages = jsonArray.toJavaList(FlowPackage.class);
        List<FlowPackageVo> flowPackageVos = convert2Vo(flowPackages);
        jsonObject.put("data", flowPackageVos);
        return jsonObject;
    }

    /**
     * 转为Vo对象
     *
     * @param flowPackages 流量包列表
     * @return VO列表
     */
    @Override
    public List<FlowPackageVo> convert2Vo(List<FlowPackage> flowPackages) {
        if (Assert.isEmpty(flowPackages)) {
            return new ArrayList<>();
        }
        ArrayList<FlowPackageVo> flowPackageVos = new ArrayList<>();
        for (FlowPackage flowPackage : flowPackages) {
            String jsonString = JSONObject.toJSONString(flowPackage);
            FlowPackageVo flowPackageVo = JSONObject.parseObject(jsonString, FlowPackageVo.class);
            flowPackageVo.setFlowPackageSizeString(KuocaiBaseUtil.autoReducedFlowUnit(flowPackage.getSize()));
            flowPackageVos.add(flowPackageVo);
        }
        return flowPackageVos;
    }

    /**
     * 查询热销流量包
     *
     * @param limit 条数
     */
    public List<FlowPackage> queryHotPackages(Integer limit) {
        QueryWrapper<FlowPackage> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "enable");
        wrapper.orderByDesc("buy_count");
        wrapper.last("limit " + limit);
        return queryByWrapper(wrapper);
    }

    /**
     * 获取新用户流量包
     *
     * @param limit 条数
     */
    public List<FlowPackage> queryNewUserPackages(Integer limit) {
        QueryWrapper<FlowPackage> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "enable");
        wrapper.eq("package_type", "activity");
        wrapper.eq("buyer_rule", "new_user");
        wrapper.orderByDesc("sort");
        wrapper.last("limit " + limit);
        return queryByWrapper(wrapper);
    }

    /**
     * 检测用户是否可购买此流量包
     *
     * @param userId      用户ID
     * @param flowPackage 流量包
     */
    public boolean checkBuy(Long userId, FlowPackage flowPackage) {
        String buyerRule = flowPackage.getBuyerRule();
        Integer dayLimit = flowPackage.getDayLimit();
        Integer userLimit = flowPackage.getUserLimit();
        flowPackage.setPurchaseAuthority(true);
        if ("activity".equals(flowPackage.getPackageType())) {
            if (Assert.notEmpty(dayLimit) && dayLimit > 0) {
                String todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                // 每日限额
                QueryWrapper<TransactionOrder> queryWrapper = new QueryWrapper<TransactionOrder>()
                        .eq("product_id", flowPackage.getId())
                        .between("create_time", todayDate + " 00:00:00", todayDate + " 23:59:59")
                        .in("order_type", "flow_package", "flow_deduction");
                List<TransactionOrder> transactionOrders = orderService.queryByWrapper(queryWrapper);
                flowPackage.setPurchaseAuthority(transactionOrders.size() < dayLimit);
                flowPackage.setDayLastCount(Math.max(dayLimit - transactionOrders.size(), 0));
                flowPackage.setTitle("已达今日售出额度");
            }
            // 命中了直接返回
            if (!flowPackage.getPurchaseAuthority()) {
                return false;
            }
            if (Assert.notEmpty(userLimit) && userLimit > 0) {
                // 用户限额
                QueryWrapper<TransactionOrder> queryWrapper = new QueryWrapper<TransactionOrder>()
                        .eq("user_id", userId)
                        .eq("product_id", flowPackage.getId());
                List<TransactionOrder> transactionOrders = orderService.queryByWrapper(queryWrapper);
                flowPackage.setPurchaseAuthority(transactionOrders.size() < userLimit);
                flowPackage.setUserLastCount(Math.max(userLimit - transactionOrders.size(), 0));
                flowPackage.setTitle("已达用户购买额度");
            }
            // 命中了直接返回
            if (!flowPackage.getPurchaseAuthority()) {
                return false;
            }
            if ("new_user".equals(buyerRule)) {
                // 新用户
                QueryWrapper<TransactionOrder> queryWrapper = new QueryWrapper<TransactionOrder>()
                        .eq("user_id", userId)
                        .in("order_type", "flow_package", "flow_deduction")
                        .eq("status", "TRADE_SUCCESS");
                List<TransactionOrder> transactionOrders = orderService.queryByWrapper(queryWrapper);
                flowPackage.setPurchaseAuthority(transactionOrders.size() == 0);
                flowPackage.setTitle("仅新用户可购买");
            }
        }
        return flowPackage.getPurchaseAuthority();
    }
}
