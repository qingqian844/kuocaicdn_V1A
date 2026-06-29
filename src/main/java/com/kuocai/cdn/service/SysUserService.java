package com.kuocai.cdn.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.symmetric.AES;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kuocai.cdn.async.SmsAsync;
import com.kuocai.cdn.component.OssClient;
import com.kuocai.cdn.config.SystemConfig;
import com.kuocai.cdn.constant.KuoCaiConstants;
import com.kuocai.cdn.constant.UserConstants;
import com.kuocai.cdn.dao.SysUserDao;
import com.kuocai.cdn.dto.UserRecordDTO;
import com.kuocai.cdn.dto.datatable.DataTableColumn;
import com.kuocai.cdn.dto.datatable.DataTableOrder;
import com.kuocai.cdn.dto.datatable.DataTableQuery;
import com.kuocai.cdn.dto.datatable.DataTableSearch;
import com.kuocai.cdn.entity.*;
import com.kuocai.cdn.enumeration.UserStatus;
import com.kuocai.cdn.exception.BusinessException;
import com.kuocai.cdn.service.base.BaseService;
import com.kuocai.cdn.util.*;
import com.kuocai.cdn.vo.SysUserBannedVO;
import com.kuocai.cdn.vo.SysUserVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户服务
 *
 * @author XUEW
 * @date 下午9:03 2023/2/12
 */
@Slf4j
@Service
public class SysUserService extends BaseService<SysUser> {


    @Value("rsa.private")
    private String privateKey;

    @Value("rsa.public")
    private String publicKey;

    @Autowired
    private SysUserDao dao;

    @Autowired
    private OssClient ossClient;

    @Autowired
    private SysRoleService roleService;

    @Autowired
    private LoginDeviceService loginDeviceService;

    @Autowired
    private SysUserAccountService accountService;

    @Autowired
    private PurchasedFlowService purchasedFlowService;

    @Autowired
    private AgentConfigService agentConfigService;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Autowired
    private SmsAsync sendSmsCode;

    @Autowired
    private SysUserBannedService sysUserBannedService;

    /**
     * 发送密码至邮箱/手机
     *
     * @param userAccount 用户账户
     */
    public void sendPassword(String userAccount) throws BusinessException {
        SysUser sysUser = userAccount.contains("@") ? queryByEmail(userAccount) : queryByPhone(userAccount);
        if (Assert.isEmpty(sysUser)) {
            throw new BusinessException("account not found or not bound").log();
        }
        throw new BusinessException("Plaintext password recovery is disabled. Please use a password reset flow.").log();
    }

    public void resetPassword(String userAccount, String code, String newPassword) throws BusinessException {
        SysUser sysUser = userAccount.contains("@") ? queryByEmail(userAccount) : queryByPhone(userAccount);
        if (Assert.isEmpty(sysUser)) {
            throw new BusinessException("账号不存在或尚未绑定").log();
        }
        String key = "password-reset:" + userAccount;
        String cachedCode = JedisUtil.getStr(key);
        if (Assert.isEmpty(cachedCode) || !cachedCode.equals(code)) {
            throw new BusinessException("验证码错误或已过期").log();
        }
        if (newPassword.length() < 8 || newPassword.length() > 16) {
            throw new BusinessException("密码长度必须为8至16位").log();
        }
        sysUser.setUserPwd(PasswordUtils.hash(newPassword));
        sysUser.setPwdSalt(null);
        save(sysUser);
        JedisUtil.delKey(key);
        JedisUtil.delKey("user:" + sysUser.getId());
        log.info("用户通过忘记密码流程重置密码成功，userId[{}]", sysUser.getId());
    }

    public boolean checkUserAccountLogin(SysUser sysUser, String password) throws BusinessException {
        if (Assert.isEmpty(sysUser.getUserPwd())) {
            throw new BusinessException("user password is empty").log();
        }
        if (PasswordUtils.isBcryptHash(sysUser.getUserPwd())) {
            return PasswordUtils.matches(password, sysUser.getUserPwd());
        }
        if (Assert.isEmpty(sysUser.getPwdSalt())) {
            return false;
        }
        AES aes = AesUtils.getAes(sysUser.getPwdSalt());
        String ciphertext = AesUtils.encryptHex(aes, password);
        boolean matched = ObjectUtil.equal(ciphertext, sysUser.getUserPwd());
        if (matched) {
            sysUser.setUserPwd(PasswordUtils.hash(password));
            sysUser.setPwdSalt(null);
            save(sysUser);
            JedisUtil.delKey("user:" + sysUser.getId());
        }
        return matched;
    }

    public String getUserSignToken(SysUser sysUser, Boolean remember, HttpServletRequest request) throws BusinessException {
        Long userId = sysUser.getId();
        // 检查用户是否被封禁
        SysUserBanned sysUserBanned = sysUserBannedService.queryByUserId(userId);
        if (sysUserBanned != null) {
            throw new BusinessException(String.format("您的账号被封禁，原因：%s，若有疑问请联系客服。", sysUserBanned.getBannedReason())).log();
        }
        // END 检查用户是否被封禁
        String token;
        String key = "token:";
        String userIdStr = userId.toString();
        SysRole sysRole = roleService.queryById(sysUser.getRoleId());
        Map<String, String> userMap = new HashMap<>();
        userMap.put("userId", userIdStr);
        userMap.put("route", sysUser.getRoute());
        userMap.put("roleCode", sysRole.getRoleCode());
        // 生成Token信息
        int oneDay = 86400;
        HttpSession session = request.getSession();
        session.setAttribute("loginUser", sysUser);
        session.setAttribute("loginUserRoleCode", sysRole.getRoleCode());
        // 记住我?
        if (remember) {
            // 记住我，默认7天
            token = JwtUtil.getToken(userMap, Calendar.DATE, 7);
            JedisUtil.setStr(key + token, userIdStr, oneDay * 7);
        } else {
            // 不记住，默认为1天
            token = JwtUtil.getToken(userMap);
            JedisUtil.setStr(key + token, userIdStr, oneDay);
        }
        return token;
    }

    /**
     * 用户登录
     *
     * @param sysUserVo 用户信息
     * @return 响应
     */
    public String loginUser(SysUserVo sysUserVo, HttpServletRequest request) throws BusinessException {
        // 通过账号搜索人员信息，在比较密码
        String userAccount = sysUserVo.getUserAccount();
        String userPwd = sysUserVo.getUserPwd();
        Long roleId = sysUserVo.getRoleId();
        List<SysUser> sysUsers = queryByAccount(userAccount, roleId);
        if (Assert.isEmpty(sysUsers)) {
            throw new BusinessException("用户不存在").log();
        }
        SysUser sysUser = sysUsers.get(0);
        if (checkUserAccountLogin(sysUser, userPwd)) {
            Long userId = sysUser.getId();
            String token = getUserSignToken(sysUser, sysUserVo.getRemember(), request);
            try {
                // 保存登录设备记录
                loginDeviceService.saveLoginDevice(userId, request);
            } catch (Exception e) {
                log.error("保存登录设备记录失败！{}", e.getMessage());
            }
            // 更新登录信息
            updateLoginInfo(userId, request);
            log.info("用户登录成功，账户：{}", userAccount);
            return token;
        }
        throw new BusinessException("请检查账号密码是否输入正确").log();
    }

    /**
     * 通过手机号查询用户
     */
    public SysUser queryByPhone(String phone) {
        List<SysUser> sysUsers = queryByObj(SysUser.builder().phone(phone).build());
        if (Assert.isEmpty(sysUsers)) {
            log.warn("不存在的手机号：{}", phone);
            return null;
        }
        return sysUsers.get(0);
    }

    /**
     * 通过邮箱查询用户
     */
    public SysUser queryByEmail(String email) {
        List<SysUser> sysUsers = queryByObj(SysUser.builder().email(email).build());
        if (Assert.isEmpty(sysUsers)) {
            log.warn("不存在的邮箱：{}", email);
            return null;
        }
        return sysUsers.get(0);
    }

    /**
     * 通过邮箱查询用户
     */
    public SysUser queryByUserName(String userName) {
        List<SysUser> sysUsers = queryByObj(SysUser.builder().userName(userName).build());
        if (Assert.isEmpty(sysUsers)) {
            log.warn("不存在的用户名：{}", userName);
            return null;
        }
        return sysUsers.get(0);
    }

    /**
     * 查询所有代理用户
     */
    public List<SysUser> queryAllAgents() {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.isNotNull("agent_level_id");
        return queryByWrapper(wrapper);
    }

    /**
     * 查询所有管理员
     */
    public List<SysUser> queryAllAdmins() {
        return queryByObj(SysUser.builder().roleId(1L).build());
    }

    private void ensureSingleAdmin(Long roleId, Long currentUserId) throws BusinessException {
        if (!Long.valueOf(1L).equals(roleId)) {
            return;
        }
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("role_id", 1L);
        if (Assert.notEmpty(currentUserId)) {
            wrapper.ne("id", currentUserId);
        }
        if (dao.selectCount(wrapper) > 0) {
            throw new BusinessException("开源版仅允许一个管理员账号");
        }
    }

    /**
     * 查询所有推荐人
     */
    public List<SysUser> queryAllReferrer() {
        return dao.queryAllReferrer();
    }

    /**
     * 更新用户头像
     *
     * @param file 文件
     * @param id   用户ID
     * @return 响应
     */
    public Boolean updateImg(MultipartFile file, Long id) throws BusinessException {
        String path = null;
        long size = file.getSize();
        try {
            path = ossClient.upload(file);
        } catch (Exception e) {
            throw new BusinessException("上次头像失败，{}", e.getMessage()).setCause(e).log();
        }
        if (Assert.isEmpty(path)) {
            throw new BusinessException("上次头像失败").log();
        }
        save(SysUser.builder().id(id).img(path).build());
        JedisUtil.delKey("user:" + id);
        return true;
    }

    /**
     * 更新密码
     *
     * @param sysUserVo 用户信息
     * @param id        用户ID
     * @return 响应
     */
    public Boolean updatePwd(SysUserVo sysUserVo, Long id) throws BusinessException {
        SysUser sysUser = queryById(id);
        if (Assert.isEmpty(sysUser)) {
            throw new BusinessException("user not found").log();
        }
        if (!checkUserAccountLogin(sysUser, sysUserVo.getOldPwd())) {
            throw new BusinessException("current password is incorrect");
        }
        sysUser.setUserPwd(PasswordUtils.hash(sysUserVo.getUserPwd()));
        sysUser.setPwdSalt(null);
        save(sysUser);
        log.info("update password success, userId[{}]", id);
        JedisUtil.delKey("user:" + id);
        return true;
    }

    public void updateLoginInfo(Long id, HttpServletRequest request) {
        String ip = "";
        try {
            ip = BrowserUtils.getIp(request);
        } catch (UnknownHostException e) {
            ip = "未知IP";
        }
        SysUser sysUser = SysUser.builder().id(id).lastLoginTime(new Date()).lastLoginIp(ip).build();
        JedisUtil.delKey("user:" + id);
        save(sysUser);
    }

    /**
     * 修改用户流量单价
     *
     * @param id        用户ID
     * @param flowPrice 价格
     */
    public SysUser updateUserFlowPrice(Long id, BigDecimal flowPrice, Integer maxDomainCount, String route, Integer enableOverseas, Integer enableGlobal) {
        SysUser sysUser = SysUser.builder().id(id).flowPrice(flowPrice).maxDomainCount(maxDomainCount).route(route).enableOverseas(enableOverseas).enableGlobal(enableGlobal).build();
        log.info("更新用户价格，目标用户[{}]，最新价格[{}]", id, flowPrice);
        Set<String> keys = JedisUtil.keys("Statistics:" + id + ":*");
        log.info("清除当前用户：{}的缓存统计数据：{}", id, keys);
        JedisUtil.delKeys(keys.toArray(new String[0]));
        JedisUtil.delKey("user:" + id);
        return save(sysUser);
    }


    /**
     * 更新邮箱信息
     *
     * @param userId 用户ID
     * @param email  邮箱
     * @param code   验证码
     * @return 响应
     */
    public Boolean updateEmailVerify(Long userId, String email, String code) throws BusinessException {
        String key = userId + ":" + email;
        String cacheCode = JedisUtil.getStr(key);
        if (Assert.isEmpty(cacheCode)) {
            throw new BusinessException("验证码已失效");
        }
        if (!code.equals(cacheCode)) {
            throw new BusinessException("验证码不正确");
        }
        SysUser sysUser = SysUser.builder().id(userId).email(email).build();
        save(sysUser);
        log.info("更新用户邮箱，目标用户[{}]，最新邮箱[{}]", userId, email);
        JedisUtil.delKey("user:" + userId);
        return true;
    }

    /**
     * 更新用户手机信息
     *
     * @param userId 用户ID
     * @param phone  手机
     * @param code   验证码
     * @return 响应
     */
    public Boolean updatePhoneVerify(Long userId, String phone, String code) throws BusinessException {
        String key = userId + ":" + phone;
        String cacheCode = JedisUtil.getStr(key);
        if (Assert.isEmpty(cacheCode)) {
            throw new BusinessException("验证码已失效");
        }
        if (!code.equals(cacheCode)) {
            throw new BusinessException("验证码不正确");
        }
        SysUser sysUser = SysUser.builder().id(userId).phone(phone).build();
        save(sysUser);
        log.info("更新用户手机号，目标用户[{}]，最新手机号[{}]", userId, phone);
        JedisUtil.delKey("user:" + userId);
        return true;
    }

    /**
     * 查询封禁用户
     * @param query 查询参数
     * @return 响应
     */
    public JSONObject queryBannedUserForDatatables(DataTableQuery query) {
        JSONObject datatables = sysUserBannedService.queryForDatatables(query);
        JSONArray jsonArray = datatables.getJSONArray("data");
        List<SysUserBanned> bannedList = jsonArray.toJavaList(SysUserBanned.class);
        if (Assert.isEmpty(bannedList)) {
            return datatables;
        }
        List<Long> userIds = bannedList.stream().map(SysUserBanned::getUserId).distinct().collect(Collectors.toList());
        List<SysUser> sysUsers = queryByIds(userIds);
        Map<Long, SysUser> sysUserMap = sysUsers.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        ArrayList<SysUserBannedVO> list = new ArrayList<>();
        for (SysUserBanned banned : bannedList) {
            String jsonString = JSONObject.toJSONString(banned);
            SysUserBannedVO vo = JSONObject.parseObject(jsonString, SysUserBannedVO.class);
            SysUser sysUser = sysUserMap.get(banned.getUserId());
            vo.setImg(sysUser.getImg());
            list.add(vo);
        }
        datatables.put("data", list);
        return datatables;
    }

    /**
     * 重写Datatable查询接口
     *
     * @param query 查询参数
     * @return 响应
     */
    public JSONObject queryUserForDatatables(DataTableQuery query) {
        JSONObject jsonObject = queryForDatatables(query);
        // 获取到分页查询出的用户信息
        JSONArray jsonArray = jsonObject.getJSONArray("data");
        List<SysUser> sysUsers = jsonArray.toJavaList(SysUser.class);
        List<Long> idList = sysUsers.stream().map(SysUser::getId).collect(Collectors.toList());
        if (Assert.isEmpty(idList)) {
            return jsonObject;
        }
        // 查询所有代理用户
        List<SysUser> allAgents = queryAllAgents();
        Map<String, SysUser> agentMap = allAgents.stream().collect(Collectors.toMap(agent -> String.valueOf(agent.getId()), agent -> agent));
        // 查询所有角色，构建角色映射
        List<SysRole> sysRoles = roleService.queryAll();
        Map<String, String> roleMap = sysRoles.stream().collect(Collectors.toMap(r -> String.valueOf(r.getId()), SysRole::getRoleName));
        // 获取用户的账户信息
        List<SysUserAccount> accounts = accountService.getSysUserAccountsByUserIds(idList);
        Map<String, SysUserAccount> accountsMap = accounts.stream().collect(Collectors.toMap(a -> String.valueOf(a.getUserId()), a -> a));
        // 获取用户是否被封禁
        List<Long> bannedList = sysUserBannedService.queryBannedUserIdList(idList);
        List<UserRecordDTO> recordDTOS = sysUsers.stream().map(sysUser -> {
            // 信息脱敏
            sysUser.desensitize();
            Long roleId = sysUser.getRoleId();
            Long userId = sysUser.getId();
            SysUser agent = agentMap.get(String.valueOf(sysUser.getAgentUserId()));
            return UserRecordDTO.builder()
                    .user(sysUser).agentUser(agent).roleName(roleMap.get(roleId.toString())).account(accountsMap.get(userId.toString()))
                    .banned(bannedList.contains(userId))
                    .build();
        }).collect(Collectors.toList());
        jsonObject.put("data", recordDTOS);
        return jsonObject;
    }

    public JSONObject queryAgentUserForDatatables(DataTableQuery query) {
        JSONObject responseData = new JSONObject();
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        // 组装分页信息
        Page<SysUser> page = new Page<>(query.getStart() / query.getLength() + 1, query.getLength());
        List<DataTableColumn> columns = query.getColumns();
        // 组装过滤条件
        if (Assert.notEmpty(columns)) {
            for (DataTableColumn column : columns) {
                String columnName = column.getData();
                if (Assert.isEmpty(columnName)) {
                    continue;
                }
                if (columnName.contains(".")) {
                    columnName = columnName.substring(columnName.lastIndexOf(".") + 1);
                }
                DataTableSearch search = column.getSearch();
                String searchValue = search.getValue();
                if (Assert.isEmpty(searchValue) || "all".equals(searchValue)) {
                    continue;
                }
                columnName = VariableNameUtil.humpToLine(columnName);
                wrapper.eq(columnName, searchValue);
            }
        }
        wrapper.isNotNull("agent_level_id");
        // 组装排序条件
        List<DataTableOrder> tableOrders = query.getOrder();
        if (Assert.notEmpty(tableOrders)) {
            DataTableOrder tableOrder = tableOrders.get(0);
            String column = tableOrder.getColumn();
            column = VariableNameUtil.humpToLine(column);
            if (column.contains(".")) {
                column = column.substring(column.lastIndexOf(".") + 1);
            }
            String dir = tableOrder.getDir();
            if ("desc".equals(dir)) {
                wrapper.orderByDesc(column);
            } else {
                wrapper.orderByAsc(column);
            }
        }
        // 组装搜索条件
        DataTableSearch search = query.getSearch();
        if (Assert.notEmpty(search)) {
            List<String> searchColumns = search.getColumns();
            if (Assert.notEmpty(searchColumns)) {
                wrapper.and(i -> {
                    i.like(searchColumns.get(0), search.getValue());
                    if (searchColumns.size() > 1) {
                        for (int j = 1; j < searchColumns.size(); j++) {
                            i.or().like(searchColumns.get(j), search.getValue());
                        }
                    }
                });
            }
        }
        IPage<SysUser> TIPage = queryByWrapperPage(wrapper, page);

        responseData.put("data", TIPage.getRecords());
        responseData.put("recordsTotal", TIPage.getTotal());
        responseData.put("recordsFiltered", TIPage.getTotal());

        List<SysUser> records = TIPage.getRecords();
        List<Long> idList = records.stream().map(SysUser::getId).collect(Collectors.toList());
        if (Assert.isEmpty(idList)) {
            return responseData;
        }

        // 查询所有角色，构建角色映射
        List<SysRole> sysRoles = roleService.queryAll();
        Map<String, String> roleMap = sysRoles.stream().collect(Collectors.toMap(r -> String.valueOf(r.getId()), SysRole::getRoleName));
        // 获取用户的账户信息
        List<SysUserAccount> accounts = accountService.getSysUserAccountsByUserIds(idList);
        Map<String, SysUserAccount> accountsMap = accounts.stream().collect(Collectors.toMap(a -> String.valueOf(a.getUserId()), a -> a));
        List<UserRecordDTO> recordDTOS = records.stream().map(sysUser -> {
            // 信息脱敏
            sysUser.desensitize();
            Long roleId = sysUser.getRoleId();
            Long userId = sysUser.getId();
            return UserRecordDTO.builder().user(sysUser).roleName(roleMap.get(roleId.toString())).account(accountsMap.get(userId.toString())).build();
        }).collect(Collectors.toList());
        responseData.put("data", recordDTOS);
        return responseData;
    }


    /**
     * 用户注册
     */
    public SysUser registerUser(String userName, String userPwd, String code, String phone, Long agentUserId) throws BusinessException {
        // 校验验证码
        String key = "register:" + phone;
        String rightCode = JedisUtil.getStr(key);
        if (!ObjectUtil.equal(rightCode, code)) {
            throw new BusinessException("验证码错误");
        }
        String ciphertext = PasswordUtils.hash(userPwd);
        // 密码加密
        String pwdSalt = null;
        // 获取密码盐
        SysUser user = SysUser.builder().userName(userName).agentUserId(agentUserId).userPwd(ciphertext).phone(phone).pwdSalt(pwdSalt).img(UserConstants.IMG).status(UserConstants.STATUS).maxDomainCount(SystemConfig.websiteBaseConfig.getMaxDomainCount()).flowPrice(SystemConfig.websiteBaseConfig.getDefaultFlowPrice()).roleId(UserConstants.ROLE_ID).autoBalance(1).route(SupportedVendorUtils.defaultVendor()).build();
        SysUser userInfo = save(user);
        // 新增一个账户表
        Long userId = userInfo.getId();
        accountService.createNewAccount(userId, userName);
        log.info("用户注册成功，用户名：[{}]，手机号：[{}]", userName, phone);
        return user;
    }


    /**
     * 用户注册_邮箱注册
     */
    public SysUser registerUserByEmail(String userName, String userPwd, String code, String email, Long agentUserId) throws BusinessException {
        // 校验验证码
        String key = "register:" + email;
        String rightCode = JedisUtil.getStr(key);
        if (!ObjectUtil.equal(rightCode, code)) {
            throw new BusinessException("验证码错误");
        }
        String ciphertext = PasswordUtils.hash(userPwd);
        // 密码加密
        String pwdSalt = null;
        // 获取密码盐
        SysUser user = SysUser.builder().route(SupportedVendorUtils.defaultVendor()).userName(userName).agentUserId(agentUserId).userPwd(ciphertext).email(email).pwdSalt(pwdSalt).img(UserConstants.IMG).status(UserConstants.STATUS).maxDomainCount(SystemConfig.websiteBaseConfig.getMaxDomainCount()).flowPrice(SystemConfig.websiteBaseConfig.getDefaultFlowPrice()).roleId(UserConstants.ROLE_ID).autoBalance(1).build();
        SysUser userInfo = save(user);
        // 新增一个账户表
        Long userId = userInfo.getId();
        accountService.createNewAccount(userId, userName);
        log.info("用户注册成功，用户名：[{}]，电子邮箱：[{}]", userName, email);
        return user;
    }

    /**
     * 新增用户
     */
    public Boolean addUser(String userName, String userPwd, String email, String phone, String idCardNum, String myWebSite, String realName, Long roleId, BigDecimal flowPrice, Integer maxDomainCount, MultipartFile file, Long agentLevelId, Integer autoBalance) throws BusinessException {
        ensureSingleAdmin(roleId, null);
        String path = "";
        try {
            String upload = ossClient.upload(file);
            path = Assert.isEmpty(upload) ? UserConstants.IMG : upload;
        } catch (Exception e) {
            throw new BusinessException("上传头像失败，{}", e.getMessage()).setCause(e).log();
        }
        // 如果有身份证则是认真状态
        String status = "";
        if (Assert.isEmpty(idCardNum)) {
            status = UserConstants.STATUS;
        } else {
            status = UserStatus.CERTIFIED.getCode();
        }

        if (Assert.isEmpty(flowPrice)) {
            flowPrice = SystemConfig.websiteBaseConfig.getDefaultFlowPrice();
        }

        if (Assert.isEmpty(maxDomainCount)) {
            maxDomainCount = SystemConfig.websiteBaseConfig.getMaxDomainCount();
        }

        String ciphertext = PasswordUtils.hash(userPwd);
        String pwdSalt = null;
        SysUser user = SysUser.builder().route(SupportedVendorUtils.defaultVendor()).userName(userName).img(path).userPwd(ciphertext).pwdSalt(pwdSalt).phone(phone).email(email).myWebsite(myWebSite).realName(realName).idCardNum(idCardNum).flowPrice(flowPrice).maxDomainCount(maxDomainCount).roleId(roleId).status(status).agentLevelId(agentLevelId).autoBalance(autoBalance).build();
        SysUser userInfo = save(user);
        // 新增一个账户表
        Long userId = userInfo.getId();
        accountService.createNewAccount(userId, userName);
        return true;
    }

    /**
     * 更新用户信息
     */
    public Boolean updateUser(Long id, String userName, String userPwd, String email, String phone,
                              String idCardNum, String myWebSite, String realName, Long roleId,
                              BigDecimal flowPrice, String type, Long agentLevelId, MultipartFile file,
                              Integer autoBalance, Long agentUserId) throws BusinessException {
        ensureSingleAdmin(roleId, id);
        SysUser saveUser = queryById(id);
        // 判断是否将等级置为无
        if (Assert.isEmpty(agentLevelId)) {
            dao.updateAgentLevelToNull(id);
        }
        String path = saveUser.getImg();
        try {
            if (Assert.notEmpty(file)) {
                path = ossClient.upload(file);
            }
        } catch (Exception e) {
            throw new BusinessException("上传头像失败，{}", e.getMessage()).setCause(e).log();
        }
        // 如果有身份证则是认真状态
        String status = "";
        if (Assert.isEmpty(idCardNum)) {
            status = UserStatus.REGISTER_NOT_CERTIFIED.getCode();
        } else {
            status = UserStatus.CERTIFIED.getCode();
        }
        if (Assert.isEmpty(flowPrice)) {
            flowPrice = UserConstants.FLOW_PRICE;
        }
        if (ObjectUtil.equal(type, "1")) {
            // 密码没有修改
            SysUser sysUser = SysUser.builder().id(id).agentUserId(agentUserId).userName(userName).img(path).userPwd(saveUser.getUserPwd()).pwdSalt(saveUser.getPwdSalt()).phone(phone).email(email).myWebsite(myWebSite).realName(realName).idCardNum(idCardNum).flowPrice(flowPrice).roleId(roleId).status(status).agentLevelId(agentLevelId).autoBalance(autoBalance).build();
            if (Assert.notEmpty(saveUser.getRealName()) && Assert.notEmpty(saveUser.getIdCardNum())) {
                sysUser.setStatus(UserStatus.CERTIFIED.getCode());
            }
            save(sysUser);
        } else {
            String ciphertext = PasswordUtils.hash(userPwd);
            String pwdSalt = null;
            SysUser sysUser = SysUser.builder().id(id).agentUserId(agentUserId).userName(userName).img(path).userPwd(ciphertext).pwdSalt(pwdSalt).phone(phone).email(email).myWebsite(myWebSite).realName(realName).idCardNum(idCardNum).flowPrice(flowPrice).roleId(roleId).status(status).agentLevelId(agentLevelId).autoBalance(autoBalance).build();
            if (Assert.notEmpty(saveUser.getRealName()) && Assert.notEmpty(saveUser.getIdCardNum())) {
                sysUser.setStatus(UserStatus.CERTIFIED.getCode());
            }
            save(sysUser);
        }
        JedisUtil.delKey("user:" + id);
        return true;
    }

    /**
     * 获取指定日期的注册数量
     */
    public String getRegisterCountByTime(String inTime) {
        return dao.getRegisterCountByTime(inTime);
    }

    /**
     * description: 获取上周和本周的注册数
     *
     * @return java.util.Map<java.lang.String, java.util.List < java.lang.String>>
     * @author bo
     * @date 2023/3/28 22:03
     */
    public Map<String, List<String>> getWeekRegisterCount() {
        Map<String, List<String>> map = new HashMap<>(2);
        List<String> thisWeekRegister = new LinkedList<>();
        int dayWeek = KuocaiBaseUtil.getNowWeekNum();
        try {
            if (JedisUtil.exists(KuoCaiConstants.LAST_WEEK_REGISTER) && dayWeek == 1) {
                map.put("lastWeekRegister", JedisUtil.getListString(KuoCaiConstants.LAST_WEEK_REGISTER));
            } else {
                List<String> latWeekRegisterCount = new ArrayList<>(Arrays.asList(dao.getLatWeekRegisterCount(KuocaiBaseUtil.getLastSunDayTime()).split(",")));
                map.put("lastWeekRegister", latWeekRegisterCount);
                JedisUtil.setList(KuoCaiConstants.LAST_WEEK_REGISTER, latWeekRegisterCount);
            }
            if (!KuocaiBaseUtil.todayIsWeeks(Calendar.MONDAY)) {
                List<String> temp = JedisUtil.getListString(KuoCaiConstants.THIS_WEEK_REGISTER);
                if (Assert.notEmpty(temp) && temp.size() == dayWeek - 1) {
                    thisWeekRegister = temp;
                } else {
                    thisWeekRegister = new ArrayList<>(Arrays.asList(dao.getLatWeekRegisterCount(KuocaiBaseUtil.accessTimeString(7 - dayWeek)).split(",")));
                    thisWeekRegister.subList(dayWeek - 1, 7).clear();
                    JedisUtil.setList(KuoCaiConstants.THIS_WEEK_REGISTER, thisWeekRegister);
                }
            }
            thisWeekRegister.add(dao.getRegisterCountByTime(KuocaiBaseUtil.accessTimeString(0)));
            map.put("thisWeekRegister", thisWeekRegister);
        } catch (Exception e) {
            log.error("获取上周注册数失败：{}", e.getMessage());
        }
        return map;

    }

    /**
     * description: 获取上周和本周的登录数
     *
     * @return java.util.Map<java.lang.String, java.util.List < java.lang.String>>
     * @author bo
     * @date 2023/3/28 22:02
     */
    public Map<String, List<String>> getWeekLoginCount() throws BusinessException {
        Map<String, List<String>> map = new HashMap<>(2);
        List<String> thisWeekLogin = new LinkedList<>();
        int dayWeek = KuocaiBaseUtil.getNowWeekNum();
        try {
            Boolean lastWeekLogin = JedisUtil.exists(KuoCaiConstants.LAST_WEEK_LOGIN);
            if (lastWeekLogin && dayWeek == 1) {
                map.put("lastWeekLogin", JedisUtil.getListString(KuoCaiConstants.LAST_WEEK_LOGIN));
            } else {
                // 获取上周登录数并且放到redis中
                List<String> latWeekLoginCount = loginDeviceService.getLatWeekLoginCount(KuocaiBaseUtil.getLastSunDayTime());
                map.put("lastWeekLogin", latWeekLoginCount);
                JedisUtil.setList(KuoCaiConstants.LAST_WEEK_LOGIN, latWeekLoginCount);
            }
            if (!KuocaiBaseUtil.todayIsWeeks(Calendar.MONDAY)) {
                List<String> temp = JedisUtil.getListString(KuoCaiConstants.THIS_WEEK_LOGIN);
                // 判断redis是否为null，且大小是否为今天的星期数
                if (Assert.notEmpty(temp) && temp.size() == dayWeek - 1) {
                    thisWeekLogin = temp;
                } else {
                    thisWeekLogin = loginDeviceService.getLatWeekLoginCount(KuocaiBaseUtil.accessTimeString(7 - dayWeek));
                    thisWeekLogin.subList(dayWeek - 1, 7).clear();
                    JedisUtil.setList(KuoCaiConstants.THIS_WEEK_LOGIN, thisWeekLogin);
                }
            }
            thisWeekLogin.add(loginDeviceService.getLoginCountByTime(KuocaiBaseUtil.accessTimeString(0)));
            map.put("thisWeekLogin", thisWeekLogin);
        } catch (Exception e) {
            throw new BusinessException("获取上周注册数失败：{}", e.getMessage());
        }
        return map;
    }

    /**
     * 根据手机号码查询用户
     */
    public List<SysUser> queryUserByPhone(String phone) {
        return queryByObj(SysUser.builder().phone(phone).build());
    }

    /**
     * 根据邮箱查询用户
     */
    public List<SysUser> queryUserByEmail(String eamil) {
        return queryByObj(SysUser.builder().email(eamil).build());
    }

    /**
     * 根据用户名称查询用户
     */
    public List<SysUser> queryUserByUserName(String userName) {
        return queryByObj(SysUser.builder().userName(userName).build());
    }

    /**
     * 根据上级代理查询用户
     */
    public List<SysUser> queryUserByAgentId(Long agentUserId) {
        return queryByObj(SysUser.builder().agentUserId(agentUserId).build());
    }

    /**
     * 根据身份证号称查询用户
     */
    public List<SysUser> queryUserByIdCard(String idCard) {
        return queryByObj(SysUser.builder().idCardNum(idCard).build());
    }

    /**
     * 删除缓存的用户信息
     */
    public void rmCacheUser(Long userId) {
        JedisUtil.delKey("user:" + userId);
    }

    /**
     * 获取传入时间前七天的注册数
     */
    public List<String> getLatWeekRegisterCount(String accessTimeString) {
        return new ArrayList<>(Arrays.asList(dao.getLatWeekRegisterCount(KuocaiBaseUtil.accessTimeString(0)).split(",")));
    }

    /**
     * 将用户设置成已经实名的状态
     *
     * @param userId 用户ID
     */
    public void user2RealName(Long userId, String realName, String idCardNum) {
        SysUser user = SysUser.builder().id(userId).status(UserStatus.CERTIFIED.getCode()).realName(realName).idCardNum(idCardNum).build();
        save(user);
    }

    /**
     * 获取缓存用户
     */
    public SysUser queryCacheUserById(Long userId) {
        String key = "user:" + userId;
        JSONObject userJson = JedisUtil.getJson(key);
        if (Assert.isEmpty(userJson)) {
            SysUser user = queryById(userId);
            JedisUtil.setJson(key, user, 3600);
            return user;
        } else {
            return JSONObject.toJavaObject(userJson, SysUser.class);
        }
    }

    /**
     * 下级用户查询代理用户的代理配置
     *
     * @param userId 下级用户ID
     * @return 代理配置
     */
    public AgentConfig queryAgentConfigByJuniorUser(Long userId) {
        SysUser sysUser = queryById(userId);
        if (Assert.isEmpty(sysUser)) {
            return null;
        }
        if (Assert.isEmpty(sysUser.getAgentUserId())) {
            return null;
        }
        return agentConfigService.queryByUserId(sysUser.getAgentUserId());
    }

    public List<SysUser> queryByAccount(String account, Long roleId) {
        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("role_id", roleId);
        wrapper.and(wp -> wp.eq("email", account).or().eq("phone", account));
        return queryByWrapper(wrapper);
    }
}
