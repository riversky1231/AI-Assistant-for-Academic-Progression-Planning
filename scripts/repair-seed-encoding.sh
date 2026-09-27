#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
dc() { docker compose -f compose.yaml -f compose.server.yaml --env-file .env.docker "$@"; }
dc config --quiet
test -f sql/migrations/20260921_repair_seed_encoding.sql
dc exec -T mysql true
dc exec -T redis true
umask 077
mkdir -p backups
backup="backups/before-encoding-$(date +%Y%m%d-%H%M%S)-$$.sql"
trap 'dc start backend >/dev/null || true' EXIT
dc stop backend
echo "Backing up database to $backup"
dc exec -T mysql sh -c 'export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysqldump -uroot --single-transaction --no-tablespaces --default-character-set=utf8mb4 "$MYSQL_DATABASE"' > "$backup"
test -s "$backup"
echo "Repairing known seed text and admission constraint"
dc exec -T mysql sh -c 'export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysql -uroot --default-character-set=utf8mb4 "$MYSQL_DATABASE"' < sql/migrations/20260921_repair_seed_encoding.sql
echo "Clearing school and recommendation caches"
dc exec -T redis sh -ec '
  export REDISCLI_AUTH="$REDIS_PASSWORD"
  for pattern in "academic:school:*" "academic:recommend:*"; do
    keys=$(redis-cli --scan --pattern "$pattern")
    printf "%s\n" "$keys" | while IFS= read -r key; do
      [ -n "$key" ] || continue
      redis-cli UNLINK "$key" >/dev/null
    done
  done
'
dc start --wait --wait-timeout 300 backend
trap - EXIT
echo "Repair complete. Backup: $backup"
