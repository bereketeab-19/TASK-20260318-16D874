# Test Coverage & README Audit — Pet Supplies (Spring Boot monolith)

## Scope and Method

- **Audit mode:** static inspection only (no `mvn`, Docker, or Testcontainers execution for this document).
- **Sources reviewed:** `src/main/java` controller mappings, `src/test/java`, `README.md`, `run_tests.sh` / `run_tests.ps1`, `docs/api-spec.md` (spot checks).
- **Project type (from [README.md](README.md#L1)):** offline **backend monolith** (no frontend application in this repository).

---

## Backend Endpoint Inventory

**Source of truth:** Spring Web MVC annotations on `@RestController` / `@Controller` classes under `src/main/java/com/petsupplies/` (there is no single `routes/api.php`-style file).

**Convention note:** Paths are literal unless a class-level `@RequestMapping` prefixes them (e.g. `/api/buyer/catalog`, `/api/reviewer/reports`).

### HTTP API endpoints (discovered)

| # | Method | Path |
|---|--------|------|
| 1 | GET | `/health` |
| 2 | GET | `/me` |
| 3 | GET | `/admin/ping` |
| 4 | POST | `/api/merchant/events/order-status` |
| 5 | POST | `/api/merchant/events/review-outcome` |
| 6 | GET | `/sessions/{sessionId}/messages` |
| 7 | GET | `/sessions/{sessionId}/messages/{messageId}` |
| 8 | POST | `/sessions/{sessionId}/messages/{messageId}/recall` |
| 9 | POST | `/sessions/{sessionId}/messages/{messageId}/read` |
| 10 | POST | `/sessions/{sessionId}/messages/image` |
| 11 | GET | `/merchant/custom-reports` |
| 12 | POST | `/merchant/custom-reports` |
| 13 | GET | `/merchant/custom-reports/{id}` |
| 14 | PATCH | `/merchant/custom-reports/{id}` |
| 15 | DELETE | `/merchant/custom-reports/{id}` |
| 16 | POST | `/merchant/custom-reports/{id}/execute` |
| 17 | GET | `/api/reviewer/reports/inventory/{merchantId}` |
| 18 | GET | `/api/reviewer/reports/inventory/{merchantId}/drill-down` |
| 19 | GET | `/api/admin/reports/definitions` |
| 20 | GET | `/api/admin/reports/inventory/{merchantId}` |
| 21 | GET | `/api/admin/reports/inventory/{merchantId}/drill-down` |
| 22 | GET | `/api/admin/reports/inventory/{merchantId}/export.csv` |
| 23 | GET | `/api/admin/reports/inventory/{merchantId}/daily` |
| 24 | GET | `/notifications` |
| 25 | PATCH | `/notifications/{id}/read` |
| 26 | GET | `/notifications/subscriptions` |
| 27 | PUT | `/notifications/subscriptions` |
| 28 | GET | `/inventory/logs` |
| 29 | POST | `/achievements` |
| 30 | GET | `/achievements/{id}/export` |
| 31 | POST | `/achievements/{id}/attachments` (multipart) |
| 32 | POST | `/attachments` (multipart) |
| 33 | POST | `/sessions` |
| 34 | GET | `/sessions` |
| 35 | GET | `/sessions/{id}` |
| 36 | POST | `/cooking/processes` |
| 37 | POST | `/cooking/checkpoint` |
| 38 | POST | `/cooking/processes/{id}/timers` |
| 39 | POST | `/cooking/processes/{id}/steps` |
| 40 | POST | `/cooking/steps/{stepId}/complete` |
| 41 | POST | `/cooking/steps/{stepId}/reminder` |
| 42 | GET | `/cooking/processes/{id}` |
| 43 | GET | `/skus/{skuId}/attributes` |
| 44 | PUT | `/skus/{skuId}/attributes` |
| 45 | GET | `/attribute-definitions` |
| 46 | POST | `/attribute-definitions` |
| 47 | DELETE | `/attribute-definitions/{id}` |
| 48 | GET | `/merchant/settings` |
| 49 | PATCH | `/merchant/settings` |
| 50 | POST | `/batch/import/skus/csv` |
| 51 | POST | `/batch/import/skus/xlsx` |
| 52 | GET | `/batch/export/skus/csv` |
| 53 | GET | `/brands` |
| 54 | POST | `/brands` |
| 55 | DELETE | `/brands/{id}` |
| 56 | GET | `/skus` |
| 57 | POST | `/skus` |
| 58 | GET | `/skus/{id}` |
| 59 | PATCH | `/skus/{id}` |
| 60 | POST | `/skus/{id}/delist` |
| 61 | GET | `/categories` |
| 62 | POST | `/categories` |
| 63 | DELETE | `/categories/{id}` |
| 64 | POST | `/products` |
| 65 | GET | `/products` |
| 66 | GET | `/products/{id}` |
| 67 | PATCH | `/products/{id}` |
| 68 | POST | `/products/{id}/delist` |
| 69 | GET | `/api/buyer/catalog/products` |
| 70 | GET | `/api/buyer/catalog/summary` |
| 71 | GET | `/technique-cards` |
| 72 | POST | `/technique-cards` |
| 73 | GET | `/technique-cards/{id}` |
| 74 | PATCH | `/technique-cards/{id}` |
| 75 | DELETE | `/technique-cards/{id}` |
| 76 | PATCH | `/users/me/password` |
| 77 | PATCH | `/users/me/contact` |
| 78 | POST | `/api/admin/approvals/request` |
| 79 | POST | `/api/admin/approvals/{id}/execute` |
| 80 | POST | `/api/admin/approvals/{id}/reject` |
| 81 | GET | `/api/admin/approvals/pending` |

**Total HTTP endpoints listed:** **81**

### Non-HTTP / separate surface

- **WebSocket (STOMP):** `MessagingController` — `/app/messages.sendText`, `/app/messages.sendImage`, `/app/sessions.create`; broker topics `/topic/...`. Covered by **unit/interceptor** tests (e.g. `TopicScopeInterceptorTest`), not a full STOMP client E2E suite in `src/test`.

---

## Test Inventory Summary

| Category | Count / notes |
|----------|----------------|
| Test classes under `src/test/java` | 30 files (includes `AbstractIntegrationTest` base) |
| JUnit 5 + Spring Boot Test + MockMvc | Primary integration style |
| Testcontainers MySQL | Shared container in `AbstractIntegrationTest` |
| Pure unit / no Spring context | `PasswordPolicyTest`, `TopicScopeInterceptorTest`, `MessageRetentionTaskTest` (Mockito) |

**Representative integration tests:** `SecurityIntegrationTest`, `DualApprovalIntegrationTest`, `CustomReportIntegrationTest`, `BatchImportAtomicityTest`, `BuyerCatalogIntegrationTest`, `MessagingMessagesQueryIntegrationTest`, `NotificationSubscriptionEnforcementIntegrationTest`, etc.

---

## API Test Mapping Table (representative)

| Endpoint | Covered (static evidence) | Test type | Test files / notes |
|----------|---------------------------|-----------|-------------------|
| GET `/health` | indirect | — | Not dedicated in grep; app starts in `@SpringBootTest` |
| GET `/me` | yes | MockMvc HTTP | `SecurityIntegrationTest` |
| GET `/admin/ping` | yes | MockMvc HTTP | `SecurityIntegrationTest`, `MerchantRoleGuardIntegrationTest` |
| POST `/api/merchant/events/*` | yes | MockMvc HTTP | `BusinessEventNotificationIntegrationTest`, `NotificationSubscriptionEnforcementIntegrationTest` |
| GET/POST `/sessions/.../messages*` | yes | MockMvc HTTP | `MessagingMessagesQueryIntegrationTest`, `MessagingRecallAndNotificationAuthzIntegrationTest`, `MessagingImageHttpIntegrationTest` |
| `/merchant/custom-reports/*` | yes | MockMvc HTTP | `CustomReportIntegrationTest` |
| `/api/reviewer/reports/inventory/*` | partial | MockMvc HTTP | `SecurityIntegrationTest` (reviewer OK / buyer 403); drill-down success path not clearly asserted |
| `/api/admin/reports/*` | partial | MockMvc HTTP | `ReportingReadAuditIntegrationTest` (**definitions**); buyer 403 on inventory summary in `SecurityIntegrationTest`; **admin inventory drill-down / export.csv / daily** lack obvious **200** MockMvc tests in static scan |
| `/notifications*` | yes | MockMvc HTTP | Multiple notification tests |
| GET `/inventory/logs` | yes | MockMvc HTTP | `InventoryLogIntegrationTest`, role guard |
| `/achievements*` | partial | Mixed | `AchievementValidationTest` (HTTP validation); `AchievementAttachmentVersioningTest` exercises **service** + multipart via service, not always REST |
| POST `/attachments` | yes | MockMvc / service | `AttachmentMagicBytesIntegrationTest`, `MessagingImageHttpIntegrationTest`; `AttachmentDedupIntegrationTest` uses `AttachmentService` + temp dir |
| `/cooking/*` | partial | MockMvc HTTP | `CookingResumptionTest` (subset of routes); not every sub-route may be hit |
| `/api/buyer/catalog/*` | yes | MockMvc HTTP | `BuyerCatalogIntegrationTest` |
| `/technique-cards/*` | yes | MockMvc HTTP | `TechniqueCardIntegrationTest` |
| `/api/admin/approvals/*` | yes | MockMvc HTTP | `DualApprovalIntegrationTest` |
| `/batch/*` | yes | MockMvc HTTP | `BatchImportAtomicityTest`, `BatchImportAtomicityXlsxTest` |
| PATCH `/skus/{id}` | yes | MockMvc HTTP | `InventoryLogIntegrationTest`, `SkuUpdateValidationIntegrationTest` |
| `/products`, `/skus` tenant isolation | yes | MockMvc HTTP | `ProductHeistIntegrationTest` |
| DELETE `/categories/{id}` | partial | MockMvc HTTP | `CategoryDeletionRequiresApprovalIntegrationTest` (expects **403**, not approval flow execute) |
| Reporting aggregates | yes | Service-only | `ReportingAggregationIntegrationTest` (**no HTTP**) |
| Contact encryption | yes | Repository | `ContactEncryptionIntegrationTest` (**no HTTP**) |
| Message anti-spam | yes | Service | `MessagingAntiSpamIntegrationTest` (**no HTTP**; `MessageService` directly) |

**Legend:** “partial” = role/negative tests or service-only coverage; may miss full success-path contract tests for every parameter combination.

---

## API Test Classification

### 1) True no-mock HTTP (MockMvc + Testcontainers DB)

Representative: `DualApprovalIntegrationTest`, `CustomReportIntegrationTest`, `SecurityIntegrationTest`, `BuyerCatalogIntegrationTest`, `MessagingMessagesQueryIntegrationTest`.

### 2) HTTP with test doubles / special config

- **@TempDir / DynamicPropertySource** for attachment dirs: e.g. `MessagingImageHttpIntegrationTest`, `AttachmentDedupIntegrationTest`, achievement tests.
- **@Primary Clock** for lockout: `SecurityIntegrationTest`, `MessagingAntiSpamIntegrationTest` (mutable clock).
- **@Sql** seed data: `MessagingRecallAndNotificationAuthzIntegrationTest`.

### 3) Non-HTTP (service/repository/unit)

- `ReportingAggregationIntegrationTest` — `ReportingService` / `ProductService` / `InventoryService`.
- `MessagingAntiSpamIntegrationTest` — `MessageService` + repository counts.
- `AchievementAttachmentVersioningTest` — `AchievementService` API.
- `ContactEncryptionIntegrationTest` — JPA round-trip on `User`.
- `PasswordPolicyTest`, `TopicScopeInterceptorTest` — unit.
- `MessageRetentionTaskTest` — Mockito unit test for scheduled purge + audit call.

---

## Mock / Fake Detection (Strict)

| Mechanism | Location |
|-----------|----------|
| Mockito (`@Mock`, `@InjectMocks`) | `MessageRetentionTaskTest` |
| `MockMultipartFile` | Attachment / achievement / messaging tests |
| `Storage::fake` (Laravel-style) | **N/A** — Java stack uses real temp dirs or Testcontainers DB |
| Spring `@MockBean` / `@Primary` test beans | Mutable `Clock` in selected tests |

---

## Coverage Summary (approximate static metrics)

| Metric | Value | Notes |
|--------|-------|--------|
| HTTP endpoints enumerated | 81 | See table above |
| Endpoints with **any** automated test signal (HTTP **or** service/repo/unit) | **High** | Many domains have at least one test |
| Endpoints with **dedicated MockMvc 200** success-path evidence | **< 100%** | Admin reporting drill/export/daily; some catalog/attribute/brand routes under-mapped in static scan |
| “True” full-stack E2E | **N/A** | No browser/Vue app in repo |

**Computed-style metrics (illustrative, not executed):**

- **HTTP route touch coverage (broad):** ~75–85% (estimated from mapping vs. test grep).
- **Strict “happy-path MockMvc for every endpoint” coverage:** **Not achieved** in static review.

---

## Unit Test Summary (Backend)

**Present:** password policy, WebSocket topic interceptor, retention task behavior (mocked), plus domain integration tests.

**Gaps called out in static review:**

- Not every **policy class** or **@ControllerAdvice** branch has a dedicated unit test.
- **WebSocket message handlers** are not covered by an in-repo STOMP client integration test (interceptor unit test only).

---

## Frontend Unit Tests

**Not applicable** — this repository does not contain a `frontend/` SPA. README describes a **monolith API** consumed by unspecified clients.

---

## README Audit

**Target:** [README.md](README.md)

### Hard gates

| Check | Verdict | Evidence |
|-------|---------|----------|
| Formatting / structure | **Pass** | Sections, lists, code fences |
| Startup instructions | **Pass** | `docker compose up --build` [README.md](README.md#L12) |
| Access / ports | **Pass** | TLS proxy `https://localhost:8443/` [README.md](README.md#L19) |
| Verification | **Pass** | `run_tests.sh` / `mvn clean verify` [README.md](README.md#L83) |
| Credentials | **Pass** | Default users/passwords [README.md](README.md#L21) |
| Environment rules | **Pass** | `APP_CRYPTO_HEX_KEY`, Docker + Java 17 |

### Medium / low issues

1. **“Fullstack” wording:** README positions the product as a monolith API; there is **no** first-party frontend in-repo — clarify if external UI is out of scope.
2. **Air-gapped builds:** documented [README.md](README.md#L80); still relies on pre-pulled images/Maven cache (honest).

### README verdict

**Pass**

---

## Test Coverage Score (0–100)

**Suggested score: 82**

### Rationale

- **Strengths:** Broad integration coverage across security, approvals, messaging, notifications, batch import, buyer catalog, custom reports, and several edge-case tests; Testcontainers-backed realism; scripts for CI/local verify.
- **Deductions:** Not every HTTP route has a clear **success-path** MockMvc test; some behavior is validated via **services** only; WebSocket messaging lacks full transport-level integration tests; no frontend test suite (N/A).

---

## Key Gaps

1. **Admin reporting:** `GET /api/admin/reports/inventory/{merchantId}/drill-down`, `export.csv`, `daily` — no strong static evidence of **ADMIN** **200** MockMvc tests (403/buyer tests exist for related surfaces).
2. **Secondary REST surfaces:** Brands, attribute definitions, merchant settings, some cooking sub-routes — coverage is uneven vs. “one test per route.”
3. **WebSocket/STOMP:** Interceptor tested; **no** automated client subscription/send/receive suite in-repo.
4. **Achievement attachment versioning:** Strong service test; REST multipart path overlap is partial.

---

## Confidence and Assumptions

- **Confidence:** **Medium–high** for inventory completeness (controllers grep); **medium** for per-route sufficiency without running tests.
- **Assumption:** Actuator endpoints (if enabled) are out of scope for “business API” unless exposed under `management.*`.

---

## Final Verdicts

| Audit | Verdict |
|-------|---------|
| **Test coverage audit** | **PASS WITH CAVEATS** — strong integration breadth; not full per-route HTTP happy-path coverage; no SPA tests (N/A). |
| **README audit** | **PASS** |

---

## Combined Final Verdicts

- **Test coverage audit:** **PASS WITH CAVEATS**
- **README audit:** **PASS**

---

*Generated for repository: Pet Supplies offline monolith (`TASK-20260318-16D874`). Static audit only — re-run after major route or test refactors.*
