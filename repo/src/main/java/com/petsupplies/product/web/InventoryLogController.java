package com.petsupplies.product.web;

import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.product.domain.InventoryLog;
import com.petsupplies.product.repo.InventoryLogRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('MERCHANT')")
public class InventoryLogController {
  private final CurrentPrincipal currentPrincipal;
  private final InventoryLogRepository inventoryLogRepository;

  public InventoryLogController(CurrentPrincipal currentPrincipal, InventoryLogRepository inventoryLogRepository) {
    this.currentPrincipal = currentPrincipal;
    this.inventoryLogRepository = inventoryLogRepository;
  }

  @GetMapping("/inventory/logs")
  public Map<String, Object> list(
      Authentication authentication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) Long skuId
  ) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    Pageable p = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
    Page<InventoryLog> result =
        skuId == null
            ? inventoryLogRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId, p)
            : inventoryLogRepository.findByMerchantIdAndSku_IdOrderByCreatedAtDesc(merchantId, skuId, p);
    return Map.of(
        "content",
        result.getContent().stream().map(this::row).toList(),
        "totalElements", result.getTotalElements(),
        "totalPages", result.getTotalPages(),
        "number", result.getNumber()
    );
  }

  private Map<String, Object> row(InventoryLog e) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", e.getId());
    m.put("skuId", e.getSku().getId());
    m.put("eventType", e.getEventType());
    m.put("quantityBefore", e.getQuantityBefore());
    m.put("quantityAfter", e.getQuantityAfter());
    m.put("referenceKind", e.getReferenceKind());
    m.put("actorUsername", e.getActorUsername());
    m.put("createdAt", e.getCreatedAt());
    return m;
  }
}
