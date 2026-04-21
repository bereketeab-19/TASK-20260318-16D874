package com.petsupplies.product.service;

import com.petsupplies.product.domain.Brand;
import com.petsupplies.product.domain.Category;
import com.petsupplies.product.domain.Product;
import com.petsupplies.product.repo.BrandRepository;
import com.petsupplies.product.repo.CategoryRepository;
import com.petsupplies.product.repo.ProductRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {
  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;
  private final BrandRepository brandRepository;
  private final Clock clock;

  public ProductService(
      ProductRepository productRepository,
      CategoryRepository categoryRepository,
      BrandRepository brandRepository,
      Clock clock
  ) {
    this.productRepository = productRepository;
    this.categoryRepository = categoryRepository;
    this.brandRepository = brandRepository;
    this.clock = clock;
  }

  @Transactional
  public Product create(String merchantId, String productCode, String name) {
    if (productRepository.findByProductCodeAndMerchantId(productCode, merchantId).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "product_code already exists");
    }
    Product p = new Product();
    p.setMerchantId(merchantId);
    p.setProductCode(productCode);
    p.setName(name);
    p.setActive(true);
    p.setCreatedAt(Instant.now(clock));
    return productRepository.save(p);
  }

  @Transactional(readOnly = true)
  public List<Product> listActive(String merchantId) {
    return productRepository.findByMerchantIdAndActiveTrueOrderByCreatedAtDesc(merchantId);
  }

  @Transactional
  public Product update(String merchantId, Long id, String name, Long categoryId, Long brandId) {
    Product p = requireScoped(id, merchantId);
    if (name != null && !name.isBlank()) {
      p.setName(name);
    }
    if (categoryId != null) {
      Category c = categoryRepository.findByIdAndMerchantId(categoryId, merchantId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
      p.setCategory(c);
    }
    if (brandId != null) {
      Brand b = brandRepository.findByIdAndMerchantId(brandId, merchantId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found"));
      p.setBrand(b);
    }
    return productRepository.save(p);
  }

  @Transactional
  public void delist(String merchantId, Long id) {
    Product p = requireScoped(id, merchantId);
    p.setActive(false);
    productRepository.save(p);
  }

  @Transactional(readOnly = true)
  public Product requireScoped(Long id, String merchantId) {
    return productRepository.findByIdAndMerchantId(id, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
  }
}

