#!/bin/sh
set -eu

mode="${1:-full}"
ts="$(date -u +%Y%m%dT%H%M%SZ)"
out_dir="/backups"

host="${MYSQL_HOST:-db}"
port="${MYSQL_PORT:-3306}"
user="${MYSQL_USER:-root}"
pass="${MYSQL_PASSWORD:-rootpass}"
db="${MYSQL_DATABASE:-petsupplies}"

mkdir -p "${out_dir}/full" "${out_dir}/binlog"

if [ "${mode}" = "full" ]; then
  echo "[backup] full dump @ ${ts}"
  mysqldump -h "${host}" -P "${port}" -u "${user}" -p"${pass}" --databases "${db}" --single-transaction --routines --events > "${out_dir}/full/${db}_${ts}.sql"
fi

if [ "${mode}" = "binlog" ] || [ "${mode}" = "full" ]; then
  echo "[backup] binlog snapshot @ ${ts}"
  mysql -h "${host}" -P "${port}" -u "${user}" -p"${pass}" -e "SHOW BINARY LOGS;" | tail -n +2 | awk '{print $1}' | while read -r log; do
    mysqlbinlog --read-from-remote-server --host="${host}" --port="${port}" --user="${user}" --password="${pass}" "${log}" > "${out_dir}/binlog/${log}_${ts}.binlog" || true
  done
fi

echo "[backup] retention"
/usr/local/bin/retention.sh

