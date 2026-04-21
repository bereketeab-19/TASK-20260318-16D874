package com.petsupplies.catalog;

import com.petsupplies.product.domain.Product;
import com.petsupplies.product.repo.ProductRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/buyer/catalog")
@PreAuthorize("hasRole('BUYER')")
public class BuyerCatalogController {
  private final ProductRepository productRepository;

  public BuyerCatalogController(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  @GetMapping("/products")
  public Map<String, Object> products(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Pageable p = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
    Page<Product> result = productRepository.findByActiveTrueOrderByIdDesc(p);
    return Map.of(
        "content",
        result.getContent().stream().map(this::productRow).toList(),
        "totalElements", result.getTotalElements(),
        "totalPages", result.getTotalPages(),
        "number", result.getNumber()
    );
  }

  @GetMapping("/summary")
  public Map<String, Object> summary() {
    long activeProducts = productRepository.countByActiveTrue();
    long merchantsWithListings = productRepository.countDistinctMerchantIdByActiveTrue();
    return Map.of(
        "activeProductCount", activeProducts,
        "merchantCountWithActiveProducts", merchantsWithListings
    );
  }

  private Map<String, Object> productRow(Product p) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", p.getId());
    m.put("merchantId", p.getMerchantId());
    m.put("productCode", p.getProductCode());
    m.put("name", p.getName());
    m.put("active", p.isActive());
    m.put("createdAt", p.getCreatedAt());
    return m;
  }
}
