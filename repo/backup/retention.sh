#!/bin/sh
set -eu

out_dir="/backups"
days="${RETENTION_DAYS:-30}"

find "${out_dir}/full" -type f -mtime +"${days}" -delete || true
find "${out_dir}/binlog" -type f -mtime +"${days}" -delete || true

