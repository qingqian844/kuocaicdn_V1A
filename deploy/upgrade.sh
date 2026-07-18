#!/usr/bin/env bash
set -Eeuo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/scripts/lib.sh"
require_root

new_jar="${1:-}"
new_license="${2:-}"
[ -s "$new_jar" ] || die "用法：upgrade.sh /path/to/new.jar [/path/to/new-license.key]"
[ -z "$new_license" ] || [ -s "$new_license" ] || die "新的授权文件不存在"
state_load || die "未找到安装状态"

"$INSTALL_DIR/backup.sh"
rollback_dir="$(mktemp -d "$INSTALL_DIR/.upgrade-rollback.XXXXXX")"
trap 'rm -rf "$rollback_dir"' EXIT
cp -p "$INSTALL_DIR/packages/kuocai-cdn.jar" "$rollback_dir/kuocai-cdn.jar"
cp -p "$INSTALL_DIR/packages/license.key" "$rollback_dir/license.key"

cp -f "$new_jar" "$INSTALL_DIR/packages/kuocai-cdn.jar.new"
mv -f "$INSTALL_DIR/packages/kuocai-cdn.jar.new" "$INSTALL_DIR/packages/kuocai-cdn.jar"
if [ -n "$new_license" ]; then
  cp -f "$new_license" "$INSTALL_DIR/packages/license.key.new"
  mv -f "$INSTALL_DIR/packages/license.key.new" "$INSTALL_DIR/packages/license.key"
  chmod 600 "$INSTALL_DIR/packages/license.key"
fi

log "启动新版本"
${COMPOSE[@]} up -d --force-recreate app
if wait_http http://127.0.0.1/health 240; then
  log "升级完成，数据库备份已保留"
  exit 0
fi

warn "新版本健康检查失败，恢复旧 JAR 和授权文件"
cp -f "$rollback_dir/kuocai-cdn.jar" "$INSTALL_DIR/packages/kuocai-cdn.jar"
cp -f "$rollback_dir/license.key" "$INSTALL_DIR/packages/license.key"
${COMPOSE[@]} up -d --force-recreate app
wait_http http://127.0.0.1/health 180 || die "旧版本恢复后仍未通过健康检查，请查看应用日志"
die "升级失败，已恢复旧版本；数据库备份未自动覆盖"
