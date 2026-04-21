# Static Delivery Acceptance and Project Architecture Audit  
**Pet Supplies Trading and Cooking Practice Management Platform (Backend)**

---

## 1. Verdict

- **Overall conclusion:** **Fail** (static-only)

**Basis (summary):** The repository is a substantial Spring Boot monolith with many prompt-aligned domains (auth lockout, catalog/SKU, messaging, notifications, reporting surfaces, cooking assistance, achievements, dual approval, TLS/backup documentation). However, several **explicit** prompt items are still missing or only partially modeled at the code/schema level—notably an **Inventory Log** resource, **permission-change execution** that keeps `users.merchant_id` consistent with `role`, **achievement export** breadth (certificate vs assessment form), **reporting-center dimensional semantics** beyond inventory-centric aggregates, and **immediate checkpoint persistence on cooking step mutations** as distinct from the 30s scheduler. These are material requirement-fit gaps, not cosmetic issues.

---

## 2. Scope and Static Verification Boundary

| Category | Included |
|----------|----------|
| **Reviewed** | `README.md`, `docs/api-spec.md`, `docs/design.md`, `docs/backup-recovery.md`, `docker-compose.yml`, `nginx/`, `backup/`, `pom.xml`, `src/main/java/**`, `src/main/resources/**` (incl. Flyway, `logback-spring.xml`), `src/test/java/**`, `run_tests.sh` |
| **Not reviewed** | Live TLS handshakes, actual DB behavior under load, real scheduler clock alignment in deployment, backup/restore execution outcomes |
| **Intentionally not executed (this audit pass)** | Application start, Docker, automated tests, network calls — per audit rules; conclusions below are from **reading** sources only |
| **Manual verification required** | WebSocket auth propagation in real browser/client; exact cron firing for daily reports; end-to-end backup retention vs prompt “hourly + 30 days”; any behavior that depends on JVM/OS timing |

---

## 3. Repository / Requirement Mapping Summary

**Prompt core:** Offline monolith; four roles (admin, merchant, buyer, reviewer); HTTP auth with password policy and lockout; product/SKU/category/brand/attributes; **inventory log** as named domain entity; IM with WS, anti-spam, attachments; notifications; **reporting center** with indicators, dimensions, custom reports, drill-down, export, scheduled generation; cooking process/step/timer/reminder with checkpoints **every 30s or on step changes**; **technique cards**; achievements with versioning and **certificate + assessment-form** exports; TLS, field encryption, dual approval, immutable audit logs, backup strategy.

**Implementation mapped:** Strong coverage for auth (`SecurityConfig`, `UserLockoutService`), product/inventory **stock on SKU** (`V4__product_inventory.sql`), messaging (`MessageService`, `AntiSpamCache`), notifications, inventory **aggregates** and daily snapshots (`ReportingRepository`, scheduled tasks), cooking processes and **30s** `CookingAutoCheckpointTask`, technique cards (`V13`, `TechniqueCardController`), custom report **definitions** executing inventory templates (`CustomReportService`), buyer catalog read API, merchant `@PreAuthorize`, audit append-only (`V2`), dual-approval workflow (`PendingApprovalService`).

**Gaps vs prompt (high level):** No first-class **Inventory Log** table/API; `PERMISSION_CHANGE` does not coordinate **merchant_id** with role; achievement **export** is single-format; reporting **dimensions** beyond inventory not evidenced; step APIs do not bump process checkpoint fields—only periodic task + explicit `checkpoint` API.

---

## 4. Section-by-section Review

*Scoring uses: Pass / Partial Pass / Fail / Not Applicable / Cannot Confirm Statistically*

### Hard Gates

#### 1.1 Documentation and static verifiability
- **Conclusion:** **Pass**
- **Rationale:** README documents compose, credentials, key endpoints, test command (`run_tests.sh:4`), backup pointers (`README.md:90`); structure matches layered Java packages.
- **Evidence:** `README.md:7`, `README.md:77`, `docs/api-spec.md:1`

#### 1.2 Material deviation from Prompt
- **Conclusion:** **Fail**
- **Rationale:** Named prompt models/behaviors are absent or narrowed (Inventory Log; dual semantics for achievement templates; dimensional reporting; step-triggered checkpoint).
- **Evidence:** no `InventoryLog` in `src/main/java` or migrations (search); `PendingApprovalService.java:164-182`; `AchievementService.java:81-94`; `CustomReportService.java:25-26`; `CookingService.java:108-157` vs `CookingAutoCheckpointTask.java:26-35`

### Delivery Completeness

#### 2.1 Core explicit requirements coverage
- **Conclusion:** **Partial Pass**
- **Rationale:** Most domains exist; explicit **Inventory Log** domain and full reporting-center dimension semantics are not implemented as stated in the prompt.
- **Evidence:** `V4__product_inventory.sql` (SKUs, not append-only log entity); `ReportingRepository.java:16-21`

#### 2.2 End-to-end 0-to-1 deliverable
- **Conclusion:** **Pass**
- **Rationale:** Full monolith: migrations, controllers, tests, compose, nginx—not a single-file demo.
- **Evidence:** `src/main/java/com/petsupplies/Application.java`, `src/main/resources/db/migration/V1__init.sql:1`

### Engineering and Architecture Quality

#### 3.1 Structure and module decomposition
- **Conclusion:** **Pass**
- **Rationale:** Domain packages (`product`, `messaging`, `reporting`, `cooking`, …), Flyway versioning, separation of concerns.
- **Evidence:** package layout under `src/main/java/com/petsupplies/`

#### 3.2 Maintainability / extensibility
- **Conclusion:** **Partial Pass**
- **Rationale:** Extensible in places (custom report JSON templates) but governance and prompt-critical abstractions (inventory history, permission transition model) are underspecified relative to DB constraints.
- **Evidence:** `V3__merchant_context.sql:7-12`; `PendingApprovalService.java:164-182`

### Engineering Details and Professionalism

#### 4.1 Error handling, logging, validation, API design
- **Conclusion:** **Partial Pass**
- **Rationale:** `GlobalExceptionHandler`, validation on DTOs, JSON security handlers; **no** `LoggerFactory`/`getLogger` usage in `src/main/java` (grep empty), so operational logging is mostly framework defaults + DB audit—not structured application logs.
- **Evidence:** `logback-spring.xml:3-14`; grep `LoggerFactory` in `src/main/java` → none; `SecurityConfig.java:81-84`

#### 4.2 Real product vs demo
- **Conclusion:** **Pass**
- **Rationale:** Compose, TLS proxy, migrations, integration tests indicate production-shaped backend.
- **Evidence:** `docker-compose.yml`, `nginx/nginx.conf`

### Prompt Understanding and Requirement Fit

#### 5.1 Business goal and constraints fit
- **Conclusion:** **Partial Pass**
- **Rationale:** Core offline monolith and role separation are addressed; several **named** prompt constraints (Inventory Log, assessment export, richer reporting, step checkpoint coupling) are incomplete.
- **Evidence:** same as §1.2 material gaps

### Aesthetics (frontend)

#### 6.1
- **Conclusion:** **Not Applicable** (backend-only repository)
- **Evidence:** `README.md:1`

---

## 5. Issues / Suggestions (Severity-Rated)

*Blocker / High first; root-cause merged*

### High

**H1 — Dual-approval `PERMISSION_CHANGE` ignores merchant tenancy rule**

| Field | Content |
|-------|---------|
| **Conclusion** | **Fail** — role updates without coordinated `merchant_id` updates |
| **Evidence** | `PendingApprovalService.java:164-182` (only `user.setRole`); `V3__merchant_context.sql:7-12` (`chk_users_merchant_id_role`) |
| **Impact** | Transitions such as MERCHANT→non-MERCHANT or reverse can violate DB check or leave inconsistent tenant binding; governance flow unreliable for real role changes. |
| **Minimum fix** | Extend payload with optional `merchantId`; validate transitions; set/clear `merchant_id` atomically with `role`; map constraint violations to 400; add integration tests for merchant-affecting transitions. |

**H2 — Prompt `Inventory Log` model absent**

| Field | Content |
|-------|---------|
| **Conclusion** | **Fail** |
| **Evidence** | No `inventory_logs` / `InventoryLog` in migrations under `src/main/resources/db/migration/` or `src/main/java` (static search). |
| **Impact** | No first-class auditable history of stock changes as a domain resource (prompt lists it alongside Product, SKU, etc.). |
| **Minimum fix** | Add schema + entity + write paths on SKU stock mutations / import / delist; read API if required by prompt. |

**H3 — Achievement export: single template vs certificate + assessment forms**

| Field | Content |
|-------|---------|
| **Conclusion** | **Fail** |
| **Evidence** | `AchievementService.java:81-94` (`format` fixed to `achievement_certificate_v1` only) |
| **Impact** | Prompt requires template exports for **completion certificates and assessment forms**; only one shape is implemented. |
| **Minimum fix** | Add second format (e.g. `achievement_assessment_form_v1`) + query param or `format` field; version schema. |

### Medium

**M1 — Reporting center: inventory-heavy vs organization/time/business dimensions**

| Field | Content |
|-------|---------|
| **Conclusion** | **Partial Fail** |
| **Evidence** | `ReportingRepository.java:16-21` (inventory summary only in this repo interface); `CustomReportService.java:25-26`, `:159-168` (templates `INVENTORY_SUMMARY`, `INVENTORY_DRILL_DOWN`) |
| **Impact** | Prompt asks for aggregates by broader dimensions; implementation centers on SKU/inventory lines. |
| **Minimum fix** | Add metric families / dimension parameters in definitions and SQL or projections. |

**M2 — Cooking: “30s or on step changes” — step paths omit explicit checkpoint**

| Field | Content |
|-------|---------|
| **Conclusion** | **Partial Fail** |
| **Evidence** | `CookingAutoCheckpointTask.java:26-35` (30s); `CookingService.java:108-157` (`addStep` / `completeStep` / `scheduleStepReminder` do not set `lastCheckpointAt` on `CookingProcess`) |
| **Impact** | Prompt ties resumption to step changes as well as periodic save; step mutations do not refresh checkpoint fields on the process. |
| **Minimum fix** | Update `CookingProcess.lastCheckpointAt`/`updatedAt` inside step mutation methods (or shared helper). |

**M3 — Application-level logging**

| Field | Content |
|-------|---------|
| **Conclusion** | **Partial Fail** |
| **Evidence** | `logback-spring.xml:3-14` exists; no `LoggerFactory` in `src/main/java` (static grep) |
| **Impact** | Troubleshooting relies heavily on defaults/audit DB; less structured operational insight. |
| **Minimum fix** | Add SLF4J at approval execution, import failures, schedulers (no secrets). |

### Low

**L1 — Backup frequency vs prompt**

| Field | Content |
|-------|---------|
| **Conclusion** | **Cannot Confirm Statistically** (static) |
| **Evidence** | `backup/crontab`, `backup/backup.sh` — compare to prompt “daily full + hourly incremental, 30 days” |
| **Impact** | May align or differ; requires reading crontab/retention scripts vs prompt. |
| **Minimum fix** | Align scripts/docs with stated RPO/RTO or document deviation. |

---

## 6. Security Review Summary

| Area | Conclusion | Evidence / reasoning |
|------|------------|----------------------|
| Authentication entry points | **Pass** | HTTP Basic + custom provider + lockout hooks: `SecurityConfig.java:33-73`, `SecurityConfig.java:77-89` |
| Route-level authorization | **Pass** | Authenticated default; `/health` permit; method security used broadly (`@EnableMethodSecurity` `SecurityConfig.java:25`) |
| Object-level authorization | **Partial Pass** | Merchant-scoped repos common; **governance** weakness on `PERMISSION_CHANGE` + `merchant_id` (`PendingApprovalService.java:164-182`, `V3__merchant_context.sql:7-12`) |
| Function-level authorization | **Pass** | Dual-approval self-block: `PendingApprovalService.java:93-101`; admin approval API: `ApprovalController.java:18-20` |
| Tenant / user isolation | **Pass** (with caveat) | Merchant id on principal + scoped queries; caveat: role change path above |
| Admin / internal / debug protection | **Pass** | Admin routes require admin role in controllers under `auditing`/`reporting`; no open debug route identified in static scan |

---

## 7. Tests and Logging Review

| Dimension | Conclusion | Evidence |
|-----------|------------|----------|
| Unit tests | **Pass (limited)** | `PasswordPolicyTest.java` under `src/test/java/com/petsupplies/core/validation/` |
| API / integration tests | **Partial Pass** | Many MockMvc + Testcontainers tests (`AbstractIntegrationTest.java:12`, `SecurityIntegrationTest.java:45`, `DualApprovalIntegrationTest.java:127`, `MerchantRoleGuardIntegrationTest.java`, etc.) |
| Logging / observability | **Partial Pass** | Masking pattern `logback-spring.xml:3-4`; no application `LoggerFactory` in main sources |
| Sensitive data in logs / responses | **Partial Pass** | Log masking regex; encrypted contact on `User` — **Cannot Confirm Statistically** for all runtime log shapes |

---

## 8. Test Coverage Assessment (Static Audit)

### 8.1 Test Overview

- **Unit + integration tests:** Yes — multiple `*Test.java` under `src/test/java/com/petsupplies/`.
- **Frameworks:** JUnit 5, Spring Boot Test, MockMvc, Testcontainers (`pom.xml:88-100` area).
- **Entry:** `run_tests.sh:4-6` runs `mvn clean verify` when `mvn` exists.
- **Documentation:** `README.md:77-81` references `./run_tests.sh`.

### 8.2 Coverage Mapping Table (risk-focused)

| Requirement / risk | Mapped test(s) | Key assertion | Coverage | Gap | Minimum addition |
|-------------------|----------------|---------------|----------|-----|------------------|
| 401 unauthenticated | `SecurityIntegrationTest.java:45-48` | `status().isUnauthorized()` | sufficient | — | — |
| 403 role boundaries | `SecurityIntegrationTest.java:51-78`, `MerchantRoleGuardIntegrationTest.java` | Forbidden on wrong role | basically covered | — | — |
| Lockout 5 / 15 min | `SecurityIntegrationTest.java:81-101` | loop + clock advance | sufficient | edge cases | optional unknown-user |
| Dual approval self-block | `DualApprovalIntegrationTest.java:147-150` | same admin 400 | sufficient | **merchant role transition** | tests for MERCHANT↔non-MERCHANT permission change |
| Tenant isolation (SKU) | `ProductHeistIntegrationTest.java` | cross-merchant 404 | basically covered | other entities | extend selectively |
| Batch import atomicity | `BatchImportAtomicityTest.java`, `BatchImportAtomicityXlsxTest.java` | malformed → no persist | sufficient | — | — |
| Attachment magic bytes | `AttachmentMagicBytesIntegrationTest.java` | bad signature 400 | basically covered | >2MB file | optional size test |
| Custom reports | `CustomReportIntegrationTest.java` | execute + tenant 404 | basically covered | non-inventory templates | when implemented |
| Inventory Log domain | — | — | **missing** | no tests (no feature) | tests when schema exists |
| Cooking step checkpoint | `CookingResumptionTest.java` | manual checkpoint | basically covered | step-change vs autosave | tests after code fix |

### 8.3 Security Coverage Audit

- **Authentication:** Covered by unauth + lockout tests (`SecurityIntegrationTest.java:45`, `:81`).
- **Route authorization:** Covered by admin/merchant/buyer matrix tests.
- **Object-level:** Partially covered (e.g. SKU heist); **permission-change merchant consistency** not covered.
- **Tenant isolation:** Partially covered; severe defects could remain in **governance** path while other tests pass.
- **Admin protection:** Covered for sample admin APIs (`SecurityIntegrationTest.java:57`).

### 8.4 Final Coverage Judgment

- **Conclusion:** **Partial Pass**
- **Boundary:** Core auth and many merchant-scoped behaviors are tested; **prompt gaps** (inventory log, governance merchant transitions, dimensional reporting, step checkpoints) are **not** exercised because features are incomplete or untested—tests can be green while material defects remain.

---

## 9. Final Notes

- This report is **static-only**: no Docker, no test run, no application start was performed to produce it.
- **Strong conclusions** (Fail / High) are anchored to **file:line** references above.
- **Runtime** claims (scheduler timing, WS auth, backup correctness) are **Manual Verification Required** unless proven by code + tests together.
- Prior short “follow-up” notes are **superseded** by this document; it is the consolidated audit artifact for `.tmp/`.

---

*Generated to match the Non-frontend static audit output structure and discipline (risk-first, evidence-first, prompt-aligned).*
