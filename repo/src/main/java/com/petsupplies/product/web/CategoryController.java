package com.petsupplies.product.web;

import com.petsupplies.core.security.CurrentPrincipal;
import com.petsupplies.product.service.CategoryService;
import com.petsupplies.product.web.dto.CreateCategoryRequest;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@PreAuthorize("hasRole('MERCHANT')")
public class CategoryController {
  private final CurrentPrincipal currentPrincipal;
  private final CategoryService categoryService;

  public CategoryController(CurrentPrincipal currentPrincipal, CategoryService categoryService) {
    this.currentPrincipal = currentPrincipal;
    this.categoryService = categoryService;
  }

  @GetMapping("/categories")
  public List<Map<String, Object>> list(Authentication authentication) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    return categoryService.listRows(merchantId).stream()
        .map(c -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("id", c.id());
          m.put("name", c.name());
          m.put("level", c.level());
          m.put("parentId", c.parentId());
          return m;
        })
        .toList();
  }

  @PostMapping("/categories")
  public Map<String, Object> create(Authentication authentication, @Valid @RequestBody CreateCategoryRequest body) {
    String merchantId = currentPrincipal.requireMerchantId(authentication);
    var c = categoryService.create(merchantId, body.name(), body.parentId());
    return Map.of("id", c.getId(), "name", c.getName(), "level", c.getLevel());
  }

  /**
   * Direct HTTP DELETE is blocked: destructive category removal must use admin dual-approval
   * {@code ACTIVE_CATEGORY_DELETION} (see {@code /api/admin/approvals/request}).
   */
  @DeleteMapping("/categories/{id}")
  public void deleteBlocked(@PathVariable("id") Long id) {
    throw new ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "Category id=" + id + " cannot be deleted here; use admin dual-approval (ACTIVE_CATEGORY_DELETION)"
    );
  }
}
