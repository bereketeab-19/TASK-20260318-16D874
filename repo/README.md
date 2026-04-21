# Pet Supplies (offline monolith)

This repository contains a fully offline, self-contained Spring Boot monolith with local MySQL storage, Nginx TLS termination, and a backup sidecar.

- Canonical blueprint: [`docs/design.md`](docs/design.md)

## Requirements
- Docker + Docker Compose
- Java **17** (matches `pom.xml`, the `Dockerfile` build/runtime images, and the Maven image used by `run_tests.sh` / `run_tests.ps1` when host Maven is unavailable)
- **Field encryption**: set **`APP_CRYPTO_HEX_KEY`** to a **64-character hex string** (32-byte AES-256 key) before starting the app outside Docker Compose. `docker-compose.yml` sets a dev-only value for local stacks; generate a unique key for any shared or production environment.

## Quick start (offline runtime)
Bring up the full offline stack:

```bash
docker compose up --build
```

The HTTPS reverse proxy listens on `https://localhost:8443/` (self-signed cert).

## Default credentials (Phase 1)
- **ADMIN**:
  - username: `admin`
  - password: `admin123!`
- **ADMIN 2**:
  - username: `admin2`
  - password: `admin2_123!`
- **BUYER**:
  - username: `buyer`
  - password: `buyer123!`
- **REVIEWER** (read-only reporting oversight; same password as buyer in dev seed):
  - username: `reviewer`
  - password: `buyer123!`
- **MERCHANT A**:
  - username: `merchantA`
  - password: `merchantA123!`
- **MERCHANT B**:
  - username: `merchantB`
  - password: `merchantB123!`

Authentication style (Phase 1): **HTTP Basic**.

## Phase 1 endpoints
- `GET /health` (public)
- `GET /me` (authenticated; returns username + authorities)
- `GET /admin/ping` (ADMIN-only; role test)
- `POST /products` (MERCHANT-only; creates a product for the current merchant)
- `GET /products/{id}` (MERCHANT-only; merchant-scoped)
- `POST /skus` (MERCHANT-only; creates a SKU for a merchant-scoped product)
- `GET /skus/{id}` (MERCHANT-only; merchant-scoped)
- `POST /batch/import/skus/csv` (MERCHANT-only; all-or-nothing)
- `POST /batch/import/skus/xlsx` (MERCHANT-only; all-or-nothing)
- `GET /batch/export/skus/csv` (MERCHANT-only)
- `GET /notifications` (MERCHANT-only; merchant-scoped)
- `GET /notifications/subscriptions` / `PUT /notifications/subscriptions` (MERCHANT-only; per-event-type delivery preferences)
- `GET /inventory/logs` (MERCHANT-only; paginated inventory change history)
- `POST /attachments` (MERCHANT-only; JPG/PNG ≤ 2MB; magic-byte validation; SHA-256 dedup per merchant)
- `GET /api/buyer/catalog/products` / `GET /api/buyer/catalog/summary` (BUYER-only; read-only catalog)

## Phase 3 (Messaging) endpoints
- WebSocket STOMP endpoint: `GET /ws`
- Publish: `/app/messages.sendText`, `/app/messages.sendImage`, `/app/sessions.create` (session creation over STOMP mirrors `POST /sessions`)
- Subscribe (merchant-scoped): `/topic/messages.{merchantId}.{sessionId}` and `/topic/sessions.{merchantId}.lifecycle` (session create events when using STOMP)
- REST: `GET /sessions/{sessionId}/messages` (paginated history with read/recall state), `GET /sessions/{sessionId}/messages/{messageId}`

## Phase 4 (Cooking & Achievements) endpoints
- `POST /cooking/processes` (MERCHANT-only; start a process)
- `POST /cooking/checkpoint` (MERCHANT-only; persist step/status for resumption)
- `GET /cooking/processes/{id}` (MERCHANT-only; fetch persisted state)
- `POST /achievements` (MERCHANT-only; required: responsiblePerson, conclusion)
- `POST /achievements/{id}/attachments` (MERCHANT-only; creates v1, v2, … append-only)

## Phase 5 (Reporting & Dual Approval) endpoints
- `POST /api/admin/approvals/request` (ADMIN-only; create PENDING request)
- `POST /api/admin/approvals/{id}/execute` (ADMIN-only; must be different admin)
- `POST /api/admin/approvals/{id}/reject` (ADMIN-only; must be different admin)
- `GET /api/admin/approvals/pending` (ADMIN-only)
- `GET /api/admin/reports/inventory/{merchantId}` (ADMIN-only; native SQL projection)

## Air-gapped / offline builds
Fully disconnected hosts still need **container base images** (e.g. `mysql:8.0`, `eclipse-temurin:17`) and **Maven dependencies** unless you preload them. Practical approach: on a connected machine run `docker pull` / `docker save` for required images, and `mvn dependency:go-offline` (or copy a populated `~/.m2/repository`) before transferring the project. Point Compose or Maven at those local artifacts when building.

## Verification
Run all tests:

```bash
./run_tests.sh
```

If `mvn` is on your `PATH`, the script runs **`mvn clean verify` on the host** (recommended on Windows: Testcontainers uses Docker Desktop’s named pipe). If Maven is not installed, it falls back to a `maven` Docker image and expects Docker daemon access (see `run_tests.sh` / `run_tests.ps1` env vars).

**Expected outcomes after a green run:** all unit and integration tests pass; the suite exercises role guards, tenant isolation, reporting, and attachment validation.

Integration tests share **one** MySQL container (`AbstractIntegrationTest`) so the Spring context’s JDBC URL stays valid for the whole suite (see `src/test/java/com/petsupplies/AbstractIntegrationTest.java`).

## Backup and restore
- Runbook: [`docs/backup-recovery.md`](docs/backup-recovery.md) (full restore, optional binlog PITR notes, **SHA-256** integrity checks on dump files).
- Scripts: [`backup/backup.sh`](backup/backup.sh) (dump + binlog snapshot), [`backup/restore.sh`](backup/restore.sh) (import a `.sql` dump; **destructive** to target schemas — use a fresh DB or verified dump).
- After restore, confirm the app with `GET /health` → `{"status":"OK"}` and optional row counts / `SELECT 1` as described in the runbook.

Windows PowerShell:

```powershell
.\run_tests.ps1
```

## Security hardening decisions (Phase 1)
- **Lockout**: username-based lockout after 5 failures for 15 minutes; persisted on `users.failed_attempts` and `users.locked_until` (restart-safe).
- **Error envelope**: deterministic JSON for validation/authn/authz/app errors.
- **Audit immutability**:
  - DB enforces append-only writes on `audit_logs` using triggers (`src/main/resources/db/migration/V2__audit_log_immutability_grants.sql`).
  - Flyway runs as a separate migrator user (`migrator_user`) to keep runtime privileges minimal.
- **Password hashing**: PBKDF2-HMAC-SHA256 (offline-friendly, no external services).

## Static audit evidence pointers (Phase 2)
- **Category depth enforcement (DB trigger)**: `src/main/resources/db/migration/V5__category_depth_triggers.sql`

## Audit & Requirement Mapping (proof guide)
- **Tenant isolation (merchant-scoped repository signatures)**:
  - `src/main/java/com/petsupplies/product/repo/`
  - `src/main/java/com/petsupplies/messaging/repo/` (merchant-scoped session/message lookups)
- **Category depth ≤ 4 (DB-enforced)**:
  - `src/main/resources/db/migration/V5__category_depth_triggers.sql`
- **Batch import all-or-nothing (CSV/XLSX)**:
  - `src/main/java/com/petsupplies/product/service/BatchImportService.java`
  - Tests: `src/test/java/com/petsupplies/BatchImportAtomicityTest.java`, `src/test/java/com/petsupplies/BatchImportAtomicityXlsxTest.java`
- **Inventory alerts (≤10 threshold) + merchant-scoped visibility**:
  - `src/main/java/com/petsupplies/product/service/InventoryService.java`
  - `src/main/java/com/petsupplies/notification/web/NotificationController.java`
- **Audit log immutability (append-only)**:
  - `src/main/resources/db/migration/V2__audit_log_immutability_grants.sql` (UPDATE/DELETE blocked by triggers)
- **Account lockout (5 failures / 15 min)**:
  - `src/main/java/com/petsupplies/user/service/UserLockoutService.java`
  - `src/main/java/com/petsupplies/user/service/DbUserDetailsService.java`
- **Dual approval (four-eyes governance)**:
  - Schema: `src/main/resources/db/migration/V8__reporting_and_approvals.sql`
  - API: `src/main/java/com/petsupplies/auditing/web/ApprovalController.java`
  - Logic: `src/main/java/com/petsupplies/auditing/service/PendingApprovalService.java`
  - Test: `src/test/java/com/petsupplies/DualApprovalIntegrationTest.java`
- **WebSocket topic security (no cross-tenant sniffing)**:
  - `src/main/java/com/petsupplies/messaging/security/TopicScopeInterceptor.java`
  - Test: `src/test/java/com/petsupplies/TopicScopeInterceptorTest.java`
- **Image SHA-256 dedup (≤2MB JPG/PNG)**:
  - `src/main/java/com/petsupplies/messaging/service/AttachmentService.java`
  - Test: `src/test/java/com/petsupplies/AttachmentDedupIntegrationTest.java`
- **180-day message retention + audit evidence**:
  - `src/main/java/com/petsupplies/scheduling/MessageRetentionTask.java`

## API spec
See `docs/api-spec.md`.

