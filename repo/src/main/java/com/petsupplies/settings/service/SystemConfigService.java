package com.petsupplies.settings.service;

import com.petsupplies.settings.repo.SystemConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemConfigService {
  /** Global default when per-merchant {@code merchant_settings} row is absent. */
  public static final String KEY_INVENTORY_ALERT_THRESHOLD = "INVENTORY_ALERT_THRESHOLD";

  private static final int FALLBACK_INVENTORY_THRESHOLD = 10;

  private final SystemConfigRepository systemConfigRepository;

  public SystemConfigService(SystemConfigRepository systemConfigRepository) {
    this.systemConfigRepository = systemConfigRepository;
  }

  /**
   * Effective low-stock alert threshold for merchants without an explicit {@code merchant_settings} row.
   * Value is read from {@code system_config} key {@link #KEY_INVENTORY_ALERT_THRESHOLD}, defaulting to 10.
   */
  @Transactional(readOnly = true)
  public int getGlobalInventoryAlertThreshold() {
    return systemConfigRepository
        .findById(KEY_INVENTORY_ALERT_THRESHOLD)
        .map(row -> parsePositiveInt(row.getConfigValue(), FALLBACK_INVENTORY_THRESHOLD))
        .orElse(FALLBACK_INVENTORY_THRESHOLD);
  }

  private static int parsePositiveInt(String raw, int defaultValue) {
    if (raw == null || raw.isBlank()) {
      return defaultValue;
    }
    try {
      int v = Integer.parseInt(raw.trim());
      if (v < 0 || v > 1_000_000) {
        return defaultValue;
      }
      return v;
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }
}
