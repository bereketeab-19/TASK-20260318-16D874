package com.petsupplies.product.web;

import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.product.service.MerchantSettingsService;
import com.petsupplies.product.web.dto.MerchantSettingsRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantSettingsController {
  private final CurrentPrincipal currentPrincipal;
  private final MerchantSettingsService merchantSettingsService;

  public MerchantSettingsController(CurrentPrincipal currentPrincipal, MerchantSettingsService merchantSettingsService) {
    this.currentPrincipal = currentPrincipal;
    this.merchantSettingsService = merchantSettingsService;
  }

  @GetMapping("/merchant/settings")
  public Map<String, Object> get(Authentication authentication) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    int t = merchantSettingsService.getLowStockThreshold(merchantId);
    return Map.of("merchantId", merchantId, "lowStockThreshold", t);
  }

  @PatchMapping("/merchant/settings")
  public Map<String, Object> patch(Authentication authentication, @Valid @RequestBody MerchantSettingsRequest body) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    var saved = merchantSettingsService.upsertLowStockThreshold(merchantId, body.lowStockThreshold());
    return Map.of("merchantId", saved.getMerchantId(), "lowStockThreshold", saved.getLowStockThreshold());
  }
}
