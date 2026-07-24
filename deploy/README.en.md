# KuocaiCDN One-Click Deployment

## Requirements

- An x86_64 server running Ubuntu, Debian, CentOS, Rocky Linux, or AlmaLinux.
- Run the installer as `root`.
- Point a site domain to the server IPv4 address and open TCP `80/443` plus UDP `443`.
- Put the open-source JAR in `packages/`.
- Allow outbound access to Docker Hub, GitHub, and public HTTPS certificate authorities.

## Install

Put the open-source JAR in:

```text
packages/KuocaiCDN-K2.1.4.0.jar
```

The JAR filename can be arbitrary. Do not create `env/app.env` manually; the installer generates it.

```bash
cd /root/kuocai-deploy
chmod +x install.sh upgrade.sh backup.sh status.sh
bash install.sh
```

For each dependency, press Enter or enter `1` for the bundled container. Enter `2` to use an external service. New installations should normally use all bundled services. External MySQL, Redis, MongoDB, RabbitMQ, and MinIO settings are validated before setup continues.

After installation:

1. Read `/opt/kuocai-cdn/env/first-login.txt` for the temporary administrator password.
2. Sign in at the administrator URL printed by the installer.
3. Complete the eight-step setup wizard.
4. After HTTPS is active, sign in through the configured domain and complete setup.
5. Delete `env/first-login.txt` after setup.

```bash
rm -f /opt/kuocai-cdn/env/first-login.txt
```

Runtime secrets are stored in `/opt/kuocai-cdn/env/app.env` with mode `600`. Never commit this file or the deployment JAR to Git.

## Operations

```bash
bash /opt/kuocai-cdn/status.sh
bash /opt/kuocai-cdn/backup.sh
bash /opt/kuocai-cdn/upgrade.sh /path/to/new.jar
```

Upgrades create a backup first. A failed health check restores the previous JAR automatically; the database backup remains available for manual recovery. The three newest backups are retained.

The application port `8000` and Caddy admin port `2019` are internal-only. Do not expose either port publicly.
