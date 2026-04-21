package com.petsupplies.auditing.domain;

public enum CriticalOperationType {
  PERMISSION_CHANGE,
  SYSTEM_CONFIG_UPDATE,
  DATA_WIPE_OR_RESTORE,
  ACTIVE_CATEGORY_DELETION
}

