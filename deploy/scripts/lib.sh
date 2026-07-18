#!/usr/bin/env bash
set -Eeuo pipefail

INSTALL_DIR="${KUOCAI_INSTALL_DIR:-/opt/kuocai-cdn}"
STATE_FILE="$INSTALL_DIR/env/install.state"
APP_ENV="$INSTALL_DIR/env/app.env"
COMPOSE=(docker compose --project-directory "$INSTALL_DIR" -f "$INSTALL_DIR/docker-compose.yml")

log() { printf '\033[1;32m[KuocaiCDN]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[KuocaiCDN]\033[0m %s\n' "$*" >&2; }
die() { printf '\033[1;31m[KuocaiCDN]\033[0m %s\n' "$*" >&2; exit 1; }

require_root() { [ "$(id -u)" -eq 0 ] || die "请使用 root 用户运行此脚本"; }

random_text() {
  local length="${1:-32}"
  local value
  value="$(openssl rand -hex $((length + 8)))"
  printf '%s' "${value:0:length}"
}

state_write() {
  : >"$STATE_FILE"
  chmod 600 "$STATE_FILE"
  local name value encoded
  for name in "$@"; do
    value="${!name-}"
    encoded="$(printf '%s' "$value" | base64 | tr -d '\r\n')"
    printf '%s=%s\n' "$name" "$encoded" >>"$STATE_FILE"
  done
}

state_load() {
  [ -f "$STATE_FILE" ] || return 1
  local name encoded value
  while IFS='=' read -r name encoded; do
    [[ "$name" =~ ^[A-Z0-9_]+$ ]] || die "安装状态文件包含无效键名"
    value="$(printf '%s' "$encoded" | base64 -d)"
    printf -v "$name" '%s' "$value"
    export "$name"
  done <"$STATE_FILE"
}

env_quote() {
  local value="$1"
  value="${value//\\/\\\\}"
  value="${value//\'/\\\'}"
  printf "'%s'" "$value"
}

env_line() { printf '%s=%s\n' "$1" "$(env_quote "$2")"; }

prompt_value() {
  local variable="$1" label="$2" default="${3-}" secret="${4-false}" value="${!variable-}"
  if [ -n "$value" ]; then return; fi
  if [ "$secret" = true ]; then
    read -r -s -p "$label${default:+ [$default]}: " value; printf '\n'
  else
    read -r -p "$label${default:+ [$default]}: " value
  fi
  value="${value:-$default}"
  [ -n "$value" ] || die "$label 不能为空"
  printf -v "$variable" '%s' "$value"
}

prompt_mode() {
  local variable="$1" service="$2" value="${!variable-}"
  if [ -n "$value" ]; then
    [ "$value" = bundled ] || [ "$value" = external ] || die "$variable 只能是 bundled 或 external"
    return
  fi
  read -r -p "$service 使用内置容器还是外部服务？[1=内置, 2=外部，默认1]: " value
  [ "${value:-1}" = 2 ] && value=external || value=bundled
  printf -v "$variable" '%s' "$value"
}

wait_container_healthy() {
  local service="$1" timeout="${2:-180}" started now status
  started="$(date +%s)"
  while true; do
    status="$(${COMPOSE[@]} ps --format json "$service" 2>/dev/null | tr -d '\r' | grep -o '"Health":"[^"]*"' | head -1 | cut -d: -f2 | tr -d '"' || true)"
    [ "$status" = healthy ] && return 0
    [ "$status" = unhealthy ] && die "$service 容器健康检查失败，请运行 status.sh 查看日志"
    now="$(date +%s)"; [ $((now-started)) -lt "$timeout" ] || die "等待 $service 启动超时"
    sleep 3
  done
}

wait_http() {
  local url="$1" timeout="${2:-180}" started now
  started="$(date +%s)"
  until curl -fsS --max-time 5 "$url" >/dev/null 2>&1; do
    now="$(date +%s)"; [ $((now-started)) -lt "$timeout" ] || return 1
    sleep 3
  done
}

mysql_external() {
  docker run --rm --network host -e MYSQL_PWD="$DB_PASSWORD" mysql:8.0.40 \
    mysql -h "$DB_IMPORT_HOST" -P "$DB_PORT" -u "$DB_USERNAME" "$@"
}

mysql_internal() {
  ${COMPOSE[@]} exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot "$@"' -- "$@"
}

database_query() {
  if [ "$MYSQL_MODE" = bundled ]; then mysql_internal "$@"; else mysql_external "$@"; fi
}
