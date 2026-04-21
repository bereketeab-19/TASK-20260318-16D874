package com.petsupplies.cooking.web;

import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.cooking.service.TechniqueCardService;
import com.petsupplies.cooking.web.dto.CreateTechniqueCardRequest;
import com.petsupplies.cooking.web.dto.UpdateTechniqueCardRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('MERCHANT')")
public class TechniqueCardController {
  private final CurrentPrincipal currentPrincipal;
  private final TechniqueCardService techniqueCardService;

  public TechniqueCardController(CurrentPrincipal currentPrincipal, TechniqueCardService techniqueCardService) {
    this.currentPrincipal = currentPrincipal;
    this.techniqueCardService = techniqueCardService;
  }

  @GetMapping("/technique-cards")
  public Map<String, Object> list(
      Authentication authentication,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(required = false) String tag
  ) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    Pageable p = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
    var result = techniqueCardService.listMaps(merchantId, tag, p);
    return Map.of(
        "content", result.getContent(),
        "totalElements", result.getTotalElements(),
        "totalPages", result.getTotalPages(),
        "number", result.getNumber()
    );
  }

  @PostMapping("/technique-cards")
  public Map<String, Object> create(Authentication authentication, @Valid @RequestBody CreateTechniqueCardRequest body) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    return techniqueCardService.create(merchantId, body);
  }

  @GetMapping("/technique-cards/{id}")
  public Map<String, Object> get(Authentication authentication, @PathVariable("id") Long id) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    return techniqueCardService.toMap(id, merchantId);
  }

  @PatchMapping("/technique-cards/{id}")
  public Map<String, Object> update(
      Authentication authentication,
      @PathVariable("id") Long id,
      @Valid @RequestBody UpdateTechniqueCardRequest body
  ) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    return techniqueCardService.update(merchantId, id, body);
  }

  @DeleteMapping("/technique-cards/{id}")
  public Map<String, Object> delete(Authentication authentication, @PathVariable("id") Long id) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    techniqueCardService.delete(merchantId, id);
    return Map.of("deleted", true);
  }
}
