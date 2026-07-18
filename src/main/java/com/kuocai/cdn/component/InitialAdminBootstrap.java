package com.kuocai.cdn.component;

import com.kuocai.cdn.entity.SysUser;
import com.kuocai.cdn.service.InstallationStateService;
import com.kuocai.cdn.service.SysUserService;
import com.kuocai.cdn.util.Assert;
import com.kuocai.cdn.util.JedisUtil;
import com.kuocai.cdn.util.PasswordUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class InitialAdminBootstrap implements ApplicationRunner {

    private final InstallationStateService installationStateService;
    private final SysUserService userService;
    private final String bootstrapPassword;

    public InitialAdminBootstrap(InstallationStateService installationStateService,
                                 SysUserService userService,
                                 @Value("${installation.bootstrap-password:}") String bootstrapPassword) {
        this.installationStateService = installationStateService;
        this.userService = userService;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!installationStateService.isPending()
                || Boolean.TRUE.equals(installationStateService.getState().getBootstrapPasswordApplied())) {
            return;
        }
        if (Assert.isEmpty(bootstrapPassword)) {
            log.warn("首次安装尚未配置临时管理员密码，保留数据库中的原管理员密码");
            return;
        }
        List<SysUser> admins = userService.queryAllAdmins();
        if (Assert.isEmpty(admins)) {
            log.error("首次安装无法设置临时密码：数据库中没有管理员账号");
            return;
        }
        SysUser admin = admins.get(0);
        admin.setUserPwd(PasswordUtils.hash(bootstrapPassword));
        admin.setPwdSalt(null);
        userService.save(admin);
        JedisUtil.delKey("user:" + admin.getId());
        installationStateService.update(state -> state.setBootstrapPasswordApplied(true), admin.getId());
        log.info("首次安装临时管理员密码已写入，管理员账号：{}", admin.getUserName());
    }
}
