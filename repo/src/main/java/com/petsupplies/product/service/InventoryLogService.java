package com.petsupplies.product.service;

import com.petsupplies.product.domain.InventoryLog;
import com.petsupplies.product.domain.Sku;
import com.petsupplies.product.repo.InventoryLogRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryLogService {
  public static final String EVENT_STOCK_INITIAL = "STOCK_INITIAL";
  public static final String EVENT_STOCK_UPDATE = "STOCK_UPDATE";
  public static final String EVENT_SKU_DELIST = "SKU_DELIST";

  private final InventoryLogRepository inventoryLogRepository;
  private final Clock clock;

  public InventoryLogService(InventoryLogRepository inventoryLogRepository, Clock clock) {
    this.inventoryLogRepository = inventoryLogRepository;
    this.clock = clock;
  }

  @Transactional
  public void record(
      String merchantId,
      Sku sku,
      String eventType,
      int quantityBefore,
      int quantityAfter,
      String referenceKind,
      String actorUsername
  ) {
    InventoryLog row = new InventoryLog();
    row.setMerchantId(merchantId);
    row.setSku(sku);
    row.setEventType(eventType);
    row.setQuantityBefore(quantityBefore);
    row.setQuantityAfter(quantityAfter);
    row.setReferenceKind(referenceKind);
    row.setActorUsername(actorUsername);
    row.setCreatedAt(Instant.now(clock));
    inventoryLogRepository.save(row);
  }
}
