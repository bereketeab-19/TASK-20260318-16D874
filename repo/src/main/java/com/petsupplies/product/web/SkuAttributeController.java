package com.petsupplies.product.web;

import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.product.service.ProductAttributeService;
import com.petsupplies.product.web.dto.UpsertSkuAttributeRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('MERCHANT')")
public class SkuAttributeController {
  private final CurrentPrincipal currentPrincipal;
  private final ProductAttributeService productAttributeService;

  public SkuAttributeController(CurrentPrincipal currentPrincipal, ProductAttributeService productAttributeService) {
    this.currentPrincipal = currentPrincipal;
    this.productAttributeService = productAttributeService;
  }

  @GetMapping("/skus/{skuId}/attributes")
  public List<Map<String, Object>> list(Authentication authentication, @PathVariable("skuId") Long skuId) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    return productAttributeService.listSkuAttributes(merchantId, skuId).stream()
        .map(v -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("attributeDefinitionId", v.getAttributeDefinition().getId());
          m.put("code", v.getAttributeDefinition().getCode());
          m.put("value", v.getValueText());
          return m;
        })
        .toList();
  }

  @PutMapping("/skus/{skuId}/attributes")
  public Map<String, Object> upsert(
      Authentication authentication,
      HttpServletRequest request,
      @PathVariable("skuId") Long skuId,
      @Valid @RequestBody UpsertSkuAttributeRequest body
  ) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    var v = productAttributeService.upsertSkuAttribute(
        merchantId,
        skuId,
        body.attributeDefinitionId(),
        body.value(),
        user.getUsername()
    );
    return Map.of(
        "id", v.getId(),
        "attributeDefinitionId", v.getAttributeDefinition().getId(),
        "value", v.getValueText()
    );
  }
}
