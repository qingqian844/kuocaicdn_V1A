#!/usr/bin/env bash
set -Eeuo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/scripts/lib.sh"
require_root
state_load || die "未找到安装状态"

timestamp="$(date +%Y%m%d-%H%M%S)"
target="$INSTALL_DIR/backups/$timestamp"
mkdir -p "$target"
chmod 700 "$INSTALL_DIR/backups" "$target"

log "备份数据库"
if [ "$MYSQL_MODE" = bundled ]; then
  ${COMPOSE[@]} exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --single-transaction --routines --events "$MYSQL_DATABASE"' >"$target/database.sql"
else
  docker run --rm --network host -e MYSQL_PWD="$DB_PASSWORD" mysql:8.0.40 \
    mysqldump -h "$DB_IMPORT_HOST" -P "$DB_PORT" -u "$DB_USERNAME" --single-transaction --routines --events "$MYSQL_DATABASE" >"$target/database.sql"
fi
cp -p "$INSTALL_DIR/packages/kuocai-cdn.jar" "$target/"
cp -p "$APP_ENV" "$STATE_FILE" "$target/"
[ ! -d "$INSTALL_DIR/secrets" ] || cp -pr "$INSTALL_DIR/secrets" "$target/"
tar -C "$INSTALL_DIR/backups" -czf "$INSTALL_DIR/backups/kuocai-backup-$timestamp.tar.gz" "$timestamp"
rm -rf "$target"
chmod 600 "$INSTALL_DIR/backups/kuocai-backup-$timestamp.tar.gz"
ls -1t "$INSTALL_DIR"/backups/kuocai-backup-*.tar.gz 2>/dev/null | tail -n +4 | xargs -r rm -f
log "备份完成：$INSTALL_DIR/backups/kuocai-backup-$timestamp.tar.gz"
