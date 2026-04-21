#!/bin/sh
# Non-interactive restore from a mysqldump produced by backup/backup.sh (or mysqldump --databases).
# WARNING: This overwrites objects in the target database. Run only against disposable or verified targets.
#
# Usage:
#   MYSQL_HOST=db MYSQL_PORT=3306 MYSQL_USER=root MYSQL_PASSWORD=rootpass \
#   ./restore.sh --dump /path/to/petsupplies_20260101T000000Z.sql
#
# Options:
#   --dump PATH       Required. Path to a .sql dump file.
#   --database NAME   Logical database name for documentation only; the dump file defines schemas.
#
# Idempotency: re-importing the same full dump may fail on existing objects unless the dump uses
# DROP/CREATE patterns. Prefer restoring into an empty MySQL instance or a fresh schema.

set -eu

dump=""

while [ "$#" -gt 0 ]; do
  case "$1" in
    --dump)
      dump="${2:-}"
      shift 2
      ;;
    --database)
      # Accepted for symmetry with backup env; mysqldump files embed CREATE DATABASE when using --databases.
      MYSQL_DATABASE="${2:-}"
      export MYSQL_DATABASE
      shift 2
      ;;
    -h|--help)
      echo "See script header in restore.sh"
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

if [ -z "${dump}" ] || [ ! -f "${dump}" ]; then
  echo "ERROR: --dump must point to an existing file." >&2
  exit 1
fi

host="${MYSQL_HOST:-db}"
port="${MYSQL_PORT:-3306}"
user="${MYSQL_USER:-root}"
pass="${MYSQL_PASSWORD:-rootpass}"
db_name="${MYSQL_DATABASE:-petsupplies}"

echo "[restore] WARNING: importing SQL will modify databases on ${host}:${port} (dump: ${dump})"
echo "[restore] target database label=${db_name}"
mysql -h "${host}" -P "${port}" -u "${user}" -p"${pass}" < "${dump}"
echo "[restore] done"
