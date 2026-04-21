CREATE TABLE custom_report_definitions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id VARCHAR(64) NOT NULL,
  name VARCHAR(200) NOT NULL,
  description TEXT NULL,
  definition_json TEXT NOT NULL,
  schedule_cron VARCHAR(120) NULL,
  schedule_timezone VARCHAR(64) NULL,
  owner_user_id BIGINT NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_custom_reports_merchant_name (merchant_id, name),
  INDEX idx_custom_reports_merchant (merchant_id)
);
