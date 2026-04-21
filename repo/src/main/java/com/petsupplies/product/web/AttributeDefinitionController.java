package com.petsupplies.product.web;

import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.product.service.ProductAttributeService;
import com.petsupplies.product.web.dto.CreateAttributeDefinitionRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
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
public class AttributeDefinitionController {
  private final CurrentPrincipal currentPrincipal;
  private final ProductAttributeService productAttributeService;

  public AttributeDefinitionController(CurrentPrincipal currentPrincipal, ProductAttributeService productAttributeService) {
    this.currentPrincipal = currentPrincipal;
    this.productAttributeService = productAttributeService;
  }

  @GetMapping("/attribute-definitions")
  public List<Map<String, Object>> list(Authentication authentication) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    return productAttributeService.listDefinitions(merchantId).stream()
        .map(a -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("id", a.getId());
          m.put("code", a.getCode());
          m.put("label", a.getLabel());
          return m;
        })
        .toList();
  }

  @PostMapping("/attribute-definitions")
  public Map<String, Object> create(
      Authentication authentication,
      HttpServletRequest request,
      @Valid @RequestBody CreateAttributeDefinitionRequest body
  ) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    var a = productAttributeService.createDefinition(merchantId, body.code(), body.label(), user.getUsername());
    return Map.of("id", a.getId(), "code", a.getCode(), "label", a.getLabel());
  }

  @DeleteMapping("/attribute-definitions/{id}")
  public Map<String, Object> delete(
      Authentication authentication,
      HttpServletRequest request,
      @PathVariable("id") Long id
  ) {
    var user = currentPrincipal.requireSecurityUser(authentication);
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    productAttributeService.deleteDefinition(merchantId, id, user.getUsername());
    return Map.of("deleted", true);
  }
}
