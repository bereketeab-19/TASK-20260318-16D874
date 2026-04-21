# Static Audit Report - Pet Supplies Backend

## 1. Verdict
- Overall conclusion: **Partial Pass**

## 2. Scope and Static Verification Boundary
- Reviewed:
  - Project docs/config: `README.md`, `docs/*.md`, `docker-compose.yml`, `Dockerfile`, `backup/*`, `nginx/nginx.conf`, `application.yml`, Flyway migrations.
  - Backend implementation: controllers, services, repositories, security config, scheduling, crypto, auditing.
  - Tests: unit and integration tests under `src/test/java` (static read only).
- Not reviewed:
  - Runtime behavior in live environment, container orchestration execution, actual DB/network/TLS handshakes.
- Intentionally not executed:
  - Application startup, tests, Docker, external services.
- Manual verification required for:
  - End-to-end startup using documented compose command.
  - Runtime TLS hardening behavior and cert trust path.
  - Backup/restore execution path correctness and RTO/RPO under failure.

## 3. Repository / Requirement Mapping Summary
- Prompt core goal: offline Spring Boot monolith for 4 roles (admin/merchant/buyer/reviewer), with auth+lockout, product/SKU/category/brand/attributes/inventory alerts, messaging (WS + anti-spam + recall/read), internal notifications with subscriptions, reporting center, cooking assistance, achievements versioning, and security/compliance controls.
- Main mapped areas:
  - Auth/security: `src/main/java/com/petsupplies/config/SecurityConfig.java`, lockout/password/profile services, audit subsystem.
  - Domain APIs: product/messaging/notification/reporting/cooking/achievement controllers/services.
  - Persistence constraints: Flyway migrations `V1..V16`.
  - Operational constraints: TLS proxy and backup cron configs.
  - Static test evidence: security/object-scope/role/access and selected business flows.

## 4. Section-by-section Review

### 4.1 Hard Gates

#### 4.1.1 Documentation and static verifiability
- Conclusion: **Partial Pass**
- Rationale:
  - Positives: README includes startup/test/config guidance and endpoint inventory; docs include API and backup runbook.
  - Material gap: documented startup command is statically inconsistent with compose build context for app image.
- Evidence:
  - `README.md:12`, `README.md:16`
  - `docker-compose.yml:27`, `docker-compose.yml:28`, `docker-compose.yml:29`
  - `docs/api-spec.md:1`, `docs/backup-recovery.md` (runbook content present)
- Manual verification note:
  - Need manual `docker compose up --build` attempt to confirm if `context: ..` resolves correctly in intended filesystem layout.

#### 4.1.2 Material deviation from Prompt
- Conclusion: **Partial Pass**
- Rationale:
  - Core monolith and most domains are implemented and prompt-aligned.
  - Major functional deviation: notification subscription preferences are stored/queryable but not enforced during notification dispatch.
- Evidence:
  - Subscription APIs: `src/main/java/com/petsupplies/notification/web/NotificationController.java:66`, `src/main/java/com/petsupplies/notification/service/NotificationSubscriptionService.java:53`
  - Event dispatch bypassing subscriptions: `src/main/java/com/petsupplies/notification/service/BusinessNotificationService.java:25`, `src/main/java/com/petsupplies/product/service/InventoryService.java:175`
  - Docs claim delivery preferences semantics: `docs/api-spec.md:90`, `docs/api-spec.md:92`

### 4.2 Delivery Completeness

#### 4.2.1 Coverage of explicit core requirements
- Conclusion: **Partial Pass**
- Rationale:
  - Implemented: lockout policy, PBKDF2 passwords, category depth <=4, unique product code/barcode constraints, inventory threshold default 10, WS session/message publish+recall/read, 180-day purge, dual approval, audit append-only, cooking checkpoint/autosave, achievement version increment/export, reporting APIs/scheduled daily generation.
  - Gaps:
    - Notification subscription effect missing (preferences not applied).
    - IM read/unread support is only write-side (`markRead`), with no message status query endpoint.
- Evidence:
  - Lockout: `src/main/java/com/petsupplies/user/service/UserLockoutService.java:16`, `:17`, `:49`
  - Category depth DB trigger: `src/main/resources/db/migration/V5__category_depth_triggers.sql:26`
  - Unique constraints: `src/main/resources/db/migration/V4__product_inventory.sql:30`, `:43`
  - 180-day retention: `src/main/java/com/petsupplies/scheduling/MessageRetentionTask.java:28`
  - Subscription preference APIs only: `src/main/java/com/petsupplies/notification/service/NotificationSubscriptionService.java:36`, `:53`
  - Messaging HTTP routes lack status/list query route beyond recall/read/image: `src/main/java/com/petsupplies/messaging/web/MessagingHttpController.java:26`, `:38`, `:49`

#### 4.2.2 0-to-1 deliverable completeness
- Conclusion: **Pass**
- Rationale:
  - Complete multi-module monolith, DB migrations, docs, backup scripts, tests, and deployment artifacts are present.
- Evidence:
  - Entrypoint: `src/main/java/com/petsupplies/Application.java:6`
  - Migrations: `src/main/resources/db/migration/V1__init.sql:1` ... `V16__notification_event_subscriptions.sql:1`
  - README/test scripts: `README.md:79`, `run_tests.sh:5`, `run_tests.ps1:5`

### 4.3 Engineering and Architecture Quality

#### 4.3.1 Structure and module decomposition
- Conclusion: **Pass**
- Rationale:
  - Clear domain-based packages and layered controller/service/repo structure; no single-file anti-pattern.
- Evidence:
  - Module organization doc: `docs/design.md:10`-`docs/design.md:20`
  - Representative domain split: `src/main/java/com/petsupplies/product/*`, `messaging/*`, `reporting/*`, `auditing/*`

#### 4.3.2 Maintainability and extensibility
- Conclusion: **Partial Pass**
- Rationale:
  - Generally maintainable with transactional service boundaries and repository scoping.
  - Maintainability risk: notification preference model is disconnected from dispatch logic, creating dead configuration surface.
- Evidence:
  - Preference persistence: `src/main/java/com/petsupplies/notification/service/NotificationSubscriptionService.java:53`
  - Dispatch paths ignoring preference check: `src/main/java/com/petsupplies/notification/service/BusinessNotificationService.java:25`, `src/main/java/com/petsupplies/product/service/InventoryService.java:175`

### 4.4 Engineering Details and Professionalism

#### 4.4.1 Error handling, logging, validation, API design
- Conclusion: **Partial Pass**
- Rationale:
  - Strong: centralized error envelope, security handlers, method validation in many DTOs, audit log trails.
  - Gaps:
    - `PATCH /skus/{id}` allows negative `stockQuantity` due missing validation annotation.
    - Log desensitization exists but only masks plain 11-18 digit sequences; coverage for diverse sensitive formats cannot be guaranteed statically.
- Evidence:
  - Global error handling: `src/main/java/com/petsupplies/core/exceptions/GlobalExceptionHandler.java:27`, `:95`
  - Security handlers: `src/main/java/com/petsupplies/core/web/SecurityExceptionHandlers.java:17`
  - Missing stock lower-bound in update DTO: `src/main/java/com/petsupplies/product/web/dto/UpdateSkuRequest.java:6`
  - Create DTO has bound (contrast): `src/main/java/com/petsupplies/product/web/dto/CreateSkuRequest.java:11`
  - Log mask pattern: `src/main/resources/logback-spring.xml:4`

#### 4.4.2 Product-like vs demo-like organization
- Conclusion: **Pass**
- Rationale:
  - Includes real DB migrations, security, scheduling, audit, backup scripts, and role-based API surfaces.
- Evidence:
  - Dual approval + audit: `src/main/java/com/petsupplies/auditing/service/PendingApprovalService.java:92`
  - Backup cron and retention: `backup/crontab:1`, `backup/retention.sh:7`

### 4.5 Prompt Understanding and Requirement Fit

#### 4.5.1 Business goal/semantics/constraints fit
- Conclusion: **Partial Pass**
- Rationale:
  - Major prompt semantics mostly implemented.
  - Significant misses: subscription preferences not affecting delivery; static startup inconsistency undermines verifiability of offline delivery claim.
- Evidence:
  - Prompt-fit implementations: lockout `UserLockoutService.java:16`, append-only audit trigger `V2__audit_log_immutability_grants.sql:12`, category depth `V5__category_depth_triggers.sql:26`, daily 2:00 reports `ReportingService.java:124`
  - Delivery mismatch: `README.md:16` vs `docker-compose.yml:28`
  - Subscription semantics mismatch: `docs/api-spec.md:92` vs `BusinessNotificationService.java:25`

### 4.6 Aesthetics (frontend-only)
- Conclusion: **Not Applicable**
- Rationale: backend-only repository.
- Evidence: no frontend application assets/routes in reviewed scope.

## 5. Issues / Suggestions (Severity-Rated)

### Blocker

1. **Compose startup path is statically inconsistent with documented command**
- Severity: **Blocker**
- Conclusion: **Fail**
- Evidence: `README.md:16`, `docker-compose.yml:28`, `docker-compose.yml:29`, `Dockerfile:1`
- Impact: reviewers/users may be unable to build/start using documented steps; hard gate 1.1 is at risk.
- Minimum actionable fix: set app build context to current repo root (or align README path/invocation with current `context: ..` layout explicitly).

### High

2. **Notification subscription preferences are not enforced in notification dispatch**
- Severity: **High**
- Conclusion: **Fail**
- Evidence: `src/main/java/com/petsupplies/notification/service/NotificationSubscriptionService.java:53`, `src/main/java/com/petsupplies/notification/service/BusinessNotificationService.java:25`, `src/main/java/com/petsupplies/product/service/InventoryService.java:175`, `docs/api-spec.md:90`, `docs/api-spec.md:92`
- Impact: merchants can disable an event type but still receive notifications, violating documented behavior and prompt intent for subscription capability.
- Minimum actionable fix: gate all publish paths by `(merchantId,eventType)` preference lookup with default-enabled fallback.

3. **IM read/unread capability is only partial (write without query surface)**
- Severity: **High**
- Conclusion: **Partial Fail**
- Evidence: `src/main/java/com/petsupplies/messaging/web/MessagingHttpController.java:38`, `src/main/java/com/petsupplies/messaging/service/MessageService.java:170`, `docs/api-spec.md:131`
- Impact: system can mark messages read but does not expose a message retrieval/read-state API, so read/unread status is not fully usable.
- Minimum actionable fix: add tenant-scoped message listing/detail endpoint returning `readAt`/`recalledAt` (or equivalent status projection).

### Medium

4. **Offline self-contained claim is not statically guaranteed (build/test paths rely on external image/dependency sources unless pre-seeded)**
- Severity: **Medium**
- Conclusion: **Cannot Confirm Statistically**
- Evidence: `README.md:3`, `Dockerfile:1`, `Dockerfile:4`, `docker-compose.yml:3`, `docker-compose.yml:58`, `run_tests.sh:17`
- Impact: in strictly air-gapped environments, build/test/start may fail if base images and Maven artifacts are not preloaded.
- Minimum actionable fix: provide an explicit air-gap packaging procedure (preloaded image tarballs + local Maven/cache mirror) and document it.

5. **SKU update path permits negative stock values**
- Severity: **Medium**
- Conclusion: **Fail**
- Evidence: `src/main/java/com/petsupplies/product/web/dto/UpdateSkuRequest.java:6`, `src/main/java/com/petsupplies/product/service/InventoryService.java:98`
- Impact: invalid inventory states can be persisted, corrupting alerts/reports.
- Minimum actionable fix: add `@Min(0)` to update DTO stock field and/or enforce invariant in service.

## 6. Security Review Summary

- Authentication entry points: **Pass**
  - Evidence: HTTP Basic and centralized auth config `src/main/java/com/petsupplies/config/SecurityConfig.java:80`, lockout in `DbUserDetailsService.java:33` and `UserLockoutService.java:49`.
- Route-level authorization: **Pass**
  - Evidence: method-level guards across domains (`@PreAuthorize`) e.g. `ApprovalController.java:19`, `ProductController.java:22`, `BuyerCatalogController.java:18`.
- Object-level authorization: **Partial Pass**
  - Evidence: scoped repository access patterns (`findByIdAndMerchantId`) e.g. `ProductService.java:83`, `MessageService.java:149`, `CustomReportService.java:81`.
  - Note: achievements rely on merchant scoping for record access, but `userId` binding integrity is not constrained by FK/ownership checks (`Achievement.java:21`).
- Function-level authorization: **Pass**
  - Evidence: sender-only recall check `MessageService.java:154`; dual-approval anti-self-execute `PendingApprovalService.java:96`.
- Tenant/user data isolation: **Pass**
  - Evidence: cross-tenant 404 behavior in service/repo patterns and dedicated tests (`ProductHeistIntegrationTest.java:45`, `MessagingRecallAndNotificationAuthzIntegrationTest.java:86`).
- Admin/internal/debug protection: **Pass**
  - Evidence: `/admin/ping` requires admin `AdminController.java:10`; admin approvals/report routes guarded by `ApprovalController.java:19`, `ReportingController.java:22`.

## 7. Tests and Logging Review

- Unit tests: **Pass (basic)**
  - Evidence: `src/test/java/com/petsupplies/core/validation/PasswordPolicyTest.java:10`, `TopicScopeInterceptorTest.java:18`.
- API/integration tests: **Partial Pass**
  - Evidence: role/auth/object-scope and multiple domain flows covered (`SecurityIntegrationTest.java:46`, `DualApprovalIntegrationTest.java:47`, `AttachmentMagicBytesIntegrationTest.java:29`, `CustomReportIntegrationTest.java:51`).
  - Gaps: no test proving subscription preference enforcement, no static test for retention scheduler execution path, no backup restore test.
- Logging categories/observability: **Pass (basic)**
  - Evidence: audit service and reporting-read audit hooks (`AuditService.java:27`, `ReportingAccessAuditService.java:25`), logback configured (`logback-spring.xml:12`).
- Sensitive-data leakage risk in logs/responses: **Partial Pass**
  - Evidence: mask exists (`logback-spring.xml:4`) and audit payloads avoid raw passwords; however masking rule scope is narrow and not comprehensively verified.

## 8. Test Coverage Assessment (Static Audit)

### 8.1 Test Overview
- Unit tests exist: yes (`PasswordPolicyTest`, `TopicScopeInterceptorTest`).
- Integration tests exist: yes (multiple `*IntegrationTest` extending `AbstractIntegrationTest`).
- Frameworks: JUnit 5, Spring Boot Test, MockMvc, Spring Security Test, Testcontainers (`pom.xml:89`, `:95`, `:99`; `AbstractIntegrationTest.java:15`).
- Test entry points documented: yes (`README.md:83`, `run_tests.sh:5`, `run_tests.ps1:5`).

### 8.2 Coverage Mapping Table

| Requirement / Risk Point | Mapped Test Case(s) | Key Assertion / Fixture / Mock | Coverage Assessment | Gap | Minimum Test Addition |
|---|---|---|---|---|---|
| 401 unauthenticated | `SecurityIntegrationTest.java:46` | `GET /me` -> 401 | sufficient | none | n/a |
| 403 role boundary | `SecurityIntegrationTest.java:52`, `MerchantRoleGuardIntegrationTest.java:22` | buyer denied admin/merchant routes | sufficient | none | n/a |
| Lockout 5 failures / 15 min | `SecurityIntegrationTest.java:82` | mutable clock + fail x5 + unlock after +16m | sufficient | none | n/a |
| Tenant isolation product/SKU | `ProductHeistIntegrationTest.java:22` | merchantB GET merchantA SKU -> 404 | sufficient | none | n/a |
| Messaging recall authz | `MessagingRecallAndNotificationAuthzIntegrationTest.java:43` | non-sender forbidden, cross-tenant not found | sufficient | none | n/a |
| Topic subscription scope | `TopicScopeInterceptorTest.java:42` | cross-merchant topic subscribe denied | sufficient | none | n/a |
| Attachment type/size/dedup | `AttachmentMagicBytesIntegrationTest.java:48`, `AttachmentDedupIntegrationTest.java:62` | signature mismatch 400; dedup behavior | basically covered | size >2MB case not covered | add multipart >2MB rejection test |
| Batch import atomicity | `BatchImportAtomicityTest.java:24`, `BatchImportAtomicityXlsxTest.java:26` | malformed row -> no persisted rows | sufficient | none | n/a |
| Dual approval (four-eyes) | `DualApprovalIntegrationTest.java:129` | self-approval blocked; second admin executes | sufficient | none | n/a |
| Notification mark-read scope | `MessagingRecallAndNotificationAuthzIntegrationTest.java:92` | other merchant mark-read -> 404 | sufficient | none | n/a |
| Notification subscription enforcement | none | n/a | missing | preferences not asserted against dispatch | add tests disabling event then asserting no notification persisted |
| Message retention 180 days purge | none | n/a | missing | scheduler/data-retention path untested | add repository/service-level retention test with controlled clock |
| Reporting access audit trail | indirect only | no direct assertion on `REPORT_READ` audit entries | insufficient | audit requirement could regress undetected | add assertion tests for audit log writes on reporting endpoints |

### 8.3 Security Coverage Audit
- Authentication: **Basically covered** (`SecurityIntegrationTest` lockout + unauthenticated).
- Route authorization: **Covered** (`SecurityIntegrationTest`, `MerchantRoleGuardIntegrationTest`, `BuyerCatalogIntegrationTest`).
- Object-level authorization: **Covered for key merchant flows** (`ProductHeistIntegrationTest`, `MessagingRecallAndNotificationAuthzIntegrationTest`, `TechniqueCardIntegrationTest`).
- Tenant/data isolation: **Covered for several domains, not exhaustive** (no dedicated isolation test for every endpoint, e.g., achievements export attachments cross-tenant).
- Admin/internal protection: **Covered** (`SecurityIntegrationTest` admin surfaces + `DualApprovalIntegrationTest`).

### 8.4 Final Coverage Judgment
- **Partial Pass**
- Boundary:
  - Covered: core authn/authz, key tenant isolation flows, dual approval, batch atomicity, attachment validations.
  - Uncovered high-risk areas: notification subscription enforcement, retention scheduler behavior, and some audit-trail assertions; severe defects in these areas could remain while current tests still pass.

## 9. Final Notes
- Static evidence indicates a substantial backend implementation with meaningful tests and security controls.
- The main acceptance risks are concentrated in startup verifiability (`docker-compose` inconsistency) and requirement semantics gaps (notification subscriptions and IM read/unread completeness).
- Runtime success claims are intentionally not made; items marked Cannot Confirm Statistically require manual validation.
