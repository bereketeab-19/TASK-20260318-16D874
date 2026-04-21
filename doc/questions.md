### 1. User Roles and Permissions

- **what sounded ambiguous:** The prompt lists four user roles (platform administrators, merchant operators, regular buyers, and reviewers) but does not define their specific permissions.
- **how it was understood:** A logical separation of duties is required. Actions like accessing the Reporting Center or managing Cooking Processes are cross-functional.
- **how it was solved:**
  - **Administrators:** Full system access, including user management and system settings.
  - **Merchant Operators:** Manage their own SPUs/SKUs, brands, inventory, and view reports relevant to their sales.
  - **Regular Buyers:** Browse products, place orders, use IM, and manage their own practice achievements.
  - **Reviewers:** Access content moderation tools and notification logs related to reviews and report handling.

### 2. Account Lockout Mechanism

- **what sounded ambiguous:** The account lockout mechanism (15 minutes after 5 failures) does not specify if it locks the username or the source IP.
- **how it was understood:** Locking by IP is too aggressive for shared networks; locking by username is the industry standard for protecting specific account integrity.
- **how it was solved:** Lockout will be triggered based on the `username`. Failed attempts will be logged immediately in the `AuditLog` table to ensure administrators can distinguish between brute-force attacks and user errors.

### 3. Product Batch Import/Export Format

- **what sounded ambiguous:** Product batch import/export file format.
- **how it was understood:** A format is needed that is human-readable, widely supported, and easy to parse in an offline Spring Boot environment.
- **how it was solved:** CSV (Comma Separated Values) will be used for all batch imports/exports. The implementation will use a transaction-based approach (all-or-nothing) to prevent partial data corruption during imports.

### 4. Inventory Alert Delivery

- **what sounded ambiguous:** Delivery method for Inventory Alerts.
- **how it was understood:** The system operates offline with no SMS/Email. The prompt mentions a "Notification Domain."
- **how it was solved:** Alerts will be distributed strictly through the internal Notification Domain. A `Notification` record will be created automatically when a product SKU stock ≤ 10, appearing in the merchant's internal dashboard view.

### 5. Definition of "Critical Operations" for Dual Approval

- **what sounded ambiguous:** Definition of "Critical Operations" requiring Dual Approval.
- **how it was understood:** The list needs to be restrictive to avoid workflow bloat while maintaining security.
- **how it was solved:** "Critical Operations" are defined as:
  - Modifying permissions for any administrator-level user.
  - Executing a full data wipe or restoration from a backup.
  - Deleting a product category that contains active products.
  - Modifying system-wide environment configurations.

### 6. Reporting Dimensions

- **what sounded ambiguous:** Definition of reporting dimensions ("organization" and "business dimensions").
- **how it was understood:** The system needs concrete mappings to generate meaningful business reports.
- **how it was solved:**
  - **Organization:** Maps to `merchant_operator` (the entity owning the inventory).
  - **Business Dimensions:** Maps to `category`, `brand`, and `SPU` (the product hierarchy).

### 7. Message Retention Policy

- **what sounded ambiguous:** Management of the 180-day message retention policy.
- **how it was understood:** The system needs to prevent the database from growing indefinitely without requiring manual intervention.
- **how it was solved:** A Spring `@Scheduled` task will run daily at a designated low-traffic time to purge `Message` records where `created_at < now() - 180 days`.

### 8. Immutable Audit Logs

- **what sounded ambiguous:** Enforcement of "Immutable Audit Logs."
- **how it was understood:** The prompt implies these logs cannot be altered or deleted after the fact.
- **how it was solved:** The `AuditLog` table will be designed with an application-level constraint that only allows `INSERT` operations. `UPDATE` and `DELETE` queries for the `AuditLog` table will not be implemented in the API service.
