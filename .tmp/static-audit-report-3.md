# Static Delivery Acceptance and Architecture Audit

## 1. Verdict
- Overall conclusion: **Fail**

## 2. Scope and Static Verification Boundary
- Reviewed:
  - Documentation and run/test/config instructions: `README.md`, `docs/api-spec.md`, `docs/design.md`
  - Build/runtime manifests and infra: `pom.xml`, `docker-compose.yml`, `Dockerfile`, `nginx/nginx.conf`, `backup/*`
  - Entry points/security/authz/object-scope: `src/main/java/com/petsupplies/config/SecurityConfig.java`, controllers/services/repos under `user`, `product`, `messaging`, `notification`, `reporting`, `auditing`, `cooking`
  - DB constraints/migrations: `src/main/resources/db/migration/*.sql`
  - Tests and test harness: `src/test/java/com/petsupplies/*`, `run_tests.sh`, `run_tests.ps1`
- Not reviewed in depth:
  - Built artifacts under `target/` (treated as non-source outputs)
- Intentionally not executed (per static-only boundary):
  - Application startup, tests, Docker, WebSocket runtime behavior, scheduled jobs runtime
- Claims requiring manual verification:
  - Actual TLS termination behavior and cipher posture in deployment
  - Runtime correctness of backup/restore operations (only backup scripts/statics were reviewed)
  - Real runtime behavior of cron schedules, retention purge, and daily report jobs

## 3. Repository / Requirement Mapping Summary
- Prompt core goal mapped: offline Spring Boot monolith for auth/session, product/catalog/inventory, messaging, notification, reporting, cooking assistance, achievements, and security/compliance controls.
- Main implementation areas found:
  - Auth/roles/lockout: `user`, `config/SecurityConfig`, `core/validation/PasswordPolicy`
  - Product/catalog/inventory/batch: `product/*`, migrations `V4`, `V5`, `V10`, `V11`
  - Messaging/attachments/websocket: `messaging/*`, migration `V6`, retention task
  - Notifications: `notification/*`, migrations `V4`, `V9`
  - Reporting + approvals: `reporting/*`, `auditing/*`, migration `V8`
  - Cooking + achievements: `cooking/*`, `achievement/*`, migration `V7`
- Major static mismatch themes:
  - Notification event coverage is narrower than prompt.
  - IM image flow is incomplete at API level.
  - Dual-approval critical-operation model is bypassable via direct endpoint.
  - Some prompt-required behaviors are only partially implemented (e.g., cooking auto-save cadence).

## 4. Section-by-section Review

### 4.1 Hard Gates

#### 4.1.1 Documentation and static verifiability
- Conclusion: **Pass**
- Rationale: Startup/test commands, credentials, endpoint maps, and module-level references are documented and statically traceable.
- Evidence:
  - `README.md:11`, `README.md:15`, `README.md:75`, `README.md:79`, `README.md:92`
  - `docs/api-spec.md:18`, `docs/api-spec.md:73`, `docs/api-spec.md:135`, `docs/api-spec.md:158`
  - `pom.xml:25`, `run_tests.sh:4`, `run_tests.ps1:3`

#### 4.1.2 Material deviation from Prompt
- Conclusion: **Fail**
- Rationale: Core prompt requirements are only partially covered (notably notification event scope, IM image message flow, critical-operation approval enforcement consistency, cooking auto-save cadence).
- Evidence:
  - Notification producers only implement low-stock events: `src/main/java/com/petsupplies/product/service/InventoryService.java:117`, `src/main/java/com/petsupplies/product/service/InventoryService.java:122`
  - IM text send only payload: `src/main/java/com/petsupplies/messaging/web/dto/SendTextMessage.java:6`, `src/main/java/com/petsupplies/messaging/web/MessagingController.java:30`
  - Attachment message send method exists but not exposed by controller endpoints: `src/main/java/com/petsupplies/messaging/service/MessageService.java:93`
  - Critical deletion still directly accessible via merchant endpoint: `src/main/java/com/petsupplies/product/web/CategoryController.java:50`
  - Prompt-modeled critical deletion in approval flow: `docs/api-spec.md:147`, `docs/api-spec.md:150`
  - No cooking auto-save scheduler; only report+retention schedulers exist: `src/main/java/com/petsupplies/reporting/service/ReportingService.java:120`, `src/main/java/com/petsupplies/scheduling/MessageRetentionTask.java:25`

### 4.2 Delivery Completeness

#### 4.2.1 Core explicit requirements coverage
- Conclusion: **Fail**
- Rationale:
  - Implemented: lockout/password policy, product/SKU CRUD+delist, category depth, batch import/export, low-stock notifications, websocket text messaging, dual approvals, reporting export/schedule, achievement versioning, TLS proxy, backup cadence.
  - Missing/partial: notification event taxonomy from prompt (order/review/report handling), image messaging workflow linkage, strict critical-op dual approval enforcement across relevant operations, cooking progress auto-save cadence.
- Evidence:
  - Implemented lockout/password: `src/main/java/com/petsupplies/user/service/UserLockoutService.java:16`, `src/main/java/com/petsupplies/core/validation/PasswordPolicy.java:10`
  - Category depth =4: `src/main/resources/db/migration/V5__category_depth_triggers.sql:26`
  - Batch atomic import: `src/main/java/com/petsupplies/product/service/BatchImportService.java:57`, `src/main/java/com/petsupplies/product/service/BatchImportService.java:123`
  - Messaging read/recall: `src/main/java/com/petsupplies/messaging/web/MessagingHttpController.java:21`, `src/main/java/com/petsupplies/messaging/web/MessagingHttpController.java:33`
  - Notification event gap: `src/main/java/com/petsupplies/product/service/InventoryService.java:122`
  - Cooking manual checkpoint only: `src/main/java/com/petsupplies/cooking/web/CookingController.java:42`, `src/main/java/com/petsupplies/cooking/service/CookingService.java:59`

#### 4.2.2 Basic end-to-end deliverable (0->1 vs fragment/demo)
- Conclusion: **Pass**
- Rationale: Repository contains full multi-module Spring Boot service, Flyway migrations, Docker compose topology, and non-trivial integration test suite.
- Evidence:
  - App entry and module breadth: `src/main/java/com/petsupplies/Application.java:6`
  - Infra: `docker-compose.yml:1`, `backup/Dockerfile:1`, `nginx/nginx.conf:8`
  - Tests: `src/test/java/com/petsupplies/AbstractIntegrationTest.java:12`

### 4.3 Engineering and Architecture Quality

#### 4.3.1 Structure and decomposition
- Conclusion: **Pass**
- Rationale: Domain-based packages and controller/service/repository decomposition are clear; schema is migration-driven and aligned with modules.
- Evidence:
  - Package decomposition reflected in source tree and docs: `docs/design.md:10`
  - Separate domain services/controllers/repos: e.g., `src/main/java/com/petsupplies/product/service/ProductService.java:17`, `src/main/java/com/petsupplies/messaging/service/MessageService.java:19`, `src/main/java/com/petsupplies/reporting/service/ReportingService.java:28`

#### 4.3.2 Maintainability and extensibility
- Conclusion: **Partial Pass**
- Rationale: Mostly maintainable, but core security/compliance behaviors rely on convention in some areas (approval bypass path, default crypto key fallback), reducing robustness.
- Evidence:
  - Approval flow exists: `src/main/java/com/petsupplies/auditing/service/PendingApprovalService.java:62`
  - Direct non-approval deletion path exists: `src/main/java/com/petsupplies/product/web/CategoryController.java:50`
  - Default fixed encryption key fallback: `src/main/resources/application.yml:38`

### 4.4 Engineering Details and Professionalism

#### 4.4.1 Error handling, logging, validation, API design
- Conclusion: **Partial Pass**
- Rationale: Strong baseline (global error envelope, validation, audit logs), but sensitive-key handling is weak and some domain constraints are only partially enforced in APIs.
- Evidence:
  - Global error handling: `src/main/java/com/petsupplies/core/exceptions/GlobalExceptionHandler.java:22`
  - Security handlers: `src/main/java/com/petsupplies/core/web/SecurityExceptionHandlers.java:17`
  - DTO validation examples: `src/main/java/com/petsupplies/product/web/dto/CreateSkuRequest.java:8`, `src/main/java/com/petsupplies/cooking/web/dto/CheckpointRequest.java:8`
  - Default crypto key fallback: `src/main/resources/application.yml:38`
  - Docker compose does not set key override: `docker-compose.yml:34`

#### 4.4.2 Product-like organization vs demo
- Conclusion: **Pass**
- Rationale: Includes operational artifacts (TLS reverse proxy, backup cron sidecar, DB grants/migrations, integration tests), beyond demo-only structure.
- Evidence:
  - Reverse proxy TLS: `nginx/nginx.conf:9`
  - Backup schedule: `backup/crontab:1`
  - DB grants split roles: `db/init/01_users_and_grants.sql:4`

### 4.5 Prompt Understanding and Requirement Fit

#### 4.5.1 Business goal and implicit constraints fit
- Conclusion: **Fail**
- Rationale: Broad alignment exists, but several explicit prompt semantics remain materially incomplete (notification event coverage, IM image-message flow, strict critical-op dual-approval envelope, cooking auto-save cadence).
- Evidence:
  - Only low-stock event notifications generated: `src/main/java/com/petsupplies/product/service/InventoryService.java:122`
  - Messaging publishes text only: `src/main/java/com/petsupplies/messaging/web/dto/SendTextMessage.java:8`
  - Attachment-only endpoint without message-link API: `src/main/java/com/petsupplies/messaging/web/AttachmentController.java:27`
  - Category deletion direct endpoint bypasses approval flow: `src/main/java/com/petsupplies/product/web/CategoryController.java:50`
  - No cooking @Scheduled autosave: `src/main/java/com/petsupplies/reporting/service/ReportingService.java:120`, `src/main/java/com/petsupplies/scheduling/MessageRetentionTask.java:25`

### 4.6 Aesthetics (frontend-only/full-stack visual)

#### 4.6.1 Visual/interaction quality
- Conclusion: **Not Applicable**
- Rationale: Backend API service repository; no frontend UI deliverable in scope.
- Evidence:
  - Backend-only structure and Spring Boot app entry: `src/main/java/com/petsupplies/Application.java:6`

## 5. Issues / Suggestions (Severity-Rated)

### Blocker / High

1. **Severity: High**
- Title: Critical-operation dual approval is bypassable via direct category deletion endpoint
- Conclusion: Fail
- Evidence:
  - Critical operation modeled for approval flow: `docs/api-spec.md:147`, `docs/api-spec.md:150`
  - Direct delete endpoint callable by merchant-scoped identity: `src/main/java/com/petsupplies/product/web/CategoryController.java:50`, `src/main/java/com/petsupplies/product/service/CategoryService.java:55`
- Impact: The security/compliance requirement "dual approval for critical operations" is not uniformly enforceable; destructive category operations can occur outside four-eyes workflow.
- Minimum actionable fix: Gate category deletion behind approval workflow (or define/expose only non-critical merchant delete semantics and move critical deletions exclusively to approval-executed path with clear policy checks).

2. **Severity: High**
- Title: Notification domain does not implement required event coverage (order status, review results, report handling)
- Conclusion: Fail
- Evidence:
  - Notification generation implemented from low-stock only: `src/main/java/com/petsupplies/product/service/InventoryService.java:117`, `src/main/java/com/petsupplies/product/service/InventoryService.java:122`
  - Notification controller/service only list/mark-read, no additional event producers: `src/main/java/com/petsupplies/notification/web/NotificationController.java:31`, `src/main/java/com/petsupplies/notification/service/NotificationService.java:23`
- Impact: Prompt-required internal event coverage is incomplete; notification center does not represent key business events.
- Minimum actionable fix: Add internal notification producers for order status updates, review outcomes, and report-handling events, with delivered/read timestamps and merchant/user scoping.

3. **Severity: High**
- Title: IM image anti-spam requirement is only partially delivered (attachment upload exists, but no image message send flow)
- Conclusion: Fail
- Evidence:
  - STOMP payload supports text only: `src/main/java/com/petsupplies/messaging/web/dto/SendTextMessage.java:6`, `src/main/java/com/petsupplies/messaging/web/MessagingController.java:30`
  - Attachment upload exists separately: `src/main/java/com/petsupplies/messaging/web/AttachmentController.java:27`
  - MessageService has attachment-send method but no exposed controller path uses it: `src/main/java/com/petsupplies/messaging/service/MessageService.java:93`
- Impact: Required IM image workflow (send + dedup constraints in messaging context) is not fully usable through public API surface.
- Minimum actionable fix: Add authenticated API/STOMP path to send message with `attachmentId`, enforce tenant ownership and size/type validations, and persist/read status consistently.

4. **Severity: High**
- Title: Encryption key management is insecure by default
- Conclusion: Fail
- Evidence:
  - Fixed default AES key fallback in config: `src/main/resources/application.yml:38`
  - Deployment env does not override key in compose: `docker-compose.yml:34`
- Impact: Deployments using defaults share a known encryption key, weakening at-rest protection for sensitive fields.
- Minimum actionable fix: Remove hardcoded default key; fail startup when `APP_CRYPTO_HEX_KEY` is absent/weak; document secure key provisioning.

### Medium

5. **Severity: Medium**
- Title: Cooking progress auto-save cadence from prompt is not implemented
- Conclusion: Partial Fail
- Evidence:
  - Manual checkpoint endpoint only: `src/main/java/com/petsupplies/cooking/web/CookingController.java:42`
  - Service updates checkpoint only when explicitly called: `src/main/java/com/petsupplies/cooking/service/CookingService.java:59`
  - Scheduled tasks exist only for reporting and message retention: `src/main/java/com/petsupplies/reporting/service/ReportingService.java:120`, `src/main/java/com/petsupplies/scheduling/MessageRetentionTask.java:25`
- Impact: Prompt-stated resilience behavior (save every 30s or on step changes) is not guaranteed by design.
- Minimum actionable fix: Add timer-driven checkpoint scheduler and/or automatic checkpoint updates on step transitions/timer events.

6. **Severity: Medium**
- Title: Cross-tenant attachment metadata reuse risk due global SHA-256 uniqueness
- Conclusion: Suspected Risk
- Evidence:
  - Global lookup by hash only: `src/main/java/com/petsupplies/messaging/service/AttachmentService.java:56`
  - Global unique SHA-256 constraint (not merchant-scoped): `src/main/resources/db/migration/V6__messaging.sql:16`, `src/main/java/com/petsupplies/messaging/domain/Attachment.java:21`
- Impact: Same file uploaded by different merchants reuses a single attachment row; this can complicate tenant data isolation and future authorization logic when attachments are linked to messages.
- Minimum actionable fix: Scope dedup key by `(merchant_id, sha256)` or enforce strict ownership checks wherever attachments are referenced.

7. **Severity: Medium**
- Title: Backup exists but restore/recovery procedure is not statically delivered
- Conclusion: Cannot Confirm Statistically
- Evidence:
  - Backup generation and retention scripts exist: `backup/backup.sh:16`, `backup/backup.sh:21`, `backup/retention.sh:7`
  - No restore script/commands documented in README: `README.md:11`, `README.md:75`
- Impact: Compliance requirement asks backup and recovery strategy; recovery operability cannot be verified from delivered artifacts.
- Minimum actionable fix: Add explicit restore playbook/scripts (full + binlog point-in-time path) and document manual verification steps.

## 6. Security Review Summary

- **Authentication entry points**: **Pass**
  - Evidence: HTTP Basic enabled globally, lockout integrated into custom auth provider, 5-fail/15-min policy in service.
  - `src/main/java/com/petsupplies/config/SecurityConfig.java:80`, `src/main/java/com/petsupplies/config/SecurityConfig.java:56`, `src/main/java/com/petsupplies/user/service/UserLockoutService.java:16`

- **Route-level authorization**: **Partial Pass**
  - Evidence: Admin/reviewer routes use `@PreAuthorize`; most merchant routes rely on `requireMerchantId()` rather than explicit role checks.
  - `src/main/java/com/petsupplies/reporting/web/ReportingController.java:20`, `src/main/java/com/petsupplies/reporting/web/ReviewerReportingController.java:20`, `src/main/java/com/petsupplies/core/security/CurrentPrincipal.java:18`

- **Object-level authorization**: **Partial Pass**
  - Evidence: scoped repo methods are widely used (`findByIdAndMerchantId`); however attachment dedup path is globally keyed by hash.
  - `src/main/java/com/petsupplies/product/repo/ProductRepository.java:9`, `src/main/java/com/petsupplies/messaging/repo/SessionRepository.java:9`, `src/main/java/com/petsupplies/messaging/service/AttachmentService.java:56`

- **Function-level authorization**: **Partial Pass**
  - Evidence: sensitive admin functions are guarded; but critical delete behavior can occur outside approval workflow.
  - `src/main/java/com/petsupplies/auditing/web/ApprovalController.java:19`, `src/main/java/com/petsupplies/product/web/CategoryController.java:50`

- **Tenant / user data isolation**: **Partial Pass**
  - Evidence: merchant-scoped queries in products/skus/messages/notifications; cross-tenant attachment row reuse risk remains.
  - `src/main/java/com/petsupplies/product/service/ProductService.java:82`, `src/main/java/com/petsupplies/messaging/service/MessageService.java:65`, `src/main/java/com/petsupplies/notification/service/NotificationService.java:26`, `src/main/java/com/petsupplies/messaging/domain/Attachment.java:21`

- **Admin / internal / debug endpoint protection**: **Pass**
  - Evidence: admin and reviewer surfaces are role-guarded; no debug/public backdoor endpoint found beyond `/health`.
  - `src/main/java/com/petsupplies/core/web/AdminController.java:10`, `src/main/java/com/petsupplies/config/SecurityConfig.java:86`

## 7. Tests and Logging Review

- **Unit tests**: **Partial Pass**
  - Evidence: focused unit tests exist (password policy, topic interceptor), but broad service-level negative-path unit coverage is limited.
  - `src/test/java/com/petsupplies/core/validation/PasswordPolicyTest.java:10`, `src/test/java/com/petsupplies/TopicScopeInterceptorTest.java:18`

- **API / integration tests**: **Partial Pass**
  - Evidence: substantial MockMvc/Testcontainers coverage for security, object scope, imports, approvals, cooking checkpoint, attachments; gaps remain for several critical prompt behaviors.
  - `src/test/java/com/petsupplies/SecurityIntegrationTest.java:45`, `src/test/java/com/petsupplies/ProductHeistIntegrationTest.java:21`, `src/test/java/com/petsupplies/DualApprovalIntegrationTest.java:45`

- **Logging categories / observability**: **Partial Pass**
  - Evidence: audit log subsystem is present and append-only triggers exist; general app logging is minimal and mostly root console pattern.
  - `src/main/java/com/petsupplies/auditing/service/AuditService.java:27`, `src/main/resources/db/migration/V2__audit_log_immutability_grants.sql:12`, `src/main/resources/logback-spring.xml:12`

- **Sensitive-data leakage risk in logs / responses**: **Partial Pass**
  - Evidence: masking pattern exists, but key-handling defaults are weak and payload logging policy depends on caller discipline.
  - `src/main/resources/logback-spring.xml:4`, `src/main/resources/application.yml:38`, `src/main/java/com/petsupplies/auditing/service/AuditService.java:27`

## 8. Test Coverage Assessment (Static Audit)

### 8.1 Test Overview
- Unit and integration tests exist.
- Frameworks: Spring Boot Test, MockMvc, AssertJ, Spring Security Test, Testcontainers, JUnit 5.
- Test entry points and infra:
  - Shared SpringBootTest + MySQL Testcontainer: `src/test/java/com/petsupplies/AbstractIntegrationTest.java:12`, `src/test/java/com/petsupplies/AbstractIntegrationTest.java:15`
  - Commands documented: `README.md:75`, `run_tests.sh:5`, `run_tests.ps1:5`

### 8.2 Coverage Mapping Table

| Requirement / Risk Point | Mapped Test Case(s) | Key Assertion / Fixture / Mock | Coverage Assessment | Gap | Minimum Test Addition |
|---|---|---|---|---|---|
| 401 unauthenticated | `src/test/java/com/petsupplies/SecurityIntegrationTest.java:46` | `status().isUnauthorized()` (`:48`) | sufficient | none | none |
| 403 role boundary for admin/reviewer routes | `src/test/java/com/petsupplies/SecurityIntegrationTest.java:52`, `:58`, `:64`, `:76` | forbidden assertions (`:54`, `:60`, `:66`, `:78`) | sufficient | merchant-route role checks not explicitly tested | add buyer/admin attempts on merchant endpoints |
| Lockout 5 failures / 15 min | `src/test/java/com/petsupplies/SecurityIntegrationTest.java:82` | 5 failures + locked + clock advance + success (`:85-:101`) | sufficient | none | none |
| Product/SKU object-level tenant isolation | `src/test/java/com/petsupplies/ProductHeistIntegrationTest.java:21` | MerchantB gets 404 on MerchantA SKU (`:45-:46`) | basically covered | only one object type/path | add cross-tenant tests for updates/delist/attributes/notifications |
| Batch import atomicity CSV/XLSX | `src/test/java/com/petsupplies/BatchImportAtomicityTest.java:24`, `src/test/java/com/petsupplies/BatchImportAtomicityXlsxTest.java:26` | bad request + zero persisted rows (`:49-:52`, `:39-:42`) | sufficient | no success-path import verification | add success import with expected counts |
| WebSocket topic tenant scope | `src/test/java/com/petsupplies/TopicScopeInterceptorTest.java:18` | cross-merchant subscribe denied (`:38-:39`) | basically covered | no positive case test | add allowed subscription test |
| Messaging anti-spam duplicate fold in 10s | `src/test/java/com/petsupplies/MessagingAntiSpamIntegrationTest.java:24` | second message folded + only one persisted (`:31-:34`) | sufficient | no boundary test at >10s | add 11-second case asserts persists |
| Attachment dedup | `src/test/java/com/petsupplies/AttachmentDedupIntegrationTest.java:30` | same bytes => one DB row/file (`:40-:44`) | basically covered | no cross-merchant isolation test; no invalid type/oversize test | add cross-tenant dedup ownership + type/size rejection tests |
| Dual approval second-admin enforcement | `src/test/java/com/petsupplies/DualApprovalIntegrationTest.java:128` | self-execute blocked, second admin succeeds (`:149-:155`) | sufficient | no bypass-path tests for critical ops | add test proving critical op cannot be performed outside approval flow |
| Cooking checkpoint/resumption | `src/test/java/com/petsupplies/CookingResumptionTest.java:23` | checkpoint then GET returns same state (`:31-:47`) | basically covered | no autosave every 30s/step-change tests | add scheduler/step-trigger autosave tests |
| Achievement required fields + version increment | `src/test/java/com/petsupplies/AchievementValidationTest.java:20`, `src/test/java/com/petsupplies/AchievementAttachmentVersioningTest.java:30` | bad request on missing fields (`:32`); v1/v2 preserved (`:41-:49`) | basically covered | no authz/object-scope negative tests | add cross-tenant achievement access tests |
| Contact encryption conversion | `src/test/java/com/petsupplies/ContactEncryptionIntegrationTest.java:24` | round-trip through converter (`:29-:30`) | insufficient | does not verify DB ciphertext at storage layer | add native query assertion that raw DB column != plaintext |

### 8.3 Security Coverage Audit
- **Authentication**: basically covered (401 + lockout) via `SecurityIntegrationTest`.
- **Route authorization**: partially covered (admin/reviewer); severe defects on merchant-route role assumptions could remain undetected.
- **Object-level authorization**: partially covered (SKU read heist + topic scope), but many object write paths are untested.
- **Tenant/data isolation**: partially covered; attachment global dedup isolation risk has no test.
- **Admin/internal protection**: covered for key admin endpoints, but no tests asserting bypass prevention on non-admin critical ops.

### 8.4 Final Coverage Judgment
- **Partial Pass**
- Covered major risks:
  - authn/authz baseline, lockout, SKU tenant read isolation, batch import rollback, anti-spam fold, dual-approval core behavior.
- Uncovered risks where severe defects could still pass tests:
  - notification event completeness, critical-operation bypass paths, IM image message flow, attachment tenant-isolation semantics, cooking auto-save cadence.

## 9. Final Notes
- Findings are static-only and evidence-based; runtime behavior was not asserted.
- Material defects are consolidated by root cause to avoid repetition.
- Areas marked Cannot Confirm Statistically require manual verification in a controlled environment.

## 10. Remediation applied (2026-04-21, code + tests)
The following map to **§5 High** items in this report and related gaps:

| Issue | Change | Evidence |
|-------|--------|----------|
| Dual-approval bypass on category delete | Merchant `DELETE /categories/{id}` returns **403** with guidance to use `ACTIVE_CATEGORY_DELETION`; `CategoryDeletionRequiresApprovalIntegrationTest` | `CategoryController#deleteBlocked`, `src/test/java/com/petsupplies/CategoryDeletionRequiresApprovalIntegrationTest.java` |
| Notification event coverage | `BusinessNotificationService` emits `ORDER_STATUS`, `REVIEW_OUTCOME`, `REPORT_HANDLING`; merchant hooks `POST /api/merchant/events/order-status`, `POST /api/merchant/events/review-outcome`; daily report job notifies | `BusinessNotificationService`, `MerchantBusinessEventController`, `ReportingService#generateDailyInventoryReports`, `BusinessEventNotificationIntegrationTest` |
| IM image message flow | STOMP `/app/messages.sendImage`; HTTP `POST /sessions/{sessionId}/messages/image`; `MessageService#sendImageMessage` | `MessagingController`, `MessagingHttpController`, `MessagingImageHttpIntegrationTest` |
| Insecure default crypto key | No default in `application.yml`; startup fails if `APP_CRYPTO_HEX_KEY` / `app.crypto.hex-key` empty; compose sets dev key; tests set key via `AbstractIntegrationTest` | `AesGcmEncryptionService`, `application.yml`, `docker-compose.yml`, `README.md` |
| Attachment cross-tenant dedup | DB unique `(merchant_id, sha256)` (V12); repository + service scoped lookup | `V12__attachment_dedup_per_merchant.sql`, `AttachmentService`, `AttachmentDedupIntegrationTest#same_bytes_different_merchants_create_separate_attachment_rows` |
| Cooking auto-save cadence (Medium) | `CookingAutoCheckpointTask` @Scheduled every 30s updates `lastCheckpointAt` for `ACTIVE` processes | `src/main/java/com/petsupplies/scheduling/CookingAutoCheckpointTask.java` |
