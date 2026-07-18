#!/usr/bin/env bash
set -Eeuo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/scripts/lib.sh"
require_root
${COMPOSE[@]} ps
printf '\nApplication health:\n'
curl -fsS --max-time 5 http://127.0.0.1/health || true
printf '\n\nRecent application logs:\n'
${COMPOSE[@]} logs --tail=80 app
