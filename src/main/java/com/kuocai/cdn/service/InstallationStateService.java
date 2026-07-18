package com.kuocai.cdn.service;

import com.kuocai.cdn.constant.ConfigBizTypeConstants;
import com.kuocai.cdn.vo.InstallationStateVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class InstallationStateService {

    private final SysConfigService sysConfigService;
    private volatile InstallationStateVo cachedState;

    public InstallationStateService(SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    public InstallationStateVo getState() {
        InstallationStateVo current = cachedState;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (cachedState == null) {
                InstallationStateVo stored = sysConfigService.getPlainConfigContentVo(
                        InstallationStateVo.class, ConfigBizTypeConstants.INSTALLATION_STATE);
                // Existing installations have no state row and must remain accessible.
                cachedState = stored == null ? InstallationStateVo.completed() : stored;
                cachedState.normalize();
            }
            return cachedState;
        }
    }

    public boolean isPending() {
        return InstallationStateVo.PENDING.equals(getState().getStatus());
    }

    public synchronized InstallationStateVo update(StateUpdater updater, Long userId) {
        InstallationStateVo state = getState();
        updater.update(state);
        state.normalize();
        sysConfigService.savePlainConfig(state, ConfigBizTypeConstants.INSTALLATION_STATE,
                userId == null ? 1L : userId);
        cachedState = state;
        return state;
    }

    @Transactional(rollbackFor = Exception.class)
    public InstallationStateVo complete(Long userId) {
        InstallationStateVo state = getState();
        if (!Boolean.TRUE.equals(state.getAdminConfigured())
                || !Boolean.TRUE.equals(state.getDomainVerified())
                || !Boolean.TRUE.equals(state.getProxyConfigured())
                || !Boolean.TRUE.equals(state.getWebsiteConfigured())) {
            throw new IllegalStateException("管理员、域名、HTTPS 和网站基础配置尚未全部完成");
        }
        return update(value -> {
            value.setStatus(InstallationStateVo.COMPLETED);
            value.setCurrentStep(8);
            value.setCompletedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }, userId);
    }

    @FunctionalInterface
    public interface StateUpdater {
        void update(InstallationStateVo state);
    }
}
