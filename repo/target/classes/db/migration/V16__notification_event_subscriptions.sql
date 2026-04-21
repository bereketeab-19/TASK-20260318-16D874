CREATE TABLE notification_event_subscriptions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  merchant_id VARCHAR(64) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uq_notification_sub_merchant_event (merchant_id, event_type),
  KEY idx_notification_sub_merchant (merchant_id)
);
