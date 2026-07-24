# KuocaiCDN 一键部署

## 安装要求

- x86_64 服务器，支持 Ubuntu、Debian、CentOS、Rocky Linux、AlmaLinux。
- 使用 `root` 执行安装。
- 域名已解析到服务器公网 IPv4，并开放 TCP `80/443` 和 UDP `443`。
- `packages/` 中放入开源版 JAR（文件名不限）。
- 服务器需要访问 Docker Hub、GitHub 和 HTTPS 证书签发服务。

## 首次安装

### 1. 放置交付文件

将整个 `deploy/` 目录上传到服务器，并把最新开源版 JAR 放入：

```text
deploy/packages/KuocaiCDN-K2.1.4.0.jar
```

JAR 文件名不限。不需要手动创建 `env/app.env`，安装器会自动生成。

### 2. 执行安装

```bash
cd /root/kuocai-deploy
chmod +x install.sh upgrade.sh backup.sh status.sh
bash install.sh
```

安装器会逐项询问 MySQL、Redis、MongoDB、RabbitMQ、MinIO 使用内置容器还是外部服务：

- 输入 `1` 或直接按回车：使用内置容器。
- 输入 `2`：使用外部服务，并继续填写连接信息。

新客户建议五项全部直接按回车。选择外部服务时，请准备主机、端口、账号和密码；安装器会执行真实连接和权限检查。程序最终安装到 `/opt/kuocai-cdn/`。

### 3. 获取临时管理员密码

安装完成后：

```bash
cat /opt/kuocai-cdn/env/first-login.txt
```

打开 `http://服务器IP/kuocaiadmin`，使用文件中的账号和临时密码登录。

### 4. 完成网页初始化

登录后自动进入 `/setup`：

1. 检查数据库、缓存、消息队列和对象存储。
2. 修改管理员资料和永久密码。
3. 验证站点域名的 DNS 和 `80` 端口。
4. 自动配置 Caddy 并申请 HTTPS 证书。
5. 配置网站名称、Logo、图标、备案号和默认参数。
6. 配置 CDN 厂商账号、DNS 和默认厂商。
7. 按需配置邮件、短信和实名认证。
8. 通过正式域名重新登录并完成初始化。

初始化完成后删除临时密码文件：

```bash
rm -f /opt/kuocai-cdn/env/first-login.txt
```

运行时密钥保存在 `/opt/kuocai-cdn/env/app.env`，权限为 `600`。该文件和部署用 JAR 均不得提交 Git。

## 重复执行与恢复

`install.sh` 可以重复执行。已有安装会幂等启动；数据库导入中断时，安装器只恢复本次认领的空数据库，不覆盖历史业务数据库。历史数据库没有 `installation_state` 时按已经完成初始化处理。

## 运维命令

```bash
# 查看容器状态和最近日志
bash /opt/kuocai-cdn/status.sh

# 立即备份数据库、JAR、环境文件和运行私钥
bash /opt/kuocai-cdn/backup.sh

# 升级 JAR
bash /opt/kuocai-cdn/upgrade.sh /path/to/new.jar
```

升级前会自动备份。新版本健康检查失败时自动恢复旧 JAR；数据库备份保留供人工恢复。默认仅保留最近三份备份。

## 常用排查

- 状态与日志：`bash /opt/kuocai-cdn/status.sh`
- 健康检查：`curl -fsS http://127.0.0.1/health`
- 初始化状态：`curl -fsS http://127.0.0.1/api/setup/status`
- 查看全部容器：`docker compose -f /opt/kuocai-cdn/docker-compose.yml ps`
- 检查端口占用：确认其他 Nginx、Apache 或面板服务没有占用 `80/443`。
- Docker 下载失败：确认服务器可以访问 `get.docker.com`、Docker Hub 和 GitHub。
- 域名验证失败：确认 A 记录指向安装时填写的公网 IPv4，并等待 DNS 生效。
- 外部服务失败：检查账号密码、数据库权限、安全组、白名单和服务监听地址。

应用仅在 Docker 内部监听 `8000`，不要额外向公网开放该端口。Caddy 管理接口 `2019` 也仅允许 Docker 内部网络访问。
