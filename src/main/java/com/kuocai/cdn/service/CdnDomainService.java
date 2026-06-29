package com.kuocai.cdn.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kuocai.cdn.api.huawei.cdn.DomainConfigureApi;
import com.kuocai.cdn.api.huawei.cdn.DomainOperationApi;
import com.kuocai.cdn.api.huawei.cdn.constant.DomainStatus;
import com.kuocai.cdn.api.huawei.cdn.constant.SourcePriority;
import com.kuocai.cdn.api.huawei.cdn.dto.DomainBodyDTO;
import com.kuocai.cdn.api.huawei.cdn.dto.DomainSourceDTO;
import com.kuocai.cdn.constant.CdnBusinessTypeMap;
import com.kuocai.cdn.constant.CdnServiceAreaMap;
import com.kuocai.cdn.constant.DomainStatusMap;
import com.kuocai.cdn.dao.CdnDomainDao;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.entity.CdnDomain;
import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.entity.SysUserAccount;
import com.kuocai.cdn.entity.TransactionOrder;
import com.kuocai.cdn.enumeration.domainmerage.CdnRoute;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.exception.CdnHuaweiException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.service.base.VoData;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.vo.CdnDomainVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 加速域名(CdnDomain)服务
 *
 * @author XUEW
 * @since 2023-02-26 23:30:24
 */
@Slf4j
@Service
public class CdnDomainService extends BaseService<CdnDomain> implements VoData<CdnDomain, CdnDomainVo> {

    @Autowired
    private CdnDomainDao dao;

    @Autowired
    private SysUserService sysUserService;

    @Resource
    private SysUserAccountService userAccountService;

    @Resource
    private TransactionOrderService transactionOrderService;

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
        jsonObject.put("data", convert2Vo(jsonArray.toJavaList(CdnDomain.class)));
        return jsonObject;
    }

    /**
     * 获取域名的详细配置
     *
     * @param domainName 域名
     * @return 域名配置
     */
    public JSONObject getDomainConfig(String domainName) throws BusinessException {
        JSONObject jsonObject = null;
        try {
            jsonObject = DomainConfigureApi.getDomainConfigs(domainName);
        } catch (CdnHuaweiException e) {
            log.error("获取域名的详细配置失败，域名：{}", domainName);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        JSONObject configs = jsonObject.getJSONObject("configs");
        // 解析主源站
        JSONArray sourcesArray = configs.getJSONArray("sources");
        for (int i = 0; i < sourcesArray.size(); i++) {
            JSONObject source = sourcesArray.getJSONObject(i);
            if (source.getInteger("priority") == SourcePriority.MAIN) {
                configs.put("main_source", source);
            }
            if (source.getInteger("priority") == SourcePriority.BACK) {
                configs.put("back_source", source);
            }
        }
        return configs;
    }

    /**
     * 根据域名ID查询域名信息
     *
     * @param domainId 域名ID
     * @return Optional<CdnDomain>
     */
    public Optional<CdnDomain> queryByDomainId(String domainId) {
        List<CdnDomain> cdnDomains = queryByObj(CdnDomain.builder().domainId(domainId).build());
        return cdnDomains.stream().findFirst();
    }

    /**
     * 根据用户ID查询域名信息
     *
     * @param userId 用户ID
     * @return List<CdnDomain>
     */
    public List<CdnDomain> queryByUserId(Long userId) {
        return queryByObj(CdnDomain.builder().userId(userId).build());
    }

    /**
     * 根据域名查询
     */
    public List<CdnDomain> queryByDomainNames(List<String> domains) {
        QueryWrapper<CdnDomain> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("domain_name", domains);
        return queryByWrapper(queryWrapper);
    }

    public List<CdnDomainVo> queryVoByUserIds(List<Long> userIds) {
        List<CdnDomainVo> result = new ArrayList<>();
        if (Assert.isEmpty(userIds)) {
            return result;
        }
        QueryWrapper<CdnDomain> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("user_id", userIds);
        return convert2Vo(queryByWrapper(queryWrapper));
    }

    /**
     * 根据域名查询
     */
    public CdnDomain queryByDomainName(String domain) {
        List<CdnDomain> cdnDomains = queryByObj(CdnDomain.builder().domainName(domain).build());
        if (Assert.notEmpty(cdnDomains)) {
            return cdnDomains.get(0);
        }
        return null;
    }

    /**
     * 根据域名查询
     */
    public List<CdnDomain> queryByDomainNames(String domains) {
        List<CdnDomain> result = new ArrayList<>();
        Arrays.stream(domains.split(",")).forEach(domain -> {
            CdnDomain cdnDomain = queryByDomainName(domain);
            if (Assert.notEmpty(cdnDomain)) {
                result.add(cdnDomain);
            }
        });
        return result;
    }

    /**
     * 查询所有的域名视图实体
     *
     * @return 域名VO列表
     */
    public CdnDomainVo getCdnDomainVoById(Long id) {
        CdnDomain domain = queryById(id);
        return convert2Vo(domain);
    }

    /**
     * 查询所有的域名视图实体
     *
     * @return 域名VO列表
     */
    public List<CdnDomainVo> getAllCdnDomainVo() {
        List<CdnDomain> domains = queryAll();
        return convert2Vo(domains);
    }

    /**
     * 查询所有的域名视图实体
     *
     * @return 域名VO列表
     */
    public List<CdnDomainVo> getMyCdnDomainVo(Long userId) {
        CdnDomain query = CdnDomain.builder().userId(userId).build();
        List<CdnDomain> domains = queryByObj(query);
        return convert2Vo(domains);
    }

    /**
     * 创建加速域名
     *
     * @param domainName   加速域名
     * @param businessType 业务类型
     * @param serviceArea  服务范围
     * @param originType   源站类别
     * @param ipOrDomain   源站
     * @return 域名信息
     */
    @Transactional(rollbackFor = {Exception.class})
    public CdnDomain create(Long userId, String domainName, String businessType, String serviceArea, String originType, String ipOrDomain) throws BusinessException {
        // 封装源站信息
        DomainSourceDTO sourceDTO = DomainSourceDTO.builder()
                .ip_or_domain(ipOrDomain)
                .origin_type(originType)
                .active_standby(1)
                .build();
        List<DomainSourceDTO> sourceDTOS = new ArrayList<>();
        sourceDTOS.add(sourceDTO);
        // 封装域名信息
        DomainBodyDTO bodyDTO = DomainBodyDTO.builder()
                .domain_name(domainName)
                .business_type(businessType)
                .service_area(serviceArea)
                .sources(sourceDTOS)
                .build();
        JSONObject jsonObject = null;
        try {
            jsonObject = DomainOperationApi.createDomain(bodyDTO);
        } catch (CdnHuaweiException e) {
            log.error("创建加速域名失败，用户：{}，域名：{}，业务类型：{}，服务范围：{}，源站类别：{}，源站：{}",
                    userId, domainName, businessType, serviceArea, originType, ipOrDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        JSONObject domain = jsonObject.getJSONObject("domain");
        // 保存域名信息
        CdnDomain cdnDomain = convertDomain(domain);
        cdnDomain.setUserId(userId);
        return save(cdnDomain);
    }

    /**
     * 启用加速域名
     *
     * @param cdnDomain 加速域名
     * @return 域名信息
     */
    @Transactional(rollbackFor = {Exception.class})
    public CdnDomain enable(CdnDomain cdnDomain) throws BusinessException {
        JSONObject jsonObject = null;
        try {
            jsonObject = DomainOperationApi.enableDomain(cdnDomain.getDomainId());
        } catch (CdnHuaweiException e) {
            log.error("启用加速域名失败，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        JSONObject domain = jsonObject.getJSONObject("domain");
        // 保存域名信息
        CdnDomain enableDomain = convertDomain(domain);
        enableDomain.setId(cdnDomain.getId());
        return save(enableDomain);
    }

    /**
     * 停用加速域名
     *
     * @param cdnDomain 加速域名
     * @return 域名信息
     */
    @Transactional(rollbackFor = {Exception.class})
    public CdnDomain disable(CdnDomain cdnDomain) throws BusinessException {
        JSONObject jsonObject = null;
        try {
            jsonObject = DomainOperationApi.disableDomain(cdnDomain.getDomainId());
        } catch (CdnHuaweiException e) {
            log.error("停用加速域名失败，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        JSONObject domain = jsonObject.getJSONObject("domain");
        // 保存域名信息
        CdnDomain enableDomain = convertDomain(domain);
        enableDomain.setId(cdnDomain.getId());
        return save(enableDomain);
    }

    /**
     * 删除加速域名
     *
     * @param cdnDomain 加速域名
     * @return 域名信息
     */
    @Transactional(rollbackFor = {Exception.class})
    public void delete(CdnDomain cdnDomain) throws BusinessException {
        try {
            DomainOperationApi.deleteDomain(cdnDomain.getDomainId());
        } catch (CdnHuaweiException e) {
            log.error("删除加速域名失败，域名信息：{}", cdnDomain);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        // 删除域名信息
        deleteById(cdnDomain.getId());
        log.info("删除加速域名成功，域名信息：{}", cdnDomain);
        // TODO 根据domainId删除源站信息
        // TODO 根据domainId删除回源信息
        // TODO 根据domainId删除统计信息
    }

    /**
     * 域名结果转换
     *
     * @param domain 响应的域名信息
     * @return 域名实体
     */
    public CdnDomain convertDomain(JSONObject domain) {
        return CdnDomain.builder()
                .domainId(domain.getString("id"))
                .domainName(domain.getString("domain_name"))
                .businessType(domain.getString("business_type"))
                .serviceArea(domain.getString("service_area"))
                .domainStatus(domain.getString("domain_status"))
                .cnameHuawei(domain.getString("cname"))
                .build();
    }


    /**
     * 根据域名查询防盗链信息
     */
    public JSONObject getHotlinkPrevention(String domainId) throws BusinessException {
        JSONObject jsonObject = null;
        try {
            jsonObject = DomainConfigureApi.getReferer(domainId);
        } catch (CdnHuaweiException e) {
            log.error("查询域名防盗链信息失败，域名ID：{}", domainId);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        JSONObject referer = jsonObject.getJSONObject("referer");
        return referer;
    }

    /**
     * 根据域名查询IP黑白名单信息
     */
    public JSONObject getIpBlackWhiteList(String domainId) throws BusinessException {
        JSONObject jsonObject = null;
        try {
            jsonObject = DomainConfigureApi.getIpAcl(domainId);
        } catch (CdnHuaweiException e) {
            log.error("查询域名IP黑白名单信息失败，域名ID：{}", domainId);
            throw new BusinessException(e.getMessage()).setCause(e).log();
        }
        JSONArray ipList = jsonObject.getJSONArray("ip_list");
        String ipString = StrUtil.join(";", ipList);
        jsonObject.put("ip_list", ipString);
        return jsonObject;
    }

    /**
     * 转为Vo对象
     *
     * @param domain 域名对象
     * @return VO
     */
    public CdnDomainVo convert2Vo(CdnDomain domain) {
        SysUser sysUser = sysUserService.queryById(domain.getUserId());
        String jsonString = JSONObject.toJSONString(domain);
        CdnDomainVo cdnDomainVo = JSONObject.parseObject(jsonString, CdnDomainVo.class);
        cdnDomainVo.setBusinessTypeName(CdnBusinessTypeMap.huawei.get(domain.getBusinessType()));
        cdnDomainVo.setServiceAreaName(CdnServiceAreaMap.huawei.get(domain.getServiceArea()));
        cdnDomainVo.setUserImg(sysUser.getImg());
        cdnDomainVo.setUserName(sysUser.getUserName());
        return cdnDomainVo;
    }

    /**
     * 转为Vo对象
     *
     * @param domains 域名对象列表
     * @return VO列表
     */
    @Override
    public List<CdnDomainVo> convert2Vo(List<CdnDomain> domains) {
        if (Assert.isEmpty(domains)) {
            return new ArrayList<>();
        }
        // 解析所有用户ID
        List<Long> userIds = domains.stream().map(CdnDomain::getUserId).distinct().collect(Collectors.toList());
        List<SysUser> sysUsers = sysUserService.queryByIds(userIds);
        Map<Long, SysUser> sysUserMap = sysUsers.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        domains = domains.stream().sorted(Comparator.comparing(CdnDomain::getCreateTime).reversed()).collect(Collectors.toList());
        ArrayList<CdnDomainVo> cdnDomainVos = new ArrayList<>();
        for (CdnDomain domain : domains) {
            String jsonString = JSONObject.toJSONString(domain);
            CdnDomainVo cdnDomainVo = JSONObject.parseObject(jsonString, CdnDomainVo.class);
            cdnDomainVo.setBusinessTypeName(CdnBusinessTypeMap.huawei.get(domain.getBusinessType()));
            cdnDomainVo.setServiceAreaName(CdnServiceAreaMap.huawei.get(domain.getServiceArea()));
            // 过滤出用户信息
            SysUser sysUser = sysUserMap.get(domain.getUserId());
            cdnDomainVo.setUserName(sysUser.getUserName());
            cdnDomainVo.setUserImg(sysUser.getImg());
            cdnDomainVo.setDomainStatusName(DomainStatusMap.huawei.get(domain.getDomainStatus()));
            cdnDomainVos.add(cdnDomainVo);
        }
        return cdnDomainVos;
    }

    /**
     * 判断当前用户是否需要停止域名
     * 余额不足零, 有没有支付的订单
     *
     * @param userId 用户id
     * @return boolean
     */
    public boolean isCanStopDomain(Long userId) {
        SysUserAccount sysUserAccount = userAccountService.queryByUserId(userId);
        if (Assert.isEmpty(sysUserAccount)) {
            return true;
        }
        List<TransactionOrder> transactionOrders = transactionOrderService.queryFlowDeductionOrderType(userId);
        if (sysUserAccount.getAccountBalance().compareTo(BigDecimal.ZERO) < 0 || Assert.notEmpty(transactionOrders)) {
            return true;
        }
        return false;
    }

    /**
     * 获取用户域名数量
     *
     * @param userId 用户ID
     * @return 数量
     */
    public int queryUserDomainCount(Long userId) {
        return queryByUserId(userId).size();
    }

    /**
     * 查询所有融合线路的域名
     */
    public List<CdnDomain> queryMergeDomains() {
        CdnDomain cdnDomain = CdnDomain.builder()
                .route(CdnRoute.HUAWEI_VOLCENGINE.getCode())
                .build();
        return queryByObj(cdnDomain);
    }

    /**
     * 更新为配置中状态
     */
    public void updateConfiguring(Long domainId) {
        CdnDomain cdnDomain = CdnDomain.builder()
                .id(domainId)
                .domainStatus(DomainStatus.CONFIGURING)
                .build();
        save(cdnDomain);
    }

    /**
     * 保存域名信息
     *
     * @param cdnDomain 保存目标
     */
    @Override
    public CdnDomain save(CdnDomain cdnDomain) {
        if (Assert.isEmpty(cdnDomain.getId())) {
            cdnDomain.setCreateTime(new Date());
        }
        cdnDomain.setUpdateTime(new Date());
        return super.save(cdnDomain);
    }

    /**
     * 删除异常的域名
     *
     * @param message 异常信息
     */
    public void deleteExceptionDomain(String message) {
        log.error("尝试删除域名，错误信息：{}", message);
        if (Assert.isEmpty(message)) {
            return;
        }
        CdnDomain cdnDomain = null;
        if (message.startsWith("您没有权限操作域名:")) {
            String domainName = message.replace("您没有权限操作域名: ", "");
            cdnDomain = queryByDomainName(domainName);
        }
        if (message.startsWith("更新加速域名状态失败，加速域名不存在：")) {
            String domainName = message.replace("更新加速域名状态失败，加速域名不存在：", "");
            cdnDomain = queryByDomainName(domainName);
        }
        if (Assert.notEmpty(cdnDomain)) {
            log.info("已删除不存在的加速域名：{}", cdnDomain.getDomainName());
            deleteById(cdnDomain.getId());
        }
    }

    /**
     * 检查域名是否属于用户
     * @param userId 用户ID
     * @param domainId 域名ID
     * @return 是否属于用户
     */
    public boolean checkUserDomain(Long userId, Long domainId) {
        Long id = dao.selectOneIdByUserIdAndId(userId, domainId);
        return Assert.notEmpty(id);
    }
}
