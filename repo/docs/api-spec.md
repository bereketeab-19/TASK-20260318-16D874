# API specification (offline monolith)

Auth style: **HTTP Basic** (Phase 1 baseline). All endpoints are under the Nginx TLS proxy at `https://localhost:8443`.

## Conventions
- **TLS**: self-signed cert; use `curl -k` for local verification.
- **Error envelope**: JSON errors are standardized via `GlobalExceptionHandler` and security handlers.
- **Tenant scope**: merchant-facing endpoints derive `merchantId` from the authenticated principal (`SecurityUser.merchantId`) and enforce it in repository signatures.
- **Roles (method security)**:
  - **Merchant business APIs** (`/products`, `/skus`, `/sessions`, `/notifications`, cooking, achievements, attachments, …) require **`ROLE_MERCHANT`**.
  - **Buyer catalog** (`/api/buyer/catalog/**`) requires **`ROLE_BUYER`**.
  - Admin / reviewer routes keep their existing role requirements.

## Health
- `GET /health` (public)
  - 200: `{"status":"OK"}`

## Auth / identity
- `GET /me` (authenticated)
  - 200: `{"username":"...","authorities":["ROLE_..."],"merchantId":"mrc_A" | null}`

## Buyer catalog (read-only, `ROLE_BUYER`)
- `GET /api/buyer/catalog/products?page=0&size=20`
  - Paginated list of **active** products across all merchants (bounded `size` ≤ 100).
  - Response: `content`, `totalElements`, `totalPages`, `number`.
- `GET /api/buyer/catalog/summary`
  - `activeProductCount`, `merchantCountWithActiveProducts`.

## Product & inventory (merchant-scoped, `ROLE_MERCHANT`)
- `GET /products`
  - Lists **active** products for the merchant (`active: true`)
- `POST /products`
  - Body: `{"productCode":"P1","name":"Product 1"}`
  - 200: `{"id":1,"productCode":"P1","name":"Product 1"}`
- `GET /products/{id}`
  - 200 or 404 (404 if not in merchant scope); includes `active`
- `PATCH /products/{id}`
  - Body (all optional): `{"name":"...","categoryId":1,"brandId":2}`
- `POST /products/{id}/delist`
  - Sets `active` to false (soft delist)

### Categories & brands (merchant-scoped)
- `GET /categories` / `POST /categories`
- `DELETE /categories/{id}` — **403** (destructive delete is blocked for merchants). Use admin dual-approval `ACTIVE_CATEGORY_DELETION` (`/api/admin/approvals/request`) when the category has no products or children.
- `GET /brands` / `POST /brands` / `DELETE /brands/{id}` (delete when no product references)

### SKU attributes (merchant-scoped)
- `GET /attribute-definitions` — list definitional attributes (code/label), e.g. `SIZE`, `FLAVOR`, `AGE_GROUP`
- `POST /attribute-definitions` — body `{"code":"SIZE","label":"Size"}`
- `DELETE /attribute-definitions/{id}`
- `GET /skus/{skuId}/attributes` — list values for a SKU
- `PUT /skus/{skuId}/attributes` — body `{"attributeDefinitionId":1,"value":"Large"}`

### Merchant settings (merchant-scoped)
- `GET /merchant/settings` — `lowStockThreshold` (default **10** when unset)
- `PATCH /merchant/settings` — body `{"lowStockThreshold":8}` (used for low-stock notifications)

### SKUs (merchant-scoped)
- `GET /skus` — lists **active** SKUs
- `POST /skus`
  - Body: `{"productId":1,"barcode":"BAR1","stockQuantity":5}`
- `GET /skus/{id}` — includes `productId`, `active`
- `PATCH /skus/{id}` — body (optional): `{"stockQuantity":3,"barcode":"BAR2"}`
- `POST /skus/{id}/delist` — sets `active` false (excluded from inventory aggregates)

### Inventory log (merchant-scoped, append-only history)
- `GET /inventory/logs?page=0&size=20` — paginated stock-change history for the merchant (`eventType`, `quantityBefore`/`quantityAfter`, `referenceKind`, `createdAt`).
- `GET /inventory/logs?skuId={id}` — same, filtered to one SKU.
- Rows are written on SKU create, stock updates, delist, and bulk stock paths (see `inventory_logs` table).

### Batch import/export (all-or-nothing)
- `POST /batch/import/skus/csv` (multipart form-data, part `file`)
  - CSV header: `productCode,productName,barcode,stockQuantity`
  - 200: `{"rowsRead":10,"productsCreated":...,"skusCreated":...}`
  - 400: validation/constraint error; **no partial writes**

- `POST /batch/import/skus/xlsx` (multipart form-data, part `file`)
  - Sheet0 header row: `productCode | productName | barcode | stockQuantity`
  - 200 / 400 same semantics as CSV

- `GET /batch/export/skus/csv` (merchant-scoped)
  - 200: CSV text

### Notifications (merchant-scoped)
- `GET /notifications`
  - Returns latest notifications for the current merchant (`deliveredAt`, `eventType`, `readAt`, …). Event types include low-stock, **`ORDER_STATUS`**, **`REVIEW_OUTCOME`**, **`REPORT_HANDLING`** (from business hooks and scheduled reporting).
- `PATCH /notifications/{id}/read`
  - Marks a notification as read for the merchant scope
- `POST /api/merchant/events/order-status` — body `{"orderRef":"ORD-1","status":"SHIPPED"}` (emits `ORDER_STATUS`)
- `POST /api/merchant/events/review-outcome` — body `{"reviewRef":"REV-1","outcome":"APPROVED"}` (emits `REVIEW_OUTCOME`)

## Messaging (Phase 3)

### WebSocket STOMP
- Endpoint: `/ws`
- Application prefix: `/app`
- Broker prefix: `/topic`

#### Publish
- Destination: `/app/messages.sendText`
- Payload:

```json
{"sessionId":123,"content":"Hello"}
```

- Destination: `/app/messages.sendImage` — image message (upload file via `POST /attachments` first)
- Payload:

```json
{"sessionId":123,"attachmentId":456,"caption":"optional"}
```

#### Subscribe (merchant-scoped)
- `/topic/messages.{merchantId}.{sessionId}`
  - Example: `/topic/messages.mrc_A.123`
  - Server blocks cross-tenant subscriptions via `TopicScopeInterceptor`

### HTTP sessions (merchant-scoped)
- `POST /sessions` — create IM session; returns `id`, `createdAt`
- `GET /sessions` — latest 50 sessions for merchant
- `GET /sessions/{id}` — fetch one session in tenant scope

### HTTP messages (merchant-scoped)
- `POST /sessions/{sessionId}/messages/{messageId}/recall` (sender only; clears content)
- `POST /sessions/{sessionId}/messages/{messageId}/read` (read receipt)
- `POST /sessions/{sessionId}/messages/image` — body `{"attachmentId":456,"caption":"optional"}` (same semantics as STOMP image send)

### Attachments (REST, merchant-scoped)
- `POST /attachments` (multipart form-data, part `file`)
  - Constraints: JPG/PNG only, ≤ 2MB; **file bytes must match** JPEG (`FF D8 FF` prefix) or PNG (standard 8-byte signature) even if `Content-Type` claims an image type.
  - Dedup: SHA-256 reuse **per merchant** (`merchant_id` + hash), not globally across tenants

## Cooking (Phase 4)
- `POST /cooking/processes`
  - 200: creates a new process for the current user
- `POST /cooking/processes/{id}/steps`
  - Body: `{"instruction":"Dice onions"}` — append ordered step to process (refreshes process `lastCheckpointAt`)
- `POST /cooking/steps/{stepId}/complete`
  - Marks step completed (`completedAt`); also refreshes process `lastCheckpointAt` (same as periodic autosave).
- `POST /cooking/steps/{stepId}/reminder`
  - Body: `{"reminderAt":"2026-04-21T12:00:00Z"}` — schedules `reminderFireAt` (checkpoint/resumption aid); also refreshes process `lastCheckpointAt`.
- `POST /cooking/processes/{id}/timers`
  - Body: `{"label":"Boil","durationSeconds":300}`
- `POST /cooking/checkpoint`
  - Body: `{"processId":1,"currentStepIndex":3,"status":"PAUSED"}`
- `GET /cooking/processes/{id}`
  - Includes `steps[]`, `timers[]` (`remainingSeconds` computed)

### Technique cards (merchant-scoped, `ROLE_MERCHANT`)
- `GET /technique-cards?tag=prep` — paginated list; optional **tag** filter (matches normalized tag names).
- `POST /technique-cards` — body `{"title":"...","body":"...","tags":["prep","knife"]}` (tags optional; created per merchant).
- `GET /technique-cards/{id}` / `PATCH /technique-cards/{id}` / `DELETE /technique-cards/{id}` — tenant-scoped; other merchants receive **404**.

## Achievements (Phase 4)
- `POST /achievements`
  - Body requires: `responsiblePerson`, `conclusion`
- `GET /achievements/{id}/export?format=...`
  - Query `format` (optional, default `achievement_certificate_v1`):
    - `achievement_certificate_v1` — completion certificate JSON.
    - `achievement_assessment_form_v1` — structured assessment form (sections + fields).
- `POST /achievements/{id}/attachments`
  - Creates new immutable versions v1, v2, … (append-only)

## User profile (authenticated)
- `PATCH /users/me/password` — body `{"newPassword":"..."}` (letters + digits, length ≥ 8)
- `PATCH /users/me/contact` — body `{"contactPhone":"+15551234567"}` (stored encrypted at rest)

## Governance / approvals (Phase 5)
Admin-only endpoints:
- `POST /api/admin/approvals/request`
  - Body:

```json
{
  "operationType": "PERMISSION_CHANGE",
  "payload": { "targetUsername": "buyer", "newRole": "REVIEWER" }
}
```

- For `newRole`: **`MERCHANT`** — payload **must** include `"merchantId":"mrc_..."` (tenant binding). For any other role, `merchant_id` is cleared server-side (must be absent or ignored).
- Constraint violations (e.g. invalid role/merchant pairing) return **400** with a generic constraint message.

Additional `operationType` values (all require a second admin to execute):
- `SYSTEM_CONFIG_UPDATE` — `payload`: `{"configKey":"feature.x","configValue":"on"}`
- `DATA_WIPE_OR_RESTORE` — offline-safe: `{"action":"REGISTER_BACKUP_VERIFICATION","backupLabel":"nightly-2026-04-01"}` (records verification metadata; no data destruction)
- `ACTIVE_CATEGORY_DELETION` — `payload`: `{"merchantId":"mrc_A","categoryId":12}` (only when category has no products or children)

- `POST /api/admin/approvals/{id}/execute`
  - Must be a **different admin** than the requester
- `POST /api/admin/approvals/{id}/reject`
  - Must be a **different admin** than the requester
- `GET /api/admin/approvals/pending`

## Reporting (Phase 5)

### Admin (read/write reporting configuration views)
- `GET /api/admin/reports/definitions` — persisted indicator definitions (metadata for report center)
- `GET /api/admin/reports/inventory/{merchantId}` — live aggregate: total active SKUs + sum of stock
- `GET /api/admin/reports/inventory/{merchantId}/drill-down?page=0&size=20` — paginated SKU lines (product linkage)
- `GET /api/admin/reports/inventory/{merchantId}/export.csv` — CSV export of active SKUs
- `GET /api/admin/reports/inventory/{merchantId}/daily?limit=30` — recent rows from scheduled daily inventory snapshots

### Reviewer (read-only oversight)
- `GET /api/reviewer/reports/inventory/{merchantId}` — same summary shape as admin inventory summary
- `GET /api/reviewer/reports/inventory/{merchantId}/drill-down` — same pagination as admin drill-down

### Merchant custom reports (`ROLE_MERCHANT`)
Definitions are stored per merchant; execution maps JSON templates onto existing inventory reporting.

- `GET /merchant/custom-reports` — list saved definitions.
- `POST /merchant/custom-reports` — body:
  - `name` (required), `description` (optional), `definitionJson` (required JSON string), `scheduleCron` / `scheduleTimezone` (optional).
  - `definitionJson` must include `"template"`. Supported templates:
    - `INVENTORY_SUMMARY` — `{ "template": "INVENTORY_SUMMARY" }`
    - `INVENTORY_DRILL_DOWN` — `{ "template": "INVENTORY_DRILL_DOWN", "page": 0, "size": 20 }`
    - `INVENTORY_MOVEMENT_TIMELINE` — **time dimension** on `inventory_logs`: `{ "template": "INVENTORY_MOVEMENT_TIMELINE", "from": "2026-01-01", "to": "2026-04-01" }` (dates **yyyy-MM-dd**, UTC day buckets; `to` must be strictly after `from`). Response includes `series[]` with `day`, `movementUnits`, `eventCount`.
- `GET /merchant/custom-reports/{id}` / `PATCH /merchant/custom-reports/{id}` / `DELETE /merchant/custom-reports/{id}`
- `POST /merchant/custom-reports/{id}/execute` — 200: `{ "reportId", "name", "data": { ... } }`; inactive reports → **400**.

