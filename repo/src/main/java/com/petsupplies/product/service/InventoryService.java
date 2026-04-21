package com.petsupplies.product.service;

import com.petsupplies.auditing.service.AuditService;
import com.petsupplies.notification.domain.Notification;
import com.petsupplies.notification.repo.NotificationRepository;
import com.petsupplies.notification.service.NotificationSubscriptionService;
import com.petsupplies.product.domain.Product;
import com.petsupplies.product.domain.Sku;
import com.petsupplies.product.repo.SkuRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventoryService {
  private final SkuRepository skuRepository;
  private final NotificationRepository notificationRepository;
  private final AuditService auditService;
  private final MerchantSettingsService merchantSettingsService;
  private final InventoryLogService inventoryLogService;
  private final NotificationSubscriptionService notificationSubscriptionService;
  private final Clock clock;

  public InventoryService(
      SkuRepository skuRepository,
      NotificationRepository notificationRepository,
      AuditService auditService,
      MerchantSettingsService merchantSettingsService,
      InventoryLogService inventoryLogService,
      NotificationSubscriptionService notificationSubscriptionService,
      Clock clock
  ) {
    this.skuRepository = skuRepository;
    this.notificationRepository = notificationRepository;
    this.auditService = auditService;
    this.merchantSettingsService = merchantSettingsService;
    this.inventoryLogService = inventoryLogService;
    this.notificationSubscriptionService = notificationSubscriptionService;
    this.clock = clock;
  }

  @Transactional
  public Sku createSku(String merchantId, Product product, String barcode, int stockQty, String actorUsername, String ip) {
    return createSku(merchantId, product, barcode, stockQty, actorUsername, ip, "SKU_CREATE");
  }

  @Transactional
  public Sku createSku(
      String merchantId,
      Product product,
      String barcode,
      int stockQty,
      String actorUsername,
      String ip,
      String referenceKind
  ) {
    Sku sku = new Sku();
    sku.setMerchantId(merchantId);
    sku.setProduct(product);
    sku.setBarcode(barcode);
    sku.setStockQuantity(stockQty);
    sku.setActive(true);
    sku.setCreatedAt(Instant.now(clock));
    Sku saved = skuRepository.save(sku);
    inventoryLogService.record(
        merchantId,
        saved,
        InventoryLogService.EVENT_STOCK_INITIAL,
        0,
        saved.getStockQuantity(),
        referenceKind,
        actorUsername
    );
    onStockEvaluated(saved, actorUsername, ip);
    return saved;
  }

  public record SkuListRow(Long id, String barcode, int stockQuantity, Long productId, boolean active) {}

  @Transactional(readOnly = true)
  public List<SkuListRow> listActiveSummaries(String merchantId) {
    return skuRepository.findByMerchantIdAndActiveTrueOrderByIdDesc(merchantId).stream()
        .map(s -> new SkuListRow(s.getId(), s.getBarcode(), s.getStockQuantity(), s.getProduct().getId(), s.isActive()))
        .toList();
  }

  @Transactional
  public Sku updateSku(String merchantId, Long skuId, Integer stockQuantity, String barcode, String actorUsername, String ip) {
    Sku sku = requireScopedSku(skuId, merchantId);
    if (!sku.isActive()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SKU is delisted");
    }
    int before = sku.getStockQuantity();
    if (barcode != null && !barcode.isBlank()) {
      sku.setBarcode(barcode.trim());
    }
    if (stockQuantity != null) {
      if (stockQuantity < 0) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stockQuantity must be >= 0");
      }
      sku.setStockQuantity(stockQuantity);
    }
    Sku saved = skuRepository.save(sku);
    if (stockQuantity != null && before != saved.getStockQuantity()) {
      inventoryLogService.record(
          merchantId,
          saved,
          InventoryLogService.EVENT_STOCK_UPDATE,
          before,
          saved.getStockQuantity(),
          "MANUAL",
          actorUsername
      );
    }
    onStockEvaluated(saved, actorUsername, ip);
    return saved;
  }

  @Transactional
  public Sku delistSku(String merchantId, Long skuId, String actorUsername, String ip) {
    Sku sku = requireScopedSku(skuId, merchantId);
    int q = sku.getStockQuantity();
    sku.setActive(false);
    Sku saved = skuRepository.save(sku);
    inventoryLogService.record(
        merchantId,
        saved,
        InventoryLogService.EVENT_SKU_DELIST,
        q,
        q,
        "DELIST",
        actorUsername
    );
    auditService.record(
        "SKU_DELISTED",
        Map.of("merchantId", merchantId, "skuId", skuId, "barcode", sku.getBarcode()),
        actorUsername,
        ip
    );
    return saved;
  }

  @Transactional(readOnly = true)
  public Sku requireScopedSku(Long id, String merchantId) {
    return skuRepository.findByIdAndMerchantId(id, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
  }

  @Transactional
  public void updateStock(Long skuId, String merchantId, int qty, String actorUsername, String ip) {
    Sku beforeSku = requireScopedSku(skuId, merchantId);
    int before = beforeSku.getStockQuantity();
    int updated = skuRepository.updateStockScoped(skuId, qty, merchantId);
    if (updated == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
    }
    Sku sku = requireScopedSku(skuId, merchantId);
    if (before != sku.getStockQuantity()) {
      inventoryLogService.record(
          merchantId,
          sku,
          InventoryLogService.EVENT_STOCK_UPDATE,
          before,
          sku.getStockQuantity(),
          "BULK",
          actorUsername
      );
    }
    onStockEvaluated(sku, actorUsername, ip);
  }

  private void onStockEvaluated(Sku sku, String actorUsername, String ip) {
    if (!sku.isActive()) {
      return;
    }
    int threshold = merchantSettingsService.getLowStockThreshold(sku.getMerchantId());
    if (sku.getStockQuantity() <= threshold) {
      if (notificationSubscriptionService.isDeliveryEnabled(sku.getMerchantId(), "LOW_STOCK")) {
        Instant now = Instant.now(clock);
        Notification n = new Notification();
        n.setMerchantId(sku.getMerchantId());
        n.setContent("Low stock for barcode=" + sku.getBarcode() + " qty=" + sku.getStockQuantity() + " (threshold=" + threshold + ")");
        n.setCreatedAt(now);
        n.setDeliveredAt(now);
        n.setEventType("LOW_STOCK");
        notificationRepository.save(n);
      }

      auditService.record(
          "INVENTORY_THRESHOLD_REACHED",
          Map.of(
              "merchantId", sku.getMerchantId(),
              "skuId", sku.getId(),
              "barcode", sku.getBarcode(),
              "stockQuantity", sku.getStockQuantity(),
              "threshold", threshold
          ),
          actorUsername,
          ip
      );
    }
  }
}
