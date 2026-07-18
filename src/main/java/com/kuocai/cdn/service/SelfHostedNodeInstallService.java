package com.kuocai.cdn.service;

import com.alibaba.fastjson.JSONObject;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.kuocai.cdn.entity.SelfHostedNode;
import com.kuocai.cdn.exception.BusinessException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Properties;

@Service
public class SelfHostedNodeInstallService {
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int COMMAND_TIMEOUT_MS = 900_000;

    private final SelfHostedCdnService selfHostedCdnService;

    public SelfHostedNodeInstallService(SelfHostedCdnService selfHostedCdnService) {
        this.selfHostedCdnService = selfHostedCdnService;
    }

    public String install(Long nodeId, String controlPlaneUrl) throws BusinessException {
        SelfHostedNode node = selfHostedCdnService.getNode(nodeId);
        if (!"root".equals(node.getSshUsername())) {
            throw new BusinessException("首版一键安装要求使用 root SSH 账号");
        }
        String password = selfHostedCdnService.decryptSshPassword(node);
        Session session = null;
        boolean installationStarted = false;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(node.getSshUsername(), node.getHost(), node.getSshPort());
            session.setPassword(password);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "password,keyboard-interactive");
            session.setConfig(config);
            session.connect(CONNECT_TIMEOUT_MS);

            String hostKey = session.getHostKey().getType() + " " + session.getHostKey().getKey();
            String fingerprint = fingerprint(session.getHostKey().getKey());
            if (node.getSshHostKey() == null || node.getSshHostKey().trim().isEmpty()) {
                selfHostedCdnService.recordHostKey(node, hostKey);
                throw new HostKeyConfirmationRequired("首次连接已记录 SSH 主机指纹 " + fingerprint + "，请核对后再次点击安装");
            }
            if (!node.getSshHostKey().equals(hostKey)) {
                throw new BusinessException("SSH 主机指纹发生变化，已拒绝安装；当前指纹：" + fingerprint);
            }

            String token = selfHostedCdnService.issueAgentToken(nodeId);
            installationStarted = true;
            upload(session, "/tmp/kuocai-edge-agent.py", readResource("self-hosted/kuocai-edge-agent.py"));
            upload(session, "/tmp/kuocai-edge-agent.json", agentConfig(nodeId, token, controlPlaneUrl));
            upload(session, "/tmp/kuocai-edge-agent.service", systemdUnit());
            execute(session, installCommand());
            selfHostedCdnService.markInstalled(node);
            return "Agent 安装成功，节点将在约 30 秒内上线";
        } catch (HostKeyConfirmationRequired e) {
            throw new BusinessException(e.getMessage());
        } catch (BusinessException e) {
            if (installationStarted && !(e instanceof HostKeyConfirmationRequired)) {
                selfHostedCdnService.markInstallFailed(node, e.getMessage());
            }
            throw e;
        } catch (Exception e) {
            selfHostedCdnService.markInstallFailed(node, e.getMessage());
            throw new BusinessException("Agent 安装失败：" + e.getMessage());
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    public String test(Long nodeId) throws BusinessException {
        SelfHostedNode node = selfHostedCdnService.getNode(nodeId);
        String password = selfHostedCdnService.decryptSshPassword(node);
        Session session = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(node.getSshUsername(), node.getHost(), node.getSshPort());
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(CONNECT_TIMEOUT_MS);
            return "SSH 连接成功，主机指纹：" + fingerprint(session.getHostKey().getKey());
        } catch (Exception e) {
            throw new BusinessException("SSH 连接失败：" + e.getMessage());
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private void upload(Session session, String remotePath, byte[] content) throws Exception {
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        try {
            channel.connect(CONNECT_TIMEOUT_MS);
            channel.put(new ByteArrayInputStream(content), remotePath);
        } finally {
            channel.disconnect();
        }
    }

    private void execute(Session session, String command) throws Exception {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        channel.setCommand(command);
        channel.setInputStream(null);
        channel.setOutputStream(output);
        channel.setErrStream(error);
        channel.connect(CONNECT_TIMEOUT_MS);
        long deadline = System.currentTimeMillis() + COMMAND_TIMEOUT_MS;
        while (!channel.isClosed()) {
            if (System.currentTimeMillis() > deadline) {
                channel.disconnect();
                throw new BusinessException("远程安装超时");
            }
            Thread.sleep(200L);
        }
        int exitStatus = channel.getExitStatus();
        channel.disconnect();
        if (exitStatus != 0) {
            String message = new String(error.toByteArray(), StandardCharsets.UTF_8).trim();
            if (message.isEmpty()) {
                message = new String(output.toByteArray(), StandardCharsets.UTF_8).trim();
            }
            throw new BusinessException("远程安装命令失败：" + truncate(message));
        }
    }

    private byte[] readResource(String path) throws Exception {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream stream = resource.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private byte[] agentConfig(Long nodeId, String token, String controlPlaneUrl) {
        JSONObject json = new JSONObject(true);
        json.put("controlPlane", controlPlaneUrl);
        json.put("nodeId", nodeId);
        json.put("token", token);
        json.put("pollIntervalSeconds", 30);
        return json.toJSONString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] systemdUnit() {
        String unit = "[Unit]\n" +
                "Description=Kuocai Self-hosted CDN Edge Agent\n" +
                "After=network-online.target nginx.service\n" +
                "Wants=network-online.target\n\n" +
                "[Service]\n" +
                "Type=simple\n" +
                "ExecStart=/usr/bin/python3 /opt/kuocai-edge/agent.py\n" +
                "Restart=always\n" +
                "RestartSec=5\n" +
                "User=root\n" +
                "NoNewPrivileges=true\n" +
                "PrivateTmp=true\n\n" +
                "[Install]\n" +
                "WantedBy=multi-user.target\n";
        return unit.getBytes(StandardCharsets.UTF_8);
    }

    static String installCommand() {
        return "set -eu\n" +
                "export DEBIAN_FRONTEND=noninteractive\n" +
                "INSTALL_LOCK=/var/run/kuocai-edge-install.lock\n" +
                "if ! mkdir \"$INSTALL_LOCK\" 2>/dev/null; then\n" +
                "  LOCK_PID=$(cat \"$INSTALL_LOCK/pid\" 2>/dev/null || true)\n" +
                "  if [ -n \"$LOCK_PID\" ] && kill -0 \"$LOCK_PID\" 2>/dev/null; then\n" +
                "    echo 'another Agent installation is already running' >&2\n" +
                "    exit 23\n" +
                "  fi\n" +
                "  rm -f \"$INSTALL_LOCK/pid\"\n" +
                "  rmdir \"$INSTALL_LOCK\" 2>/dev/null || { echo 'another Agent installation is already running' >&2; exit 23; }\n" +
                "  mkdir \"$INSTALL_LOCK\"\n" +
                "fi\n" +
                "echo $$ > \"$INSTALL_LOCK/pid\"\n" +
                "trap 'rm -f \"$INSTALL_LOCK/pid\"; rmdir \"$INSTALL_LOCK\" 2>/dev/null || true' EXIT\n" +
                "install_yum_packages() {\n" +
                "  if command -v python3 >/dev/null 2>&1 && command -v nginx >/dev/null 2>&1 && command -v curl >/dev/null 2>&1 && rpm -q ca-certificates >/dev/null 2>&1; then\n" +
                "    return 0\n" +
                "  fi\n" +
                "  if [ -r /etc/os-release ]; then . /etc/os-release; fi\n" +
                "  if [ \"${ID:-}\" = \"centos\" ] && [ \"${VERSION_ID%%.*}\" = \"7\" ]; then\n" +
                "    cat > /etc/yum.repos.d/kuocai-centos7-vault.repo <<'KUOCAI_REPO'\n" +
                "[kuocai-centos7-base]\n" +
                "name=Kuocai CentOS 7 Base Vault\n" +
                "baseurl=https://mirrors.aliyun.com/centos-vault/7.9.2009/os/$basearch/\n" +
                "enabled=1\n" +
                "gpgcheck=1\n" +
                "gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7\n" +
                "\n" +
                "[kuocai-centos7-updates]\n" +
                "name=Kuocai CentOS 7 Updates Vault\n" +
                "baseurl=https://mirrors.aliyun.com/centos-vault/7.9.2009/updates/$basearch/\n" +
                "enabled=1\n" +
                "gpgcheck=1\n" +
                "gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7\n" +
                "\n" +
                "[kuocai-centos7-extras]\n" +
                "name=Kuocai CentOS 7 Extras Vault\n" +
                "baseurl=https://mirrors.aliyun.com/centos-vault/7.9.2009/extras/$basearch/\n" +
                "enabled=1\n" +
                "gpgcheck=1\n" +
                "gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7\n" +
                "\n" +
                "[kuocai-centos7-epel]\n" +
                "name=Kuocai EPEL 7 Archive\n" +
                "baseurl=https://mirrors.aliyun.com/epel-archive/7/$basearch/\n" +
                "enabled=1\n" +
                "gpgcheck=1\n" +
                "gpgkey=https://mirrors.aliyun.com/epel/RPM-GPG-KEY-EPEL-7\n" +
                "KUOCAI_REPO\n" +
                "    yum clean metadata >/dev/null 2>&1 || true\n" +
                "    yum -y --setopt=timeout=30 --setopt=retries=2 --setopt=skip_if_unavailable=True --disablerepo='*' --enablerepo='kuocai-centos7-*' install python3 nginx ca-certificates curl\n" +
                "  else\n" +
                "    yum -y --setopt=timeout=30 --setopt=retries=2 --setopt=skip_if_unavailable=True install python3 nginx ca-certificates curl\n" +
                "  fi\n" +
                "}\n" +
                "if command -v apt-get >/dev/null 2>&1; then apt-get update -y && (apt-get install -y python3 nginx ca-certificates curl libnginx-mod-stream || apt-get install -y python3 nginx ca-certificates curl); " +
                "elif command -v dnf >/dev/null 2>&1; then (dnf install -y python3 nginx ca-certificates curl nginx-mod-stream || dnf install -y python3 nginx ca-certificates curl); " +
                "elif command -v yum >/dev/null 2>&1; then install_yum_packages; yum -y --setopt=timeout=30 --setopt=retries=2 --setopt=skip_if_unavailable=True install nginx-mod-stream || true; " +
                "else echo 'unsupported package manager' >&2; exit 12; fi\n" +
                "command -v python3 >/dev/null 2>&1 || { echo 'python3 installation failed' >&2; exit 13; }\n" +
                "command -v nginx >/dev/null 2>&1 || { echo 'nginx installation failed' >&2; exit 14; }\n" +
                "install -d -m 700 /etc/kuocai-edge /etc/kuocai-edge/releases /opt/kuocai-edge\n" +
                "install -d -m 755 /etc/nginx /etc/nginx/conf.d /var/cache/kuocai-cdn /var/log/nginx\n" +
                "if id www-data >/dev/null 2>&1; then chown -R www-data:www-data /var/cache/kuocai-cdn; elif id nginx >/dev/null 2>&1; then chown -R nginx:nginx /var/cache/kuocai-cdn; fi\n" +
                "if command -v setsebool >/dev/null 2>&1; then setsebool -P httpd_can_network_connect 1 || true; fi\n" +
                "install -m 700 /tmp/kuocai-edge-agent.py /opt/kuocai-edge/agent.py\n" +
                "install -m 600 /tmp/kuocai-edge-agent.json /etc/kuocai-edge/agent.json\n" +
                "install -m 644 /tmp/kuocai-edge-agent.service /etc/systemd/system/kuocai-edge-agent.service\n" +
                "mkdir -p /etc/kuocai-edge/releases/0\n" +
                "ln -sfn /etc/kuocai-edge/releases/0 /etc/nginx/kuocai-edge\n" +
                "systemctl daemon-reload\n" +
                "systemctl enable --now nginx\n" +
                "systemctl enable --now kuocai-edge-agent\n" +
                "rm -f /tmp/kuocai-edge-agent.py /tmp/kuocai-edge-agent.json /tmp/kuocai-edge-agent.service\n";
    }

    private String fingerprint(String base64Key) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(Base64.getDecoder().decode(base64Key));
        return "SHA256:" + Base64.getEncoder().withoutPadding().encodeToString(digest);
    }

    private String truncate(String value) {
        if (value == null) {
            return "unknown error";
        }
        return value.length() > 800 ? value.substring(value.length() - 800) : value;
    }

    private static final class HostKeyConfirmationRequired extends BusinessException {
        private HostKeyConfirmationRequired(String message) {
            super(message);
        }
    }
}
