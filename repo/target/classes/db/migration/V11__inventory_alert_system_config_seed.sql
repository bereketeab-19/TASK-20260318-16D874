-- Global default for low-stock evaluation when merchant_settings row is missing.
INSERT INTO system_config (config_key, config_value, updated_at)
SELECT 'INVENTORY_ALERT_THRESHOLD', '10', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
  SELECT 1 FROM system_config WHERE config_key = 'INVENTORY_ALERT_THRESHOLD'
);
