#!/usr/bin/env bash
set -Eeuo pipefail

SOURCE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SOURCE_DIR/scripts/lib.sh"
require_root

SCHEMA_IMPORT_MARKER="$INSTALL_DIR/env/schema-importing"

trap 'warn "安装在第 $LINENO 行失败。修复问题后可重新执行 install.sh，现有数据不会被清空。"' ERR

check_platform() {
  [ "$(uname -m)" = x86_64 ] || die "首版仅支持 x86_64，当前架构：$(uname -m)"
  [ -r /etc/os-release ] || die "无法识别 Linux 发行版"
  . /etc/os-release
  case "${ID,,}" in
    ubuntu|debian|centos|rocky|almalinux) ;;
    *) die "暂不支持当前系统：${PRETTY_NAME:-$ID}" ;;
  esac
}

install_packages() {
  if command -v apt-get >/dev/null 2>&1; then
    apt-get update -y
    DEBIAN_FRONTEND=noninteractive apt-get install -y ca-certificates curl openssl tar
  elif command -v dnf >/dev/null 2>&1; then
    dnf install -y ca-certificates curl openssl tar
  else
    yum install -y ca-certificates curl openssl tar
  fi
}

install_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    log "安装 Docker Engine"
    curl -fsSL https://get.docker.com -o /tmp/kuocai-get-docker.sh
    sh /tmp/kuocai-get-docker.sh
    rm -f /tmp/kuocai-get-docker.sh
  fi
  systemctl enable --now docker >/dev/null 2>&1 || service docker start
  if ! docker compose version >/dev/null 2>&1; then
    log "安装 Docker Compose 插件"
    local version=v2.29.7 target=/usr/local/lib/docker/cli-plugins/docker-compose
    mkdir -p "$(dirname "$target")"
    curl -fL "https://github.com/docker/compose/releases/download/$version/docker-compose-linux-x86_64" -o "$target"
    chmod +x "$target"
  fi
  docker info >/dev/null 2>&1 || die "Docker 服务未正常运行"
}

copy_delivery() {
  local jar sql
  jar="$(find "$SOURCE_DIR/packages" -maxdepth 1 -type f -name '*.jar' | head -1 || true)"
  sql="$SOURCE_DIR/sql/KuocaiCDN-empty-install.sql"
  [ -s "$jar" ] || die "请先把开源版 JAR 放入 $SOURCE_DIR/packages/"
  [ -s "$sql" ] || die "缺少空数据库文件 $sql"

  mkdir -p "$INSTALL_DIR"/{packages,env,caddy,scripts,sql,backups,secrets}
  if [ "$(readlink -f "$SOURCE_DIR")" != "$(readlink -f "$INSTALL_DIR")" ]; then
    cp -f "$SOURCE_DIR/docker-compose.yml" "$SOURCE_DIR/install.sh" "$SOURCE_DIR/upgrade.sh" \
      "$SOURCE_DIR/backup.sh" "$SOURCE_DIR/status.sh" "$SOURCE_DIR/README.md" \
      "$SOURCE_DIR/README.en.md" "$INSTALL_DIR/"
    cp -f "$SOURCE_DIR/caddy/Caddyfile" "$INSTALL_DIR/caddy/Caddyfile"
    cp -f "$SOURCE_DIR/scripts/lib.sh" "$INSTALL_DIR/scripts/lib.sh"
    cp -f "$sql" "$INSTALL_DIR/sql/KuocaiCDN-empty-install.sql"
    if [ ! -s "$STATE_FILE" ] || [ ! -s "$INSTALL_DIR/packages/kuocai-cdn.jar" ]; then
      if [ "$(readlink -f "$jar")" != "$(readlink -f "$INSTALL_DIR/packages/kuocai-cdn.jar" 2>/dev/null || true)" ]; then
        cp -f "$jar" "$INSTALL_DIR/packages/kuocai-cdn.jar"
      fi
    fi
  fi
  chmod 700 "$INSTALL_DIR"/*.sh "$INSTALL_DIR/scripts/lib.sh"
  chmod 700 "$INSTALL_DIR/secrets"
}

generate_rsa_pair() {
  local prefix="$1" private_file
  private_file="$(mktemp)"
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$private_file" >/dev/null 2>&1
  printf -v "${prefix}_PRIVATE_KEY" '%s' "$(openssl pkcs8 -topk8 -nocrypt -in "$private_file" -outform DER | base64 | tr -d '\r\n')"
  printf -v "${prefix}_PUBLIC_KEY" '%s' "$(openssl pkey -in "$private_file" -pubout -outform DER | base64 | tr -d '\r\n')"
  rm -f "$private_file"
}

configure_services() {
  prompt_mode MYSQL_MODE MySQL
  prompt_mode REDIS_MODE Redis
  prompt_mode MONGODB_MODE MongoDB
  prompt_mode RABBITMQ_MODE RabbitMQ
  prompt_mode MINIO_MODE MinIO

  INSTALLATION_PUBLIC_IP="${INSTALLATION_PUBLIC_IP:-$(curl -4fsS --max-time 8 https://api.ipify.org 2>/dev/null || true)}"
  prompt_value INSTALLATION_PUBLIC_IP "服务器公网 IPv4"
  INSTALLATION_BOOTSTRAP_PASSWORD="${INSTALLATION_BOOTSTRAP_PASSWORD:-Kc9$(random_text 15)}"
  JWT_PRIVATE_KEY=""; JWT_PUBLIC_KEY=""; CONFIG_RSA_PRIVATE_KEY=""; CONFIG_RSA_PUBLIC_KEY=""
  generate_rsa_pair JWT
  generate_rsa_pair CONFIG_RSA
  PASSWORD_LEGACY_AES_KEY="${PASSWORD_LEGACY_AES_KEY:-$(random_text 32)}"
  BILLING_API_TOKEN="${BILLING_API_TOKEN:-$(random_text 48)}"

  MYSQL_DATABASE=kuocai_cdn
  if [ "$MYSQL_MODE" = bundled ]; then
    DB_HOST=mysql; DB_IMPORT_HOST=mysql; DB_PORT=3306; DB_USERNAME=kuocai
    DB_PASSWORD="$(random_text 40)"; MYSQL_ROOT_PASSWORD="$(random_text 48)"
  else
    prompt_value DB_IMPORT_HOST "外部 MySQL 主机"
    prompt_value DB_PORT "外部 MySQL 端口" 3306
    prompt_value MYSQL_DATABASE "数据库名" kuocai_cdn
    [[ "$MYSQL_DATABASE" =~ ^[A-Za-z0-9_]+$ ]] || die "数据库名只能包含字母、数字和下划线"
    prompt_value DB_USERNAME "数据库用户名"
    prompt_value DB_PASSWORD "数据库密码" "" true
    DB_HOST="$DB_IMPORT_HOST"
    case "$DB_HOST" in localhost|127.0.0.1) DB_HOST=host.docker.internal ;; esac
    MYSQL_ROOT_PASSWORD=""
  fi
  DB_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${MYSQL_DATABASE}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"

  if [ "$REDIS_MODE" = bundled ]; then
    REDIS_HOST=redis; REDIS_PORT=6379; REDIS_PASSWORD="$(random_text 40)"; REDIS_DB=5
  else
    prompt_value REDIS_HOST "外部 Redis 主机"; prompt_value REDIS_PORT "外部 Redis 端口" 6379
    prompt_value REDIS_PASSWORD "Redis 密码（无密码请输入 none）" "" true
    [ "$REDIS_PASSWORD" = none ] && REDIS_PASSWORD=""
    prompt_value REDIS_DB "Redis DB" 5
    case "$REDIS_HOST" in localhost|127.0.0.1) REDIS_HOST=host.docker.internal ;; esac
  fi

  if [ "$MONGODB_MODE" = bundled ]; then
    MONGO_HOST=mongodb; MONGO_PORT=27017; MONGO_USERNAME=kuocai; MONGO_PASSWORD="$(random_text 40)"; MONGO_AUTH_DB=admin; MONGO_DATABASE=kuocai_cdn
  else
    prompt_value MONGO_HOST "外部 MongoDB 主机"; prompt_value MONGO_PORT "外部 MongoDB 端口" 27017
    prompt_value MONGO_USERNAME "MongoDB 用户名"; prompt_value MONGO_PASSWORD "MongoDB 密码" "" true
    prompt_value MONGO_AUTH_DB "MongoDB 认证库" admin; prompt_value MONGO_DATABASE "MongoDB 数据库" kuocai_cdn
    case "$MONGO_HOST" in localhost|127.0.0.1) MONGO_HOST=host.docker.internal ;; esac
  fi

  if [ "$RABBITMQ_MODE" = bundled ]; then
    RABBITMQ_HOST=rabbitmq; RABBITMQ_PORT=5672; RABBITMQ_USERNAME=kuocai; RABBITMQ_PASSWORD="$(random_text 40)"; RABBITMQ_VHOST=/
  else
    prompt_value RABBITMQ_HOST "外部 RabbitMQ 主机"; prompt_value RABBITMQ_PORT "外部 RabbitMQ 端口" 5672
    prompt_value RABBITMQ_USERNAME "RabbitMQ 用户名"; prompt_value RABBITMQ_PASSWORD "RabbitMQ 密码" "" true
    prompt_value RABBITMQ_VHOST "RabbitMQ vhost" /
    case "$RABBITMQ_HOST" in localhost|127.0.0.1) RABBITMQ_HOST=host.docker.internal ;; esac
  fi

  if [ "$MINIO_MODE" = bundled ]; then
    MINIO_ENDPOINT=http://minio:9000; MINIO_ACCESS_KEY=kuocai; MINIO_SECRET_KEY="$(random_text 40)"; MINIO_BUCKET=uploads; MINIO_PUBLIC_URL=""
  else
    prompt_value MINIO_ENDPOINT "外部 MinIO Endpoint（含 http/https）"
    prompt_value MINIO_ACCESS_KEY "MinIO Access Key"; prompt_value MINIO_SECRET_KEY "MinIO Secret Key" "" true
    prompt_value MINIO_BUCKET "MinIO Bucket" uploads; prompt_value MINIO_PUBLIC_URL "MinIO 公网地址（没有可填 none）" none
    [ "$MINIO_PUBLIC_URL" = none ] && MINIO_PUBLIC_URL=""
    MINIO_ENDPOINT="${MINIO_ENDPOINT/\/\/localhost/\/\/host.docker.internal}"
    MINIO_ENDPOINT="${MINIO_ENDPOINT/\/\/127.0.0.1/\/\/host.docker.internal}"
  fi
}

write_runtime_files() {
  local profiles=()
  [ "$MYSQL_MODE" = bundled ] && profiles+=(bundled-mysql)
  [ "$REDIS_MODE" = bundled ] && profiles+=(bundled-redis)
  [ "$MONGODB_MODE" = bundled ] && profiles+=(bundled-mongodb)
  [ "$RABBITMQ_MODE" = bundled ] && profiles+=(bundled-rabbitmq)
  [ "$MINIO_MODE" = bundled ] && profiles+=(bundled-minio)
  local profile_csv; profile_csv="$(IFS=,; echo "${profiles[*]}")"
  printf 'COMPOSE_PROFILES=%s\n' "$profile_csv" >"$INSTALL_DIR/.env"

  {
    env_line TZ Asia/Shanghai; env_line SPRING_PROFILES_ACTIVE prod; env_line SERVER_PORT 8000
    env_line DB_URL "$DB_URL"; env_line DB_USERNAME "$DB_USERNAME"; env_line DB_PASSWORD "$DB_PASSWORD"
    env_line MYSQL_DATABASE "$MYSQL_DATABASE"; env_line MYSQL_USER "$DB_USERNAME"; env_line MYSQL_PASSWORD "$DB_PASSWORD"; env_line MYSQL_ROOT_PASSWORD "$MYSQL_ROOT_PASSWORD"
    env_line REDIS_HOST "$REDIS_HOST"; env_line REDIS_PORT "$REDIS_PORT"; env_line REDIS_PASSWORD "$REDIS_PASSWORD"; env_line REDIS_DB "$REDIS_DB"
    env_line MONGO_HOST "$MONGO_HOST"; env_line MONGO_PORT "$MONGO_PORT"; env_line MONGO_USERNAME "$MONGO_USERNAME"; env_line MONGO_PASSWORD "$MONGO_PASSWORD"; env_line MONGO_AUTH_DB "$MONGO_AUTH_DB"; env_line MONGO_DATABASE "$MONGO_DATABASE"
    env_line MONGO_INITDB_ROOT_USERNAME "$MONGO_USERNAME"; env_line MONGO_INITDB_ROOT_PASSWORD "$MONGO_PASSWORD"
    env_line RABBITMQ_HOST "$RABBITMQ_HOST"; env_line RABBITMQ_PORT "$RABBITMQ_PORT"; env_line RABBITMQ_USERNAME "$RABBITMQ_USERNAME"; env_line RABBITMQ_PASSWORD "$RABBITMQ_PASSWORD"; env_line RABBITMQ_VHOST "$RABBITMQ_VHOST"
    env_line RABBITMQ_DEFAULT_USER "$RABBITMQ_USERNAME"; env_line RABBITMQ_DEFAULT_PASS "$RABBITMQ_PASSWORD"; env_line RABBITMQ_DEFAULT_VHOST "$RABBITMQ_VHOST"
    env_line MINIO_ENDPOINT "$MINIO_ENDPOINT"; env_line MINIO_ACCESS_KEY "$MINIO_ACCESS_KEY"; env_line MINIO_SECRET_KEY "$MINIO_SECRET_KEY"; env_line MINIO_BUCKET "$MINIO_BUCKET"; env_line MINIO_PUBLIC_URL "$MINIO_PUBLIC_URL"
    env_line MINIO_ROOT_USER "$MINIO_ACCESS_KEY"; env_line MINIO_ROOT_PASSWORD "$MINIO_SECRET_KEY"
    env_line JWT_PUBLIC_KEY "$JWT_PUBLIC_KEY"; env_line JWT_PRIVATE_KEY "$JWT_PRIVATE_KEY"; env_line CONFIG_RSA_PUBLIC_KEY "$CONFIG_RSA_PUBLIC_KEY"; env_line CONFIG_RSA_PRIVATE_KEY "$CONFIG_RSA_PRIVATE_KEY"
    env_line PASSWORD_LEGACY_AES_KEY "$PASSWORD_LEGACY_AES_KEY"; env_line BILLING_API_TOKEN "$BILLING_API_TOKEN"
    env_line INSTALLATION_BOOTSTRAP_PASSWORD "$INSTALLATION_BOOTSTRAP_PASSWORD"; env_line INSTALLATION_PUBLIC_IP "$INSTALLATION_PUBLIC_IP"; env_line INSTALLATION_SECRETS_DIR /app/secrets
    env_line CADDY_ADMIN_URL http://caddy:2019; env_line CADDY_UPSTREAM app:8000
  } >"$APP_ENV"
  chmod 600 "$APP_ENV" "$INSTALL_DIR/.env"

  state_write MYSQL_MODE REDIS_MODE MONGODB_MODE RABBITMQ_MODE MINIO_MODE DB_HOST DB_IMPORT_HOST DB_PORT MYSQL_DATABASE DB_USERNAME DB_PASSWORD MYSQL_ROOT_PASSWORD REDIS_HOST REDIS_PORT REDIS_PASSWORD REDIS_DB MONGO_HOST MONGO_PORT MONGO_USERNAME MONGO_PASSWORD MONGO_AUTH_DB MONGO_DATABASE RABBITMQ_HOST RABBITMQ_PORT RABBITMQ_USERNAME RABBITMQ_PASSWORD RABBITMQ_VHOST MINIO_ENDPOINT MINIO_ACCESS_KEY MINIO_SECRET_KEY MINIO_BUCKET MINIO_PUBLIC_URL INSTALLATION_PUBLIC_IP INSTALLATION_BOOTSTRAP_PASSWORD
  {
    printf '管理员账号：admin\n'
    printf '临时密码：%s\n' "$INSTALLATION_BOOTSTRAP_PASSWORD"
    printf '首次访问：http://%s/kuocaiadmin\n' "$INSTALLATION_PUBLIC_IP"
  } >"$INSTALL_DIR/env/first-login.txt"
  chmod 600 "$INSTALL_DIR/env/first-login.txt"
}

tcp_check() {
  local host="$1" port="$2" label="$3"
  case "$host" in host.docker.internal) host=127.0.0.1 ;; esac
  timeout 6 bash -c "</dev/tcp/$host/$port" 2>/dev/null || die "$label 无法连接：$host:$port"
}

start_dependencies() {
  cd "$INSTALL_DIR"
  ${COMPOSE[@]} pull
  local services=()
  [ "$MYSQL_MODE" = bundled ] && services+=(mysql)
  [ "$REDIS_MODE" = bundled ] && services+=(redis)
  [ "$MONGODB_MODE" = bundled ] && services+=(mongodb)
  [ "$RABBITMQ_MODE" = bundled ] && services+=(rabbitmq)
  [ "$MINIO_MODE" = bundled ] && services+=(minio)
  if [ ${#services[@]} -gt 0 ]; then
    ${COMPOSE[@]} up -d "${services[@]}"
    for service in "${services[@]}"; do wait_container_healthy "$service" 240; done
  fi
  [ "$REDIS_MODE" = external ] && tcp_check "$REDIS_HOST" "$REDIS_PORT" Redis
  [ "$MONGODB_MODE" = external ] && tcp_check "$MONGO_HOST" "$MONGO_PORT" MongoDB
  [ "$RABBITMQ_MODE" = external ] && tcp_check "$RABBITMQ_HOST" "$RABBITMQ_PORT" RabbitMQ
  if [ "$MINIO_MODE" = external ]; then
    local minio_health="${MINIO_ENDPOINT/host.docker.internal/127.0.0.1}/minio/health/live"
    curl -fsS --max-time 8 "$minio_health" >/dev/null || die "MinIO 健康检查失败：$MINIO_ENDPOINT"
  fi
}

initialize_database() {
  log "验证并初始化 MySQL"
  local database_created=false
  if [ "$MYSQL_MODE" = external ]; then
    tcp_check "$DB_IMPORT_HOST" "$DB_PORT" MySQL
    mysql_external -Nse 'SELECT 1' >/dev/null || die "外部 MySQL 账号验证失败"
    local database_exists
    database_exists="$(mysql_external -Nse "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='${MYSQL_DATABASE}'" | tr -d '[:space:]')"
    if [ "${database_exists:-0}" = 0 ]; then
      mysql_external -e "CREATE DATABASE \`${MYSQL_DATABASE}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci" \
        || die "数据库不存在，且当前账号没有创建数据库权限"
      database_created=true
    fi
  fi

  local table_count marker_database marker_owned schema_imported=false
  table_count="$(database_query -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${MYSQL_DATABASE}'" 2>/dev/null | tr -d '[:space:]')"
  table_count="${table_count:-0}"

  if [ -s "$SCHEMA_IMPORT_MARKER" ]; then
    marker_database="$(sed -n 's/^database=//p' "$SCHEMA_IMPORT_MARKER" | head -1)"
    marker_owned="$(sed -n 's/^created_by_installer=//p' "$SCHEMA_IMPORT_MARKER" | head -1)"
    [ "$marker_database" = "$MYSQL_DATABASE" ] || die "数据库导入恢复标记与当前数据库不一致，请检查 $SCHEMA_IMPORT_MARKER"
    log "检测到上次数据库结构导入未完成，正在安全恢复"
    if [ "$marker_owned" = true ]; then
      if database_query -e "DROP DATABASE IF EXISTS \`${MYSQL_DATABASE}\`"; then
        database_query -e "CREATE DATABASE \`${MYSQL_DATABASE}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci" \
          || die "无法重新创建上次安装器创建的数据库"
      else
        warn "当前 MySQL 账号没有删除数据库权限，将改用表级结构恢复"
      fi
    fi
    table_count=0
  fi

  if [ "$table_count" = 0 ]; then
    log "导入空数据库结构"
    [ "$MYSQL_MODE" = bundled ] && database_created=true
    {
      printf 'database=%s\n' "$MYSQL_DATABASE"
      printf 'created_by_installer=%s\n' "$database_created"
    } >"$SCHEMA_IMPORT_MARKER"
    chmod 600 "$SCHEMA_IMPORT_MARKER"
    if [ "$MYSQL_MODE" = bundled ]; then
      ${COMPOSE[@]} exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot "$MYSQL_DATABASE"' <"$INSTALL_DIR/sql/KuocaiCDN-empty-install.sql"
    else
      docker run --rm --network host -i -e MYSQL_PWD="$DB_PASSWORD" mysql:8.0.40 mysql -h "$DB_IMPORT_HOST" -P "$DB_PORT" -u "$DB_USERNAME" "$MYSQL_DATABASE" <"$INSTALL_DIR/sql/KuocaiCDN-empty-install.sql"
    fi
    rm -f "$SCHEMA_IMPORT_MARKER"
    schema_imported=true
  elif ! database_query -Nse "SHOW TABLES LIKE 'sys_config'" "$MYSQL_DATABASE" | grep -q sys_config; then
    die "目标数据库非空且不是 KuocaiCDN 数据库，为防止覆盖已停止安装"
  else
    log "检测到已有 KuocaiCDN 数据库，跳过结构导入"
  fi

  if [ "$schema_imported" = true ]; then
    database_query -Nse "SELECT 1 FROM sys_config WHERE biz_type='installation_state' LIMIT 1" "$MYSQL_DATABASE" | grep -q 1 \
      || die "数据库结构已导入，但首次初始化状态缺失"
  elif ! database_query -Nse "SELECT 1 FROM sys_config WHERE biz_type='installation_state' LIMIT 1" "$MYSQL_DATABASE" | grep -q 1; then
    log "历史数据库没有首次安装状态，按已完成初始化兼容处理"
  fi
}

start_application() {
  log "启动 KuocaiCDN 和 Caddy"
  ${COMPOSE[@]} up -d app caddy
  wait_container_healthy app 300
  wait_http http://127.0.0.1/health 120 || die "应用健康检查失败，请运行 $INSTALL_DIR/status.sh 查看日志"
  local diagnostics
  diagnostics="$(curl -fsS --max-time 30 -H 'Accept: application/json' http://127.0.0.1/api/setup/status)"
  if printf '%s' "$diagnostics" | grep -q '"ok":false'; then
    printf '%s\n' "$diagnostics" >&2
    die "应用已启动，但至少一个基础服务凭证验证失败"
  fi
  touch "$INSTALL_DIR/.install-complete"
  chmod 600 "$INSTALL_DIR/.install-complete"
}

resume_existing() {
  log "检测到已有安装配置，执行幂等启动"
  state_load || die "无法读取安装状态"
  cd "$INSTALL_DIR"
  ${COMPOSE[@]} up -d
  wait_http http://127.0.0.1/health 240 || die "应用未通过健康检查"
  log "服务运行正常：http://${INSTALLATION_PUBLIC_IP}/kuocaiadmin"
}

check_platform
install_packages
install_docker
copy_delivery
if [ -s "$STATE_FILE" ]; then
  state_load || die "无法读取安装状态"
  if [ -f "$INSTALL_DIR/.install-complete" ]; then resume_existing; exit 0; fi
  log "检测到上次未完成的安装，继续执行依赖、数据库和应用启动步骤"
  start_dependencies
  initialize_database
  start_application
  exit 0
fi
configure_services
write_runtime_files
start_dependencies
initialize_database
start_application

log "安装完成"
printf '\n首次登录信息保存在：%s\n' "$INSTALL_DIR/env/first-login.txt"
printf '请访问：http://%s/kuocaiadmin\n' "$INSTALLATION_PUBLIC_IP"
printf '完成初始化后请妥善删除首次登录信息文件。\n'
