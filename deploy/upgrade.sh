#!/usr/bin/env bash
set -Eeuo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/scripts/lib.sh"
require_root

new_jar="${1:-}"
[ -s "$new_jar" ] || die "用法：upgrade.sh /path/to/new.jar"
state_load || die "未找到安装状态"

"$INSTALL_DIR/backup.sh"
rollback_dir="$(mktemp -d "$INSTALL_DIR/.upgrade-rollback.XXXXXX")"
trap 'rm -rf "$rollback_dir"' EXIT
cp -p "$INSTALL_DIR/packages/kuocai-cdn.jar" "$rollback_dir/kuocai-cdn.jar"

cp -f "$new_jar" "$INSTALL_DIR/packages/kuocai-cdn.jar.new"
mv -f "$INSTALL_DIR/packages/kuocai-cdn.jar.new" "$INSTALL_DIR/packages/kuocai-cdn.jar"

log "启动新版本"
${COMPOSE[@]} up -d --force-recreate app
if wait_http http://127.0.0.1/health 240; then
  log "升级完成，数据库备份已保留"
  exit 0
fi

warn "新版本健康检查失败，恢复旧 JAR"
cp -f "$rollback_dir/kuocai-cdn.jar" "$INSTALL_DIR/packages/kuocai-cdn.jar"
${COMPOSE[@]} up -d --force-recreate app
wait_http http://127.0.0.1/health 180 || die "旧版本恢复后仍未通过健康检查，请查看应用日志"
die "升级失败，已恢复旧版本；数据库备份未自动覆盖"
