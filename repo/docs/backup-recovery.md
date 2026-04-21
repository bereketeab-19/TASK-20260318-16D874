# Backup and recovery

This stack uses the sidecar in `backup/` to run `mysqldump` and optional binary log snapshots. Use this document for **full restore** verification and **integrity checks** before importing a dump.

## Full restore from `mysqldump`

1. **Stop the application** (or point it at a database that is not receiving traffic) so connections do not fight the restore.
2. **Verify the dump file** (see [Integrity](#integrity-checksums) below).
3. Run the restore script from a shell with the MySQL client installed (same network as the DB as in Docker Compose):

```bash
chmod +x backup/restore.sh
MYSQL_HOST=127.0.0.1 MYSQL_PORT=3307 MYSQL_USER=root MYSQL_PASSWORD=rootpass \
  ./backup/restore.sh --dump /path/to/petsupplies_20260101T120000Z.sql
```

Adjust `MYSQL_*` to match your environment. Dumps created with `mysqldump --databases petsupplies` include `CREATE DATABASE` / `USE` statements; importing applies them as written.

4. **Smoke-test** the database:

```sql
SELECT 1;
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'petsupplies';
```

5. **Start the application** and call `GET /health` — expect `{"status":"OK"}`.

### Caveats

- Restoring onto a non-empty instance can fail with “already exists” errors unless the dump drops objects first or you import into a fresh server.
- The restore script streams the file into `mysql`; it does not drop schemas automatically.

## Point-in-time recovery (binary logs)

The backup sidecar can snapshot binary logs (`backup.sh binlog` or as part of `full`). **Point-in-time recovery** is a high-level process:

1. Restore the last **full** dump taken *before* the target time.
2. Apply binary log events from the log sequence up to the desired timestamp using `mysqlbinlog` with `--stop-datetime` (or position-based cutoffs).

Example pipeline (illustrative — paths and log names depend on your server):

```bash
mysqlbinlog --stop-datetime='2026-04-21 14:30:00' /path/to/binlog.000042 | \
  mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD"
```

**Caveats:** binlog format, `gtid_mode`, and replication topology affect the exact procedure. Test on a copy of the environment first.

## Integrity (checksums)

After `backup.sh` writes `full/*.sql`, compute a checksum and store it alongside the file:

```bash
sha256sum /backups/full/petsupplies_20260101T120000Z.sql > /backups/full/petsupplies_20260101T120000Z.sql.sha256
```

Before restore:

```bash
sha256sum -c /backups/full/petsupplies_20260101T120000Z.sql.sha256
```

A mismatch means the file was altered or corrupted in transit; do not restore until resolved.

## Relationship to `backup/backup.sh`

`backup/backup.sh` writes under `/backups` in the backup container. On the host, map a volume if you need persistent copies. The same checksum workflow applies to any `mysqldump` output you produce manually.
