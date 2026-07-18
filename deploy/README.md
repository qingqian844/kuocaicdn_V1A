# KuocaiCDN 一键部署

## 安装要求

- x86_64 服务器，支持 Ubuntu、Debian、CentOS、Rocky Linux、AlmaLinux。
- 使用 `root` 执行安装。
- 域名已解析到服务器公网 IPv4，并开放 TCP `80/443` 和 UDP `443`。
- `packages/` 中放入授权版 JAR（文件名不限）和客户的 `license.key`。
- 服务器需要访问 Docker Hub、GitHub 和 HTTPS 证书签发服务。

## 首次安装

```bash
bash install.sh
```

安装器会逐项询问 MySQL、Redis、MongoDB、RabbitMQ、MinIO 使用内置容器还是外部服务。选择外部服务时，请准备主机、端口、账号和密码；安装器会执行真实连接和权限检查。

安装完成后：

1. 查看 `/opt/kuocai-cdn/env/first-login.txt` 获取临时管理员密码。
2. 打开安装器输出的管理地址并登录。
3. 按八步向导完成管理员密码、授权域名、HTTPS、网站信息、厂商和可选模块配置。
4. HTTPS 生效后，通过配置域名重新登录，再执行“完成初始化”。
5. 初始化完成后删除 `env/first-login.txt`。

运行时密钥保存在 `/opt/kuocai-cdn/env/app.env`，权限为 `600`。微信商户私钥由向导写入 `/opt/kuocai-cdn/secrets/`，不写入数据库。以上文件、客户 JAR 和授权文件均不得提交 Git。

## 重复执行与恢复

`install.sh` 可以重复执行。已有安装会幂等启动；数据库导入中断时，安装器只恢复本次认领的空数据库，不覆盖历史业务数据库。历史数据库没有 `installation_state` 时按已经完成初始化处理。

## 运维命令

```bash
# 查看容器状态和最近日志
bash /opt/kuocai-cdn/status.sh

# 立即备份数据库、JAR、授权、环境文件和运行私钥
bash /opt/kuocai-cdn/backup.sh

# 升级 JAR，可同时替换授权文件
bash /opt/kuocai-cdn/upgrade.sh /path/to/new.jar [/path/to/new-license.key]
```

升级前会自动备份。新版本健康检查失败时自动恢复旧 JAR 和授权文件；数据库备份保留供人工恢复。默认仅保留最近三份备份。

## 常用排查

- 状态与日志：`bash /opt/kuocai-cdn/status.sh`
- 健康检查：`curl -fsS http://127.0.0.1/health`
- 初始化状态：`curl -fsS http://127.0.0.1/api/setup/status`
- 查看全部容器：`docker compose -f /opt/kuocai-cdn/docker-compose.yml ps`
- 检查端口占用：确认其他 Nginx、Apache 或面板服务没有占用 `80/443`。

应用仅在 Docker 内部监听 `8000`，不要额外向公网开放该端口。Caddy 管理接口 `2019` 也仅允许 Docker 内部网络访问。
