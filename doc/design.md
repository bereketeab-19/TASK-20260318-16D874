# System Architecture and Implementation Plan (`docs/design.md`)

## 1. Architecture Overview
The system is designed as a fully offline, self-contained monolithic Spring Boot application utilizing local MySQL storage. The architecture follows a strict three-tier design (Controllers, Services, Repositories) bounded by domain-driven modules. It relies exclusively on internal services, utilizing an Nginx reverse proxy for TLS termination and a dedicated sidecar container for automated backups, ensuring compliance with offline constraints.

## 2. Module Breakdown and Code Structure
The Java package structure is organized by business domain to enforce high cohesion and strict module boundaries.

```text
com.petsupplies/
├── config/              // Security, WebSocket, Jackson, and Logging configurations
├── core/                // Global exceptions, base entities, validation, utility classes
├── user/                // Auth, User management, Roles, and Dual Approval logic
├── product/             // SPU, SKU, Category (depth ≤ 4), Brand, Inventory
├── messaging/           // WebSocket STOMP controllers, Message persistence, Anti-spam
├── notification/        // Internal alert generation and distribution
├── reporting/           // Custom reports, aggregations, Scheduled generation tasks
├── cooking/             // Process, Step, Timer, and Reminder state management
├── achievement/         // Practice attachments, structural fields, Version control
└── auditing/            // Immutable append-only logging aspect and repositories
```

## 3. Domain Model and Database Schema
The schema is mapped via Spring Data JPA. Constraints such as category depth and unique indexes are enforced at both the entity and database levels.

* **User & Auth:** `users` (id, username, password_hash, role, locked_until, failed_attempts), `roles` (ADMIN, MERCHANT, BUYER, REVIEWER).
* **Product Domain:** `categories` (parent_id self-referencing), `brands`, `products` (product_code UNIQUE), `skus` (barcode UNIQUE, stock_quantity).
* **Messaging:** `sessions`, `messages` (content, hash_signature, sent_at).
* **Notifications:** `notifications` (user_id, content, read_at).
* **Cooking & Achievements:** `cooking_processes`, `cooking_steps`, `achievements`, `attachment_versions`.
* **Security:** `audit_logs` (action, payload, timestamp), `pending_approvals`.

**Data Protection at Rest:**
* Sensitive fields (e.g., phone numbers, ID numbers) utilize a JPA `@Convert` annotation with an `AttributeConverter` applying AES-256 symmetric encryption before database persistence.

## 4. Core Technical Implementations

### Authentication and Security
* **Lockout Mechanism:** Spring Security custom `AuthenticationFailureHandler` tracks failures by `username`. At 5 failures, the account is locked for 15 minutes. Failed attempts generate immediate `AuditLog` entries.
* **Object-Level Authorization (Tenant Isolation):** Merchant access is strictly scoped. Repository queries for merchant endpoints append `WHERE merchant_id = ?` to guarantee static verifiability of data isolation.
* **Dual Approval:** Critical operations (modifying Admin permissions, data wipe/restore, deleting active categories, modifying env configs) create a `PENDING` record. A separate authenticated Admin must invoke an `/approve` endpoint to execute the transaction.

### Product Management and Inventory
* **Batch Import/Export:** Dedicated transactional endpoints utilizing **Apache POI** for Excel (`.xlsx`) and OpenCSV for `.csv`. Uses an all-or-nothing `@Transactional` strategy to prevent partial data corruption.
* **Inventory Alerts:** Handled internally via the `Notification` domain. A scheduled or event-driven check generates internal alerts when stock ≤ 10, completely avoiding external SMS/Email dependencies.

### Instant Messaging (IM) Domain
* **WebSockets:** Configured via STOMP over WebSockets. Session creation is available both via REST (`POST /sessions`) and STOMP (`/app/sessions.create`); the latter publishes to `/topic/sessions.{merchantId}.lifecycle` and is subject to the same tenant-scoped subscription rules as chat message topics (`TopicScopeInterceptor`).
* **Anti-Spam & Deduplication:** * Text folding: Cache recent messages by session; fold identical strings within 10 seconds.
    * Image deduplication: Calculate SHA-256 hashes of uploaded JPG/PNG files (≤ 2MB). Store hashes in the DB; if a hash exists, reuse the local file path instead of saving a duplicate.
* **Retention Policy:** A Spring `@Scheduled` task executes daily during low-traffic hours to `DELETE` messages older than 180 days.

### Error Handling and Logging
* **Global Exceptions:** A `@RestControllerAdvice` intercepts all application, security, and validation (`MethodArgumentNotValidException`) errors to return standardized, statically verifiable JSON error payloads.
* **Input Validation:** Jakarta Validation annotations (`@NotNull`, `@Size`, `@Pattern`) strictly enforce contracts on all incoming DTOs.
* **Log Desensitization:** Configured via `logback-spring.xml`. A custom regex-based layout pattern masks sensitive data (IDs, phone numbers) before writing to standard out.

## 5. Infrastructure and Containerization
The system requires zero external dependencies and starts via a single `docker compose up` command.

**`docker-compose.yml` Services:**
1.  `app`: The Spring Boot monolithic service.
2.  `db`: Local MySQL 8.0 instance with persistent volume mounts.
3.  `proxy`: Nginx reverse proxy sidecar. Holds self-signed certificates to handle **TLS termination**, statically fulfilling the TLS requirement.
4.  `backup`: Alpine-based Cron sidecar. Executes `backup.sh` to run `mysqldump` (daily full backups at 00:00, hourly incremental binlog backups), mounting outputs to a local volume with a 30-day retention script.

## 6. Testing and Verification Strategy
Validation is enforced via the canonical `run_tests.sh` executing `mvn clean verify`.

**Coverage Boundaries:**
* **Integration Tests (`@SpringBootTest`):** Employs Testcontainers (MySQL) to verify repository queries, transactions, and REST endpoints. Target API surface coverage is >90%.
* **Security Boundary Auditing:** Dedicated test classes must explicitly prove:
    * `401 Unauthenticated` rejections.
    * `403 Unauthorized` role-boundary violations.
    * Object-level isolation preservation (Tenant A cannot access Tenant B's data).
* **Documentation:** `README.md` will provide explicit run commands, default admin credentials, and mapping documentation so human reviewers can statically verify the entry points without modifying code.

## 7. Phased Execution Sequence
Execution will proceed vertically. Development will not advance to the next phase until the current phase passes tests, linter checks, and `run_tests.sh`.

* **Phase 1: Base Scaffold & Security:** Docker orchestration (Proxy, App, DB, Backup), Spring Boot setup, Auth domain, Lockout, Global Exception Handler, and Audit Logging aspect.
* **Phase 2: Product & Inventory:** SPU/SKU entities, tenant isolation queries, CSV/Excel batch imports, and Threshold internal notifications.
* **Phase 3: Messaging:** WebSocket config, file hashing deduplication, anti-spam logic, and the 180-day scheduled purge task.
* **Phase 4: Achievements & Cooking:** Versioned attachments, Process/Step/Timer entities.
* **Phase 5: Reporting & Dual Approval:** Scheduled report generation, data aggregation, and the critical operation approval workflow.
* **Phase 6: Verification & Packaging:** Final execution of all static and runtime tests, cleanup of local IDE files, and artifact packaging.

