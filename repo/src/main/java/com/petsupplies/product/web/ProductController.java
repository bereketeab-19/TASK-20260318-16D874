package com.petsupplies.product.web;

import com.petsupplies.auditing.service.AuditService;
import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.product.domain.Product;
import com.petsupplies.product.service.ProductService;
import com.petsupplies.product.web.dto.CreateProductRequest;
import com.petsupplies.product.web.dto.UpdateProductRequest;
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
public class ProductController {
  private final CurrentPrincipal currentPrincipal;
  private final ProductService productService;
  private final AuditService auditService;

  public ProductController(CurrentPrincipal currentPrincipal, ProductService productService, AuditService auditService) {
    this.currentPrincipal = currentPrincipal;
    this.productService = productService;
    this.auditService = auditService;
  }

  @PostMapping("/products")
  public Map<String, Object> create(
      Authentication authentication,
      HttpServletRequest request,
      @Valid @RequestBody CreateProductRequest body
  ) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);

    Product p = productService.create(merchantId, body.productCode(), body.name());

    auditService.record(
        "PRODUCT_CREATED",
        Map.of("productId", p.getId(), "merchantId", merchantId, "productCode", p.getProductCode()),
        user.getUsername(),
        request.getRemoteAddr()
    );

    return Map.of("id", p.getId(), "productCode", p.getProductCode(), "name", p.getName());
  }

  @GetMapping("/products")
  public java.util.List<Map<String, Object>> list(Authentication authentication) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    return productService.listActive(merchantId).stream()
        .map(p -> Map.<String, Object>of(
            "id", p.getId(),
            "productCode", p.getProductCode(),
            "name", p.getName(),
            "active", p.isActive()
        ))
        .toList();
  }

  @GetMapping("/products/{id}")
  public Map<String, Object> get(Authentication authentication, @PathVariable("id") Long id) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    Product p = productService.requireScoped(id, merchantId);
    return Map.of(
        "id", p.getId(),
        "productCode", p.getProductCode(),
        "name", p.getName(),
        "active", p.isActive()
    );
  }

  @PatchMapping("/products/{id}")
  public Map<String, Object> update(
      Authentication authentication,
      @PathVariable("id") Long id,
      @Valid @RequestBody UpdateProductRequest body
  ) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    Product p = productService.update(merchantId, id, body.name(), body.categoryId(), body.brandId());
    return Map.of(
        "id", p.getId(),
        "productCode", p.getProductCode(),
        "name", p.getName(),
        "active", p.isActive()
    );
  }

  @PostMapping("/products/{id}/delist")
  public Map<String, Object> delist(Authentication authentication, @PathVariable("id") Long id) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    productService.delist(merchantId, id);
    Product p = productService.requireScoped(id, merchantId);
    return Map.of("id", p.getId(), "active", p.isActive());
  }
}

