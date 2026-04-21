ALTER TABLE skus
  ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE skus SET active = TRUE WHERE active IS NULL;

CREATE TABLE merchant_settings (
  merchant_id VARCHAR(64) NOT NULL PRIMARY KEY,
  low_stock_threshold INT NOT NULL DEFAULT 10,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE attribute_definitions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  merchant_id VARCHAR(64) NOT NULL,
  code VARCHAR(64) NOT NULL,
  label VARCHAR(120) NOT NULL,
  UNIQUE KEY uq_attr_def_merchant_code (merchant_id, code),
  INDEX idx_attr_def_merchant (merchant_id)
);

CREATE TABLE sku_attribute_values (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sku_id BIGINT NOT NULL,
  attribute_definition_id BIGINT NOT NULL,
  value_text VARCHAR(512) NOT NULL,
  UNIQUE KEY uq_sku_attr (sku_id, attribute_definition_id),
  INDEX idx_sav_sku (sku_id),
  CONSTRAINT fk_sav_sku FOREIGN KEY (sku_id) REFERENCES skus(id) ON DELETE CASCADE,
  CONSTRAINT fk_sav_def FOREIGN KEY (attribute_definition_id) REFERENCES attribute_definitions(id) ON DELETE CASCADE
);

ALTER TABLE cooking_steps
  ADD COLUMN completed_at TIMESTAMP NULL,
  ADD COLUMN reminder_fire_at TIMESTAMP NULL;

CREATE TABLE report_indicator_definitions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL UNIQUE,
  label VARCHAR(200) NOT NULL,
  description TEXT NULL
);

INSERT INTO report_indicator_definitions (code, label, description) VALUES
('INV_SKU_COUNT', 'Inventory SKU count', 'Active SKU rows for merchant'),
('INV_STOCK_SUM', 'Inventory stock sum', 'Sum of stock_quantity for active SKUs'),
('INV_LOW_STOCK_ALERTS', 'Low-stock notifications', 'Derived from threshold breaches (audit)');

INSERT INTO users (username, password_hash, role, merchant_id, failed_attempts, locked_until)
SELECT 'reviewer', password_hash, 'REVIEWER', NULL, 0, NULL FROM users WHERE username = 'buyer' LIMIT 1;
