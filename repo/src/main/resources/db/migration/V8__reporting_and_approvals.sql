CREATE TABLE pending_approvals (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  operation_type VARCHAR(64) NOT NULL,
  payload_json TEXT NOT NULL,
  status VARCHAR(16) NOT NULL,
  requester_user_id BIGINT NOT NULL,
  requester_username VARCHAR(80) NOT NULL,
  approver_user_id BIGINT NULL,
  approver_username VARCHAR(80) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  decided_at TIMESTAMP NULL,
  executed_at TIMESTAMP NULL,
  INDEX idx_pending_approvals_status (status),
  INDEX idx_pending_approvals_created_at (created_at)
);

CREATE TABLE daily_inventory_reports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id VARCHAR(64) NOT NULL,
  report_date DATE NOT NULL,
  total_skus BIGINT NOT NULL,
  total_stock BIGINT NOT NULL,
  generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_daily_inventory_reports (merchant_id, report_date),
  INDEX idx_daily_inventory_reports_merchant_date (merchant_id, report_date)
);

-- Seed a second ADMIN for four-eyes approval tests
-- Password: admin2_123!
INSERT INTO users (username, password_hash, role, merchant_id, failed_attempts, locked_until)
VALUES ('admin2', 'pbkdf2_sha256$310000$ktq3t1eT1IbbM5P1J1ChCQ==$rEBHhsyyVwFr9BPFxNAUBOZdT1VQUW9/Jb3CjQ5Q7Do=', 'ADMIN', NULL, 0, NULL);

