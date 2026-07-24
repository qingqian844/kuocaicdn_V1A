package com.kuocai.cdn.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SelfHostedNodeInstallServiceTest {

    @Test
    void installCommandUsesArchivedRepositoriesForCentos7() {
        String command = SelfHostedNodeInstallService.installCommand();

        assertTrue(command.contains("${ID:-}\" = \"centos"));
        assertTrue(command.contains("${VERSION_ID%%.*}\" = \"7"));
        assertTrue(command.contains("mirrors.aliyun.com/centos-vault/7.9.2009"));
        assertTrue(command.contains("mirrors.aliyun.com/epel-archive/7/$basearch"));
        assertTrue(command.contains("--disablerepo='*' --enablerepo='kuocai-centos7-*'"));
        assertTrue(command.contains("/var/run/kuocai-edge-install.lock"));
        assertTrue(command.contains("another Agent installation is already running"));
        assertTrue(command.contains("rpm -q ca-certificates"));
        assertTrue(command.contains("--setopt=timeout=30"));
    }

    @Test
    void installCommandVerifiesRuntimeBeforeCreatingNginxLink() {
        String command = SelfHostedNodeInstallService.installCommand();

        int nginxCheck = command.indexOf("command -v nginx");
        int nginxDirectory = command.indexOf("/etc/nginx /etc/nginx/conf.d");
        int nginxLink = command.indexOf("ln -sfn /etc/kuocai-edge/releases/0 /etc/nginx/kuocai-edge");

        assertTrue(nginxCheck >= 0);
        assertTrue(nginxDirectory > nginxCheck);
        assertTrue(nginxLink > nginxDirectory);
    }

    @Test
    void reinstallRestartsRunningAgentAndVerifiesItIsActive() {
        String command = SelfHostedNodeInstallService.installCommand();

        int installAgent = command.indexOf("install -m 700 /tmp/kuocai-edge-agent.py");
        int restartAgent = command.indexOf("systemctl restart kuocai-edge-agent");
        int verifyAgent = command.indexOf("systemctl is-active --quiet kuocai-edge-agent");

        assertTrue(restartAgent > installAgent);
        assertTrue(verifyAgent > restartAgent);
    }
}
