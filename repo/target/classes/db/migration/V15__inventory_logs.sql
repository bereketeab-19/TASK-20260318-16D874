CREATE TABLE inventory_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,
  merchant_id VARCHAR(64) NOT NULL,
  sku_id BIGINT NOT NULL,
  event_type VARCHAR(32) NOT NULL,
  quantity_before INT NOT NULL,
  quantity_after INT NOT NULL,
  reference_kind VARCHAR(32) NULL,
  actor_username VARCHAR(80) NULL,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_inventory_logs_merchant_created (merchant_id, created_at),
  KEY idx_inventory_logs_sku (sku_id),
  CONSTRAINT fk_inventory_logs_sku FOREIGN KEY (sku_id) REFERENCES skus (id)
);
