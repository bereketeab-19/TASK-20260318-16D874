package com.petsupplies.product.web;

import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.product.domain.Sku;
import com.petsupplies.product.service.InventoryService;
import com.petsupplies.product.service.ProductService;
import com.petsupplies.product.web.dto.CreateSkuRequest;
import com.petsupplies.product.web.dto.UpdateSkuRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('MERCHANT')")
public class SkuController {
  private final CurrentPrincipal currentPrincipal;
  private final ProductService productService;
  private final InventoryService inventoryService;

  public SkuController(CurrentPrincipal currentPrincipal, ProductService productService, InventoryService inventoryService) {
    this.currentPrincipal = currentPrincipal;
    this.productService = productService;
    this.inventoryService = inventoryService;
  }

  @GetMapping("/skus")
  public java.util.List<Map<String, Object>> list(Authentication authentication) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    return inventoryService.listActiveSummaries(merchantId).stream()
        .map(sku -> Map.<String, Object>of(
            "id", sku.id(),
            "barcode", sku.barcode(),
            "stockQuantity", sku.stockQuantity(),
            "productId", sku.productId(),
            "active", sku.active()
        ))
        .toList();
  }

  @PostMapping("/skus")
  public Map<String, Object> create(
      Authentication authentication,
      HttpServletRequest request,
      @Valid @RequestBody CreateSkuRequest body
  ) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    var product = productService.requireScoped(body.productId(), merchantId);

    Sku sku = inventoryService.createSku(
        merchantId,
        product,
        body.barcode(),
        body.stockQuantity(),
        user.getUsername(),
        request.getRemoteAddr()
    );

    return Map.of(
        "id", sku.getId(),
        "barcode", sku.getBarcode(),
        "stockQuantity", sku.getStockQuantity()
    );
  }

  @GetMapping("/skus/{id}")
  public Map<String, Object> get(Authentication authentication, @PathVariable("id") Long id) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    Sku sku = inventoryService.requireScopedSku(id, merchantId);
    return Map.of(
        "id", sku.getId(),
        "barcode", sku.getBarcode(),
        "stockQuantity", sku.getStockQuantity(),
        "productId", sku.getProduct().getId(),
        "active", sku.isActive()
    );
  }

  @PatchMapping("/skus/{id}")
  public Map<String, Object> update(
      Authentication authentication,
      HttpServletRequest request,
      @PathVariable("id") Long id,
      @Valid @RequestBody UpdateSkuRequest body
  ) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    Sku sku = inventoryService.updateSku(merchantId, id, body.stockQuantity(), body.barcode(), user.getUsername(), request.getRemoteAddr());
    return Map.of(
        "id", sku.getId(),
        "barcode", sku.getBarcode(),
        "stockQuantity", sku.getStockQuantity(),
        "active", sku.isActive()
    );
  }

  @PostMapping("/skus/{id}/delist")
  public Map<String, Object> delist(
      Authentication authentication,
      HttpServletRequest request,
      @PathVariable("id") Long id
  ) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    Sku sku = inventoryService.delistSku(merchantId, id, user.getUsername(), request.getRemoteAddr());
    return Map.of("id", sku.getId(), "active", sku.isActive());
  }
}

