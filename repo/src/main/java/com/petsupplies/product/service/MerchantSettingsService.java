package com.petsupplies.product.service;

import com.petsupplies.product.domain.MerchantSettings;
import com.petsupplies.product.repo.MerchantSettingsRepository;
import com.petsupplies.settings.service.SystemConfigService;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MerchantSettingsService {
  private final MerchantSettingsRepository merchantSettingsRepository;
  private final SystemConfigService systemConfigService;
  private final Clock clock;

  public MerchantSettingsService(
      MerchantSettingsRepository merchantSettingsRepository,
      SystemConfigService systemConfigService,
      Clock clock
  ) {
    this.merchantSettingsRepository = merchantSettingsRepository;
    this.systemConfigService = systemConfigService;
    this.clock = clock;
  }

  public int getLowStockThreshold(String merchantId) {
    return merchantSettingsRepository
        .findById(merchantId)
        .map(MerchantSettings::getLowStockThreshold)
        .orElseGet(systemConfigService::getGlobalInventoryAlertThreshold);
  }

  @Transactional
  public MerchantSettings upsertLowStockThreshold(String merchantId, int threshold) {
    if (threshold < 0 || threshold > 1_000_000) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lowStockThreshold out of range");
    }
    MerchantSettings row = merchantSettingsRepository.findById(merchantId).orElse(new MerchantSettings());
    row.setMerchantId(merchantId);
    row.setLowStockThreshold(threshold);
    row.setUpdatedAt(Instant.now(clock));
    return merchantSettingsRepository.save(row);
  }
}
