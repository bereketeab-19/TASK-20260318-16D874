package com.petsupplies.product.web;

import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.product.service.BrandService;
import com.petsupplies.product.web.dto.CreateBrandRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('MERCHANT')")
public class BrandController {
  private final CurrentPrincipal currentPrincipal;
  private final BrandService brandService;

  public BrandController(CurrentPrincipal currentPrincipal, BrandService brandService) {
    this.currentPrincipal = currentPrincipal;
    this.brandService = brandService;
  }

  @GetMapping("/brands")
  public List<Map<String, Object>> list(Authentication authentication) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    return brandService.list(merchantId).stream()
        .map(b -> Map.<String, Object>of("id", b.getId(), "name", b.getName()))
        .toList();
  }

  @PostMapping("/brands")
  public Map<String, Object> create(Authentication authentication, @Valid @RequestBody CreateBrandRequest body) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    var b = brandService.create(merchantId, body.name());
    return Map.of("id", b.getId(), "name", b.getName());
  }

  @DeleteMapping("/brands/{id}")
  public Map<String, Object> delete(Authentication authentication, @PathVariable("id") Long id) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    brandService.deleteIfUnused(merchantId, id);
    return Map.of("deleted", true);
  }
}
